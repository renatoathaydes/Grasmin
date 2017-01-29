package com.athaydes.grasmin

import groovy.transform.CompileStatic
import jasmin.Main
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement

import java.lang.reflect.Modifier
import java.util.logging.Logger

@CompileStatic
class Grasmin {

    static final Logger log = Logger.getLogger( Grasmin.name )
    static final File dir = File.createTempDir( 'Grasmin', '' )
    static final List<String> fieldNamePrefixesToSkip = [ '__$', '$staticClassInfo', 'metaClass' ].asImmutable()

    private JasminTyper typer = new JasminTyper()

    Class createJasminClass( String assemblerMethodBody, File targetDir, String className, MethodNode methodNode ) {
        def dir = targetDir ?: dir
        dir.mkdirs()
        def classFile = dir.toPath().resolve( "${className}.class" ).toFile()

        withTempFile( className ) { File tmpJFile ->
            tmpJFile.write jasminFileContents( className, assemblerMethodBody, methodNode )
            Main jasmin = new Main()
            jasmin.run( '-d', dir.absolutePath, tmpJFile.absolutePath )
        }

        try {
            def loader = new GroovyClassLoader()
            loader.addClasspath( classFile.parent )
            return loader.loadClass( className )
        } catch ( e ) {
            throw new RuntimeException( "Unable to create assembler due to compilation errors", e )
        } finally {
            if ( !targetDir ) classFile.deleteOnExit()
        }

    }

    void createJasminClass( ClassNode classNode, File targetDir ) {
        targetDir = targetDir ?: dir
        log.info( "Checking class ${classNode.name}" )

        def methodBodies = classNode.methods.findResults { method ->
            def methodBody = extractJasminMethodBody( method.code )
            if ( !methodBody ) {
                log.warning( "Method ${method.name} does not contain an Expression with JasminCode" )
                return null
            }
            log.info( "-- Checking method ${method.name} from ${method.typeDescriptor}" )
            """|.method ${typer.modifiersString( method.modifiers )} ${typer.typeDescriptorOf( method )}
               |${methodBody}
               |.end method
               |""".stripMargin()
        }

        def modifiers = typer.modifiersString classNode.modifiers
        def fields = fieldsOf( classNode )

        String classDeclaration = """
            |.class ${modifiers} ${classNode.name}
            |.super ${typer.className( classNode.superClass.name )}
            |${fields.collect { it.descriptor }.join( '\n' )}
            |${staticClassInitializer( classNode, fields )}
            |${constructorsFor( classNode, fields )}
            |
            |""".stripMargin() + methodBodies.join( '\n' )

        log.fine classDeclaration

        withTempFile( classNode.name ) { File tmpJFile ->
            writeDebugFileFor( classNode.getAnnotations( new ClassNode( JasminCode ) ).first(), classDeclaration )
            tmpJFile.write classDeclaration
            Main jasmin = new Main()
            jasmin.run( '-d', targetDir.absolutePath, tmpJFile.absolutePath )
        }
    }

    private String staticClassInitializer( ClassNode classNode, Collection<Map> fields ) {
        def staticFields = fields.findAll { it.value != null && Modifier.isStatic( it.modifiers as int ) }
        if ( staticFields ) {
            """
            |.method static public <clinit>()V
            |    .limit stack ${staticFields.size() + 1}
            |${staticFields.collect { initializeStaticField( it, classNode ) }.join( '\n' )}
            |    return
            |.end method
            """
        } else {
            ''
        }
    }

    private String initializeStaticField( Map field, ClassNode classNode ) {
        """\
        |    ldc ${field.value}
        |    putstatic ${typer.className( classNode.name )}/${field.name} ${field.fieldType}""".stripMargin()
    }

    private String constructorsFor( ClassNode classNode, Collection<Map> fields ) {
        def initializedFields = fields.findAll { it.value != null && !Modifier.isStatic( it.modifiers as int ) }
        if ( classNode.declaredConstructors || initializedFields ) {
            """\
            |${createConstructors( classNode.declaredConstructors )}
            |${
                classNode.declaredConstructors.any { it.parameters.size() == 0 } ?
                        '' : defaultConstructorWith( initializedFields, classNode )
            }""".stripMargin()
        } else {
            defaultConstructor
        }
    }

    private String defaultConstructorWith( Collection<Map> initializedFields, ClassNode classNode ) {
        """\
        |.method public <init>()V
        |    .limit stack ${initializedFields.size() + 1}
        |    aload_0
        |    invokenonvirtual java/lang/Object/<init>()V
        |${initializedFields.collect { initializeField( it, classNode ) }.join( '\n' )}
        |    return
        |.end method"""
    }

    private String createConstructors( List<ConstructorNode> constructorNodes ) {
        constructorNodes.collect { constructor ->
            """
            |.method ${typer.modifiersString( constructor.modifiers )} <init>(${
                constructor.parameters.collect { Parameter p ->
                    typer.typeNameFor( p.originType.typeClass.name )
                }.join( '' )
            })V
            |${extractJasminMethodBody( constructor.code )}
            |.end method
            |""".stripMargin()
        }.join( '\n' )
    }

    private String initializeField( Map field, ClassNode classNode ) {
        """\
        |    aload_0
        |    ldc ${field.value}
        |    putfield ${typer.className( classNode.name )}/${field.name} ${field.fieldType}""".stripMargin()
    }

    private Collection<Map> fieldsOf( ClassNode classNode ) {
        classNode.fields.findResults { FieldNode node ->
            if ( fieldNamePrefixesToSkip.any { prefix -> node.name.startsWith( prefix ) } ) {
                return null
            }
            def initialValue = null
            if ( node.hasInitialExpression() ) {
                initialValue = valueOfExpression( node.initialExpression )
                if ( node.type.name == 'java.lang.String' ) {
                    initialValue = '"' + initialValue + '"'
                }
            }
            def typeName = typer.typeNameFor( node.type.name )
            def fieldDescriptor = ".field ${typer.modifiersString( node.modifiers )} ${node.name} ${typeName}"
            [ name : node.name, descriptor: fieldDescriptor, fieldType: typeName,
              value: initialValue, modifiers: node.modifiers ]
        } as Collection<Map>
    }

    private static void writeDebugFileFor( AnnotationNode annotation, String text ) {
        def outputFileMember = annotation?.getMember( 'outputDebugFile' )
        String debugFileName = outputFileMember instanceof ConstantExpression ?
                ( ( ConstantExpression ) outputFileMember ).value :
                ''

        if ( debugFileName ) {
            log.fine( "Writing JasminCode to debug file: $debugFileName" )
            try {
                def jFile = new File( debugFileName )
                jFile.parentFile.mkdirs()
                jFile.write( text )
            } catch ( e ) {
                log.warning( "Could not write to debug file '$debugFileName': $e" )
            }
        }
    }

    private String jasminFileContents( String className, String methodBody, MethodNode methodNode ) {
        """
        |.class public $className
        |.super java/lang/Object
        |
        |${defaultConstructor}
        |
        |.method public static ${typer.typeDescriptorOf( methodNode, 'run' )}
        |    $methodBody
        |.end method""".stripMargin()
    }

    static String extractJasminMethodBody( Statement statement ) {
        def firstStatement = firstStatementOf( statement )
        if ( firstStatement instanceof ExpressionStatement ) {
            def exprStatement = firstStatement as ExpressionStatement
            return valueOfExpression( exprStatement.expression )
        }
        return null
    }

    private static String valueOfExpression( Expression expression ) {
        if ( expression instanceof PropertyExpression ) {
            return Eval.me( expression.text )
        } else {
            return expression.text
        }

    }

    private static Statement firstStatementOf( Statement statement ) {
        if ( statement instanceof BlockStatement ) {
            statement.statements?.first()
        } else {
            statement
        }
    }

    private static void withTempFile( String fileName, Closure useFile ) {
        def tmpJFile = dir.toPath().resolve( fileName + '.j' ).toFile()
        try {
            useFile tmpJFile
        } finally {
            tmpJFile.delete()
        }
    }

    static final defaultConstructor = """
        |.method private <init>()V
        |    aload_0
        |    invokenonvirtual java/lang/Object/<init>()V
        |    return
        |.end method""".stripMargin()

}


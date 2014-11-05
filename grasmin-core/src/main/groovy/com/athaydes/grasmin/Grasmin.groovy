package com.athaydes.grasmin

import jasmin.Main
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement

import java.util.logging.Logger

class Grasmin {

    static final Logger log = Logger.getLogger( Grasmin.name )
    static final dir = File.createTempDir( 'Grasmin', '' )
    static final fieldNamePrefixesToSkip = [ '__$', '$staticClassInfo', 'metaClass' ].asImmutable()

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

        def classDeclaration = """
            |.class ${modifiers} ${classNode.name}
            |.super ${typer.className( classNode.superClass.name )}
            |
            |${fields.collect { it.descriptor }.join( '\n' )}
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

    private String constructorsFor( ClassNode classNode, Collection<Map> fields ) {
        def initializedFields = fields.findAll { it.value != null }
        if ( classNode.declaredConstructors || initializedFields ) {
            """\
            |${createConstructors( classNode.declaredConstructors )}
            |${
                containsDefaultConstructor( classNode.declaredConstructors ) ?
                        '' : defaultConstructorWith( initializedFields, classNode )
            }""".stripMargin()
        } else {
            defaultConstructor
        }
    }

    private boolean containsDefaultConstructor( List<ConstructorNode> constructors ) {
        constructors.any { it.parameters.size() == 0 }
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
                constructor.parameters.collect {
                    typer.typeNameFor( it.originType.typeClass.name )
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
        classNode.fields.findResults {
            if ( fieldNamePrefixesToSkip.any { prefix -> it.name.startsWith( prefix ) } ) {
                return null
            }
            def initialValue = null
            if ( it.hasInitialExpression() ) {
                initialValue = valueOfExpression( it.initialExpression )
                if ( it.type.name == 'java.lang.String' ) {
                    initialValue = '"' + initialValue + '"'
                }
            }
            def typeName = typer.typeNameFor( it.type.name )
            def fieldDescriptor = ".field ${typer.modifiersString( it.modifiers )} ${it.name} ${typeName}"
            [ name: it.name, descriptor: fieldDescriptor, fieldType: typeName, value: initialValue ]
        }
    }

    private void writeDebugFileFor( AnnotationNode annotation, String text ) {
        String debugFileName = ( annotation.getMember( 'outputDebugFile' ) as ConstantExpression ).value
        if ( debugFileName ) {
            log.fine( "Writing JasminCode to debug file: $debugFileName" )
            try {
                new File( debugFileName ).write( text )
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

    String extractJasminMethodBody( Statement statement ) {
        def firstStatement = firstStatementOf( statement )
        if ( firstStatement instanceof ExpressionStatement ) {
            return valueOfExpression( firstStatement.expression )
        }
        return null
    }

    private String valueOfExpression( Expression expression ) {
        if ( expression instanceof PropertyExpression ) {
            return Eval.me( expression.text )
        } else {
            return expression.text
        }

    }

    private Statement firstStatementOf( Statement statement ) {
        if ( statement instanceof BlockStatement ) {
            statement.statements?.first()
        } else {
            statement
        }
    }

    private void withTempFile( String fileName, Closure useFile ) {
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


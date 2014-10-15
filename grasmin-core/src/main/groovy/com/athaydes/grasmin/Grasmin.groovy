package com.athaydes.grasmin

import jasmin.Main
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement

import java.util.logging.Logger

class Grasmin {

    static final Logger log = Logger.getLogger( Grasmin.name )
    static final dir = File.createTempDir( 'Grasmin', '' )

    private JasminTyper typer = new JasminTyper()
    private Main jasmin = new Main()

    Class createJasminClass( String assemblerMethodBody, File targetDir, String className, MethodNode methodNode ) {
        def dir = targetDir ?: dir
        dir.mkdirs()
        def classFile = dir.toPath().resolve( "${className}.class" ).toFile()

        withTempFile { File tmpJFile ->
            tmpJFile << jasminFileContents( className, assemblerMethodBody, methodNode )
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

    private void withTempFile( Closure useFile ) {
        def tmpJFile = dir.toPath().resolve( System.nanoTime().toString() + '.j' ).toFile()
        try {
            useFile tmpJFile
        } finally {
            tmpJFile.delete()
        }
    }

    void createJasminClass( ClassNode classNode, File targetDir ) {
        log.warning( "Checking class ${classNode.name}" )
        def methodBodies = classNode.methods.findResults { method ->
            def methodBody = extractJasminMethodBody( method )
            if ( !methodBody ) return null
            log.warning( "-- Checking method ${method.name} from ${method.typeDescriptor}" )
            """|.method ${typer.modifiersString( method.modifiers )} ${typer.typeDescriptorOf( method )}
               |${methodBody}
               |.end method
               |""".stripMargin()
        }

        def modifiers = typer.modifiersString classNode.modifiers
        def classDeclaration = """
            |.class ${modifiers} ${classNode.name}
            |.super ${typer.className( classNode.superClass.name )}
            |
            |${defaultConstructor}
            |
            |""".stripMargin() + methodBodies.join( '\n' )
        log.info classDeclaration
        withTempFile { File tmpJFile ->
            tmpJFile.write classDeclaration
            //FIXME output file is correct but will be overwritten!
            jasmin.run( '-d', targetDir.absolutePath, tmpJFile.absolutePath )
        }
    }

    def jasminFileContents( String className, String methodBody, MethodNode methodNode ) {
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

    String extractJasminMethodBody( MethodNode methodNode ) {
        def statement = methodNode.code
        def firstStatement
        if ( statement instanceof BlockStatement ) {
            firstStatement = statement.statements?.first()
        } else {
            firstStatement = statement
        }
        if ( firstStatement instanceof ExpressionStatement ) {
            def expr = firstStatement.expression
            String assemblerText
            if ( expr instanceof PropertyExpression ) {
                assemblerText = Eval.me( expr.text )
            } else {
                assemblerText = expr.text
            }
            return assemblerText
        }
        log.warning( "Method ${methodNode.name} does not contain an Expression with JasminCode" )
        return null
    }

    static final defaultConstructor = """
        |.method private <init>()V
        |    aload_0
        |    invokenonvirtual java/lang/Object/<init>()V
        |    return
        |.end method""".stripMargin()

}


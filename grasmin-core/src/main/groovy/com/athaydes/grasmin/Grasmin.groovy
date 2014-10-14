package com.athaydes.grasmin

import jasmin.Main
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.stmt.ExpressionStatement

class Grasmin {

    static final dir = File.createTempDir( 'Grassembler', '' )
    JasminTyper typer = new JasminTyper()

    Class createJasminClass( String assemblerMethodBody, File targetDir, String className, MethodNode methodNode ) {
        def dir = targetDir ?: dir
        dir.mkdirs()
        def tmpFile = dir.toPath().resolve( System.nanoTime().toString() + '.j' )
        def classFile = dir.toPath().resolve( "${className}.class" ).toFile()
        tmpFile << jasminFileContents( className, assemblerMethodBody, methodNode )

        def jasmin = new Main()

        jasmin.run( '-d', dir.absolutePath, tmpFile.toFile().absolutePath )

        try {
            def loader = new GroovyClassLoader()
            loader.addClasspath( classFile.parent )
            return loader.loadClass( className )
        } catch ( e ) {
            throw new RuntimeException( "Unable to create assembler due to compilation errors", e )

        } finally {
            if ( !targetDir ) classFile.deleteOnExit()
            tmpFile.toFile().delete()
        }

    }

    def jasminFileContents( String className, String methodBody, MethodNode methodNode ) {
        """
        .class public $className
        .super java/lang/Object

        ;
        ; standard initializer (calls java.lang.Object's initializer)
        ;
        .method private <init>()V
            aload_0
            invokenonvirtual java/lang/Object/<init>()V
            return
        .end method

        .method public static ${typer.typeDescriptorOf( methodNode, 'run' )}
            $methodBody
        .end method"""
    }

    String extractJasminMethodBody( MethodNode methodNode ) {
        List existingStatements = methodNode.code.statements
        if ( existingStatements.size() > 0 ) {
            def statement = existingStatements.first()
            if ( statement instanceof ExpressionStatement ) {
                def expr = statement.expression
                String assemblerText
                if ( expr instanceof PropertyExpression ) {
                    assemblerText = Eval.me( expr.text )
                } else {
                    assemblerText = expr.text
                }

                return assemblerText
            }
        }
        throw new RuntimeException( "Method ${methodNode.name} does not contain an Expression with JasminCode" )
    }

}


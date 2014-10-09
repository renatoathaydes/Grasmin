package com.athaydes.grasmin

import jasmin.Main
import org.codehaus.groovy.ast.MethodNode

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

}


package com.athaydes.grasmin

import groovy.transform.CompileStatic
import org.junit.Test

class Hello {

    @JasminCode
    static void main( args ) {
        """
        .limit stack 2
        getstatic java/lang/System/out Ljava/io/PrintStream;
        ldc "Hello World!"
        invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
        return
        """
    }

    @Test
    void testSum() {
        assert sum( 2, 3 ) == 5
        assert sum( 6, 5 ) == 11
    }

    @CompileStatic
    @JasminCode
    int sum( int a, int b ) {
        """
        .limit stack 2
        .limit locals 2
        iload_0
        iload_1
        iadd
        ireturn
        """
        5000 // ignored, but makes the IDE happy
    }

}

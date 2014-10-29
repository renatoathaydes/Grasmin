package grasmin.test_target

import com.athaydes.grasmin.JasminCode
import groovy.transform.CompileStatic

@CompileStatic
@JasminCode(outputFile = 'the-jasmin-code-class.j')
class JasminCodeClass extends Object {

    private  String name = 'Joda'

    int get10() {
        """
        .limit stack 1
        ldc 10
        ireturn"""
        -1
    }

    String hello( String name ) {
        """
        .limit locals 2
        .limit stack 2
        ldc "Hello "
        aload_1
        invokevirtual java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
        areturn
        """
        //"Hello ".concat( "blha" )
    }

    public String getName() {
        """
        aload_0
        getfield grasmin/test_target/JasminCodeClass/name Ljava/lang/String;
        areturn
        """
    }


}

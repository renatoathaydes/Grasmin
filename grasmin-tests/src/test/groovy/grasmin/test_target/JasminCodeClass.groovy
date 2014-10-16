package grasmin.test_target

import com.athaydes.grasmin.JasminCode
import groovy.transform.CompileStatic

@CompileStatic
@JasminCode
class JasminCodeClass {

    int get10() {
        ".limit stack 1 \n ldc 10 \n ireturn"
        -1
    }

    String hello( String name ) {
        """
        .limit locals 2
        .limit stack 5
        .line 19
        ldc " "
        iconst_2                                              ; create array of 2 elements to pass to join
        anewarray  java/lang/CharSequence
        dup
        iconst_0
        ldc "Hello"
        aastore
        dup
        iconst_1
        aload_1
        .line 30
        aastore
        .line 32
        invokestatic java/lang/String/join(Ljava/lang/CharSequence;[Ljava/lang/CharSequence;)Ljava/lang/String;
        areturn
        """
        //String.join( " ", "Hello", name )

    }


}

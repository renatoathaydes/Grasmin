package grasmin.test_target

import com.athaydes.grasmin.JasminCode
import groovy.transform.CompileStatic

@CompileStatic
@JasminCode( outputDebugFile = 'build/arrays-code.j' )
class ArraysCode {

    static void main( String[] args ) {
        """\
        .limit stack 2
        getstatic java/lang/System/out Ljava/io/PrintStream;
        ldc "Hello World!"
        invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
        return
        """
    }

    int len( boolean[] b ) {
        """\
        .limit locals 2
        .limit stack 1
        aload_1
        arraylength
        ireturn
        """
        0
    }

    String[] stringify( int[] ints ) {
        """\
        .limit locals 4
        .limit stack 3
        aload_1
        arraylength                       ; put the length of the array on the stack
        istore_2                          ; store the array length on local variable 2
        iload_2                           ; read the array length
        anewarray java/lang/String        ; create a String array with the same length as the input array
        astore_3                          ; store the result in the local variable 3
        Loop:
        iload_2
        ifeq End                          ; check if the remaining length is 0, go to End if so
        iinc 2 -1                         ; decrement index
        aload_1                           ; load the input array
        iload_2                           ; load current index
        iaload                            ; read current value
        invokestatic java/lang/Integer.toString(I)Ljava/lang/String;
        aload_3                           ; load the result array
        swap                              ; swap so the String value is on top of the stack
        iload_2                           ; load current index
        swap                              ; swap so the stack has arrayRef, index, value
        aastore                           ; put the result String into the result array
        goto Loop                         ; loop
        End:
        aload_3
        areturn
        """
    }

}

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
        .limit locals 3
        .limit stack 5
        aload_1
        arraylength                       ; put the length of the array on the stack
        dup
        dup                               ; Stack: length, length, length |TOP
        anewarray java/lang/String        ; create a String array with the same length as the input array
        astore_2                          ; store the result in the local variable 2
                                          ; Stack: length, length |TOP
        Loop:
        ifeq End                          ; check if the remaining length is 0, go to End if so
        iconst_1                          ; Stack: length, 1 |TOP
        isub                              ; decrement index
        dup                               ; Stack: index, index |TOP
        aload_2                           ; load the result array
        swap                              ; Stack: index, resultArray, index |TOP
        dup                               ; Stack: index, resultArray, index, index |TOP
        aload_1                           ; load the input array
        swap                              ; Stack: index, resultArray, index, inputArray, index |TOP
        iaload                            ; read current value
                                          ; Stack: index, resultArray, index, intValue |TOP
        invokestatic java/lang/Integer.toString(I)Ljava/lang/String;
                                          ; Stack: index, resultArray, index, stringValue |TOP
        aastore                           ; put the result String into the result array
        dup                               ; Stack: index, index |TOP
        goto Loop                         ; loop
        End:
        aload_2
        areturn
        """
    }

}

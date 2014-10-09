package grasmin

import com.athaydes.grasmin.JasminCode
import groovy.transform.CompileStatic
import groovy.transform.TailRecursive

/**
 *
 */
class GrasminTest extends GroovyTestCase {

    void testGcd() {
        assert groovyGcd( 42, 56 ) == 14
        assert groovyStaticGcd( 42, 56 ) == 14
        assert tailRecursiveGcd( 42, 56 ) == 14
        assert grasmineGcd( 42, 56 ) == 14
    }

    void testReturnInt() {
        assert return500() == 500
    }

    int groovyGcd( int u, int v ) {
        if ( u < v ) {
            def tmp = u
            u = v
            v = tmp
        }
        if ( v == 0 ) {
            return u
        }
        return groovyGcd( v, u - v )
    }

    @CompileStatic
    @TailRecursive
    int tailRecursiveGcd( int u, int v ) {
        if ( u < v ) {
            def tmp = u
            u = v
            v = tmp
        }
        if ( v == 0 ) {
            return u
        }
        return tailRecursiveGcd( v, u - v )
    }

    @CompileStatic
    int groovyStaticGcd( int u, int v ) {
        int small = u
        int large = v
        while ( small != 0 ) {
            if ( large < small ) {
                def tmp = small
                small = large
                large = tmp
            }
            if ( small != 0 ) large -= small

        }
        return large
    }

    @JasminCode
    @CompileStatic
    int grasmineGcd( int a, int b ) {
        """
        .limit stack 3
        .limit locals 3

        Begin:
        iload_0
        iload_1                ; result: var0 -> var1

        if_icmplt     Swap     ; if var0 < var1 Swap
        iload_0
        iload_1                ; result: var0 (large) -> var1 (small)
        dup
        istore_2               ; var2 = small
        goto          A

        Swap:
        iload_1
        iload_0                ; result: var1 (large) -> var0 (small)
        dup
        istore_2               ; var2 = small

        A:
        ifeq          Return   ; if smaller == 0 Return
        iload_2                ; result: large -> small
        swap                   ; result: small -> large
        iload_2                ; result: small -> large -> small
        isub                   ; result: small -> large-small
        istore_0
        istore_1
        goto          Begin

        Return:
        ireturn
        """
        0
    }

    @JasminCode
    @CompileStatic
    int return500() {
        """
        .limit stack 1
        ldc 500
        ireturn
        """
        0
    }

}

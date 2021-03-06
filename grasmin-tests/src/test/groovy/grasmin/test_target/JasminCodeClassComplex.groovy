package grasmin.test_target

import com.athaydes.grasmin.JasminCode

@JasminCode( outputDebugFile = 'build/the-jasmin-code-complex-class.j' )
class JasminCodeClassComplex extends Object {

    private String name = 'Joda'
    private int i

    public static final String staticString = 'a-string'
    public static final int staticInt = 123

    JasminCodeClassComplex() {
        """\
        .limit stack 2
        aload_0
        invokenonvirtual java/lang/Object/<init>()V
        aload_0
        ldc "John"
        putfield grasmin/test_target/JasminCodeClassComplex/name Ljava/lang/String;
        aload_0
        bipush 55
        putfield grasmin/test_target/JasminCodeClassComplex/i I
        return"""
    }

    String concat( String a, String b ) {
        """\
        .limit locals 3
        .limit stack 2
        aload_1
        aload_2
        invokevirtual java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
        areturn"""
        //"Hello ".concat( "blha" )
    }

    public String getName() {
        """\
        aload_0
        getfield grasmin/test_target/JasminCodeClassComplex/name Ljava/lang/String;
        areturn"""
    }

    public int getI() {
        """\
        aload_0
        getfield grasmin/test_target/JasminCodeClassComplex/i I
        ireturn"""
        111 // ignored
    }

}

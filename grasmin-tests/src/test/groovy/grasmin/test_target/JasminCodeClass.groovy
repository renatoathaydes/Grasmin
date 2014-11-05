package grasmin.test_target

import com.athaydes.grasmin.JasminCode

@JasminCode( outputDebugFile = 'the-jasmin-code-class.j' )
class JasminCodeClass extends Object {

    private String name = 'Joda'
    private int i

    JasminCodeClass( String name, int i ) {
        """\
        .limit locals 3
        .limit stack 2
        aload_0
        invokenonvirtual java/lang/Object/<init>()V
        aload_0
        aload_1
        putfield grasmin/test_target/JasminCodeClass/name Ljava/lang/String;
        aload_0
        iload_2
        putfield grasmin/test_target/JasminCodeClass/i I
        return"""
    }

    int get10() {
        """\
        .limit stack 1
        ldc 10
        ireturn"""
        -1
    }

    String hello( String name ) {
        """\
        .limit locals 2
        .limit stack 2
        ldc "Hello "
        aload_1
        invokevirtual java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
        areturn"""
        //"Hello ".concat( "blha" )
    }

    public String getName() {
        """\
        aload_0
        getfield grasmin/test_target/JasminCodeClass/name Ljava/lang/String;
        areturn"""
    }

    public int getI() {
        """\
        aload_0
        getfield grasmin/test_target/JasminCodeClass/i I
        ireturn"""
        111 // ignored
    }

}

import com.athaydes.grasmin.JasminCode

@JasminCode
final class AnnotatedClass {

    int returns10() {
        10
    }

    @JasminCode
    void annotatedMethodJasminCode() {
        """
        .limit stack 2
        getstatic java/lang/System/out Ljava/io/PrintStream;
        ldc "Hello World!"
        invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
        return
        """
    }

    void noAnnotationJasminCode() {
        """
        .limit stack 2
        getstatic java/lang/System/out Ljava/io/PrintStream;
        ldc "Hello World!"
        invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
        return
        """
    }

}

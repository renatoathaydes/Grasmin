import com.athaydes.grasmin.JasminCode
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassNode

/**
 * See JVM instructions on http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-6.html
 */
class Test1 {

    @JasminCode
    @CompileStatic
    void exampleJasminCode() {
        """
        .limit stack 2
        getstatic java/lang/System/out Ljava/io/PrintStream;
        ldc "Hello World!"
        invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
        return
        """
        0
    }

}

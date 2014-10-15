package grasmin

import com.athaydes.grasmin.Grasmin
import grasmin.test_target.JasminCodeClass
import org.junit.Test

/**
 *
 */
class ClassLevelJasminCodeTest {

    @Test
    void shouldCompileAllMethods() {
        new JasminCodeClass(  ).'10' == 10
    }

}

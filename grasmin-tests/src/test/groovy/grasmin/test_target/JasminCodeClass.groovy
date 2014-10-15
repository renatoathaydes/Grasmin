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

}

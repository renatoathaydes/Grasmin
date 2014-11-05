package grasmin

import grasmin.test_target.JasminCodeClass
import org.junit.Test

/**
 *
 */
class ClassLevelJasminCodeTest {

    def jasminCode = new JasminCodeClass()

    @Test
    void shouldCompileAllMethods() {
        assert jasminCode.'10' == 10
        assert jasminCode.hello( 'abc' ) == 'Hello abc'
    }

    @Test
    void shouldBeAbleToSeeFields() {
        assert jasminCode.getName() == 'Joda'
    }

    @Test
    void constructorWorks() {
        def instance = new JasminCodeClass('Spock', 42)
        assert instance.getName() == 'Spock'
        assert instance.getI() == 42
    }

}

package grasmin

import grasmin.test_target.JasminCodeClass
import grasmin.test_target.JasminCodeClassComplex
import org.junit.Test

/**
 *
 */
class ClassLevelJasminCodeTest {

    @Test
    void shouldCompileAllMethods() {
        def jasminCode = new JasminCodeClass()
        assert jasminCode.'10' == 10
        assert jasminCode.hello( 'abc' ) == 'Hello abc'
    }

    @Test
    void shouldBeAbleToSeeFields() {
        def jasminCode = new JasminCodeClass()
        assert jasminCode.getName() == 'Joda'
    }

    @Test
    void constructorWorks() {
        def instance = new JasminCodeClass( 'Spock', 42 )
        assert instance.getName() == 'Spock'
        assert instance.getI() == 42
    }

    @Test
    void canDeclareDefaultConstructor() {
        def instance = new JasminCodeClassComplex()
        assert instance.getName() == 'John'
        assert instance.getI() == 55
    }

}

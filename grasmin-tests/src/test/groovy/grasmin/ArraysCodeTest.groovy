package grasmin

import grasmin.test_target.ArraysCode
import spock.lang.Specification
import spock.lang.Unroll

class ArraysCodeTest extends Specification {

    def "Methods that take String arrays can be compiled correctly"() {
        when: 'we call the main method of a Grasmin class'
        new ArraysCode().main( [ 'hello', 'world' ] as String[] )

        then: 'no error occurs'
        noExceptionThrown()
    }

    @Unroll
    "Can use array argument"() {
        when: 'we ask for the length of an array'
        def result = new ArraysCode().len( array as boolean[] )

        then: 'the correct answer is given'
        result == length

        where:
        array                        | length
        [ ]                          | 0
        [ true ]                     | 1
        [ true, true ]               | 2
        [ true, false, true, false ] | 4
    }

    @Unroll
    "Can read and return arrays"() {
        when: 'we stringify a int[]'
        def result = new ArraysCode().stringify( ints as int[] )

        then: 'the expected array is given'
        Arrays.equals( result, ( expectedArray as String[] ) )

        where:
        ints              | expectedArray
        [ ]               | [ ]
        [ 1 ]             | [ '1' ]
        [ 400, 257, 100 ] | [ '400', '257', '100' ]
    }

}

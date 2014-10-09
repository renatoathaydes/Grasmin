package com.athaydes.grasmin

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.stmt.Statement
import spock.lang.Specification

/**
 *
 */
class JasminTyperSpec extends Specification {

    def "A Class object can be translated into a JVM type descriptor as per the class file format"() {
        given: 'A JasminTyper'
        def jasminTyper = new JasminTyper()

        when: 'A class is translated to a JVM type descriptor'
        def result = jasminTyper.typeNameFor( type.name )

        then: 'The result is as described in the class file format specification'
        result == expected

        where:
        type    | expected
        boolean | 'Z'
        float   | 'F'
        int     | 'I'
        Boolean | 'Ljava/lang/Boolean;'
        String  | 'Ljava/lang/String;'
        Thread  | 'Ljava/lang/Thread;'
    }

    def "Type Descriptors can be correctly discovered from a MethodNode"() {
        given: 'A JasminTyper'
        def jasminTyper = new JasminTyper()

        and: 'A MethodNode for some example methods'
        def node = methodNode( methodName, paramTypes, returnType )

        when: 'The type descriptor for a MethodNode is requested'
        def result = newMethodName ?
                jasminTyper.typeDescriptorOf( node, newMethodName ) :
                jasminTyper.typeDescriptorOf( node )

        then: 'The result is as described in the class file format specification'
        result == expected

        where:
        methodName | paramTypes              | returnType | newMethodName || expected
        'a'        | [ ]                     | void       | null          || 'a()V'
        'xyz'      | [ boolean ]             | int        | null          || 'xyz(Z)I'
        'm'        | [ int, double, Thread ] | Object     | 'run'         || 'run(IDLjava/lang/Thread;)Ljava/lang/Object;'

    }

    final paramNames = 'a'..'z' as LinkedList

    def methodNode( methodName, paramTypes, returnType ) {
        new MethodNode( methodName, 0, new ClassNode( returnType ),
                paramTypes.collect { new Parameter( new ClassNode( it ), paramNames.remove() ) } as Parameter[],
                [ ] as ClassNode[], new Statement() )
    }

}

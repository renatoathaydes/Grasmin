package com.athaydes.grasmin

import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.stmt.Statement
import spock.lang.Specification

import java.lang.reflect.Modifier

/**
 *
 */
@TypeChecked
class JasminTyperSpec extends Specification {

    def "A Class object can be translated into a JVM type descriptor as per the class file format"(
            Class type, String expected ) {
        when: 'A class is translated to a JVM type descriptor'
        def result = JasminTyper.typeNameFor( type.name )

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

    def "Type Descriptors can be correctly discovered from a MethodNode"(
            String methodName, Collection<Class> paramTypes, Class returnType, String newMethodName, String expected ) {
        given: 'A MethodNode for some example methods'
        def node = methodNode( methodName, paramTypes, returnType )

        when: 'The type descriptor for a MethodNode is requested'
        def result = newMethodName ?
                JasminTyper.typeDescriptorOf( node, newMethodName ) :
                JasminTyper.typeDescriptorOf( node )

        then: 'The result is as described in the class file format specification'
        result == expected

        where:
        methodName | paramTypes              | returnType | newMethodName || expected
        'a'        | [ ]                     | void       | null          || 'a()V'
        'xyz'      | [ boolean ]             | int        | null          || 'xyz(Z)I'
        'm'        | [ int, double, Thread ] | Object     | 'run'         || 'run(IDLjava/lang/Thread;)Ljava/lang/Object;'

    }

    final LinkedList<String> paramNames = 'a'..'z' as LinkedList

    MethodNode methodNode( String methodName, Collection<Class> paramTypes, Class returnType ) {
        new MethodNode( methodName, 0, new ClassNode( returnType ),
                paramTypes.collect { new Parameter( new ClassNode( it ), paramNames.remove() ) } as Parameter[],
                [ ] as ClassNode[], new Statement() )
    }

    // IntelliJ  has trouble with int methods
    @TypeChecked( TypeCheckingMode.SKIP )
    "Can collect all modifiers of a class"( int modifiers, String expected ) {
        when: "All modifiers of a class are requested"
        def result = JasminTyper.modifiersString( modifiers )

        then: "The expected value is returned"
        result == expected

        where:
        modifiers                                                       | expected
        0                                                               | ''
        Modifier.PUBLIC                                                 | 'public'
        Modifier.PUBLIC.or( Modifier.ABSTRACT )                         | 'public abstract'
        Modifier.PROTECTED.or( Modifier.FINAL ).or( Modifier.VOLATILE ) | 'protected final volatile'
    }

}

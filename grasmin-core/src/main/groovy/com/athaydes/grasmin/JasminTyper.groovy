package com.athaydes.grasmin

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter

import java.lang.reflect.Modifier

/**
 * Translates classes into Jasmin types (JVM types).
 */
@CompileStatic
class JasminTyper {

    static String typeDescriptorOf( MethodNode methodNode, String newMethodName = null ) {
        def paramTypes = methodNode.parameters.collect { Parameter p -> typeNameFor( p.type.name ) }.join( '' )
        def returnType = typeNameFor( methodNode.returnType.name )
        "${newMethodName ?: methodNode.name}(${paramTypes})${returnType}"
    }

    static String className( String javaClassName ) {
        javaClassName.replace( '.', '/' )
    }

    static String typeNameFor( String type ) {
        switch ( type ) {
        // primitive types
            case 'int': return 'I'
            case 'float': return 'F'
            case 'byte': return 'B'
            case 'char': return 'C'
            case 'double': return 'D'
            case 'long': return 'J'
            case 'short': return 'S'
            case 'boolean': return 'Z'
        // arrays of primitive types
            case 'int[]': return '[I'
            case 'float[]': return '[F'
            case 'byte[]': return '[B'
            case 'char[]': return '[C'
            case 'double[]': return '[D'
            case 'long[]': return '[J'
            case 'short[]': return '[S'
            case 'boolean[]': return '[Z'
        // void
            case 'void':
            case 'Void': return 'V'
        // references
            default: return nonPrimitiveTypeDescription( type )
        }
    }

    static String nonPrimitiveTypeDescription( String type ) {
        type.startsWith( '[' ) ?
                // arrays already come with the correct type name
                type :
                'L' + className( type ) + ';'
    }

    static String modifiersString( int modifiers ) {
        def result = Modifier.isPublic( modifiers ) ? 'public ' : ''
        result += Modifier.isPrivate( modifiers ) ? 'private ' : ''
        result += Modifier.isProtected( modifiers ) ? 'protected ' : ''
        result += Modifier.isAbstract( modifiers ) ? 'abstract ' : ''
        result += Modifier.isFinal( modifiers ) ? 'final ' : ''
        result += Modifier.isStatic( modifiers ) ? 'static ' : ''
        result += Modifier.isSynchronized( modifiers ) ? 'synchronized ' : ''
        result += Modifier.isVolatile( modifiers ) ? 'volatile ' : ''
        result += Modifier.isTransient( modifiers ) ? 'transient ' : ''
        result += Modifier.isStrict( modifiers ) ? 'strictfp' : ''
        result.trim()
    }

}

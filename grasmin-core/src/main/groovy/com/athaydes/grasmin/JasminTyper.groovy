package com.athaydes.grasmin

import org.codehaus.groovy.ast.MethodNode

import java.lang.reflect.Modifier

/**
 * Translates classes into Jasmin types (JVM types).
 */
class JasminTyper {

    String typeDescriptorOf( MethodNode methodNode, String newMethodName = null ) {
        def paramTypes = methodNode.parameters.collect { typeNameFor( it.type.name ) }.join( '' )
        def returnType = typeNameFor( methodNode.returnType.name )
        "${newMethodName ?: methodNode.name}(${paramTypes})${returnType}"
    }

    String className( String javaClassName ) {
        javaClassName.replace( '.', '/' )
    }

    String typeNameFor( String type ) {
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
            default: return jvmType( type )
        }
    }

    String jvmType( String type ) {
        'L' + className( type ) + ';'
    }

    String modifiersString( int modifiers ) {
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

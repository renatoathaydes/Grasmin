package com.athaydes.grasmin

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode

/**
 * Translates classes into Jasmin types (JVM types).
 */
class JasminTyper {

    String typeDescriptorOf( MethodNode methodNode, String newMethodName = null ) {
        def paramTypes = methodNode.parameters.collect { typeNameFor( it.type.name ) }.join( '' )
        def returnType = typeNameFor( methodNode.returnType.name )
        "${newMethodName ?: methodNode.name}(${paramTypes})${returnType}"
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
        'L' + type.replace( '.', '/' ) + ';'
    }

}

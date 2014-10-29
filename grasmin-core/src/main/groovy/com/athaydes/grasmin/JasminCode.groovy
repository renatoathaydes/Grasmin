package com.athaydes.grasmin

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Groovy AST Transform for specifying that the annotated element should be compiled as Jasmin code.
 *
 * A String (or simple expression that evaluates to a String) containing Jasmin code
 * (basically, JVM instructions) should be the first element of any annotated method.
 * <p/>
 * If a class is annotated, all of its methods will be considered as being annotated, and the whole
 * class will be turned into a single Jasmin 'j' file before being compiled to bytecode.
 * <p/>
 * If a method is annotated, a class will be generated to host a single method with the Jasmin code
 * provided. The annotated method will then delegate to the generated one, which incurs a runtime
 * cost. For this reason, prefer to annotate classes, as they do not incur this overhead.
 */
@Retention( RetentionPolicy.SOURCE )
@Target( [ ElementType.METHOD, ElementType.TYPE ] )
@GroovyASTTransformationClass( [ "com.athaydes.grasmin.GrasminASTTransformation" ] )
public @interface JasminCode {

    /**
     * path to a file where the Jasmin file contents should be output for debugging.
     *
     * @return path to debugging file
     */
    String outputDebugFile() default ""

}

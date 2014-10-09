package com.athaydes.grasmin

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 *
 */
@Retention( RetentionPolicy.SOURCE )
@Target( [ ElementType.METHOD ] )
@GroovyASTTransformationClass( [ "com.athaydes.grasmin.GrasminASTTransformation" ] )
public @interface JasminCode {
}

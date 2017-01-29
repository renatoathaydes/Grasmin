package com.athaydes.grasmin

import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.tools.ast.TransformTestHelper

class GrasminASTTransformationTest extends GroovyTestCase {

    void testTransformation() {
        def helper = new TransformTestHelper(
                new GrasminASTTransformation(),
                CompilePhase.SEMANTIC_ANALYSIS )

        def test1 = helper.parse( this.class.getResource( '/Test1.groovy' ).path as File )
        def inst = test1.newInstance()
        assert inst.exampleJasminCode() == null
    }

}

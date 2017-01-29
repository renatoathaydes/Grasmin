package com.athaydes.grasmin

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import java.util.logging.Logger

@GroovyASTTransformation( phase = CompilePhase.SEMANTIC_ANALYSIS )
@CompileStatic
class GrasminASTTransformation implements ASTTransformation {

    Logger log = Logger.getLogger( GrasminASTTransformation.name )
    static final Set processedSourceUnits = [ ]
    static final Grasmin grasmin = new Grasmin()

    void visit( ASTNode[] astNodes, SourceUnit sourceUnit ) {
        if ( !processedSourceUnits.add( sourceUnit ) ) return

        log.fine "Transforming sourceUnit ${sourceUnit.name}"

        if ( !sourceUnit || !sourceUnit.AST ) return

        def processNodes = { List<? extends AnnotatedNode> nodes ->
            def annotatedNodes = nodes.findAll { AnnotatedNode node ->
                node.getAnnotations( new ClassNode( JasminCode ) )
            }
            for ( AnnotatedNode node in annotatedNodes ) {
                switch ( node ) {
                    case ClassNode:
                        process( node as ClassNode, sourceUnit )
                        break
                    case MethodNode:
                        process( node as MethodNode, sourceUnit )
                        break
                    default:
                        throw new Exception( "Cannot annotate nodes of this type with @JasminCode: ${node?.class?.name}" )
                }
            }
        }

        List<ClassNode> classNodes = sourceUnit.getAST().getClasses()
        List<MethodNode> methods = classNodes*.getMethods().flatten() as List<MethodNode>

        processNodes classNodes
        processNodes methods
    }

    private void process( ClassNode classNode, SourceUnit sourceUnit ) {
        log.fine "Processing class $classNode.name"
        def targetDir = sourceUnit.configuration.targetDirectory

        grasmin.createJasminClass( classNode, targetDir )

        sourceUnit.AST.classes.remove( classNode ) // replaced Groovy class with Jasmin class
    }

    private void process( MethodNode method, SourceUnit sourceUnit ) {
        if ( method.declaringClass.getAnnotations( new ClassNode( JasminCode ) ) ) {
            return
        }

        log.fine "Processing method $method.name"

        try {
            rewriteMethod( sourceUnit, method, grasmin.extractJasminMethodBody( method.code ) )
        } catch ( Throwable e ) {
            log.warning e.toString() + ' ' + e.getCause()?.toString()
        }
    }

    private static void rewriteMethod( SourceUnit sourceUnit, MethodNode method, String assemblerText ) {
        if ( !assemblerText ) {
            throw new Exception( "No Jasmin code found in method ${method.name}" )
        }
        def targetDir = sourceUnit.configuration.targetDirectory
        def className = classNameFor( method )
        def jasminClass = grasmin.createJasminClass( assemblerText, targetDir, className, method )
        def classNode = new ClassNode( jasminClass )

        if ( !targetDir ) sourceUnit.getAST().addClass( classNode )

        def jasminMethodCall = grassemblyStatement( classNode, method )
        method.code = jasminMethodCall
    }

    static String classNameFor( MethodNode methodNode ) {
        methodNode.declaringClass.name.replace( '.', '_' ) + '_' + methodNode.name
    }

    private static Statement grassemblyStatement( ClassNode jasminClass, MethodNode methodNode ) {
        new ExpressionStatement(
                new MethodCallExpression(
                        new ClassExpression( jasminClass ),
                        "run",
                        new ArgumentListExpression( methodNode.parameters )
                )
        )
    }

}

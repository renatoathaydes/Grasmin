package com.athaydes.grasmin

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import java.util.logging.Logger

@GroovyASTTransformation( phase = CompilePhase.SEMANTIC_ANALYSIS )
class GrasminASTTransformation implements ASTTransformation {

    Logger log = Logger.getLogger( GrasminASTTransformation.name )
    static final Set processedSourceUnits = [ ]
    static final Grasmin grasmin = new Grasmin()

    public void visit( ASTNode[] nodes, SourceUnit sourceUnit ) {
        if ( !processedSourceUnits.add( sourceUnit ) ) return

        log.fine "Transforming sourceUnit ${sourceUnit.name}"

        if ( !sourceUnit || !sourceUnit.AST ) return
        List methods = sourceUnit.getAST().getClasses()*.getMethods().flatten()

        methods.findAll { MethodNode method ->
            method.getAnnotations( new ClassNode( JasminCode ) )
        }.each { MethodNode method ->
            log.fine "Found method annotated with @JasminCode $method.name"

            List existingStatements = method.code.statements
            if ( existingStatements.size() >= 1 ) {
                def statement = existingStatements.first()
                if ( statement instanceof ExpressionStatement ) {
                    def expr = statement.expression
                    def assemblerText
                    if ( expr instanceof PropertyExpression ) {
                        assemblerText = Eval.me( expr.text )
                    } else {
                        assemblerText = expr.text
                    }

                    try {
                        existingStatements.clear()
                        def targetDir = sourceUnit.configuration.targetDirectory
                        def className = classNameFor( method )
                        def jasminClass = grasmin.createJasminClass( assemblerText, targetDir, className, method )
                        def classNode = new ClassNode( jasminClass )

                        if ( !targetDir ) sourceUnit.getAST().addClass( classNode )
                        def jasminMethodCall = grassemblyStatement( classNode, method )
                        existingStatements.add( jasminMethodCall )
                    } catch ( Throwable e ) {
                        log.warning e.toString() + ' ' + e.getCause()?.toString()
                    }
                }
            }
        }
    }

    static classNameFor( MethodNode methodNode ) {
        methodNode.declaringClass.name.replace( '.', '_' ) + '_' + methodNode.name
    }

    private Statement grassemblyStatement( ClassNode jasminClass, MethodNode methodNode ) {
        new ExpressionStatement(
                new MethodCallExpression(
                        new ClassExpression( jasminClass ),
                        "run",
                        new ArgumentListExpression( methodNode.parameters )
                )
        )
    }

}

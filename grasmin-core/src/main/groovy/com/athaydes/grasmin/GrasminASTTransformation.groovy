package com.athaydes.grasmin

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
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
    JasminTyper typer = new JasminTyper()

    public void visit( ASTNode[] astNodes, SourceUnit sourceUnit ) {
        if ( !processedSourceUnits.add( sourceUnit ) ) return

        log.fine "Transforming sourceUnit ${sourceUnit.name}"

        if ( !sourceUnit || !sourceUnit.AST ) return

        def processNodes = { List<AnnotatedNode> nodes ->
            nodes.findAll { node ->
                node.getAnnotations( new ClassNode( JasminCode ) )
            }.each { AnnotatedNode node ->
                process( node, sourceUnit )
            }
        }

        List classNodes = sourceUnit.getAST().getClasses()
        List methods = classNodes*.getMethods().flatten()

        processNodes classNodes
        processNodes methods
    }

    private void process( ClassNode classNode, SourceUnit sourceUnit ) {
        log.fine "Processing class $classNode"
        def targetDir = sourceUnit.configuration.targetDirectory

        def methodBodies = classNode.methods.collect { method ->
            """|.method ${typer.modifiersString( method.modifiers )} ${typer.typeDescriptorOf( method )}
               |${grasmin.extractJasminMethodBody( method )}
               |.end method
               |""".stripMargin()
        }

        def modifiers = typer.modifiersString classNode.modifiers
        def classDeclaration = """
            |.class ${modifiers} ${classNode.name}
            |.super ${typer.className( classNode.superClass.name )}
            |""".stripMargin()
        def classBody = """
            |.method public <init>()V
            |   aload_0
            |   invokespecial java/lang/Object/<init>()V
            |   return
            |.end method
            |
            |""".stripMargin() + methodBodies.join( '\n' )
        log.info classDeclaration + classBody
    }

    private void process( MethodNode method, SourceUnit sourceUnit ) {
        if ( method.declaringClass.getAnnotations( new ClassNode( JasminCode ) ) ) {
            return
        }

        log.info "Processing method $method"

        try {
            rewriteMethod( sourceUnit, method, grasmin.extractJasminMethodBody( method ) )
        } catch ( Throwable e ) {
            log.warning e.toString() + ' ' + e.getCause()?.toString()
        }
    }

    private void rewriteMethod( SourceUnit sourceUnit, MethodNode method, String assemblerText ) {
        method.code.statements.clear()
        def targetDir = sourceUnit.configuration.targetDirectory
        def className = classNameFor( method )
        def jasminClass = grasmin.createJasminClass( assemblerText, targetDir, className, method )
        def classNode = new ClassNode( jasminClass )

        if ( !targetDir ) sourceUnit.getAST().addClass( classNode )

        def jasminMethodCall = grassemblyStatement( classNode, method )
        method.code.statements.add( jasminMethodCall )
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

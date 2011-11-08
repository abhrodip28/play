package play.template2.compile;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassCodeExpressionTransformer;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.GroovyClassVisitor;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.codehaus.groovy.transform.powerassert.StatementReplacingVisitorSupport;
import play.templates.JavaExtensions;

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class GTGroovyPimpTransformer implements ASTTransformation {

    static class Trans extends ClassCodeExpressionTransformer {

        final SourceUnit sourceUnit;

        Trans(SourceUnit sourceUnit) {
            this.sourceUnit = sourceUnit;
        }

        @Override
        protected SourceUnit getSourceUnit() {
            return sourceUnit;
        }

        @Override
        public Expression transform(Expression exp) {
            if ( exp instanceof MethodCallExpression) {
                MethodCallExpression me = (MethodCallExpression)exp;

                if ("format".equals(me.getMethodAsString())) {
                    ClassExpression ce = new ClassExpression( new ClassNode(JavaExtensions.class));
                    ArgumentListExpression args = (ArgumentListExpression)me.getArguments();
                    args.getExpressions().add(0, me.getObjectExpression());
                    exp = new MethodCallExpression(ce, "format", me.getArguments() );
                }
            }
            return exp.transformExpression(this);
        }


        /*
        @Override
        public void visitExpressionStatement(ExpressionStatement es) {

            if ( es.getExpression() instanceof MethodCallExpression) {
                MethodCallExpression me = (MethodCallExpression)es.getExpression();

                if ("format".equals(me.getMethodAsString())) {
                    ClassExpression ce = new ClassExpression( new ClassNode(JavaExtensions.class));
                    es.setExpression(new MethodCallExpression(ce, "fubar", me.getArguments() ));
                }
            }
            super.visitExpressionStatement(es);

        }
        */


    }

    public void visit(ASTNode[] nodes, SourceUnit source) {


        for (ClassNode classNode : source.getAST().getClasses() ) {

            classNode.visitContents(new Trans(source));

        }

    }
}
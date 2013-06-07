package de.hopp.generator;

import de.upb.hni.vmagic.expression.BinaryExpression;
import de.upb.hni.vmagic.expression.Expression;
import de.upb.hni.vmagic.expression.Literal;
import de.upb.hni.vmagic.expression.Name;
import de.upb.hni.vmagic.expression.Parentheses;
import de.upb.hni.vmagic.expression.UnaryExpression;
import de.upb.hni.vmagic.object.Constant;

public class ExpressionUnparser extends de.upb.hni.vmagic.expression.ExpressionVisitor {

    private final StringBuffer buffer;

    public ExpressionUnparser() {
        this.buffer = new StringBuffer();
    }

    public ExpressionUnparser(StringBuffer buffer) {
        this.buffer = buffer;
    }

    public static String unparseExpression(Expression expression) {
        StringBuffer buffer = new StringBuffer();
        ExpressionUnparser p = new ExpressionUnparser(buffer);
        p.visit(expression);
        return buffer.toString();
    }

    @Override
    public String toString() {
        return buffer.toString();
    }

    @Override
    protected void visitLiteral(Literal expression) {
        buffer.append(expression.toString());
    }

    @Override
    protected void visitName(Name object) {
        // TODO the rest? -_-
        if(object instanceof Constant) {
            Constant c = (Constant)object;
            buffer.append(c.getIdentifier());
        } else buffer.append(object.toString());
    }

    @Override
    protected void visitParentheses(Parentheses par) {
        buffer.append('(');
        visit(par.getExpression());
        buffer.append(')');
    }

    @Override
    protected void visitBinaryExpression(BinaryExpression expression) {
        buffer.append('(');

        visit(expression.getLeft());

        // TODO add more expressions
        switch(expression.getExpressionKind()) {
        case ABS:
            break;
        case AND:
            break;
        case CONCATENATE:
            break;
        case DIVIDE: buffer.append("/");
            break;
        case EQUALS:
            break;
        case GREATER_EQUALS:
            break;
        case GREATER_THAN:
            break;
        case LESS_EQUALS:
            break;
        case LESS_THAN:
            break;
        case MINUS: buffer.append("-");
            break;
        case MOD:
            break;
        case MULTIPLY: buffer.append("*");
            break;
        case NAND:
            break;
        case NOR:
            break;
        case NOT:
            break;
        case NOT_EQUALS:
            break;
        case OR:
            break;
        case PLUS: buffer.append("+");
            break;
        case POW:
            break;
        case REM:
            break;
        case ROL:
            break;
        case ROR:
            break;
        case SLA:
            break;
        case SLL:
            break;
        case SRA:
            break;
        case SRL:
            break;
        case XNOR:
            break;
        case XOR:
            break;
        default: throw new RuntimeException("unsupported binary operator");
        }

        visit(expression.getRight());

        buffer.append(')');
    }

    @Override
    protected void visitUnaryExpression(UnaryExpression expression) {
        buffer.append('(');

        // TODO add more expressions
        switch(expression.getExpressionKind()) {
        case ABS:
            break;
        case AND:
            break;
        case CONCATENATE:
            break;
        case DIVIDE:
            break;
        case EQUALS:
            break;
        case GREATER_EQUALS:
            break;
        case GREATER_THAN:
            break;
        case LESS_EQUALS:
            break;
        case LESS_THAN:
            break;
        case MINUS: buffer.append("-");
            break;
        case MOD:
            break;
        case MULTIPLY:
            break;
        case NAND:
            break;
        case NOR:
            break;
        case NOT:
            break;
        case NOT_EQUALS:
            break;
        case OR:
            break;
        case PLUS: buffer.append("+");
            break;
        case POW:
            break;
        case REM:
            break;
        case ROL:
            break;
        case ROR:
            break;
        case SLA:
            break;
        case SLL:
            break;
        case SRA:
            break;
        case SRL:
            break;
        case XNOR:
            break;
        case XOR:
            break;
        default: throw new RuntimeException("unsupported unary operator");
        }

        visit(expression.getExpression());

        buffer.append(')');
    }


}

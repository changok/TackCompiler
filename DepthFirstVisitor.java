import java.util.List;

abstract class DepthFirstVisitor extends Visitor {
  private DepthFirstVisitor v(AstNode ast) {
    ast.accept(this);
    return this;
  }
  private DepthFirstVisitor v(List<? extends AstNode> asts) {
    for (AstNode a : asts)
      v(a);
    return this;
  }

  // ---------------- top-level ----------------
  Object visit(Program ast) { return v(ast._functions); }
  Object visit(FunDef ast) { return v(ast._name).v(ast._type).v(ast._body); }

  // ---------------- types ----------------
  Object visit(ArrayType ast) { return v(ast._elem); }
  Object visit(RecordType ast) { return v(ast._fields); }
  Object visit(FieldType ast) { return v(ast._field).v(ast._type); }
  Object visit(PrimitiveType ast) { return this; }
  Object visit(NullType ast) { return this; }
  Object visit(FunType ast) { return v(ast._formals).v(ast._returnType); }

  // ---------------- statements ----------------
  Object visit(VarDef ast) { return v(ast._var).v(ast._rhs); }
  Object visit(AssignStmt ast) { return v(ast._lhs).v(ast._rhs); }
  Object visit(BlockStmt ast) { return v(ast._stmts); }
  Object visit(CallStmt ast) { return v(ast._expr); }
  Object visit(ForStmt ast) { return v(ast._var).v(ast._expr).v(ast._body); }
  Object visit(IfStmt ast) {
    v(ast._cond).v(ast._thenBranch);
    if (null != ast._elseBranch)
      v(ast._elseBranch);
    return this;
  }
  Object visit(ReturnStmt ast) {
    if (null != ast._expr)
      v(ast._expr);
    return this;
  }
  Object visit(WhileStmt ast) { return v(ast._cond).v(ast._body); }

  // ---------------- expressions ----------------
  Object visit(InfixExpr ast) { return v(ast._lhs).v(ast._rhs); }
  Object visit(PrefixExpr ast) { return v(ast._base); }
  Object visit(CallExpr ast) { return v(ast._base).v(ast._actuals); }
  Object visit(CastExpr ast) { return v(ast._base).v(ast._targetType); }
  Object visit(FieldExpr ast) { return v(ast._base).v(ast._field); }
  Object visit(SubscriptExpr ast) { return v(ast._base).v(ast._subscript); }
  Object visit(ParenExpr ast) { return v(ast._base); }

  // ---------------- identifiers ----------------
  Object visit(FieldId ast) { return this; }
  Object visit(FunId ast) { return this; }
  Object visit(VarId ast) { return this; }

  // ---------------- literals ----------------
  Object visit(ArrayLit ast) { return v(ast._elems); }
  Object visit(RecordLit ast) { return v(ast._fields); }
  Object visit(FieldLit ast) { return v(ast._field).v(ast._expr); }
  Object visit(BoolLit ast) { return this; }
  Object visit(IntLit ast) { return this; }
  Object visit(NullLit ast) { return this; }
  Object visit(StringLit ast) { return this; }
}

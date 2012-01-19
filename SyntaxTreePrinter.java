import java.io.PrintWriter;
import java.util.Stack;

class SyntaxTreePrinter extends Visitor {
  PrintWriter _writer;
  Stack<AstNode> _stack;

  SyntaxTreePrinter(PrintWriter writer) {
    _writer = writer;
    _stack = new Stack<AstNode>();
  }

  SyntaxTreePrinter begin(AstNode ast) {
    return begin(ast, null);
  }
  SyntaxTreePrinter begin(AstNode ast, String attr) {
    for (int i=0, n=_stack.size(); i<n; i++)
      p("  ");
    _stack.push(ast);
    p(ast.getClass().getSimpleName());
    if (null != attr)
      p(" ").p(attr);
    return pln();
  }
  SyntaxTreePrinter end(AstNode ast) {
    AstNode top = _stack.pop();
    assert top == ast;
    return this;
  }

  SyntaxTreePrinter p(AstNode ast) {
    ast.accept(this);
    return this;
  }
  SyntaxTreePrinter p(String s) {
    assert(-1 == s.indexOf('\n'));
    _writer.print(s);
    return this;
  }
  SyntaxTreePrinter pln() {
    _writer.println();
    return this;
  }

  // ---------------- top-level ----------------
  Object visit(Program ast) {
    begin(ast);
    if (null != ast._raw)
      p(ast._raw);
    else
      for (FunDef f : ast._functions)
        p(f);
    return end(ast);
  }

  Object visit(FunDef ast) {
    return begin(ast).p(ast._name).p(ast._type).p(ast._body).end(ast);
  }

  Object visit(FunDefListHead ast) {
    begin(ast);
    if (null != ast._first)
      p(ast._first).p(ast._tail);
    return end(ast);
  }
  Object visit(FunDefListTail ast) {
    begin(ast);
    if (null != ast._next)
      p(ast._next).p(ast._tail);
    return end(ast);
  }

  // ---------------- types ----------------
  Object visit(ArrayType ast) {
    return begin(ast).p(ast._elem).end(ast);
  }

  Object visit(RecordType ast) {
    begin(ast);
    if (null != ast._raw)
      p(ast._raw);
    else
      for (FieldType f : ast._fields)
        p(f);
    return end(ast);
  }

  Object visit(FieldTypeListHead ast) {
    begin(ast);
    if (null != ast._first)
      p(ast._first).p(ast._tail);
    return end(ast);
  }
  Object visit(FieldTypeListTail ast) {
    begin(ast);
    if (null != ast._next)
      p(ast._next).p(ast._tail);
    return end(ast);
  }

  Object visit(FieldType ast) {
    return begin(ast).p(ast._field).p(ast._type).end(ast);
  }

  Object visit(PrimitiveType ast) {
    return begin(ast, ast._name).end(ast);
  }

  Object visit(NullType ast) {
    return begin(ast).end(ast);
  }

  Object visit(FunType ast) {
    return begin(ast).p(ast._formals).p(ast._returnType).end(ast);
  }

  // ---------------- statements ----------------
  Object visit(VarDef ast) {
    return begin(ast).p(ast._var).p(ast._rhs).end(ast);
  }

  Object visit(AssignStmt ast) {
    return begin(ast).p(ast._lhs).p(ast._rhs).end(ast);
  }

  Object visit(BlockStmt ast) {
    begin(ast);
    if (null != ast._raw)
      p(ast._raw);
    else
      for (Stmt s : ast._stmts)
        p(s);
    return end(ast);
  }

  Object visit(CallStmt ast) {
    return begin(ast).p(ast._expr).end(ast);
  }

  Object visit(ForStmt ast) {
    return begin(ast).p(ast._var).p(ast._expr).p(ast._body).end(ast);
  }

  Object visit(IfStmt ast) {
    begin(ast).p(ast._cond).p(ast._thenBranch);
    if (null != ast._elseBranch)
      p(ast._elseBranch);
    return end(ast);
  }

  Object visit(WhileStmt ast) {
    return begin(ast).p(ast._cond).p(ast._body).end(ast);
  }

  Object visit(ReturnStmt ast) {
    begin(ast);
    if (null != ast._expr)
      p(ast._expr);
    return end(ast);
  }

  Object visit(StmtListHead ast) {
    begin(ast);
    if (null != ast._first)
      p(ast._first).p(ast._tail);
    return end(ast);
  }
  Object visit(StmtListTail ast) {
    begin(ast);
    if (null != ast._next)
      p(ast._next).p(ast._tail);
    return end(ast);
  }

  // ---------------- expressions ----------------
  Object visit(InfixExpr ast) {
    return begin(ast, ast._op).p(ast._lhs).p(ast._rhs).end(ast);
  }
  Object visit(InfixExprHead ast) {
    return begin(ast).p(ast._lhs).p(ast._tail).end(ast);
  }
  Object visit(InfixExprTail ast) {
    begin(ast);
    if (null != ast._rhs)
      p(ast._op).p(ast._rhs).p(ast._tail);
    return end(ast);
  }

  Object visit(PrefixExpr ast) {
    return begin(ast, ast._op).p(ast._base).end(ast);
  }

  Object visit(PostfixExprHead ast) {
    return begin(ast).p(ast._base).p(ast._tail).end(ast);
  }
  Object visit(PostfixExprTail ast) {
    assert PostfixExprTail.class == ast.getClass() : "must not be subtype";
    return begin(ast).end(ast);
  }

  Object visit(CallExpr ast) {
    begin(ast).p(ast._base);
    for (Expr e : ast._actuals)
      p(e);
    return end(ast);
  }
  Object visit(CallExprTail ast) {
    return begin(ast).p(ast._actuals).p(ast._tail).end(ast);
  }

  Object visit(CastExpr ast) {
    return begin(ast).p(ast._base).p(ast._targetType).end(ast);
  }
  Object visit(CastExprTail ast) {
    return begin(ast).p(ast._targetType).p(ast._tail).end(ast);
  }

  Object visit(FieldExpr ast) {
    return begin(ast).p(ast._base).p(ast._field).end(ast);
  }
  Object visit(FieldExprTail ast) {
    return begin(ast).p(ast._field).p(ast._tail).end(ast);
  }

  Object visit(SubscriptExpr ast) {
    return begin(ast).p(ast._base).p(ast._subscript).end(ast);
  }
  Object visit(SubscriptExprTail ast) {
    return begin(ast).p(ast._subscript).p(ast._tail).end(ast);
  }

  Object visit(ExprListHead ast) {
    begin(ast);
    if (null != ast._first)
      p(ast._first).p(ast._tail);
    return end(ast);
  }
  Object visit(ExprListTail ast) {
    begin(ast);
    if (null != ast._next)
      p(ast._next).p(ast._tail);
    return end(ast);
  }

  Object visit(ParenExpr ast) {
    return begin(ast).p(ast._base).end(ast);
  }

  // ---------------- identifiers ----------------
  Object visit(FieldId ast) {
    return begin(ast, ast._id).end(ast);
  }

  Object visit(FunId ast) {
    return begin(ast, ast._id).end(ast);
  }

  Object visit(VarId ast) {
    return begin(ast, ast._id).end(ast);
  }

  // ---------------- literals ----------------
  Object visit(ArrayLit ast) {
    begin(ast);
    if (null != ast._raw)
      p(ast._raw);
    else
      for (Expr e : ast._elems)
        p(e);
    return end(ast);
  }

  Object visit(RecordLit ast) {
    begin(ast);
    if (null != ast._raw)
      p(ast._raw);
    else
      for (FieldLit f : ast._fields)
        p(f);
    return end(ast);
  }

  Object visit(FieldLitListHead ast) {
    begin(ast);
    if (null != ast._first)
      p(ast._first).p(ast._tail);
    return end(ast);
  }
  Object visit(FieldLitListTail ast) {
    begin(ast);
    if (null != ast._next)
      p(ast._next).p(ast._tail);
    return end(ast);
  }

  Object visit(FieldLit ast) {
    return begin(ast).p(ast._field).p(ast._expr).end(ast);
  }

  Object visit(BoolLit ast) {
    return begin(ast, Boolean.toString(ast._value)).end(ast);
  }
  Object visit(IntLit ast) {
    return begin(ast, Integer.toString(ast._value)).end(ast);
  }
  Object visit(NullLit ast) {
    return begin(ast).end(ast);
  }
  Object visit(StringLit ast) {
    return begin(ast, ast._token).end(ast);
  }
}

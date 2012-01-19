import java.io.PrintWriter;
import java.util.List;

class PrettyPrinter extends Visitor {
  PrintWriter _writer;
  boolean _lineStart;
  int _indent;

  PrettyPrinter(PrintWriter writer) {
    _writer = writer;
    _lineStart = true;
    _indent = 0;
  }

  PrettyPrinter p(AstNode ast) {
    ast.accept(this);
    return this;
  }
  PrettyPrinter p(String s) {
    printIndentIfLineStart();
    assert(-1 == s.indexOf('\n'));
    _writer.print(s);
    return this;
  }
  PrettyPrinter p(boolean b) {
    printIndentIfLineStart();
    _writer.print(b);
    return this;
  }
  PrettyPrinter p(int i) {
    printIndentIfLineStart();
    _writer.print(i);
    return this;
  }
  PrettyPrinter p(List<Expr> ls) {
    boolean first = true;
    for (Expr e : ls) {
      if (first) first = false;
      else p(", ");
      p(e);
    }
    return this;
  }
  PrettyPrinter indent() {
    _indent++;
    return this;
  }
  PrettyPrinter dedent() {
    _indent--;
    assert 0 <= _indent;
    return this;
  }
  PrettyPrinter pln() {
    _writer.println();
    _lineStart = true;
    return this;
  }
  PrettyPrinter printIndentIfLineStart() {
    if (_lineStart) {
      _lineStart = false;
      for (int i=0, n=_indent; i<_indent; i++)
        _writer.print("  ");
    }
    return this;
  }

  // ---------------- top-level ----------------
  Object visit(Program ast) {
    if (null != ast._raw) {
      p(ast._raw);
    } else {
      boolean first = true;
      for (FunDef f : ast._functions) {
        if (first) first = false;
        else       pln();
        p(f);
      }
    }
    return this;
  }

  Object visit(FunDef ast) {
    return p(ast._name).p(" = fun ").p(ast._type).p(" ").p(ast._body);
  }

  Object visit(FunDefListHead ast) {
    p("FunDefListHead(");
    if (null != ast._first)
      p(ast._first).p(", ").p(ast._tail);
    return p(")");
  }
  Object visit(FunDefListTail ast) {
    p("FunDefListTail(");
    if (null != ast._next)
      p(ast._next).p(", ").p(ast._tail);
    return p(")");
  }

  // ---------------- types ----------------
  Object visit(ArrayType ast) {
    p("[");
    if (null != ast._elem)
      p(ast._elem);
    return p("]");
  }

  Object visit(RecordType ast) {
    p("(");
    if (null != ast._raw) {
      p(ast._raw);
    } else {
      boolean first = true;
      for (FieldType f : ast._fields) {
        if (first) first = false;
        else       p(", ");
        p(f);
      }
    }
    return p(")");
  }

  Object visit(FieldTypeListHead ast) {
    p("FieldTypeListHead(");
    if (null != ast._first)
      p(ast._first).p(", ").p(ast._tail);
    return p(")");
  }
  Object visit(FieldTypeListTail ast) {
    p("FieldTypeListTail(");
    if (null != ast._next)
      p(ast._next).p(", ").p(ast._tail);
    return p(")");
  }

  Object visit(FieldType ast) {
    return p(ast._field).p(" : ").p(ast._type);
  }

  Object visit(PrimitiveType ast) {
    return p(ast._name);
  }

  Object visit(NullType ast) {
    return p("null");
  }

  Object visit(FunType ast) {
    return p(ast._formals).p(" -> ").p(ast._returnType);
  }

  // ---------------- statements ----------------
  Object visit(VarDef ast) {
    return p(ast._var).p(" = ").p(ast._rhs).p(";");
  }

  Object visit(AssignStmt ast) {
    return p(ast._lhs).p(" := ").p(ast._rhs).p(";");
  }

  Object visit(BlockStmt ast) {
    p("{").indent().pln();
    if (null != ast._raw)
      p(ast._raw);
    else
      for (Stmt s : ast._stmts)
        p(s).pln();
    return dedent().p("}");
  }

  Object visit(CallStmt ast) {
    return p(ast._expr).p(";");
  }

  Object visit(ForStmt ast) {
    return p("for ").p(ast._var).p(" in ").p(ast._expr).p(" ").p(ast._body);
  }

  Object visit(IfStmt ast) {
    p("if ").p(ast._cond).p(" ").p(ast._thenBranch);
    if (null != ast._elseBranch)
      p(" else ").p(ast._elseBranch);
    return this;
  }

  Object visit(WhileStmt ast) {
    return p("while ").p(ast._cond).p(" ").p(ast._body);
  }

  Object visit(ReturnStmt ast) {
    p("->");
    if (null != ast._expr)
      p(" ").p(ast._expr);
    return p(";");
  }

  Object visit(StmtListHead ast) {
    p("StmtListHead(");
    if (null != ast._first)
      p(ast._first).p(", ").p(ast._tail);
    return p(")");
  }
  Object visit(StmtListTail ast) {
    p("StmtListTail(");
    if (null != ast._next)
      p(ast._next).p(", ").p(ast._tail);
    return p(")");
  }

  // ---------------- expressions ----------------
  Object visit(InfixExpr ast) {
    return p(ast._lhs).p(" ").p(ast._op).p(" ").p(ast._rhs);
  }
  Object visit(InfixExprHead ast) {
    return p("InfixExprHead(").p(ast._lhs).p(", ").p(ast._tail).p(")");
  }
  Object visit(InfixExprTail ast) {
    p("InfixExprTail(");
    if (null != ast._rhs)
      p(ast._op).p(", ").p(ast._rhs).p(", ").p(ast._tail);
    return p(")");
  }

  Object visit(PrefixExpr ast) {
    return p(ast._op).p(ast._base);
  }

  Object visit(PostfixExprHead ast) {
    return p("PostfixExprHead(").p(ast._base).p(", ").p(ast._tail).p(")");
  }
  Object visit(PostfixExprTail ast) {
    assert PostfixExprTail.class == ast.getClass() : "must not be subtype";
    return p("PostfixExprTail()");
  }

  Object visit(CallExpr ast) {
    return p(ast._base).p("(").p(ast._actuals).p(")");
  }
  Object visit(CallExprTail ast) {
    return p("CallExprTail((").p(ast._actuals).p("), ").p(ast._tail).p(")");
  }

  Object visit(CastExpr ast) {
    return p(ast._base).p(" : ").p(ast._targetType);
  }
  Object visit(CastExprTail ast) {
    return p("CastExprTail(").p(ast._targetType).p(", ").p(ast._tail).p(")");
  }

  Object visit(FieldExpr ast) {
    return p(ast._base).p(".").p(ast._field);
  }
  Object visit(FieldExprTail ast) {
    return p("FieldExprTail(").p(ast._field).p(", ").p(ast._tail).p(")");
  }

  Object visit(SubscriptExpr ast) {
    return p(ast._base).p("[").p(ast._subscript).p("]");
  }
  Object visit(SubscriptExprTail ast) {
    return p("SubscriptExprTail(").p(ast._subscript).p(", ").p(ast._tail).p(")");
  }

  Object visit(ExprListHead ast) {
    p("ExprListHEad(");
    if (null != ast._first)
      p(ast._first).p(", ").p(ast._tail);
    return p(")");
  }
  Object visit(ExprListTail ast) {
    p("ExprListTail(");
    if (null != ast._next)
      p(ast._next).p(", ").p(ast._tail);
    return p(")");
  }

  Object visit(ParenExpr ast) {
    return p("(").p(ast._base).p(")");
  }

  // ---------------- identifiers ----------------
  Object visit(FieldId ast) {
    return p(ast._id);
  }

  Object visit(FunId ast) {
    return p(ast._id);
  }

  Object visit(VarId ast) {
    return p(ast._id);
  }

  // ---------------- literals ----------------
  Object visit(ArrayLit ast) {
    p("[");
    if (null != ast._raw)
      p(ast._raw);
    else
      p(ast._elems);
    return p("]");
  }

  Object visit(RecordLit ast) {
    p("(");
    if (null != ast._raw) {
      p(ast._raw);
    } else {
      boolean first = true;
      for (FieldLit f : ast._fields) {
        if (first) first = false;
        else       p(", ");
        p(f);
      }
    }
    return p(")");
  }

  Object visit(FieldLitListHead ast) {
    p("FieldLitListHead(");
    if (null != ast._first)
      p(ast._first).p(", ").p(ast._tail);
    return p(")");
  }
  Object visit(FieldLitListTail ast) {
    p("FieldLitListTail(");
    if (null != ast._next)
      p(ast._next).p(", ").p(ast._tail);
    return p(")");
  }

  Object visit(FieldLit ast) {
    return p(ast._field).p(" = ").p(ast._expr);
  }

  Object visit(BoolLit ast) {
    return p(ast._value);
  }
  Object visit(IntLit ast) {
    return p(ast._value);
  }
  Object visit(NullLit ast) {
    return p("null");
  }
  Object visit(StringLit ast) {
    return p(ast._token);
  }
}

import java.util.*;

//see aho_et_al_2007 Figure 5.13 (Page 321)
class TreeNormalizer extends Visitor {
  // ---------------- top-level ----------------
  Object visit(Program ast) {
    @SuppressWarnings("unchecked")
      List<FunDef> functions = (List<FunDef>)ast._raw.accept(this);
    ast._functions = functions;
    ast._raw = null;
    return ast;
  }

  Object visit(FunDef ast) {
    ast._name = (FunId)ast._name.accept(this);
    ast._type = (FunType)ast._type.accept(this);
    ast._body = (BlockStmt)ast._body.accept(this);
    return ast;
  }

  Object visit(FunDefListHead ast) {
    List<FunDef> result = new ArrayList<FunDef>();
    if (null == ast._first)
      return result;
    result.add((FunDef)ast._first.accept(this));
    ast._tail._inh = result;
    return ast._tail.accept(this);
  }
  Object visit(FunDefListTail ast) {
    if (null == ast._next)
      return ast._inh;
    ast._inh.add((FunDef)ast._next.accept(this));
    ast._tail._inh = ast._inh;
    return ast._tail.accept(this);
  }

  // ---------------- types ----------------
  Object visit(ArrayType ast) {
    ast._elem = (Type)ast._elem.accept(this);
    return ast;
  }

  Object visit(RecordType ast) {
    if (null != ast._raw) {
      @SuppressWarnings("unchecked")
	List<FieldType> fields = (List<FieldType>)ast._raw.accept(this);
      ast._fields = fields;
      ast._raw = null;
    }
    return ast;
  }

  Object visit(FieldTypeListHead ast) {
    List<FieldType> result = new ArrayList<FieldType>();
    if (null == ast._first)
      return result;
    result.add((FieldType)ast._first.accept(this));
    ast._tail._inh = result;
    return ast._tail.accept(this);
  }
  Object visit(FieldTypeListTail ast) {
    if (null == ast._next)
      return ast._inh;
    ast._inh.add((FieldType)ast._next.accept(this));
    ast._tail._inh = ast._inh;
    return ast._tail.accept(this);
  }

  Object visit(FieldType ast) {
    ast._field = (FieldId)ast._field.accept(this);
    ast._type = (Type)ast._type.accept(this);
    return ast;
  }

  Object visit(PrimitiveType ast) {
    return ast;
  }

  Object visit(FunType ast) {
    ast._formals = (RecordType)ast._formals.accept(this);
    ast._returnType = (Type)ast._returnType.accept(this);
    return ast;
  }

  // ---------------- statements ----------------
  Object visit(VarDef ast) {
    ast._var = (VarId)ast._var.accept(this);
    ast._rhs = (Expr)ast._rhs.accept(this);
    return ast;
  }

  Object visit(AssignStmt ast) {
    ast._lhs = (Expr)ast._lhs.accept(this);
    ast._rhs = (Expr)ast._rhs.accept(this);
    return ast;
  }

  Object visit(BlockStmt ast) {
    @SuppressWarnings("unchecked")
      List<Stmt> stmts = (List<Stmt>)ast._raw.accept(this);
    ast._stmts = stmts;
    ast._raw = null;
    return ast;
  }

  Object visit(CallStmt ast) {
    ast._expr = (Expr)ast._expr.accept(this);
    return ast;
  }

  Object visit(ForStmt ast) {
    ast._var = (VarId)ast._var.accept(this);
    ast._expr = (Expr)ast._expr.accept(this);
    ast._body = (BlockStmt)ast._body.accept(this);
    return ast;
  }

  Object visit(IfStmt ast) {
    ast._cond = (Expr)ast._cond.accept(this);
    ast._thenBranch = (BlockStmt)ast._thenBranch.accept(this);
    if (null != ast._elseBranch)
      ast._elseBranch = (BlockStmt)ast._elseBranch.accept(this);
    return ast;
  }

  Object visit(WhileStmt ast) {
    ast._cond = (Expr)ast._cond.accept(this);
    ast._body = (BlockStmt)ast._body.accept(this);
    return ast;
  }

  Object visit(ReturnStmt ast) {
    if (null != ast._expr)
      ast._expr = (Expr)ast._expr.accept(this);
    return ast;
  }

  Object visit(StmtListHead ast) {
    List<Stmt> result = new ArrayList<Stmt>();
    if (null == ast._first)
      return result;
    result.add((Stmt)ast._first.accept(this));
    ast._tail._inh = result;
    return ast._tail.accept(this);
  }
  Object visit(StmtListTail ast) {
    if (null == ast._next)
      return ast._inh;
    ast._inh.add((Stmt)ast._next.accept(this));
    ast._tail._inh = ast._inh;
    return ast._tail.accept(this);
  }

  // ---------------- expressions ----------------
  Object visit(InfixExprHead ast) {
    ast._tail._inh = (Expr)ast._lhs.accept(this);
    return ast._tail.accept(this);
  }
  Object visit(InfixExprTail ast) {
    if (null == ast._rhs)
      return ast._inh;
    Expr rhs = (Expr)ast._rhs.accept(this);
    ast._tail._inh = new InfixExpr(ast._inh._loc, ast._op, ast._inh, rhs);
    return ast._tail.accept(this);
  }

  Object visit(PrefixExpr ast) {
    ast._base = (Expr)ast._base.accept(this);
    return ast;
  }

  Object visit(PostfixExprHead ast) {
    ast._tail._inh = (Expr)ast._base.accept(this);
    return ast._tail.accept(this);
  }
  Object visit(PostfixExprTail ast) {
    assert PostfixExprTail.class == ast.getClass() : "must not be subtype";
    return ast._inh;
  }
  Object visit(CallExprTail ast) {
    @SuppressWarnings("unchecked")
      List<Expr> actuals = (List<Expr>)ast._actuals.accept(this);
    Expr callee = ast._inh;
    if (callee instanceof VarId)
      callee = new FunId(callee._loc, ((VarId)callee)._id);
    ast._tail._inh = new CallExpr(callee._loc, callee, actuals);
    return ast._tail.accept(this);
  }
  Object visit(CastExprTail ast) {
    Type type = (Type)ast._targetType.accept(this);
    ast._tail._inh = new CastExpr(ast._inh._loc, ast._inh, type);
    return ast._tail.accept(this);
  }

  Object visit(FieldExprTail ast) {
    FieldId field = (FieldId)ast._field.accept(this);
    ast._tail._inh = new FieldExpr(ast._inh._loc, ast._inh, field);
    return ast._tail.accept(this);
  }
  Object visit(SubscriptExprTail ast) {
    Expr subscript = (Expr)ast._subscript.accept(this);
    ast._tail._inh = new SubscriptExpr(ast._inh._loc, ast._inh, subscript);
    return ast._tail.accept(this);
  }

  Object visit(ExprListHead ast) {
    List<Expr> result = new ArrayList<Expr>();
    if (null == ast._first)
      return result;
    result.add((Expr)ast._first.accept(this));
    ast._tail._inh = result;
    return ast._tail.accept(this);
  }
  Object visit(ExprListTail ast) {
    if (null == ast._next)
      return ast._inh;
    ast._inh.add((Expr)ast._next.accept(this));
    ast._tail._inh = ast._inh;
    return ast._tail.accept(this);
  }

  Object visit(ParenExpr ast) {
    ast._base = (Expr)ast._base.accept(this);
    return ast;
  }

  // ---------------- identifiers ----------------
  Object visit(FieldId ast) {
    return ast;
  }

  Object visit(FunId ast) {
    return ast;
  }

  Object visit(VarId ast) {
    return ast;
  }

  // ---------------- literals ----------------
  Object visit(ArrayLit ast) {
    @SuppressWarnings("unchecked")
      List<Expr> elems = (List<Expr>)ast._raw.accept(this);
    ast._elems = elems;
    ast._raw = null;
    return ast;
  }

  Object visit(RecordLit ast) {
    @SuppressWarnings("unchecked")
      List<FieldLit> fields = (List<FieldLit>)ast._raw.accept(this);
    ast._fields = fields;
    ast._raw = null;
    return ast;
  }

  Object visit(FieldLitListHead ast) {
    List<FieldLit> result = new ArrayList<FieldLit>();
    if (null == ast._first)
      return result;
    result.add((FieldLit)ast._first.accept(this));
    ast._tail._inh = result;
    return ast._tail.accept(this);
  }
  Object visit(FieldLitListTail ast) {
    if (null == ast._next)
      return ast._inh;
    ast._inh.add((FieldLit)ast._next.accept(this));
    ast._tail._inh = ast._inh;
    return ast._tail.accept(this);
  }

  Object visit(FieldLit ast) {
    ast._field = (FieldId)ast._field.accept(this);
    ast._expr = (Expr)ast._expr.accept(this);
    return ast;
  }

  Object visit(BoolLit ast) {
    return ast;
  }

  Object visit(IntLit ast) {
    return ast;
  }

  Object visit(NullLit ast) {
    return ast;
  }

  Object visit(StringLit ast) {
    return ast;
  }
}

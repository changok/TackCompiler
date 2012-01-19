class ScopeAnalyzer extends DepthFirstVisitor {
  SymbolTable _symTab;

  ScopeAnalyzer() {
    _symTab = new SymbolTable();
  }

  private void def(Symbol sym) {
    if (_symTab.contains(sym.name())) {
      ErrorPrinter.print(sym.loc(),
                         "Duplicate definition of '" + sym.name() + "'");
      ErrorPrinter.print(_symTab.get(sym.name()).loc(),
                         "... previous definition of '" + sym.name() + "'");
    } else {
      _symTab.def(sym);
    }
  }

  // ---------------- top-level ----------------
  Object visit(FunDef ast) {
    FunSym sym = new FunSym(_symTab._current, ast);
    def(sym);
    ast._heldScope = new Scope(ast, _symTab._current);
    _symTab.push(ast._heldScope);
    ast._type.accept(this);
    for (Symbol formal : ast._type._formals._heldScope._symbols.values()) {
      FieldSym field = (FieldSym)formal;
      VarSym var = new VarSym(ast._heldScope, field._def);
      _symTab.def(var);
    }
    ast._body._needsScope = false;
    ast._body.accept(this);
    _symTab.pop(ast._heldScope);
    return null;
  }

  // ---------------- types ----------------
  Object visit(RecordType ast) {
    ast._heldScope = new Scope(ast, _symTab._current);
    _symTab.push(ast._heldScope);
    for (FieldType f : ast._fields)
      f.accept(this);
    _symTab.pop(ast._heldScope);
    return null;
  }

  Object visit(FieldType ast) {
    FieldSym sym = new FieldSym(_symTab._current, ast);
    def(sym);
    ast._field.accept(this);
    ast._type.accept(this);
    return null;
  }

  // ---------------- statements ----------------
  Object visit(VarDef ast) {
    VarSym sym = new VarSym(_symTab._current, ast);
    def(sym);
    ast._var.accept(this);
    ast._rhs.accept(this);
    return null;
  }

  Object visit(BlockStmt ast) {
    if (ast._needsScope) {
      ast._heldScope = new Scope(ast, _symTab._current);
      _symTab.push(ast._heldScope);
    }
    for (Stmt s : ast._stmts) {
      if (s instanceof BlockStmt)
	((BlockStmt)s)._needsScope = true;
      s.accept(this);
    }
    if (ast._needsScope)
      _symTab.pop(ast._heldScope);
    return null;
  }

  Object visit(ForStmt ast) {
    ast._heldScope = new Scope(ast, _symTab._current);
    _symTab.push(ast._heldScope);
    VarSym sym = new VarSym(_symTab._current, ast);
    def(sym);
    ast._var.accept(this);
    ast._expr.accept(this);
    ast._body._needsScope = false;
    ast._body.accept(this);
    _symTab.pop(ast._heldScope);
    return null;
  }

  Object visit(IfStmt ast) {
    ast._thenBranch._needsScope = true;
    return super.visit(ast);
  }

  Object visit(WhileStmt ast) {
    ast._body._needsScope = true;
    return super.visit(ast);
  }

  // ---------------- literals ----------------
  Object visit(RecordLit ast) {
    ast._heldScope = new Scope(ast, _symTab._current);
    _symTab.push(ast._heldScope);
    for (FieldLit f : ast._fields)
      f.accept(this);
    _symTab.pop(ast._heldScope);
    return null;
  }

  Object visit(FieldLit ast) {
    FieldSym sym = new FieldSym(_symTab._current, ast);
    def(sym);
    ast._field.accept(this);
    ast._expr.accept(this);
    return null;
  }
}

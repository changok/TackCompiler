import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

//see aho_et_al_2007 Figures 6.19, 6.36, 6.37, 6.52 (pp. 379, 402, 404, 423)
class IRGenerator extends DepthFirstVisitor {
  SymbolTable _symTab;
  List<Label> _pendingLabels = new ArrayList<Label>();
  static final ConstantAddr TRUE_CONSTANT =
    new ConstantAddr(new BoolLit(null, "true"));
  ConstantAddr FALSE_CONSTANT =
    new ConstantAddr(new BoolLit(null, "false"));
  ConstantAddr ZERO_CONSTANT =
    new ConstantAddr(new IntLit(null, "0"));
  ConstantAddr ONE_CONSTANT =
    new ConstantAddr(new IntLit(null, "1"));

  IRGenerator(SymbolTable symTab) { _symTab = symTab; }

  private IRGenerator label(Label lbl) {
    assert null != lbl;
    _pendingLabels.add(lbl);
    return this;
  }

  private FunDef currentFunction() {
    for (Scope s = _symTab._current; null != s; s = s._parent)
      if (null != s._owner && s._owner instanceof FunDef)
        return (FunDef)s._owner;
    return null;
  }

  private IRGenerator gen(Instruction instr) {
    assert 0 == instr._labels.size();
    instr._labels.addAll(_pendingLabels);
    _pendingLabels.clear();
    currentFunction()._sym._instructions.add(instr);
    return this;
  }

  private String nextUniqueName(Map<String, Address> names, String base) {
    String result = base;
    int i = 0;
    while (names.containsKey(result)) {
      result = base + i;
      i++;
    }
    return result;
  }

  private Map<String, Address> findAddresses(Map<String, Address> result,
                                             Scope scope) {
    for (final String srcName : scope._symbols.keySet()) {
      Symbol sym = scope._symbols.get(srcName);
      if (sym instanceof VarSym) {
        VarSym varSym = (VarSym)sym;
        String tgtName = nextUniqueName(result, srcName);
        varSym._addr = new NameAddr(varSym, tgtName);
        result.put(tgtName, varSym._addr);
      }
    }
    for (final Scope child : scope._children)
      findAddresses(result, child);
    return result;
  }

  private Label newLabel() {
    FunDef fun = currentFunction();
    Map<String, Address> addresses = fun._sym._addresses;
    String tgtName = nextUniqueName(addresses, "L_" + fun._name._id);
    Label result = new Label(tgtName);
    addresses.put(tgtName, result);
    return result;
  }

  private TempAddr newTemp(Type type) {
    FunDef fun = currentFunction();
    Map<String, Address> addresses = fun._sym._addresses;
    String tgtName = nextUniqueName(addresses, "t");
    TempAddr result = new TempAddr(type, tgtName);
    addresses.put(tgtName, result);
    return result;
  }

  // ---------------- top-level ----------------
  Object visit(Program ast) {
    List<FunSym> result = new ArrayList<FunSym>(ast._functions.size());
    for (FunDef funAst : ast._functions) {
      FunSym funSym = (FunSym)funAst.accept(this);
      assert funSym == funAst._sym;
      result.add(funSym);
    }
    return result;
  }

  Object visit(FunDef ast) {
    _symTab.push(ast._heldScope);
    assert null == ast._sym._instructions && null == ast._sym._addresses;
    assert 0 == _pendingLabels.size();
    ast._sym._instructions = new ArrayList<Instruction>();
    ast._sym._addresses = findAddresses(new TreeMap<String, Address>(),
                                        ast._heldScope);
    ast._body._nextLabel = newLabel();
    ast._body.accept(this);
    label(ast._body._nextLabel);
    Type returnType = ast._type._returnType;
    if (returnType.equals(PrimitiveType.VOIDT))
      gen(new ReturnInstr(ast._loc, null));
    else
      gen(new ReturnInstr(ast._loc, ZERO_CONSTANT));
    assert 0 == _pendingLabels.size();
    _symTab.pop(ast._heldScope);
    return ast._sym;
  }

  // ---------------- statements ----------------
  Object visit(VarDef ast) {
    Address rhsAddr = (Address)ast._rhs.accept(this);
    Address lhsAddr = ast._sym._addr;
    gen(new CopyInstr(ast._loc, lhsAddr, rhsAddr));
    return null;
  }

  Object visit(AssignStmt ast) {
    Address rhsAddr = (Address)ast._rhs.accept(this);
    if (ast._lhs instanceof VarId) {
      Address lhsAddr = ((VarId)ast._lhs)._sym._addr;
      gen(new CopyInstr(ast._loc, lhsAddr, rhsAddr));
    } else if (ast._lhs instanceof SubscriptExpr) {
      SubscriptExpr lhsAst = (SubscriptExpr)ast._lhs;
      Address baseAddr = (Address)lhsAst._base.accept(this);
      Address subscriptAddr = (Address)lhsAst._subscript.accept(this);
      gen(new ArrWriteInstr(ast._loc, baseAddr, subscriptAddr, rhsAddr));
    } else if (ast._lhs instanceof FieldExpr) {
      FieldExpr lhsAst = (FieldExpr)ast._lhs;
      Address baseAddr = (Address)lhsAst._base.accept(this);
      FieldSym fieldSym = (FieldSym)lhsAst._field._sym;
      gen(new RecWriteInstr(ast._loc, baseAddr, fieldSym, rhsAddr));
    } else {
      assert false : "should never reach here";
    }
    return null;    
  }

  Object visit(BlockStmt ast) {
    if (null != ast._heldScope)
      _symTab.push(ast._heldScope);
    for (int i=0, n=ast._stmts.size(); i<(n-1); i++) {
      Stmt s = ast._stmts.get(i);
      s._nextLabel = newLabel();
      s.accept(this);
      label(s._nextLabel);
    }
    if (0 < ast._stmts.size()) {
      Stmt s = ast._stmts.get(ast._stmts.size() - 1);
      s._nextLabel = ast._nextLabel;
      s.accept(this);
    }
    if (null != ast._heldScope)
      _symTab.pop(ast._heldScope);
    return null;
  }

  Object visit(CallStmt ast) {
    ast._expr.accept(this);
    return null;
  }

  Object visit(ForStmt ast) {
    _symTab.push(ast._heldScope);
    Address arrayAddr = (Address)ast._expr.accept(this);
    Address sizeAddr = newTemp(PrimitiveType.INTT);
    gen(new ParamInstr(ast._loc, arrayAddr, 0, 1));
    gen(new CallInstr(ast._loc, sizeAddr, Intrinsics.get(_symTab, "size"), 1));
    Address indexAddr = newTemp(PrimitiveType.INTT);
    gen(new CopyInstr(ast._loc, indexAddr, ZERO_CONSTANT));
    Label begin = newLabel();
    label(begin);
    ast._body._nextLabel = newLabel();
    gen(new RelopJumpInstr(ast._loc,">=", indexAddr, sizeAddr,ast._nextLabel));
    Address varAddr = ast._var._sym._addr;
    gen(new ArrReadInstr(ast._loc, varAddr, arrayAddr, indexAddr));
    ast._body.accept(this);
    label(ast._body._nextLabel);
    gen(new InfixInstr(ast._loc, "+", indexAddr, indexAddr, ONE_CONSTANT));
    gen(new UncondJumpInstr(ast._loc, begin));
    _symTab.pop(ast._heldScope);
    return null;
  }

  Object visit(IfStmt ast) {
    if (null == ast._elseBranch) {
      ast._cond._trueLabel = newLabel();
      ast._cond._falseLabel = ast._nextLabel;
      ast._thenBranch._nextLabel = ast._nextLabel;
      ast._cond.accept(this);
      label(ast._cond._trueLabel);
      ast._thenBranch.accept(this);
    } else {
      ast._cond._trueLabel = newLabel();
      ast._cond._falseLabel = newLabel();
      ast._thenBranch._nextLabel = ast._nextLabel;
      ast._elseBranch._nextLabel = ast._nextLabel;
      ast._cond.accept(this);
      label(ast._cond._trueLabel);
      ast._thenBranch.accept(this);
      gen(new UncondJumpInstr(ast._loc, ast._nextLabel));
      label(ast._cond._falseLabel);
      ast._elseBranch.accept(this);
    }
    return null;
  }

  Object visit(ReturnStmt ast) {
    Address valAddr = null;
    if (null != ast._expr)
      valAddr = (Address)ast._expr.accept(this);
    gen(new ReturnInstr(ast._loc, valAddr));
    return null;
  }

  Object visit(WhileStmt ast) {
    assert null != ast._nextLabel;
    ast._cond._trueLabel = newLabel();
    ast._cond._falseLabel = ast._nextLabel;
    ast._body._nextLabel = newLabel();
    label(ast._body._nextLabel);
    ast._cond.accept(this);
    label(ast._cond._trueLabel);
    ast._body.accept(this);
    gen(new UncondJumpInstr(ast._loc, ast._body._nextLabel));
    return null;
  }

  // ---------------- expressions ----------------
  private Address wrapJumpingCode(Expr ast) {
    assert ast._type.equals(PrimitiveType.BOOLT);
    assert null == ast._trueLabel && null == ast._falseLabel;
    Address outAddr = newTemp(ast._type);
    ast._trueLabel = newLabel();
    ast._falseLabel = newLabel();
    gen(new CopyInstr(ast._loc, outAddr, TRUE_CONSTANT));
    ast.accept(this); //recursive call to generate wrapped code
    label(ast._falseLabel);
    gen(new CopyInstr(ast._loc, outAddr, FALSE_CONSTANT));
    label(ast._trueLabel);
    ast._trueLabel = null;
    ast._falseLabel = null;
    return outAddr;
  }

  private Address wrapValueCode(Expr ast) {
    assert ast._type.equals(PrimitiveType.BOOLT);
    assert null != ast._trueLabel && null != ast._falseLabel;
    Label trueLabel = ast._trueLabel, falseLabel = ast._falseLabel;
    ast._trueLabel = null;
    ast._falseLabel = null;
    Address condAddr = (Address)ast.accept(this);
    ast._trueLabel = trueLabel;
    ast._falseLabel = falseLabel;
    gen(new TrueJumpInstr(ast._loc, condAddr, trueLabel));
    gen(new UncondJumpInstr(ast._loc, falseLabel));
    return null;
  }

  Object visit(InfixExpr ast) {
    if ("||".equals(ast._op)) {
      if (null == ast._trueLabel)
        return wrapJumpingCode(ast);
      ast._lhs._trueLabel = ast._trueLabel;
      ast._lhs._falseLabel = newLabel();
      ast._rhs._trueLabel = ast._trueLabel;
      ast._rhs._falseLabel = ast._falseLabel;
      ast._lhs.accept(this);
      label(ast._lhs._falseLabel);
      ast._rhs.accept(this);
      return null;
    } else if ("&&".equals(ast._op)) {
      if (null == ast._trueLabel)
        return wrapJumpingCode(ast);
      ast._lhs._trueLabel = newLabel();
      ast._lhs._falseLabel = ast._falseLabel;
      ast._rhs._trueLabel = ast._trueLabel;
      ast._rhs._falseLabel = ast._falseLabel;
      ast._lhs.accept(this);
      label(ast._lhs._trueLabel);
      ast._rhs.accept(this);
      return null;
    } else if ("==".equals(ast._op) || "!=".equals(ast._op) ||
               "<=".equals(ast._op) || "<".equals(ast._op) ||
               ">=".equals(ast._op) || ">".equals(ast._op)) {
      if (null == ast._trueLabel)
        return wrapJumpingCode(ast);
      Address lhsAddr = (Address)ast._lhs.accept(this);
      Address rhsAddr = (Address)ast._rhs.accept(this);
      gen(new RelopJumpInstr(ast._loc,ast._op,lhsAddr,rhsAddr,ast._trueLabel));
      gen(new UncondJumpInstr(ast._loc, ast._falseLabel));
    } else if ("+".equals(ast._op)) {
      if (ast._lhs._type.equals(PrimitiveType.STRINGT)) {
        assert PrimitiveType.STRING == ((PrimitiveType)ast._rhs._type)._name;
        Address lhsAddr = (Address)ast._lhs.accept(this);
        Address rhsAddr = (Address)ast._rhs.accept(this);
        Address outAddr = newTemp(ast._type);
        gen(new ParamInstr(ast._lhs._loc, lhsAddr, 0, 2));
        gen(new ParamInstr(ast._rhs._loc, rhsAddr, 1, 2));
        gen(new CallInstr(ast._loc, outAddr, Intrinsics.get(_symTab, "append"), 2));
        return outAddr;
      } else {
        Address lhsAddr = (Address)ast._lhs.accept(this);
        Address rhsAddr = (Address)ast._rhs.accept(this);
        Address outAddr = newTemp(ast._type);
        gen(new InfixInstr(ast._loc, ast._op, outAddr, lhsAddr, rhsAddr));
        return outAddr;
      }
    } else if ("-".equals(ast._op) || "*".equals(ast._op) ||
               "/".equals(ast._op) || "%".equals(ast._op)) {
      Address lhsAddr = (Address)ast._lhs.accept(this);
      Address rhsAddr = (Address)ast._rhs.accept(this);
      Address outAddr = newTemp(ast._type);
      gen(new InfixInstr(ast._loc, ast._op, outAddr, lhsAddr, rhsAddr));      
      return outAddr;
    } else {
      assert false : ast._op;
    }
    return null;
  }

  Object visit(PrefixExpr ast) {
    if ("-".equals(ast._op)) {
      Address inAddr = (Address)ast._base.accept(this);
      Address outAddr = newTemp(ast._type);
      gen(new PrefixInstr(ast._loc, ast._op, outAddr, inAddr));
      return outAddr;
    } else if ("!".equals(ast._op)) {
      if (null == ast._trueLabel)
        return wrapJumpingCode(ast);
      ast._base._falseLabel = ast._trueLabel;
      ast._base._trueLabel = ast._falseLabel;
      return ast._base.accept(this);
    } else {
      assert false : ast._op;
    }
    return null;
  }
    
  Object visit(CallExpr ast) {
    if (null != ast._trueLabel)
      return wrapValueCode(ast);
    List<Address> paramAddrs = new ArrayList<Address>(ast._actuals.size());
    for (Expr expr : ast._actuals) {
      Address inAddr = (Address)expr.accept(this);
      paramAddrs.add(inAddr);
    }
    boolean isVoid = ast._type.equals(PrimitiveType.VOIDT);
    Address outAddr = isVoid ? null : newTemp(ast._type);
    FunSym fun = ((FunId)ast._base)._sym;
    for (int index=0, arity=ast._actuals.size(); index<arity; index++) {
      Expr expr = ast._actuals.get(index);
      Address inAddr = paramAddrs.get(index);
      gen(new ParamInstr(expr._loc, inAddr, index, arity));
    }
    gen(new CallInstr(ast._loc, outAddr, fun, ast._actuals.size()));
    return outAddr;
  }

  Object visit(CastExpr ast) {
    if (null != ast._trueLabel)
      return wrapValueCode(ast);
    Address outAddr = newTemp(ast._type);
    Address inAddr = (Address)ast._base.accept(this);
    gen(new CastInstr(ast._loc, outAddr, inAddr, ast._type));
    return outAddr;
  }

  Object visit(FieldExpr ast) {
    if (null != ast._trueLabel)
      return wrapValueCode(ast);
    Address outAddr = newTemp(ast._type);
    Address inAddr = (Address)ast._base.accept(this);
    FieldSym field = (FieldSym)ast._field._sym;
    gen(new RecReadInstr(ast._loc, outAddr, inAddr, field));
    return outAddr;
  }

  Object visit(SubscriptExpr ast) {
    if (null != ast._trueLabel)
      return wrapValueCode(ast);
    Address outAddr = newTemp(ast._type);
    Address baseAddr = (Address)ast._base.accept(this);
    Address subscriptAddr = (Address)ast._subscript.accept(this);
    gen(new ArrReadInstr(ast._loc, outAddr, baseAddr, subscriptAddr));
    return outAddr;
  }

  Object visit(ParenExpr ast) {
    if (null != ast._trueLabel)
      return wrapValueCode(ast);
    Address outAddr = (Address)ast._base.accept(this);
    return outAddr;
  }

  // ---------------- identifiers ----------------
  Object visit(VarId ast) {
    if (null != ast._trueLabel)
      return wrapValueCode(ast);
    Address outAddr = ast._sym._addr;
    return outAddr;
  }

  // ---------------- literals ----------------
  Object visit(ArrayLit ast) {
    Address eSizeAddr = new SizeofAddr(ast._type);
    int n = ast._elems.size();
    ConstantAddr aSizeAddr =
      new ConstantAddr(new IntLit(null, Integer.toString(n)));
    aSizeAddr._literal._type = PrimitiveType.INTT;
    Address outAddr = newTemp(ast._type);
    gen(new ParamInstr(ast._loc, eSizeAddr, 0, 2));
    gen(new ParamInstr(ast._loc, aSizeAddr, 1, 2));
    gen(new CallInstr(ast._loc,outAddr,Intrinsics.get(_symTab,"newArray"), 2));
    for (int i=0; i<n; i++) {
      Expr elemAst = ast._elems.get(i); 
      Address subscriptAddr =
        new ConstantAddr(new IntLit(null, Integer.toString(i)));
      Address elemAddr = (Address)elemAst.accept(this);
      gen(new ArrWriteInstr(elemAst._loc, outAddr, subscriptAddr, elemAddr));
    }
    return outAddr;
  }

  Object visit(RecordLit ast) {
    _symTab.push(ast._heldScope);
    Address rSizeAddr = new SizeofAddr(ast._type);
    Address outAddr = newTemp(ast._type);
    gen(new ParamInstr(ast._loc, rSizeAddr, 0, 1));
    gen(new CallInstr(ast._loc,outAddr,Intrinsics.get(_symTab,"newRecord"),1));
    for (FieldLit fieldAst : ast._fields) {
      Address fieldAddr = (Address)fieldAst._expr.accept(this);
      gen(new RecWriteInstr(fieldAst._loc, outAddr, fieldAst._sym, fieldAddr));
    }
    _symTab.pop(ast._heldScope);
    return outAddr;
  }

  Object visit(BoolLit ast) {
    if (null == ast._trueLabel)
      return new ConstantAddr(ast);
    if (ast._value)
      gen(new UncondJumpInstr(ast._loc, ast._trueLabel));
    else
      gen(new UncondJumpInstr(ast._loc, ast._falseLabel));
    return null;
  }

  Object visit(IntLit ast) { return new ConstantAddr(ast); }

  Object visit(NullLit ast) { return new ConstantAddr(ast); }

  Object visit(StringLit ast) { return new ConstantAddr(ast); }
}

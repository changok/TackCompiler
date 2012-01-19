import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import xtc.tree.Location;

class SemanticAnalyzer extends DepthFirstVisitor {
  SymbolTable _symTab;
  static final NullType NULL_TYPE = new NullType();

  SemanticAnalyzer(SymbolTable symTab) { _symTab = symTab; }

  private static boolean knownType(Type type) {
    if (null == type)
      return false;
    if (type instanceof ArrayType)
      return knownType(((ArrayType)type)._elem);
    if (type instanceof RecordType)
      for (FieldType t : ((RecordType)type)._fields)
        if (!knownType(t))
          return false;
    if (type instanceof FieldType)
      return knownType(((FieldType)type)._type);
    if (type instanceof FunType)
      return knownType(((FunType)type)._formals)
        && knownType(((FunType)type)._returnType);
    return true;
  }

  private static boolean sameType(Type t1, Type t2) {
    if (t1 == null || t2 == null)
      return true;
    return t1.equals(t2);
  }

 private  static boolean subType(Type t1, Type t2) {
    if (sameType(t1, t2))
      return true;
    if (t1 instanceof NullType)
      return t2 instanceof NullType || t2 instanceof RecordType;
    if (t1.getClass() != t2.getClass())
      return false;
    if (t1 instanceof PrimitiveType)
      return ((PrimitiveType)t1)._name == ((PrimitiveType)t2)._name;
    if (t1 instanceof ArrayType)
      return sameType(((ArrayType)t1)._elem, ((ArrayType)t2)._elem);
    if (t1 instanceof RecordType) {
      RecordType r1 = (RecordType)t1, r2 = (RecordType)t2;
      if (r1._fields.size() < r2._fields.size())
        return false;
      for (int i=0, n=r2._fields.size(); i<n; i++)
        if (!sameType(r1._fields.get(i), r2._fields.get(i)))
          return false;
      return true;
    }
    if (t1 instanceof FieldType)
      return sameType(t1, t2);
    if (t1 instanceof FunType) {
      FunType f1 = (FunType)t1, f2 = (FunType)t2;
      return subType(f2._formals, f1._formals)
        && subType(f1._returnType, f2._returnType);
    }
    assert false : "unimplemented case?";
    return false;
  }

  private static boolean castable(Type t1, Type t2) {
    if (subType(t1, t2))
      return true;
    if (t1 instanceof RecordType && t2 instanceof NullType)
      return true;
    if (t1.getClass() != t2.getClass())
      return false;
    if (t1 instanceof PrimitiveType)
      return true;
    if (t1 instanceof ArrayType)
      return sameType(((ArrayType)t1)._elem, ((ArrayType)t2)._elem);
    if (t1 instanceof RecordType)
      return subType(t1, t2) || subType(t2, t1);
    if (t1 instanceof FieldType)
      return sameType(t1, t2);
    if (t1 instanceof FunType) {
      FunType f1 = (FunType)t1, f2 = (FunType)t2;
      return castable(f2._formals, f1._formals)
        && castable(f1._returnType, f2._returnType);
    }
    assert false : "unimplemented case?";
    return false;
  }

  // ---------------- top-level ----------------
  Object visit(FunDef ast) {
    _symTab.push(ast._heldScope);
    super.visit(ast);
    _symTab.pop(ast._heldScope);
    return ast._type;
  }

  // ---------------- types ----------------
  Object visit(RecordType ast) {
    _symTab.push(ast._heldScope);
    for (FieldType f : ast._fields)
      f.accept(this);
    _symTab.pop(ast._heldScope);
    return null;
  }

  // ---------------- statements ----------------
  Object visit(VarDef ast) {
    Type type = (Type)ast._rhs.accept(this);
    if (!knownType(type))
      ErrorPrinter.print(ast._loc, "Could not resolve type for variable '"
                         + ast._var._id + "'");
    return null;
  }

  Object visit(AssignStmt ast) {
    Type lhsType = (Type)ast._lhs.accept(this);
    Type rhsType = (Type)ast._rhs.accept(this);
    if (ast._lhs instanceof VarId
        || ast._lhs instanceof SubscriptExpr
        || ast._lhs instanceof FieldExpr) {
      if (!sameType(rhsType, lhsType)) {
        if (subType(rhsType, lhsType))
          ast._rhs = new CastExpr(ast._rhs, lhsType);
        else
          ErrorPrinter.print(ast._loc, "Cannot assign to '" + lhsType
                             + "' from '" + rhsType + "'");
      }
    } else {
      ErrorPrinter.print(ast._loc, "Assignment to immutable expression");
    }
    return null;
  }

  Object visit(BlockStmt ast) {
    if (null != ast._heldScope)
      _symTab.push(ast._heldScope);
    for (Stmt s : ast._stmts)
      s.accept(this);
    if (null != ast._heldScope)
      _symTab.pop(ast._heldScope);
    return null;
  }

  Object visit(ForStmt ast) {
    _symTab.push(ast._heldScope);
    String varName = ast._var._id;
    Type exprType = (Type)ast._expr.accept(this);
    if (knownType(exprType)) {
      if (!(exprType instanceof ArrayType))
        ErrorPrinter.print(ast._loc, "Subject of for-loop must be array");
    } else {
      ErrorPrinter.print(ast._loc, "Could not resolve type for variable '"
                         + varName + "'");
    }
    ast._body.accept(this);
    _symTab.pop(ast._heldScope);
    return null;
  }

  Object visit(IfStmt ast) {
    Type condType = (Type)ast._cond.accept(this);
    if (!sameType(condType, PrimitiveType.BOOLT))
      ErrorPrinter.print(ast._cond._loc, "Boolean expected");
    ast._thenBranch.accept(this);
    if (null != ast._elseBranch)
      ast._elseBranch.accept(this);
    return null;
  }

  Object visit(ReturnStmt ast) {
    Type act = PrimitiveType.VOIDT;
    if (null != ast._expr)
      act = (Type)ast._expr.accept(this);
    FunDef fun = null;
    for (Scope s = _symTab._current; null != s; s = s._parent)
      if (null != s._owner && s._owner instanceof FunDef)
        fun = (FunDef)s._owner;
    Type frm = fun._type._returnType;
    if (!sameType(act, frm)) {
      if (subType(act, frm))
        ast._expr = new CastExpr(ast._expr, frm);
      else
        ErrorPrinter.print(ast._loc, "Expected return value of type '"
                           + frm + "', found '" + act + "'");
    }
    return null;
  }

  Object visit(WhileStmt ast) {
    Type condType = (Type)ast._cond.accept(this);
    if (!sameType(condType, PrimitiveType.BOOLT))
      ErrorPrinter.print(ast._cond._loc, "Boolean expected");
    ast._body.accept(this);
    return null;
  }

  // ---------------- expressions ----------------
  Object visit(InfixExpr ast) {
    Type lhsType = (Type)ast._lhs.accept(this);
    Type rhsType = (Type)ast._rhs.accept(this);
    if ("||".equals(ast._op) || "&&".equals(ast._op)) {
      ast._type = PrimitiveType.BOOLT;
      if (!sameType(lhsType, PrimitiveType.BOOLT))
        ErrorPrinter.print(ast._lhs._loc, "Boolean expected");
      if (!sameType(rhsType, PrimitiveType.BOOLT))
        ErrorPrinter.print(ast._rhs._loc, "Boolean expected");
    } else if ("==".equals(ast._op) || "!=".equals(ast._op)) {
      ast._type = PrimitiveType.BOOLT;
      if (!sameType(lhsType, rhsType)) {
        if ((lhsType instanceof NullType || lhsType instanceof RecordType) &&
            castable(lhsType, rhsType)) {
          //no explicit conversion required to compare pointers to records
        } else {
          ErrorPrinter.print(ast._loc, "Cannot compare '"
                             + lhsType + "' and '" + rhsType + "'");
        }
      }
    } else if ("<=".equals(ast._op) || "<".equals(ast._op)
               || ">=".equals(ast._op) || ">".equals(ast._op)) {
      ast._type = PrimitiveType.BOOLT;
      if (!sameType(lhsType, PrimitiveType.INTT))
        ErrorPrinter.print(ast._lhs._loc, "Integer expected");
      if (!sameType(rhsType, PrimitiveType.INTT))
        ErrorPrinter.print(ast._rhs._loc, "Integer expected");
    } else if ("+".equals(ast._op)) {
      if (sameType(lhsType, PrimitiveType.STRINGT) ||
	  sameType(rhsType, PrimitiveType.STRINGT)) {
        ast._type = PrimitiveType.STRINGT;
        if (!sameType(lhsType, PrimitiveType.STRINGT)) {
          if (castable(lhsType, PrimitiveType.STRINGT))
            ast._lhs = new CastExpr(ast._lhs, PrimitiveType.STRINGT);
          else
            ErrorPrinter.print(ast._lhs._loc, "Cannot convert from type '"
                               + lhsType + "' to type 'string'");
        }
        if (!sameType(rhsType, PrimitiveType.STRINGT)) {
          if (castable(rhsType, PrimitiveType.STRINGT))
            ast._rhs = new CastExpr(ast._rhs, PrimitiveType.STRINGT);
          else
            ErrorPrinter.print(ast._rhs._loc, "Cannot convert from type '"
                               + rhsType + "' to type 'string'");
        }
      } else {
        ast._type = PrimitiveType.INTT;
        if (!sameType(lhsType, PrimitiveType.INTT))
          ErrorPrinter.print(ast._lhs._loc, "Integer expected");
        if (!sameType(rhsType, PrimitiveType.INTT))
          ErrorPrinter.print(ast._rhs._loc, "Integer expected");
      }
    } else if ("-".equals(ast._op) || "*".equals(ast._op)
               || "/".equals(ast._op) || "%".equals(ast._op)) {
      ast._type = PrimitiveType.INTT;
      if (!sameType(lhsType, PrimitiveType.INTT))
        ErrorPrinter.print(ast._lhs._loc, "Integer expected");
      if (!sameType(rhsType, PrimitiveType.INTT))
        ErrorPrinter.print(ast._rhs._loc, "Integer expected");
    } else {
      assert false : ast._op;
    }
    return ast._type;
  }

  Object visit(PrefixExpr ast) {
    Type baseType = (Type)ast._base.accept(this);
    if ("-".equals(ast._op)) {
      ast._type = PrimitiveType.INTT;
      if (!sameType(baseType, PrimitiveType.INTT))
        ErrorPrinter.print(ast._base._loc, "Integer expected");
    } else if ("!".equals(ast._op)) {
      ast._type = PrimitiveType.BOOLT;
      if (!sameType(baseType, PrimitiveType.BOOLT))
        ErrorPrinter.print(ast._base._loc, "Boolean expected");
    } else {
      assert false : ast._op;
    }
    return ast._type;
  }

  Object visit(CallExpr ast) {
    FunSym callee = null;
    if (ast._base instanceof FunId)
      callee = (FunSym)ast._base.accept(this);
    else
      ErrorPrinter.print(ast._loc, "Function name must be simple identifier");
    List<Type> actuals = new ArrayList<Type>();
    for (Expr expr : ast._actuals)
      actuals.add((Type)expr.accept(this));
    if (null != callee) {
      FunType funType = callee._def._type;
      ast._type = funType._returnType;
      List<FieldType> formals = funType._formals._fields;
      if (actuals.size() == formals.size())
        for (int i=0, n=formals.size(); i<n; i++) {
          Type act = actuals.get(i), frm = formals.get(i)._type;
          if (!sameType(act, frm)) {
            if (subType(act, frm))
              ast._actuals.set(i, new CastExpr(ast._actuals.get(i), frm));
            else if (Intrinsics.get(_symTab, "size") == callee &&
                     act instanceof ArrayType)
              /*allow any array type for generic intrinsic*/;
            else
              ErrorPrinter.print(ast._actuals.get(i)._loc,
                                 "Formal '" + formals.get(i)._field._id
                                 + "' of function '" + callee.name()
                                 + "' expects '" + frm + "', found '"
                                 + act + "' instead");
          }
        }
      else
        ErrorPrinter.print(ast._loc, "Function '" + callee.name() + "' has "
                           + formals.size() + " formals, but there are "
                           + actuals.size() + " actuals");
    }
    return ast._type;
  }

  Object visit(CastExpr ast) {
    Type srcType = (Type)ast._base.accept(this);
    Type tgtType = ast._targetType;
    ast._type = ast._targetType;
    if (!castable(srcType, tgtType))
      ErrorPrinter.print(ast._loc, "Cannot cast from type '" + srcType
                         + "' to type '" + tgtType + "'");
    return ast._type;
  }

  Object visit(FieldExpr ast) {
    Type baseType = (Type)ast._base.accept(this);
    if (knownType(baseType)) {
      if (baseType instanceof RecordType) {
        Scope scope = ((RecordType)baseType)._heldScope;
        String name = ast._field._id;
        if (scope.contains(name)) {
          FieldSym sym = (FieldSym)scope.get(name);
          ast._field._sym = sym;
          ast._type = sym.type()._type;
        } else {
          ErrorPrinter.print(ast._field._loc, "Unknown field '" + name + "'");
        }
      } else {
        ErrorPrinter.print(ast._loc, "Base of field expression must be record");
      }
    }
    return ast._type;
  }

  Object visit(SubscriptExpr ast) {
    Type baseType = (Type)ast._base.accept(this);
    Type subscriptType = (Type)ast._subscript.accept(this);
    if (null != baseType) {
      if (baseType instanceof ArrayType)
        ast._type = ((ArrayType)baseType)._elem;
      else
        ErrorPrinter.print(ast._loc, "Base of subscript must be array");
    }
    if (null != subscriptType) {
      if (!sameType(subscriptType, PrimitiveType.INTT))
        ErrorPrinter.print(ast._subscript._loc, "Integer expected");
    }
    return ast._type;
  }

  Object visit(ParenExpr ast) {
    ast._type = (Type)ast._base.accept(this);
    return ast._type;
  }

  // ---------------- identifiers ----------------
  Object visit(FieldId ast) {
    Symbol s = _symTab.lookup(ast._id);
    if (null == s)
      ErrorPrinter.print(ast._loc, "Unknown field '" + ast._id + "'");
    else if (!(s instanceof FieldSym))
      ErrorPrinter.print(ast._loc, "Field name expected");
    else
      ast._sym = s;
    return null == ast._sym ? null : ast._sym.type();
  }

  Object visit(FunId ast) {
    Symbol s = _symTab.lookup(ast._id);
    if (null == s)
      ErrorPrinter.print(ast._loc, "Unknown function '" + ast._id + "'");
    else if (!(s instanceof FunSym))
      ErrorPrinter.print(ast._loc, "Function name expected");
    else
      ast._sym = (FunSym)s;
    return null == ast._sym ? null : ast._sym;
  }

  Object visit(VarId ast) {
    Symbol s = _symTab.lookup(ast._id);
    if (null == s) {
      ErrorPrinter.print(ast._loc, "Unknown variable '" + ast._id + "'");
    } else if (!(s instanceof VarSym)) {
      ErrorPrinter.print(ast._loc, "Variable name expected");
    } else {
      ast._sym = (VarSym)s;
      ast._type = ast._sym.type();
    }
    return ast._type;
  }

  // ---------------- literals ----------------
  Object visit(ArrayLit ast) {
    Type elemType = null;
    for (Expr expr : ast._elems) {
      Type typ = (Type)expr.accept(this);
      if (knownType(typ)) {
        if (knownType(elemType)) {
          if (!sameType(typ, elemType))
            ErrorPrinter.print(expr._loc,
                               "Expected element of type '" + elemType +
                               "', found '" + typ + "'");
        } else {
          elemType = typ;
        }
      } else {
        ErrorPrinter.print(expr._loc, "Could not resolve array element type");
      }
    }
    ast._type = new ArrayType(ast._loc, elemType);
    return ast._type;
  }

  Object visit(RecordLit ast) {
    _symTab.push(ast._heldScope);
    List<FieldType> fieldTypes = new ArrayList<FieldType>();
    for (FieldLit lit : ast._fields)
      fieldTypes.add((FieldType)lit.accept(this));
    boolean anyNull = false;
    for (FieldType typ : fieldTypes)
      if (null == typ)
        anyNull = true;
    if (!anyNull) {
      RecordType result = new RecordType(ast._loc, fieldTypes);
      result._heldScope = ast._heldScope;
      ast._type = result;
    }
    _symTab.pop(ast._heldScope);
    return ast._type;
  }

  Object visit(FieldLit ast) {
    ast._field.accept(this);
    Type type = (Type)ast._expr.accept(this);
    if (knownType(type))
      ast._type = new FieldType(ast._loc, ast._field, type);
    else
      ErrorPrinter.print(ast._loc, "Could not resolve type for field '"
                         + ast._field._id + "'");
    return ast._type;
  }

  Object visit(BoolLit ast) {
    ast._type = new PrimitiveType(ast._loc, PrimitiveType.BOOL);
    return ast._type;
  }

  Object visit(IntLit ast) {
    ast._type = new PrimitiveType(ast._loc, PrimitiveType.INT);
    return ast._type;
  }

  Object visit(NullLit ast) {
    ast._type = NULL_TYPE;
    return ast._type;
  }

  Object visit(StringLit ast) {
    ast._type = new PrimitiveType(ast._loc, PrimitiveType.STRING);
    return ast._type;
  }
}

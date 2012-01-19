import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import xtc.tree.Location;

abstract class Symbol {
  Scope _scope;
  Symbol(Scope scope) { _scope = scope;}
  abstract Location loc();
  abstract String name();
  abstract Type type();
}

class FieldSym extends Symbol {
  AstNode _def;
  FieldSym(Scope scope, AstNode def) {
    super(scope);
    _def = def;
    if (def instanceof FieldLit)
      ((FieldLit)def)._sym = this;
    else
      ((FieldType)def)._sym = this;
  }
  Location loc() { return _def._loc; }
  static String name(AstNode def) {
    if (def instanceof FieldLit)
      return ((FieldLit)def)._field._id;
    return ((FieldType)def)._field._id;
  }
  String name() { return name(_def); }
  static FieldType type(AstNode def) {
    if (def instanceof FieldLit)
      return ((FieldLit)def)._type;
    return (FieldType)def;
  }
  FieldType type() { return type(_def); }
}

class FunSym extends Symbol {
  FunDef _def;
  List<Instruction> _instructions;
  Map<String, Address> _addresses;
  FunSym(Scope scope, FunDef def) {
    super(scope);
    _def = def;
    _instructions = null;
    _addresses = null;
    def._sym = this;
  }
  Location loc() { return _def._loc; }
  static String name(FunDef def) { return def._name._id; }
  String name() { return name(_def); }
  FunType type() { return _def._type; }
}

class VarSym extends Symbol {
  AstNode _def;
  NameAddr _addr;
  VarSym(Scope scope, AstNode def) {
    super(scope);
    if (def instanceof FieldType) {
      FieldType f = (FieldType)def;
      f._sym = this;
      f._field._sym = this;
    } else if (def instanceof ForStmt) {
      ForStmt f = (ForStmt)def;
      f._sym = this;
      f._var._sym = this;
    } else {
      VarDef v = (VarDef)def;
      v._sym = this;
      v._var._sym = this;
    }
    _def = def;
    _addr = null;
  }
  Location loc() { return _def._loc; }
  static String name(AstNode def) {
    if (def instanceof FieldType)
      return ((FieldType)def)._field._id;
    if (def instanceof ForStmt)
      return ((ForStmt)def)._var._id;
    else
      return ((VarDef)def)._var._id;
  }
  String name() { return name(_def); }
  static Type type(AstNode def) {
    if (def instanceof FieldType) {
      return ((FieldType)def)._type;
    } else if (def instanceof ForStmt) {
      Type arrayType = ((ForStmt)def)._expr._type;
      if (null != arrayType && arrayType instanceof ArrayType)
        return ((ArrayType)arrayType)._elem;
      return null;
    } else {
      return ((VarDef)def)._rhs._type;
    }
  }
  Type type() { return type(_def); }
}

class SymbolTable {
  Scope _topLevel;
  Scope _current;
  SymbolTable() {
    _topLevel = new Scope(null, null);
    _current = _topLevel;
  }
  Scope pop(Scope scope) {
    assert _current == scope && _current != _topLevel;
    _current = scope._parent;
    return _current;
  }
  void push(Scope scope) {
    assert _current == scope._parent : _current + " not parent of " + scope;
    _current = scope;
  }
  boolean contains(String key) { return _current.contains(key); }
  void def(Symbol sym) { _current.def(sym); }
  Symbol get(String key) { return _current.get(key); }
  Symbol lookup(String key) {
    for (Scope s = _current; null != s; s = s._parent)
      if (s.contains(key))
        return s.get(key);
    return null;
  }
  PrintWriter print(PrintWriter w) {
    _topLevel.print(w, 0);
    w.println();
    return w;
  }
}

class Scope implements Comparable<Scope> {
  AstNode _owner;
  Scope _parent;
  Set<Scope> _children;
  Map<String, Symbol> _symbols;
  List<ConstantAddr> _rodatas;	// for assembly code
  Scope(AstNode owner, Scope parent) {
    _owner = owner;
    _parent = parent;
    if (null != parent)
      _parent._children.add(this);
    _children = new TreeSet<Scope>();
    _symbols = new TreeMap<String, Symbol>();
    
    _rodatas = new ArrayList<ConstantAddr>();
  }
  public int compareTo(Scope other) {
    if (null == _owner)
      if (null == other._owner)
        return hashCode() - other.hashCode();
      else
        return -1;
    else
      if (null == other._owner)
        return +1;
      else
        return _owner._loc.compareTo(other._owner._loc);    
  }
  boolean contains(String key) { return _symbols.containsKey(key); }
  void def(Symbol sym) {
    String key = sym.name();
    assert !_symbols.containsKey(key);
    _symbols.put(key, sym);
  }
  Symbol get(String key) { return _symbols.get(key); }
  
  PrintWriter print(PrintWriter w, int indent) {
    for (int i=0; i<indent; i++)
      w.print("  ");
    w.print("Scope(");
    if (null != _owner) {
      w.print("owner " + _owner.getClass().getSimpleName());
      if (_owner instanceof FunDef)
        w.print(" " + ((FunDef)_owner)._name._id);
      else if (_owner instanceof ForStmt)
        w.print(" " + ((ForStmt)_owner)._var._id);
      w.print(", ");
    }
    w.print("symbols (");
    boolean first = true;
    for (Symbol s : _symbols.values()) {
      if (first) first = false;
      else w.print(", ");
      w.print(s.name());
    }
    w.print(")");
    for (Scope s : _children) {
      w.println(",");
      s.print(w, indent + 1);
    }
    w.print(")");
    return w;
  }
  public String toString() {
    StringWriter w = new StringWriter();
    print(new PrintWriter(w), 0);
    return w.toString();
  }
}

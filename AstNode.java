import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import xtc.tree.Location;

abstract class AstNode {
  Location _loc;
  AstNode(Location loc) { _loc = loc; }
  abstract Object accept(Visitor visitor);
  public String toString() {
    StringWriter w = new StringWriter();
    PrettyPrinter prettyPrinter = new PrettyPrinter(new PrintWriter(w));
    this.accept(prettyPrinter);
    return w.toString();
  }
}

// ---------------- top-level ----------------
class Program extends AstNode {
  FunDefListHead _raw;
  List<FunDef> _functions;
  Program(Location loc, FunDefListHead raw) {
    super(loc); _raw = raw; _functions = null;
  }
  Program(Location loc, List<FunDef> functions) {
    super(loc); _raw = null; _functions = functions;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
  Object accept(IRVisitor visitor) { return visitor.visit(this); }
}

class FunDef extends AstNode {
  FunId _name;
  FunType _type;
  BlockStmt _body;
  Scope _heldScope;
  FunSym _sym;
  //int _frameSize;
  CCStack _stack;
  FunDef(Location loc, FunId name, FunType type, BlockStmt body) {
    super(loc); _name = name; _type = type; _body = body;
    _heldScope = null; _sym = null;
  }
  FunDef(Location loc, FunId name, FunType type, List<Instruction> instrs) {
    super(loc); _name = name; _type = type; _body = null;
    _sym = new FunSym(null, this);
    _sym._instructions = instrs;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
  Object accept(IRVisitor visitor) { return visitor.visit(this); }
}

class FunDefListHead extends AstNode {
  FunDef _first;
  FunDefListTail _tail;
  FunDefListHead(Location loc) { super(loc); _first = null; _tail = null; }
  FunDefListHead(Location loc,FunDef first,FunDefListTail tail) {
    super(loc); _first = first; _tail = tail;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}
class FunDefListTail extends AstNode {
  List<FunDef> _inh;
  FunDef _next;
  FunDefListTail _tail;
  FunDefListTail(Location loc) { super(loc); _next = null; _tail = null; }
  FunDefListTail(Location loc,FunDef next,FunDefListTail tail) {
    super(loc); _next = next; _tail = tail;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

// ---------------- types ----------------
abstract class Type extends AstNode {
  Type(Location loc) { super(loc); }
  public boolean equals(Object t2) {
    assert false : "subclass of Type must override equals() method: "
      + t2.getClass().getSimpleName();
    return false;
  }
}

class ArrayType extends Type {
  Type _elem;
  ArrayType(Location loc, Type elem) { super(loc); _elem = elem; }
  Object accept(Visitor visitor) { return visitor.visit(this); }
  public boolean equals(Object t2) {
    return t2 instanceof ArrayType && _elem.equals(((ArrayType)t2)._elem);
  }
}

class RecordType extends Type {
  FieldTypeListHead _raw;
  List<FieldType> _fields;
  Scope _heldScope;
  RecordType(Location loc, FieldTypeListHead raw) {
    super(loc); _raw = raw; _fields = null; _heldScope = null;
  }
  RecordType(Location loc, List<FieldType> fields) {
    super(loc); _raw = null; _fields = fields; _heldScope = null;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
  public boolean equals(Object t2) {
    if (!(t2 instanceof RecordType))
      return false;
    RecordType r2 = (RecordType)t2;
    if (_fields.size() != r2._fields.size())
      return false;
    for (int i=0, n=_fields.size(); i<n; i++)
      if (!_fields.get(i).equals(r2._fields.get(i)))
	return false;
    return true;
  }
}

class FieldTypeListHead extends AstNode {
  FieldType _first;
  FieldTypeListTail _tail;
  FieldTypeListHead(Location loc) { super(loc); _first = null; _tail = null; }
  FieldTypeListHead(Location loc, FieldType first, FieldTypeListTail tail) {
    super(loc); _first = first; _tail = tail;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}
class FieldTypeListTail extends AstNode {
  List<FieldType> _inh;
  FieldType _next;
  FieldTypeListTail _tail;
  FieldTypeListTail(Location loc) { super(loc); _next = null; _tail = null; }
  FieldTypeListTail(Location loc, FieldType next, FieldTypeListTail tail) {
    super(loc); _next = next; _tail = tail;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}  

class FieldType extends Type  {
  FieldId _field;
  Type _type;
  Symbol _sym;
  FieldType(Location loc, FieldId field, Type type) {
    super(loc); _field = field; _type = type; _sym = null;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
  public boolean equals(Object t2) {
    if (!(t2 instanceof FieldType))
      return false;
    FieldType f2 = (FieldType)t2;
    return _field._id.equals(f2._field._id) && _type.equals(f2._type);
  }
}

class NullType extends Type {
  NullType() { super(null); }
  Object accept(Visitor visitor) { return visitor.visit(this); }
  public boolean equals(Object t2) { return t2 instanceof NullType; }
}

class PrimitiveType extends Type {
  static final String BOOL = "bool".intern();
  static final PrimitiveType BOOLT = new PrimitiveType(null, BOOL);
  static final String INT = "int".intern();
  static final PrimitiveType INTT = new PrimitiveType(null, INT);
  static final String STRING = "string".intern();
  static final PrimitiveType STRINGT = new PrimitiveType(null, STRING);
  static final String VOID = "void".intern();
  static final PrimitiveType VOIDT = new PrimitiveType(null, VOID);
  String _name;
  PrimitiveType(Location loc, String name) {
    super(loc);
    assert BOOL == name || INT == name || STRING == name || VOID == name;
    _name = name;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
  public boolean equals(Object t2) {
    if (!(t2 instanceof PrimitiveType))
      return false;
    return _name.equals(((PrimitiveType)t2)._name);
  }
}

class FunType extends Type {
  RecordType _formals;
  Type _returnType;
  FunType(Location loc, RecordType formals, Type returnType) {
    super(loc); _formals = formals; _returnType = returnType;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
  public boolean equals(Object t2) {
    if (!(t2 instanceof FunType))
      return false;
    FunType f2 = (FunType)t2;
    return _formals.equals(f2._formals) && _returnType.equals(f2._returnType);
  }
}

// ---------------- statements ----------------
abstract class Stmt extends AstNode {
  Label _nextLabel;
  Stmt(Location loc) { super(loc); _nextLabel = null; }
}

class VarDef extends Stmt {
  VarId _var;
  Expr _rhs;
  VarSym _sym;
  VarDef(Location loc, VarId var, Expr rhs) {
    super(loc); _var = var; _rhs = rhs; _sym = null;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class AssignStmt extends Stmt {
  Expr _lhs;
  Expr _rhs;
  AssignStmt(Location loc, Expr lhs, Expr rhs) {
    super(loc); _lhs = lhs; _rhs = rhs;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class BlockStmt extends Stmt {
  StmtListHead _raw;
  List<Stmt> _stmts;
  boolean _needsScope;
  Scope _heldScope;
  BlockStmt(Location loc, StmtListHead raw) {
    super(loc); _raw=raw; _stmts=null; _needsScope=true; _heldScope=null;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class CallStmt extends Stmt {
  Expr _expr;
  CallStmt(Location loc, Expr expr) { super(loc); _expr = expr; }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class ForStmt extends Stmt {
  VarId _var;
  Expr _expr;
  BlockStmt _body;
  Scope _heldScope;
  VarSym _sym;
  ForStmt(Location loc, VarId var, Expr expr, BlockStmt body) {
    super(loc); _var = var; _expr = expr; _body = body;
    _heldScope = null; _sym = null;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class IfStmt extends Stmt {
  Expr _cond;
  BlockStmt _thenBranch;
  BlockStmt _elseBranch;
  IfStmt(Location loc, Expr cond, BlockStmt thenBranch, BlockStmt elseBranch) {
    super(loc); _cond=cond; _thenBranch=thenBranch; _elseBranch=elseBranch;
  }
  IfStmt(Location loc, Expr cond, BlockStmt thenBranch) {
    super(loc); _cond=cond; _thenBranch=thenBranch; _elseBranch=null;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class ReturnStmt extends Stmt {
  Expr _expr;
  ReturnStmt(Location loc, Expr expr) { super(loc); _expr = expr; }
  ReturnStmt(Location loc) { super(loc); _expr = null; }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class WhileStmt extends Stmt {
  Expr _cond;
  BlockStmt _body;
  WhileStmt(Location loc, Expr cond, BlockStmt body) {
    super(loc); _cond = cond; _body = body;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class StmtListHead extends AstNode {
  Stmt _first;
  StmtListTail _tail;
  StmtListHead(Location loc) { super(loc); _first = null; _tail = null; }
  StmtListHead(Location loc, Stmt first, StmtListTail tail) {
    super(loc); _first = first; _tail = tail;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}
class StmtListTail extends AstNode {
  List<Stmt> _inh;
  Stmt _next;
  StmtListTail _tail;
  StmtListTail(Location loc) { super(loc); _next = null; _tail = null; }
  StmtListTail(Location loc, Stmt next, StmtListTail tail) {
    super(loc); _next = next; _tail = tail;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

// ---------------- expressions ----------------
abstract class Expr extends AstNode {
  Type _type;
  Label _trueLabel, _falseLabel;
  Expr(Location loc) {
    super(loc); _type = null; _trueLabel = null; _falseLabel = null;
  }
}

class InfixExpr extends Expr {
  String _op;
  Expr _lhs;
  Expr _rhs;
  InfixExpr(Location loc, String op, Expr lhs, Expr rhs) {
    super(loc); _op = op; _lhs = lhs; _rhs = rhs;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}
class InfixExprHead extends Expr {
  Expr _lhs;
  InfixExprTail _tail;
  InfixExprHead(Location loc, Expr lhs, InfixExprTail tail) {
    super(loc); _lhs = lhs; _tail = tail;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}
class InfixExprTail extends AstNode {
  Expr _inh;
  String _op;
  Expr _rhs;
  InfixExprTail _tail;
  InfixExprTail(Location loc) {
    super(loc); _inh = null; _op = null; _rhs = null; _tail = null;
  }
  InfixExprTail(Location loc, String op, Expr rhs, InfixExprTail tail) {
    super(loc); _inh = null; _op = op; _rhs = rhs; _tail = tail;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class PrefixExpr extends Expr {
  String _op;
  Expr _base;
  PrefixExpr(Location loc, String op, Expr base) {
    super(loc); _op = op; _base = base;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class PostfixExprHead extends Expr {
  Expr _base;
  PostfixExprTail _tail;
  PostfixExprHead(Location loc, Expr base, PostfixExprTail tail) {
    super(loc); _base = base; _tail = tail;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}
class PostfixExprTail extends AstNode {
  Expr _inh;
  PostfixExprTail(Location loc) { super(loc); _inh = null; }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class CallExpr extends Expr {
  Expr _base;
  List<Expr> _actuals;
  CallExpr(Location loc, Expr base, List<Expr> actuals) {
    super(loc); _base = base; _actuals = actuals;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}
class CallExprTail extends PostfixExprTail {
  ExprListHead _actuals;
  PostfixExprTail _tail;
  CallExprTail(Location loc, ExprListHead actuals, PostfixExprTail tail) {
    super(loc); _actuals = actuals; _tail = tail;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class CastExpr extends Expr {
  Expr _base;
  Type _targetType;
  CastExpr(Location loc, Expr base, Type type) {
    super(loc); _base = base; _targetType = type;
  }
  CastExpr(Expr base, Type type) {
    this(base._loc, base, type);
    _type = _targetType;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}
class CastExprTail extends PostfixExprTail {
  Type _targetType;
  PostfixExprTail _tail;
  CastExprTail(Location loc, Type type, PostfixExprTail tail) {
    super(loc); _targetType = type; _tail = tail;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class FieldExpr extends Expr {
  Expr _base;
  FieldId _field;
  FieldExpr(Location loc, Expr base, FieldId field) {
    super(loc); _base = base; _field = field;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}
class FieldExprTail extends PostfixExprTail {
  FieldId _field;
  PostfixExprTail _tail;
  FieldExprTail(Location loc, FieldId field, PostfixExprTail tail) {
    super(loc); _field = field; _tail = tail;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class SubscriptExpr extends Expr {
  Expr _base;
  Expr _subscript;
  SubscriptExpr(Location loc, Expr base, Expr subscript) {
    super(loc); _base = base; _subscript = subscript;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}
class SubscriptExprTail extends PostfixExprTail {
  Expr _subscript;
  PostfixExprTail _tail;
  SubscriptExprTail(Location loc, Expr subscript, PostfixExprTail tail) {
    super(loc); _subscript = subscript; _tail = tail;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class ExprListHead extends AstNode {
  Expr _first;
  ExprListTail _tail;
  ExprListHead(Location loc) { super(loc); _first = null; _tail = null; }
  ExprListHead(Location loc, Expr first, ExprListTail tail) {
    super(loc); _first = first; _tail = tail;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}
class ExprListTail extends AstNode {
  List<Expr> _inh;
  Expr _next;
  ExprListTail _tail;
  ExprListTail(Location loc) { super(loc); _next = null; _tail = null; }
  ExprListTail(Location loc, Expr next, ExprListTail tail) {
    super(loc); _next = next; _tail = tail;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
} 

class ParenExpr extends Expr {
  Expr _base;
  ParenExpr(Location loc, Expr base) { super(loc); _base = base; }
  Object accept(Visitor visitor) { return visitor.visit(this); }
} 

// ---------------- identifiers ----------------
class FieldId extends AstNode {
  String _id;
  Symbol _sym;
  int _offset;
  FieldId(Location loc, String id) { super(loc); _id = id; _sym = null; _offset = -1;}
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class FunId extends Expr {
  String _id;
  FunSym _sym;
  FunId(Location loc, String id) { super(loc); _id = id; _sym = null; }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class VarId extends Expr {
  String _id;
  VarSym _sym;
  VarId(Location loc, String id) { super(loc); _id = id; }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

// ---------------- literals ----------------
class ArrayLit extends Expr {
  ExprListHead _raw;
  List<Expr> _elems;
  ArrayLit(Location loc, ExprListHead raw) {
    super(loc); _raw = raw; _elems = null;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class RecordLit extends Expr {
  FieldLitListHead _raw;
  List<FieldLit> _fields;
  Scope _heldScope;
  RecordLit(Location loc, FieldLitListHead raw) {
    super(loc); _raw = raw; _fields = null; _heldScope = null;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class FieldLitListHead extends AstNode {
  FieldLit _first;
  FieldLitListTail _tail;
  FieldLitListHead(Location loc) { super(loc); _first = null; _tail = null; }
  FieldLitListHead(Location loc, FieldLit first, FieldLitListTail tail) {
    super(loc); _first = first; _tail = tail;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}
class FieldLitListTail extends AstNode {
  List<FieldLit> _inh;
  FieldLit _next;
  FieldLitListTail _tail;
  FieldLitListTail(Location loc) { super(loc);  _next = null; _tail = null; }
  FieldLitListTail(Location loc, FieldLit next, FieldLitListTail tail) {
    super(loc); _next = next; _tail = tail;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class FieldLit extends AstNode {
  FieldId _field;
  Expr _expr;
  FieldSym _sym;
  FieldType _type;
  FieldLit(Location loc, FieldId field, Expr expr) {
    super(loc); _field = field; _expr = expr; _sym = null; _type = null;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class BoolLit extends Expr {
  boolean _value;
  BoolLit(Location loc, String token) {
    super(loc); _value = "true".equals(token);
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class IntLit extends Expr {
  int _value;
  IntLit(Location loc, String token) {
    super(loc); _value = Integer.parseInt(token);
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class NullLit extends Expr {
  NullLit(Location loc) { super(loc); }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

class StringLit extends Expr {
  String _token;
  StringLit(Location loc, String token) {
    super(loc); _token = token;
  }
  Object accept(Visitor visitor) { return visitor.visit(this); }
}

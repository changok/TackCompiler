import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import xtc.tree.Location;

abstract class Address {
  String _name;
  Address(String name) { _name = name; }
  abstract Type type();
  abstract Object accept(IRVisitor visitor);
}

class Label extends Address {
  Label(String name) { super(name); }
  Type type() { assert false; return null; }
  Object accept(IRVisitor visitor) { return visitor.visit(this); }
}

class NameAddr extends Address {
  VarSym _sym;
  int _offset;
  NameAddr(VarSym sym, String name) {
    super(name);
    assert null != name;
    _sym = sym;
    _offset = -1;
  }
  Type type() { return _sym.type(); }
  Object accept(IRVisitor visitor) { return visitor.visit(this); }
}

class ConstantAddr extends Address {
  Expr _literal;
  String _label;
  ConstantAddr(Expr literal) {
    super(null);
    _literal = literal;
    _label = null;
    assert literal instanceof BoolLit
      || literal instanceof IntLit
      || literal instanceof NullLit
      || literal instanceof StringLit;
  }
  Type type() { return _literal._type; }
  Object accept(IRVisitor visitor) { return visitor.visit(this); }
}

class TempAddr extends Address {
  Type _type;
  int _offset;
  TempAddr(Type type, String name) { super(name); _type = type; _offset = -1; }
  Type type() { return _type; }
  Object accept(IRVisitor visitor) { return visitor.visit(this); }
}

class SizeofAddr extends Address {
  Type _ofType;
  SizeofAddr(Type ofType) { super(null); _ofType = ofType; }
  Type type() { return PrimitiveType.INTT; }
  Object accept(IRVisitor visitor) { return visitor.visit(this); }
}

abstract class Instruction {
  Location _loc;
  List<Label> _labels;
  String _layout;
  Instruction(Location loc) { _loc = loc; _labels = new ArrayList<Label>(0); _layout = null;}
  abstract Object accept(IRVisitor visitor);
}

class CopyInstr extends Instruction {
  Address _out, _in;
  CopyInstr(Location loc, Address out, Address in) {
    super(loc); _out = out; _in = in;
  }
  Object accept(IRVisitor visitor) { return visitor.visit(this); }
}

class InfixInstr extends Instruction {
  String _op;
  Address _out, _lhs, _rhs;
  InfixInstr(Location loc, String op, Address out, Address lhs, Address rhs) {
    super(loc); _op = op; _out = out; _lhs = lhs; _rhs = rhs;
  }
  Object accept(IRVisitor visitor) { return visitor.visit(this); }
}

class PrefixInstr extends Instruction {
  String _op;
  Address _out, _in;
  PrefixInstr(Location loc, String op, Address out, Address in) {
    super(loc); _op = op; _out = out; _in = in;
  }
  Object accept(IRVisitor visitor) { return visitor.visit(this); }
}

class CastInstr extends Instruction {
  Address _out, _in;
  Type _type;
  CastInstr(Location loc, Address out, Address in, Type type) {
    super(loc); _out = out; _in = in; _type = type;
  }
  Object accept(IRVisitor visitor) { return visitor.visit(this); }
}

class UncondJumpInstr extends Instruction {
  Label _tgt;
  UncondJumpInstr(Location loc, Label tgt) {
    super(loc); _tgt = tgt;
  }
  Object accept(IRVisitor visitor) { return visitor.visit(this); }
}

class TrueJumpInstr extends Instruction {
  Address _cond;
  Label _tgt;
  TrueJumpInstr(Location loc, Address cond, Label tgt) {
    super(loc); _cond = cond; _tgt = tgt;
  }
  Object accept(IRVisitor visitor) { return visitor.visit(this); }
}

class FalseJumpInstr extends Instruction {
  Address _cond;
  Label _tgt;
  FalseJumpInstr(Location loc, Address cond, Label tgt) {
    super(loc); _cond = cond; _tgt = tgt;
  }
  Object accept(IRVisitor visitor) { return visitor.visit(this); }
}

class RelopJumpInstr extends Instruction {
  String _op;
  Address _lhs, _rhs;
  Label _tgt;
  RelopJumpInstr(Location loc, String op, Address lhs, Address rhs, Label tgt) {
    super(loc); _op = op; _lhs = lhs; _rhs = rhs; _tgt = tgt;
  }
  Object accept(IRVisitor visitor) { return visitor.visit(this); }
}

class ParamInstr extends Instruction {
  Address _in;
  int _index, _arity;
  ParamInstr(Location loc, Address in, int index, int arity) {
    super(loc);
    _in = in;
    assert 0 <= index && index < arity;
    _index = index;
    _arity = arity;
  }
  Object accept(IRVisitor visitor) { return visitor.visit(this); }
}

class CallInstr extends Instruction {
  Address _out;
  FunSym _fun;
  int _arity;
  CallInstr(Location loc, Address out, FunSym fun, int arity) {
    super(loc); _out = out; _fun = fun; _arity = arity;
  }
  Object accept(IRVisitor visitor) { return visitor.visit(this); }
}

class ReturnInstr extends Instruction {
  Address _val;
  ReturnInstr(Location loc, Address val) {
    super(loc); _val = val;
  }
  Object accept(IRVisitor visitor) { return visitor.visit(this); }
}

class ArrReadInstr extends Instruction {
  Address _out, _base, _subscript;
  ArrReadInstr(Location loc, Address out, Address base, Address subscript) {
    super(loc); _out = out; _base = base; _subscript = subscript;
  }
  Object accept(IRVisitor visitor) { return visitor.visit(this); }
}

class ArrWriteInstr extends Instruction {
  Address _base, _subscript, _in;
  ArrWriteInstr(Location loc, Address base, Address subscript, Address in) {
    super(loc); _base = base; _subscript = subscript; _in = in;
  }
  Object accept(IRVisitor visitor) { return visitor.visit(this); }
}

class RecReadInstr extends Instruction {
  Address _out, _base;
  FieldSym _field;
  RecReadInstr(Location loc, Address out, Address base, FieldSym field) {
    super(loc); _out = out; _base = base; _field = field;
  }
  Object accept(IRVisitor visitor) { return visitor.visit(this); }
}

class RecWriteInstr extends Instruction {
  Address _base;
  FieldSym _field;
  Address _in;
  RecWriteInstr(Location loc, Address base, FieldSym field, Address in) {
    super(loc); _base = base; _field = field; _in = in;
  }
  Object accept(IRVisitor visitor) { return visitor.visit(this); }
}

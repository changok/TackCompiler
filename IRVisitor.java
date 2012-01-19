abstract class IRVisitor {
  // ---------------- top-level ----------------
  Object visit(Program ir) {
    for (FunDef fun : ir._functions)
      fun.accept(this);
    return null;
  }

  Object visit(FunDef ir) {
    for (Instruction instr : ir._sym._instructions)
      instr.accept(this);
    return ir._name._id;
  }

  Object visit(Type ir) {
    return ir.toString();
  }

  Object visit(Label ir) {
    return ir._name;
  }

  // ---------------- addresses ----------------
  Object visit(NameAddr ir) {
    return ir._name;
  }

  Object visit(ConstantAddr ir) {
    return ir._literal;
  }

  Object visit(TempAddr ir) {
    return ir._name;
  }

  Object visit(SizeofAddr ir) {
    return ir._ofType;
  }

  // ---------------- instructions for computing values ----------------
  Object visit(CopyInstr ir) {
    ir._out.accept(this);
    ir._in.accept(this);
    return null;
  }

  Object visit(InfixInstr ir) {
    ir._out.accept(this);
    ir._lhs.accept(this);
    ir._rhs.accept(this);
    return ir._op;
  }

  Object visit(PrefixInstr ir) {
    ir._out.accept(this);
    ir._in.accept(this);
    return ir._op;
  }

  Object visit(CastInstr ir) {
    ir._out.accept(this);
    ir._in.accept(this);
    return ir._type;
  }

  // ---------------- instructions for jumping ----------------
  Object visit(UncondJumpInstr ir) {
    ir._tgt.accept(this);
    return null;
  }

  Object visit(TrueJumpInstr ir) {
    ir._cond.accept(this);
    ir._tgt.accept(this);
    return null;
  }

  Object visit(FalseJumpInstr ir) {
    ir._cond.accept(this);
    ir._tgt.accept(this);
    return null;
  }

  Object visit(RelopJumpInstr ir) {
    ir._lhs.accept(this);
    ir._rhs.accept(this);
    ir._tgt.accept(this);
    return ir._op;
  }

  // ---------------- instructions for functions ----------------
  Object visit(ParamInstr ir) {
    ir._in.accept(this);
    return ir._index + " : " + ir._arity;
  }

  Object visit(CallInstr ir) {
    if (null != ir._out)
      ir._out.accept(this);
    return ir._fun.name() + " : " + ir._arity;
  }

  Object visit(ReturnInstr ir) {
    if (null != ir._val)
      ir._val.accept(this);
    return null;
  }

  // ---------------- instructions for memory access ----------------
  Object visit(ArrReadInstr ir) {
    ir._out.accept(this);
    ir._base.accept(this);
    ir._subscript.accept(this);
    return null;
  }

  Object visit(ArrWriteInstr ir) {
    ir._base.accept(this);
    ir._subscript.accept(this);
    ir._in.accept(this);
    return null;
  }

  Object visit(RecReadInstr ir) {
    ir._out.accept(this);
    ir._base.accept(this);
    return ir._field;
  }

  Object visit(RecWriteInstr ir) {
    ir._base.accept(this);
    ir._in.accept(this);
    return ir._field;
  }
}

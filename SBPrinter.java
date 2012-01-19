import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
class RegisterAddr {
	String _reg; boolean _held;
	RegisterAddr(String reg, boolean held) { _reg = reg; _held = false;}
}
class Registers {
	// both _reg and _held, works concurrently
	List<RegisterAddr> _reg;
	Registers() {
		// callee_registers.put("%rbp", null);
		_reg = new ArrayList<RegisterAddr>();
		
		// 14 registers below
		_reg.add(new RegisterAddr("%r10", false));
		_reg.add(new RegisterAddr("%r11", false));
		// else : %rbp, %rsp
		// callee-save registers; belong to the caller
		_reg.add(new RegisterAddr("%rbx", false)); // [0]
		_reg.add(new RegisterAddr("%r12", false)); // [1]
		_reg.add(new RegisterAddr("%r13", false)); // [2]
		_reg.add(new RegisterAddr("%r14", false)); // [3]
		_reg.add(new RegisterAddr("%r15", false)); // [4]
		
		_reg.add(new RegisterAddr("%rax", false)); // [return]
		
		// caller-save registers; belong to the callee
		// [0-5] regs for param
		_reg.add(new RegisterAddr("%r9", false));	// [5]
		_reg.add(new RegisterAddr("%r8", false));	// [4]
		_reg.add(new RegisterAddr("%rcx", false)); // [3]
		_reg.add(new RegisterAddr("%rdx", false)); // [2]
		_reg.add(new RegisterAddr("%rsi", false)); // [1]
		_reg.add(new RegisterAddr("%rdi", false)); // [0]
	}
	String getFreeReg() {
		for (RegisterAddr r : _reg) {
			if (!r._held) {
				r._held = true;
				return r._reg;
			}
		}
		assert false : "all register are occupied by somewhere";
		return null;
	}
	String getParamReg(int index) {
		RegisterAddr r = _reg.get((_reg.size()-1) - index);	// reverse order
		if (!r._held) {
			r._held = true;
			return r._reg;
		}
		assert false : "all param register are occupied by somewhere";
		return null;
	}
	String freeRegister(String reg) {
		for (RegisterAddr r : _reg) {
			if (reg.equals(r._reg)) {
				r._held = false;
				return "success";
			}
		}
		assert false : reg + " is never used";
		return null;
	}
	void freeAllRegister() {
		for (RegisterAddr r : _reg) {
			r._held = false;
		}
	}
	String getThisReg(String reg) {
		for (RegisterAddr r : _reg) {
			if (reg.equals(r._reg)) {
				r._held = true;
				return r._reg;
			}
		}
		assert false : reg + " is now being used by someone";
		return null;
	}
	
}

class SBPrinter extends IRVisitor {
  PrintWriter _writer; 
  int _lcount;
  int _indent;
  boolean _lineStart;
  List<String> _rodatas;
  SymbolTable _symTab;
  Registers _regAddr;
  FunDef _owner;

  SBPrinter(PrintWriter writer, SymbolTable symTab) {
    _writer = writer;
    _lcount = 0;
    _indent = 0;
    _rodatas = new ArrayList<String>();
    _symTab = symTab;
    _lineStart = true;
    _regAddr = new Registers();
    _owner = null;
  }

  SBPrinter p(Address ir) {
    ir.accept(this);
    return this;
  }
  SBPrinter p(String s) {
	  printIndentIfLineStart();
    _writer.print(s);
    return this;
  }
  SBPrinter p(int i) {
	  printIndentIfLineStart();
    _writer.print(i);
    return this;
  }
  SBPrinter pln() {
    _writer.println();
    _lineStart = true;
    return this;
  }

  SBPrinter indent() {
	  _indent++;
	  return this;
  }
  SBPrinter dedent() {
	  _indent--;
	  return this;
  }
  SBPrinter printIndentIfLineStart() {
	  if (_lineStart) {
		  _lineStart = false;
		  for (int i = 0; i < _indent; i++)
			  _writer.print("    ");
	  }
	  return this;
  }
  SBPrinter compare(String a, String b) { return p("cmp ").p(a).p(", ").p(b);}
  
  String getNewStringLabel() {
	  return "S_" + _lcount++;
  }
  
  // for .rodata
  void pushNewStringLit(String rodatas) {
	  _rodatas.add(rodatas);
  }
  void popAllStringLits() {
	  if (_rodatas != null) {
		  for (String s : _rodatas)
			  p(s);
	  }
  }
  SBPrinter move(String a, String b) { return p("mov ").p(a).p(", ").p(b); }
  SBPrinter move(String a, Address b) { return p("mov ").p(a).p(", ").p(b); }
  SBPrinter move(Address a, String b) { return p("mov ").p(a).p(", ").p(b); }
  SBPrinter move(Address a, Address b) { return p("mov ").p(a).p(", ").p(b); }
  int getRecordOffset(Type t, String name) {
	  if (t instanceof RecordType) {
		  RecordType rt = (RecordType)t;
		  int i = 0;
		  for ( FieldType f : rt._fields) {
			  if (f._field._id.equals(name))
				  return i;
			  i-=8;
		  }
		  assert false : "no such " + name + " defined";
	  } else
		  assert false : "unexpected error";
	  return 0;
  }
  // ---------------- top-level ----------------
  Object visit(Program ir) {
	  p(".intel_syntax").pln();
    for (int i=0, n=ir._functions.size(); i<n; i++) {
      FunDef fun = ir._functions.get(i);
      fun.accept(this);
      if (i != n - 1)
	pln();
    }
    return null;
  }
  
  Object visit(FunDef ir) {
	  // prologue begin
	  _symTab.push(ir._heldScope);
	  assert _owner == null;
	  _owner = ir;	// held owner
	  // beginning of .rodata section (string literals)
	  if (!_symTab._current._rodatas.isEmpty()) {
		  List<ConstantAddr> rodatas = _symTab._current._rodatas;
		  // read-only data section
		  p(".section .rodata").pln();
		  for (int i = 0; i < rodatas.size(); i++) {
			  ConstantAddr stringAddr = rodatas.get(i);
			  assert stringAddr._label != null: "FunDef : stringAddr is null : " + stringAddr._literal.toString();
			  indent();
			  p(stringAddr._label).p(":").pln();
			  indent().p(".string\t").p(stringAddr._literal.toString()).dedent().pln();
			  dedent();
		  }
	  }
	  
	  // beginning of .text section (instructions)
	  p(".text").pln();
	  indent();
	  indent().p(".global\t").p(ir._name._id).pln().p(".type\t").p(ir._name._id).p(",\t@function").pln().dedent();
	  
	  p(ir._name._id).p(":").pln();
	  
	  indent().p("push %rbp").pln().p("mov %rbp, %rsp").dedent().pln();
	  if (ir._stack.getFrameSize() != 0) {
		  indent().p("sub %rsp, ").p(ir._stack.getFrameSize()).dedent().pln();
	  }
	  // calling conventions
	  for (int i = 0; i < ir._type._formals._fields.size(); i++) {
		  assert ir._type._formals._fields.get(i)._field != null;
		  String param = ir._type._formals._fields.get(i)._field._id;
		  if (i < 6) {
			  // move param's local, caller-save reg
			  indent().p("mov [%rbp").p(ir._stack.getOffset(param)).p("], ").p(_regAddr.getParamReg(i)).pln().dedent();
			  _regAddr.freeAllRegister();
		  }
		  else {
			  // move param's local, caller's memory
			  indent().p("mov [%rbp").p(ir._stack.getOffset(param)).p("], [%rbp+").p(8*(i-4)).p("]").pln().dedent();
		  }
	  }
	  
	  // prologue end

	  // for each instructions' part
	  
    for (Instruction instr : ir._sym._instructions) {
      for (Label label : instr._labels)
        p(label).p(":");
      if (!instr._labels.isEmpty())
        pln();
      //p("  ");
      indent();
      instr.accept(this);
      dedent();
    }
    
    // epilogue belongs to returnInstruction
    // end of .text section
    dedent();
    _owner = null; // release owner
    _symTab.pop(ir._heldScope);
    _regAddr.freeAllRegister();	// free all register
    return null;
  }

  Object visit(Type ir) {
    assert false : "use 'p(Type t)' instead";
    return null;
  }

  Object visit(Label ir) {
    p(ir._name);
    return null;
  }

  // ---------------- addresses ----------------
  Object visit(NameAddr ir) {
	  p(_owner._stack.getMemory(ir._name));
    return null;
  }

  // BoolLit, IntLit, NullLit, StringLit
  Object visit(ConstantAddr ir) {
    if (ir._literal instanceof StringLit) {
    	assert ir._label != null: "ConstantAddr : label is null : " + ir._literal.toString();
    	p("OFFSET FLAT:").p(ir._label);
    }
    else if (ir._literal instanceof BoolLit) {
    	BoolLit boolLit = (BoolLit)ir._literal;
    	assert boolLit._trueLabel == null;
    	if (boolLit._value == true)
    		p("1 /* true */");
    	else if (boolLit._value == false)
    		p("0 /* false */");
    	else
    		assert false: "ConstantAddr: unexpected error";
    }
    else if (ir._literal instanceof NullLit) {
    	p(0);
    }
    else {
    	p(ir._literal.toString());
    }
    return null;
  }

  Object visit(TempAddr ir) {
	  p(_owner._stack.getMemory(ir._name));
    return null;
  }
  
  int getElemType(Type tgt) {
	  if (tgt instanceof ArrayType) {
		  ArrayType arrtype = (ArrayType)tgt;
		  return getElemType(arrtype._elem);
	  }
	  else if (tgt instanceof FieldType) {
		  assert false : "not implemented";
		  FieldType fdtype = (FieldType)tgt;
		  fdtype._sym.type();
	  }
	  else if (tgt instanceof RecordType) {
		  return 8;
	  }
	  else if ("int".equals(tgt.toString()))
		  return 8;
	  else if ("string".equals(tgt.toString()))
		  return 8;
	  else if ("bool".equals(tgt.toString()))
		  return 8;
	  else if ("void".equals(tgt.toString()))
		  return 8;
	  else
		  assert false: "not expected case";
	  return 0;
  }

  Object visit(SizeofAddr ir) {
	int elemsize = getElemType(ir._ofType);
	  p(elemsize);
    return null;
  }

  // ---------------- instructions for computing values ----------------
  Object visit(CopyInstr ir) {
	  String reg = _regAddr.getFreeReg(); 
	  move(reg, ir._in).pln();
	  move(ir._out, reg).pln();
	  _regAddr.freeRegister(reg);
    return null;
  }

  Object visit(InfixInstr ir) {
	  if (ir._op.equals("+")) {
		  String reg1 = _regAddr.getFreeReg();
		  String reg2 = _regAddr.getFreeReg();
		  move(reg1, ir._lhs).pln();
		  move(reg2, ir._rhs).pln();
		  p("add ").p(reg1).p(", ").p(reg2).pln();
		  move(ir._out, reg1).pln();
		  _regAddr.freeRegister(reg1);
		  _regAddr.freeRegister(reg2);
	  }
	  else if (ir._op.equals("-")) {
		  String reg1 = _regAddr.getFreeReg();
		  String reg2 = _regAddr.getFreeReg();
		  move(reg1, ir._lhs).pln();
		  move(reg2, ir._rhs).pln();
		  p("sub ").p(reg1).p(", ").p(reg2).pln();
		  move(ir._out, reg1).pln();
		  _regAddr.freeRegister(reg1);
		  _regAddr.freeRegister(reg2);
	  }
	  else if (ir._op.equals("*")) {
		  String reg1 = _regAddr.getFreeReg();
		  String reg2 = _regAddr.getFreeReg();
		  move(reg1, ir._lhs).pln();
		  move(reg2, ir._rhs).pln();
		  p("imul ").p(reg1).p(", ").p(reg2).pln();
		  move(ir._out, reg1).pln();
		  
		  _regAddr.freeRegister(reg1);
		  _regAddr.freeRegister(reg2);
	  }
	  else if (ir._op.equals("/")) {
		  String rax = _regAddr.getThisReg("%rax");
		  String rdx = _regAddr.getThisReg("%rdx");	// hold %rdx
		  String freereg = _regAddr.getFreeReg();
		  
		  move(rax, ir._lhs).pln();
		  move(freereg, ir._rhs).pln();
		  
		  move(rdx, rax).pln();
		  
		  p("sar %rdx, 63").pln();
		  p("idiv ").p(freereg).pln();
		  move(ir._out, rax).pln();
		  
		  _regAddr.freeRegister(rax);
		  _regAddr.freeRegister(rdx);
		  _regAddr.freeRegister(freereg);
	  }
	  else if (ir._op.equals("%")) {
		  String rax = _regAddr.getThisReg("%rax");
		  String rdx = _regAddr.getThisReg("%rdx");	// hold %rdx
		  String freereg = _regAddr.getFreeReg();
		  
		  move(rax, ir._lhs).pln();
		  move(freereg, ir._rhs).pln();
		  
		  move(rdx, rax).pln();
		  
		  p("sar %rdx, 63").pln();
		  p("idiv ").p(freereg).pln();
		  move(ir._out, rdx).pln();
		  
		  _regAddr.freeRegister(rax);
		  _regAddr.freeRegister(rdx);
		  _regAddr.freeRegister(freereg);
	  }
	  
	  else
		  assert false : "unexpected case";
	  
    return null;
  }

  Object visit(PrefixInstr ir) {
	  //assert false: "not yet implemented";
	  if ("-".equals(ir._op)) {
		  String reg = _regAddr.getFreeReg();
		  move(reg, ir._in).pln();
		  p("neg ").p(reg).pln();
		  move(ir._out, reg).pln();
		  _regAddr.freeRegister(reg);
	  }
	  else if ("!".equals(ir._op)) {
		  assert false : "not yet implemented";
	  }
	  else
		  assert false : "unexpected error";
    return null;
  }

  Object visit(CastInstr ir) {
	  //assert false: "not yet implemented";
	  String outType = ir._type.toString();
	  String inType = ir._in.type().toString();
	  String rdi = _regAddr.getThisReg("%rdi");
	  if (outType.toString().equals(inType)) {
		  move(rdi, ir._in).pln();
		  move(ir._out, rdi).pln();
	  }
	  else if ("string".equals(inType) && "int".equals(outType)) {
		  move(rdi, ir._in).pln();
		  p("call string2int").pln();
		  move(ir._out, "%rax").pln();
	  }
	  else if ("int".equals(inType) && "string".equals(outType)) {
		  move(rdi, ir._in).pln();
		  p("call int2string").pln();
		  move(ir._out, "%rax").pln();
	  }
	  else if ("bool".equals(inType) && "int".equals(outType)) {
		  move(rdi, ir._in).pln();
		  p("call bool2int").pln();
		  move(ir._out, "%rax").pln();
	  }
	  else if ("int".equals(inType) && "bool".equals(outType)) {
		  move(rdi, ir._in).pln();
		  p("call int2bool").pln();
		  move(ir._out, "%rax").pln();
	  }
	  else if ("string".equals(inType) && "bool".equals(outType)) {
		  move(rdi, ir._in).pln();
		  p("call string2bool").pln();
		  move(ir._out, "%rax").pln();
	  }
	  else if ("bool".equals(inType) && "string".equals(outType)) {
		  move(rdi, ir._in).pln();
		  p("call int2string").pln();
		  move(ir._out, "%rax").pln();
	  }
	  else {
		  move(rdi, ir._in).pln();
		  move(ir._out, rdi).pln();
	  }
	  _regAddr.freeRegister(rdi);
    return null;
  }

  // ---------------- instructions for jumping ----------------
  Object visit(UncondJumpInstr ir) {
	  p("jmp ").p(ir._tgt).pln();
    return null;
  }

  Object visit(TrueJumpInstr ir) {
	  String reg = _regAddr.getFreeReg();
	  move(reg, ir._cond).pln();
	  compare(reg, "1").pln(); // 1 : true
	  p("je ").p(ir._tgt).pln();
	  _regAddr.freeRegister(reg);
    return null;
  }

  Object visit(FalseJumpInstr ir) {
	  String reg = _regAddr.getFreeReg();
	  move(reg, ir._cond).pln();
	  compare(reg, "1").pln(); // 1 : true
	  p("jne ").p(ir._tgt).pln();
	  _regAddr.freeRegister(reg);
    return null;
  }

  Object visit(RelopJumpInstr ir) {
	  String reg1 = _regAddr.getFreeReg();
	  String reg2 = _regAddr.getFreeReg();
	  move(reg1, ir._lhs).pln();
	  move(reg2, ir._rhs).pln();
	  compare(reg1, reg2).pln(); // 1 : true
	  if ("==".equals(ir._op)) {
		  p("je ");
	  } else if ("!=".equals(ir._op)) {
		  p("jne ");
	  } else if (">".equals(ir._op)) {
		  p("jg ");
	  } else if (">=".equals(ir._op)) {
		  p("jge ");
	  } else if ("<".equals(ir._op)) {
		  p("jl ");
	  } else if ("<=".equals(ir._op)) {
		  p("jle ");
	  }
	  p(ir._tgt).pln();
	  _regAddr.freeRegister(reg1);
	  _regAddr.freeRegister(reg2);
    return null;
  }

  // ---------------- instructions for functions ----------------
  Object visit(ParamInstr ir) {
	  if(ir._index < 6) {
		  String paramReg = _regAddr.getParamReg(ir._index);
		  move(paramReg, ir._in).pln();
		  _regAddr.freeRegister(paramReg);
	  }
	  else {
		  assert false: "ParamInstr (arity > 6): not yet implemented";
	  }
    return null;
  }

  Object visit(CallInstr ir) {
	  p("call ").p(ir._fun.name()).pln();
    if (null != ir._out) {
    	move(ir._out, "%rax").pln();
    }
    return null;
  }

  Object visit(ReturnInstr ir) {
    if (null != ir._val) {
      p("mov %rax, ").p(ir._val).pln();
    }
    p("mov %rsp, %rbp").pln().p("pop  %rbp").pln().p("ret").pln();
    return null;
  }

  // ---------------- instructions for memory access ----------------
  Object visit(ArrReadInstr ir) {
	  String reg1 = _regAddr.getFreeReg();
	  String reg2 = _regAddr.getFreeReg();
	  move(reg1, ir._base).pln();
	  move(reg1, "[" + reg1 + "+8]").pln();
	  move(reg2, ir._subscript).pln();
	  p("sal ").p(reg2).p(", 3").pln();
	  p("add ").p(reg2).p(", ").p(reg1).pln();
	  move(reg2, "[" + reg2 + "]").pln();
	  move(ir._out, reg2).pln();
	  _regAddr.freeRegister(reg1);
	  _regAddr.freeRegister(reg2);
    return null;
  }

  Object visit(ArrWriteInstr ir) {
	  String reg1 = _regAddr.getFreeReg();
	  String reg2 = _regAddr.getFreeReg();
	  String reg3 = _regAddr.getFreeReg();
	  move(reg1, ir._base).pln();
	  move(reg1, "[" + reg1 + "+8]").pln();
	  move(reg2, ir._subscript).pln();
	  p("sal ").p(reg2).p(", 3").pln();
	  p("add ").p(reg2).p(", ").p(reg1).pln();
	  move(reg3, ir._in).pln();
	  move("[" + reg2 + "]", reg3).pln();
	  _regAddr.freeRegister(reg1);
	  _regAddr.freeRegister(reg2);
	  _regAddr.freeRegister(reg3);
    return null;
  }

  Object visit(RecReadInstr ir) {
	  String reg1 = _regAddr.getFreeReg();
	  String reg2 = _regAddr.getFreeReg();
	  int offset = getRecordOffset(ir._base.type(), ir._field.name());
	  move(reg1, ir._base).pln();
	  p("add ").p(reg1).p(", ").p(offset).pln();
	  move(reg2, "[" + reg1 + "]").pln();
	  move(ir._out, reg2).pln();
	  _regAddr.freeRegister(reg1);
	  _regAddr.freeRegister(reg2);
    return null;
  }
  
  Object visit(RecWriteInstr ir) {
	  String reg1 = _regAddr.getFreeReg();
	  String reg2 = _regAddr.getFreeReg();
	  int offset = getRecordOffset(ir._base.type(), ir._field.name());
	  move(reg1, ir._base).pln();
	  move(reg2, ir._in).pln();
	  p("add ").p(reg1).p(", ").p(offset).pln();
	  move("[" + reg1 + "]", reg2).pln();
	  _regAddr.freeRegister(reg1);
	  _regAddr.freeRegister(reg2);
    return null;
  }
}

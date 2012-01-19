abstract class Visitor {
  Object defaultVisit(AstNode ast) {
    assert false : "not implemented";
    return null;
  }

  // ---------------- top-level ----------------
  Object visit(Program ast) { return defaultVisit(ast); }

  Object visit(FunDef ast) { return defaultVisit(ast); }

  Object visit(FunDefListHead ast) { return defaultVisit(ast); }
  Object visit(FunDefListTail ast) { return defaultVisit(ast); }

  // ---------------- types ----------------
  Object visit(ArrayType ast) { return defaultVisit(ast); }
  Object visit(RecordType ast) { return defaultVisit(ast); }
  Object visit(NullType ast) { return defaultVisit(ast); }

  Object visit(FieldTypeListHead ast) { return defaultVisit(ast); }
  Object visit(FieldTypeListTail ast) { return defaultVisit(ast); }

  Object visit(FieldType ast) { return defaultVisit(ast); }

  Object visit(PrimitiveType ast) { return defaultVisit(ast); }

  Object visit(FunType ast) { return defaultVisit(ast); }

  // ---------------- statements ----------------
  Object visit(VarDef ast) { return defaultVisit(ast); }
  Object visit(AssignStmt ast) { return defaultVisit(ast); }
  Object visit(BlockStmt ast) { return defaultVisit(ast); }
  Object visit(CallStmt ast) { return defaultVisit(ast); }
  Object visit(ForStmt ast) { return defaultVisit(ast); }
  Object visit(IfStmt ast) { return defaultVisit(ast); }
  Object visit(ReturnStmt ast) { return defaultVisit(ast); }
  Object visit(WhileStmt ast) { return defaultVisit(ast); }

  Object visit(StmtListHead ast) { return defaultVisit(ast); }
  Object visit(StmtListTail ast) { return defaultVisit(ast); }

  // ---------------- expressions ----------------
  Object visit(InfixExpr ast) { return defaultVisit(ast); }
  Object visit(InfixExprHead ast) { return defaultVisit(ast); }
  Object visit(InfixExprTail ast) { return defaultVisit(ast); }

  Object visit(PrefixExpr ast) { return defaultVisit(ast); }

  Object visit(PostfixExprHead ast) { return defaultVisit(ast); }
  Object visit(PostfixExprTail ast) { return defaultVisit(ast); }

  Object visit(CallExpr ast) { return defaultVisit(ast); }
  Object visit(CallExprTail ast) { return defaultVisit(ast); }

  Object visit(CastExpr ast) { return defaultVisit(ast); }
  Object visit(CastExprTail ast) { return defaultVisit(ast); }

  Object visit(FieldExpr ast) { return defaultVisit(ast); }
  Object visit(FieldExprTail ast) { return defaultVisit(ast); }

  Object visit(SubscriptExpr ast) { return defaultVisit(ast); }
  Object visit(SubscriptExprTail ast) { return defaultVisit(ast); }

  Object visit(ExprListHead ast) { return defaultVisit(ast); }
  Object visit(ExprListTail ast) { return defaultVisit(ast); }

  Object visit(ParenExpr ast) { return defaultVisit(ast); }

  // ---------------- identifiers ----------------
  Object visit(FieldId ast) { return defaultVisit(ast); }
  Object visit(FunId ast) { return defaultVisit(ast); }
  Object visit(VarId ast) { return defaultVisit(ast); }

  // ---------------- literals ----------------
  Object visit(ArrayLit ast) { return defaultVisit(ast); }

  Object visit(RecordLit ast) { return defaultVisit(ast); }

  Object visit(FieldLitListHead ast) { return defaultVisit(ast); }
  Object visit(FieldLitListTail ast) { return defaultVisit(ast); }
  Object visit(FieldLit ast) { return defaultVisit(ast); }

  Object visit(BoolLit ast) { return defaultVisit(ast); }
  Object visit(IntLit ast) { return defaultVisit(ast); }
  Object visit(NullLit ast) { return defaultVisit(ast); }
  Object visit(StringLit ast) { return defaultVisit(ast); }
}

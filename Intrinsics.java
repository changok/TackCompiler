import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

class Intrinsics {
  private static void defIntrinsic(SymbolTable symTab, String name, String typeString) {
    if (symTab.contains(name)) {
      ErrorPrinter.print(symTab.get(name).loc(),
                         "Redefinition of intrinsic '" + name + "'");
      return;
    }
    final Reader reader = new StringReader(typeString);
    final TackParser parser = new TackParser(reader, "(intrinsic)");
    xtc.parser.Result result = null;
    try {
      result = parser.pfunType1(0);
    } catch (final IOException e) {
      assert false;
    }
    if (!result.hasValue()) {
      ErrorPrinter.print(parser, result.parseError());
      ErrorPrinter.exit();
    }
    final FunType rawTypeAst = (FunType)result.semanticValue();
    final TreeNormalizer normalizer = new TreeNormalizer();
    final FunType typeAst = (FunType)rawTypeAst.accept(normalizer);
    final FunId id = new FunId(null, name);
    final FunDef fun = new FunDef(null, id, typeAst, (BlockStmt)null);
    final FunSym sym = new FunSym(symTab._current, fun);
    symTab.def(sym);
  }

  static void defIntrinsics(SymbolTable symTab) {
    defIntrinsic(symTab, "append", "(lhs: string, rhs: string)-> string");
    defIntrinsic(symTab, "bool2int", "(b: bool)-> int");
    defIntrinsic(symTab, "bool2string", "(b: bool)-> string");
    defIntrinsic(symTab, "int2bool", "(i: int)-> bool");
    defIntrinsic(symTab, "int2string", "(i: int)-> string");    
    defIntrinsic(symTab, "length", "(s: string)-> int");
    defIntrinsic(symTab, "newArray","(eSize: int, aSize: int)-> [()]");
    defIntrinsic(symTab, "newRecord", "(rSize: int)-> ()");
    defIntrinsic(symTab, "print", "(s: string)-> void");
    defIntrinsic(symTab, "range", "(start: int, end: int)-> [int]");
    defIntrinsic(symTab, "size", "(a: [()])-> int");
    defIntrinsic(symTab, "string2bool", "(s: string)-> bool");
    defIntrinsic(symTab, "string2int", "(s: string)-> int");    
    defIntrinsic(symTab, "stringEqual", "(lhs:string, rhs:string)-> bool");    
  }

  static FunSym get(SymbolTable symTab, String name) {
    FunSym result = (FunSym)symTab._topLevel.get(name);
    assert null != result : name;
    return result;
  }
}

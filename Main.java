import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import xtc.tree.Location;

class Main {
  static PrintWriter pfw(String fileName) throws IOException {
    return new PrintWriter(new FileWriter(fileName), true);
  }

  public static void main(final String[] args) throws IOException {
    String fileName = args[0];
    final Reader reader = new FileReader(fileName);
    final TackParser parser = new TackParser(reader, fileName);
    final xtc.parser.Result result = parser.pprogram(0);
    if (!result.hasValue()) {
      Location loc = parser.location(result.index);
      System.err.println(loc.toString() + ": Syntax error.");
      System.exit(-1);
    }
    final AstNode rawAst = (AstNode)result.semanticValue();
    final TreeNormalizer normalizer = new TreeNormalizer();
    final Program ast = (Program)rawAst.accept(normalizer);
    ScopeAnalyzer scopeAnalyzer = new ScopeAnalyzer();
    ast.accept(scopeAnalyzer);
    Intrinsics.defIntrinsics(scopeAnalyzer._symTab);
    SemanticAnalyzer semanticAnalyzer =
      new SemanticAnalyzer(scopeAnalyzer._symTab);
    ast.accept(semanticAnalyzer);
    if (0 < ErrorPrinter._count)
      ErrorPrinter.exit();
    IRGenerator irGenerator = new IRGenerator(scopeAnalyzer._symTab);
    ast.accept(irGenerator);
    SBGenerator sbGen = new SBGenerator(new PrintWriter(System.out, true), scopeAnalyzer._symTab);
    ast.accept(sbGen);
    SBPrinter sbvisitor = new SBPrinter(new PrintWriter(System.out, true), scopeAnalyzer._symTab);
    ast.accept(sbvisitor);
  }
}

import java.io.IOException;
import xtc.parser.ParseError;
import xtc.parser.ParseException;
import xtc.parser.ParserBase;
import xtc.tree.Location;

class ErrorPrinter {
  static int _count;
  ErrorPrinter() { _count = 0; }
  static void exit() {
    if (0 == _count)
      System.exit(0);
    if (1 == _count)
      System.err.println("There was 1 error.");
    else
      System.err.println("There were " + _count + " errors.");
    System.exit(-1);      
  }
  static void print(ParserBase parser, ParseError err) {
    _count++;
    try {
      parser.signal(err);
    } catch (ParseException exc) {
      System.err.println(exc.getMessage());
    } catch (IOException exc) {
      System.exit(-2);
    }
    if (100 <= _count)
      exit();
  }
  static void print(Location loc, String msg) {
    _count++;
    System.err.println(loc.toString() + ": " + msg + ".");
    if (100 <= _count)
      exit();
  }
}

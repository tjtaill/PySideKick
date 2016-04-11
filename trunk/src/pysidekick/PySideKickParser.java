package pysidekick;

import errorlist.DefaultErrorSource;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.gjt.sp.jedit.Buffer;
import pyparser.Python3Lexer;
import pyparser.Python3Parser;
import sidekick.SideKickParsedData;
import sidekick.SideKickParser;

public class PySideKickParser extends SideKickParser {


    public PySideKickParser() {
        super("python");
    }

    @Override
    public SideKickParsedData parse(Buffer buffer, DefaultErrorSource defaultErrorSource) {
        PySideKickParsedData data = new PySideKickParsedData( buffer.getPath() );
        Python3Lexer lexer = new Python3Lexer( new ANTLRInputStream(buffer.getText()));
        CommonTokenStream tokens = new CommonTokenStream(lexer);


        Python3Parser parser = new Python3Parser(tokens);

        ParseTree tree = parser.file_input();

        ParseTreeWalker walker = new ParseTreeWalker();

        PySideKickListener listener = new PySideKickListener(data, buffer);

        walker.walk(listener, tree);

        return data;
    }
}

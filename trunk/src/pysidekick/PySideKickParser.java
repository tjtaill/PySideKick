package pysidekick;

import errorlist.DefaultErrorSource;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import pyparser.Python3Lexer;
import pyparser.Python3Parser;
import sidekick.SideKickCompletion;
import sidekick.SideKickCompletionPopup;
import sidekick.SideKickParsedData;
import sidekick.SideKickParser;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PySideKickParser extends SideKickParser {
    private SideKickCompletionPopup lastCompletionPopup;
    private PyCompletion lastCompletion;
    private PyCompletionBuilder completionBuilder = new PyCompletionBuilder();
    private final static Pattern FROM_IMPORT = Pattern.compile("^\\s*from\\s*?(\\S)+\\s*$");
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

        completionBuilder = new PyCompletionBuilder();

        PySideKickListener listener = new PySideKickListener(data, buffer, completionBuilder);

        walker.walk(listener, tree);

        return data;
    }

    @Override
    public boolean supportsCompletion() {
        return true;
    }

    @Override
    public boolean canCompleteAnywhere() {
        return false;
    }

    @Override
    public String getInstantCompletionTriggers() {
        return " .";
    }

    public SideKickCompletionPopup getCompletionPopup(View view,
                                                      int caretPosition, SideKickCompletion complete, boolean active)
    {
        lastCompletionPopup = new SideKickCompletionPopup(view, this, caretPosition,
                complete, active);
        lastCompletion.setCompletionPopup( lastCompletionPopup );
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                lastCompletionPopup.setSelectedIndex(0);
            }
        });
        return lastCompletionPopup;
    }

    // should return "from", "import", "," or some other stuff which will be ignored.
    private String getStringBeforeSpace(JEditTextArea textArea, int caret) {
        if ( caret <= 0 )
            return "";

        int pos = caret;
        char c = textArea.getText(--pos, 1).charAt(0);
        StringBuilder identifier = new StringBuilder();
        boolean isFirstChar = true;
        while ( Character.isJavaIdentifierPart(c) || c == ',' || (isFirstChar && c == ' ') ) {
            if ( isFirstChar && c == ' ') {
                isFirstChar = false;
                c = textArea.getText(--pos, 1).charAt(0);
                continue;
            }
            isFirstChar = false;
            identifier.insert(0, c);
            if ( c == ',' ) break;
            if (pos == 0) break;
            c = textArea.getText(--pos, 1).charAt(0);
        }
        return identifier.toString();
    }

    private String getIdentifierBeforeDot(JEditTextArea textArea, int caret) {
        if ( caret <= 0 )
            return "";

        int pos = caret;
        char c = textArea.getText(--pos, 1).charAt(0);
        StringBuilder identifier = new StringBuilder();
        boolean isFirstChar = true;
        while ( Character.isJavaIdentifierPart(c) || c == '.' ) {
            if ( isFirstChar && c == '.' ) {
                isFirstChar = false;
                c = textArea.getText(--pos, 1).charAt(0);
                continue;
            }
            isFirstChar = false;
            identifier.insert(0, c);
            if (pos == 0) break;
            c = textArea.getText(--pos, 1).charAt(0);
        }
        return identifier.toString();
    }

    @Override
    public SideKickCompletion complete(EditPane editPane, int caret) {
        Buffer buffer = editPane.getBuffer();
        JEditTextArea textArea = editPane.getTextArea();
        char c = textArea.getText(caret-1, 1).charAt(0);
        List<String> completions = Collections.EMPTY_LIST;
        if (c == ' ') {
            if ( moduleContext(textArea, caret ) ) {

                completions = completionBuilder.moduleCompletion();
            } else if ( moduleNamesContext(textArea, caret) ) {
                String module = getModuleAfterFrom(textArea, caret);
                if (! module.isEmpty() ) {
                    completionBuilder.addModule(module);
                    completions = completionBuilder.namespaceCompletion(module);
                }
            }
        } else if (c == '.'){
            String namespace = getIdentifierBeforeDot(textArea, caret);
            completions = completionBuilder.namespaceCompletion(namespace);
        }
        lastCompletion = new PyCompletion(editPane.getView(), "", completions);
        return lastCompletion;
    }

    private String getModuleAfterFrom(JEditTextArea textArea, int caret) {
        int lineNo = textArea.getCaretLine();
        String line = textArea.getLineText( lineNo );
        Matcher matcher = FROM_IMPORT.matcher(line);
        String module = "";
        if (matcher.matches() ) {
            module = matcher.group(1);
        }
        return module;
    }

    private boolean moduleNamesContext(JEditTextArea textArea, int caret) {
        int lineNo = textArea.getCaretLine();
        String line = textArea.getLineText( lineNo );
        int pos = textArea.getCaretPosition() - textArea.getLineStartOffset(lineNo);


        int fromIndex = line.lastIndexOf("from");
        if (fromIndex == -1 || fromIndex < pos) return false;

        int importIndex = line.lastIndexOf("import");
        if (importIndex == -1 || importIndex < fromIndex || importIndex < pos) return false;

        // either space after import or space after comma should trigger module names completion
        String beforeSpace = getStringBeforeSpace(textArea, caret);
        switch (beforeSpace) {
            case "import":
            case ",":
                return true;
            default:
                return false;
        }
    }

    public static boolean logicalXOR(boolean x, boolean y) {
        return ( ( x || y ) && ! ( x && y ) );
    }

    private boolean moduleContext(JEditTextArea textArea, int caret) {
        int lineNo = textArea.getCaretLine();
        String line = textArea.getLineText( lineNo );
        int pos = textArea.getCaretPosition() - textArea.getLineStartOffset(lineNo);

        int fromIndex = line.lastIndexOf("from");
        int importIndex = line.lastIndexOf("import");
        if ( ! logicalXOR(fromIndex > -1, importIndex > -1) ) return false;
        // check if there is just a space between from or import keyword or a space after comma value
        String beforeSpace = getStringBeforeSpace(textArea, caret);
        switch (beforeSpace) {
            case "from":
            case "import":
            case ",":
                return true;
            default:
                return false;
        }
    }

}

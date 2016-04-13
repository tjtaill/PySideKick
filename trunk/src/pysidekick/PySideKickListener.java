package pysidekick;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.NotNull;
import org.gjt.sp.jedit.Buffer;
import pyparser.Python3BaseListener;
import pyparser.Python3Parser;
import sidekick.enhanced.SourceAsset;

import javax.swing.text.Position;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PySideKickListener extends Python3BaseListener {
    private PySideKickParsedData data;
    private Buffer buffer;
    private SourceAsset lastFunction;
    private int bufferIntervalStart;

    public PySideKickListener(PySideKickParsedData data, Buffer buffer) {
        this.data = data;
        this.buffer = buffer;
    }

    private Position begin(int line, Buffer buffer) {
        return buffer.createPosition(buffer.getLineStartOffset(line));
    }

    @Override
    public void enterImport_from(@NotNull Python3Parser.Import_fromContext ctx) {
        int line = ctx.getStart().getLine() - 1;
        String importNames = ctx.dotted_name().getText();
        if ( data.imports == null ) {
            SourceAsset imports = new SourceAsset("imports", line, begin(line, buffer) );
            data.imports = new DefaultMutableTreeNode(imports);
            data.root.add(data.imports);
        }
        SourceAsset imp = new SourceAsset(importNames, line, begin(line, buffer));
        data.imports.add(new DefaultMutableTreeNode(imp));

    }

    @Override
    public void enterImport_name(@NotNull Python3Parser.Import_nameContext ctx) {
        int line = ctx.getStart().getLine() - 1;
        String importNames = ctx.dotted_as_names().getText();
        if ( data.imports == null ) {
            SourceAsset imports = new SourceAsset("imports", line, begin(line, buffer) );
            data.imports = new DefaultMutableTreeNode(imports);
            data.root.add(data.imports);
        }
        SourceAsset imp = new SourceAsset(importNames, line, begin(line, buffer));
        data.imports.add( new DefaultMutableTreeNode(imp) );
    }

    @Override
    public void enterClassdef(@NotNull Python3Parser.ClassdefContext ctx) {
        int line = ctx.getStart().getLine() - 1;
        String className = ctx.NAME().getText();
        if (data.classes == null) {
            SourceAsset classes = new SourceAsset("classes", line, begin(line, buffer) );
            data.classes = new DefaultMutableTreeNode(classes);
            data.root.add( data.classes );
        }
        SourceAsset type = new SourceAsset(className, line, begin(line, buffer));
        data.classes.add(new DefaultMutableTreeNode(type));
    }

    private String getRawText(ParserRuleContext ctx) {
        int start = ctx.start.getStartIndex();
        return getRawText(start, ctx);
    }

    private String getRawText(int start, ParserRuleContext ctx) {
        CharStream input = ctx.start.getInputStream();
        int stop = ctx.stop.getStopIndex();
        Interval interval = new Interval(start, stop);
        return input.getText(interval);
    }


    @Override
    public void enterFuncdef(@NotNull Python3Parser.FuncdefContext ctx) {
        int line = ctx.getStart().getLine() - 1;
        String functionName = ctx.NAME().getText();
        String parameters = ctx.parameters().getText();
        String text = getRawText(bufferIntervalStart, ctx);
        if (data.functions == null) {
            SourceAsset functions = new SourceAsset("functions", line, begin(line, buffer) );
            data.functions = new DefaultMutableTreeNode(functions);
            data.root.add( data.functions );
        }
        String signature = functionName +  parameters;
        lastFunction = new SourceAsset(signature, line, begin(line, buffer));
        data.functions.add(new DefaultMutableTreeNode(lastFunction));
    }

    @Override
    public void enterReturn_stmt(@NotNull Python3Parser.Return_stmtContext ctx) {
        int line = ctx.getStart().getLine() - 1;
        String text = ctx.testlist().getText();
        System.out.println(text);
    }

    @Override
    public void enterExpr_stmt(@NotNull Python3Parser.Expr_stmtContext ctx) {
        int line = ctx.getStart().getLine() - 1;
        String text = ctx.getText();
        Python3Parser.AugassignContext augassign = ctx.augassign();
        // we want assignment

        Pattern assignmentPattern = Pattern.compile("([^!=]+)=([^=]+)");
        Matcher matcher = assignmentPattern.matcher(text);
        if ( matcher.matches() ) {
            String assignTo = matcher.group(1);
            String assignFrom = matcher.group(2);
            if (data.variables == null) {
                SourceAsset variables = new SourceAsset("variables", line, begin(line, buffer) );
                data.variables = new DefaultMutableTreeNode(variables);
                data.root.add( data.variables );
            }
            SourceAsset variableAsset = new SourceAsset(assignTo, line, begin(line, buffer));
            data.variables.add( new DefaultMutableTreeNode(variableAsset));
        }
    }

    @Override
    public void enterFile_input(@NotNull Python3Parser.File_inputContext ctx) {
        bufferIntervalStart = ctx.start.getStartIndex();
    }
}

package pysidekick;

import jep.JepException;
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
    private SourceAsset lastFunctionAsset;
    private DefaultMutableTreeNode lastFunctionNode;
    DefaultMutableTreeNode lastClass;
    private int bufferIntervalStart;
    Python3Parser.ClassdefContext classCtx;
    private Python3Parser.FuncdefContext funcCtx;


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
        classCtx = ctx;
        int line = ctx.getStart().getLine() - 1;
        String className = ctx.NAME().getText();
        if (data.classes == null) {
            SourceAsset classes = new SourceAsset("classes", line, begin(line, buffer) );
            data.classes = new DefaultMutableTreeNode(classes);
            data.root.add( data.classes );
        }
        SourceAsset type = new SourceAsset(className, line, begin(line, buffer));
        lastClass = new DefaultMutableTreeNode(type);
        data.classes.add(lastClass);
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
        funcCtx = ctx;
        int line = ctx.getStart().getLine() - 1;
        String functionName = ctx.NAME().getText();
        String parameters = ctx.parameters().getText();
        ParserRuleContext endCtx = classCtx == null ? ctx : classCtx;
        String text = getRawText(bufferIntervalStart, endCtx);
        DefaultMutableTreeNode functionNode = null;
        if ( classCtx == null ) {
            if (data.functions == null) {
                SourceAsset functionAsset = new SourceAsset("functions", line, begin(line, buffer));
                data.functions = new DefaultMutableTreeNode(functionAsset);
                data.root.add(data.functions);
                functionNode = data.functions;
            }

            data.functions.add(new DefaultMutableTreeNode(lastFunctionAsset));
        } else {
            functionNode = getClassFunctions(lastClass, line);
        }
        String signature = functionName +  parameters + " : None";
        lastFunctionAsset = new SourceAsset(signature, line, begin(line, buffer));
        functionNode.add( new DefaultMutableTreeNode(lastFunctionAsset) );
    }

    private DefaultMutableTreeNode getClassFunctions(DefaultMutableTreeNode classNode, int line) {
        DefaultMutableTreeNode functions = null;
        for(int i = 0; i < classNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) classNode.getChildAt(i);
            SourceAsset sa = (SourceAsset) child.getUserObject();
            if ( sa.getName().equals("functions") ) {
                functions = child;
                break;
            }
        }
        if ( functions == null ) {
            functions = new DefaultMutableTreeNode( new SourceAsset("functions", line, begin(line, buffer)) );
            classNode.add( functions );
        }

        return functions;
    }

    private DefaultMutableTreeNode getVariablesNode(DefaultMutableTreeNode node, int line) {
        DefaultMutableTreeNode variables = null;
        for(int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            SourceAsset sa = (SourceAsset) child.getUserObject();
            if ( sa.getName().equals("variables") ) {
                variables = child;
                break;
            }
        }
        if ( variables == null ) {
            variables = new DefaultMutableTreeNode( new SourceAsset("variables", line, begin(line, buffer)) );
            node.add(variables);
        }

        return variables;
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
        // TODO deal with augmented assignment operators too
        Python3Parser.AugassignContext augassign = ctx.augassign();
        // we want assignment

        Pattern assignmentPattern = Pattern.compile("([^!=]+)=([^=]+)");
        Matcher matcher = assignmentPattern.matcher(text);
        if ( matcher.matches() ) {
            final String assignTo = matcher.group(1);
            String assignFrom = matcher.group(2);
            DefaultMutableTreeNode variablesNode = null;
            final StringBuffer type = new StringBuffer();
            type.append(" : None");
            String pythonBlock = "";
            if (funcCtx != null && classCtx != null) {
                if ( assignTo.startsWith("self.") ) {
                    variablesNode = getVariablesNode(lastClass, line);
                } else {
                    variablesNode = getVariablesNode(lastFunctionNode, line);
                }
            } else if (funcCtx != null) {
                variablesNode = getVariablesNode(lastFunctionNode, line);
            }  else {
                if (data.variables == null) {
                    SourceAsset variables = new SourceAsset("variables", line, begin(line, buffer));
                    data.variables = new DefaultMutableTreeNode(variables);
                    data.root.add(data.variables);
                }
                variablesNode = data.variables;
                pythonBlock = getRawText(bufferIntervalStart, ctx);
            }
            try {
                PyEvaluator.JepEval jepEval = PyEvaluator.addBlock(pythonBlock);
                type.delete(0, type.length());
                type.append(" : " + PyEvaluator.varType(jepEval, assignTo));
                jepEval.jep.close();
            } catch (JepException e) {
                e.printStackTrace();
            }
            SourceAsset variableAsset = new SourceAsset(assignTo + type.toString(), line, begin(line, buffer));
            variablesNode.add(new DefaultMutableTreeNode(variableAsset));
        }
    }

    @Override
    public void exitClassdef(@NotNull Python3Parser.ClassdefContext ctx) {
        classCtx = null;
        lastClass = null;
    }

    @Override
    public void exitFuncdef(@NotNull Python3Parser.FuncdefContext ctx) {
        funcCtx = null;
        lastFunctionAsset = null;

    }

    @Override
    public void enterFile_input(@NotNull Python3Parser.File_inputContext ctx) {
        bufferIntervalStart = ctx.start.getStartIndex();
    }
}

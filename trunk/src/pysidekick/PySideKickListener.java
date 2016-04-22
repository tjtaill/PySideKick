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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PySideKickListener extends Python3BaseListener {
    private PySideKickParsedData data;
    private Buffer buffer;
    private SourceAsset lastFunctionAsset;
    private DefaultMutableTreeNode lastFunctionNode;
    private DefaultMutableTreeNode lastClass;
    private int bufferIntervalStart;
    private Python3Parser.ClassdefContext classCtx;
    private Python3Parser.FuncdefContext funcCtx;
    private Set<String> importStatements = new HashSet<>();

    private static class PyImportAsset extends SourceAsset {
        private final String importStatement;

        public PyImportAsset(String name, int lineNo, Position start, String importStatment) {
            super(name, lineNo, start);
            this.importStatement = importStatment;
        }

        public String getImportStatement() {
            return importStatement;
        }
    }

    public PySideKickListener(PySideKickParsedData data, Buffer buffer) {
        this.data = data;
        this.buffer = buffer;
    }

    private Position begin(int line, Buffer buffer) {
        return buffer.createPosition(buffer.getLineStartOffset(line));
    }

    @Override
    public void enterImport_from(@NotNull Python3Parser.Import_fromContext ctx) {
        importStatements.add( getRawText(ctx) );
        /*
        // TODO: Not an import but should add the import symbols into the lookup tables
        int line = ctx.getStart().getLine() - 1;
        String importName = ctx.dotted_name().getText();
        if ( data.imports == null ) {
            SourceAsset imports = new SourceAsset("imports", line, begin(line, buffer) );
            data.imports = new DefaultMutableTreeNode(imports);
            data.root.add(data.imports);
        }
        SourceAsset imp = new SourceAsset(importName, line, begin(line, buffer));
        data.imports.add(new DefaultMutableTreeNode(imp));
        */
    }



    @Override
    public void enterImport_name(@NotNull Python3Parser.Import_nameContext ctx) {
        importStatements.add( getRawText(ctx) );
        int line = ctx.getStart().getLine() - 1;

        List<Python3Parser.Dotted_as_nameContext> danCtxs = ctx.dotted_as_names().dotted_as_name();
        for (Python3Parser.Dotted_as_nameContext danCtx : danCtxs) {
            String importName = danCtx.dotted_name().getText();
            DefaultMutableTreeNode importNode = null;
            if (funcCtx == null && classCtx != null) {
                importNode = getChildNode(lastClass, "imports", line);
            } else if (funcCtx != null) {
                importNode = getChildNode(lastFunctionNode, "imports", line);
            }  else {
                if (data.imports == null) {
                    SourceAsset imports = new SourceAsset("imports", line, begin(line, buffer));
                    data.imports = new DefaultMutableTreeNode(imports);
                    data.root.add(data.imports);
                }
                importNode = data.imports;
            }
            SourceAsset imp = new SourceAsset(importName, line, begin(line, buffer));
            importNode.add( new DefaultMutableTreeNode(imp) );
        }
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



    private String getRawText(ParserRuleContext ctx, int start, int end) {
        CharStream input = ctx.start.getInputStream();
        Interval interval = new Interval(start, end);
        return input.getText(interval);
    }

    private String getRawText(ParserRuleContext ctx) {
        int start = ctx.start.getStartIndex();
        return getRawText(start, ctx);
    }

    private String getRawText(int start, ParserRuleContext ctx) {
        int stop = ctx.stop.getStopIndex();
        return getRawText(ctx, start, stop);
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

            functionNode = data.functions;
        } else {
            functionNode = getClassFunctions(lastClass, line);
        }
        String signature = functionName +  parameters + " : None";
        lastFunctionAsset = new SourceAsset(signature, line, begin(line, buffer));
        lastFunctionNode = new DefaultMutableTreeNode(lastFunctionAsset);
        functionNode.add( lastFunctionNode );
    }

    private DefaultMutableTreeNode getChildNode(DefaultMutableTreeNode parent, String childName, int line) {
        DefaultMutableTreeNode child = null;
        for(int i = 0; i < parent.getChildCount(); i++) {
            child = (DefaultMutableTreeNode) parent.getChildAt(i);
            SourceAsset sa = (SourceAsset) child.getUserObject();
            if ( sa.getName().equals(childName) ) {
                break;
            } else {
                child = null;
            }
        }
        if ( child == null ) {
            child = new DefaultMutableTreeNode( new SourceAsset(childName, line, begin(line, buffer)) );
            parent.add( child );
        }

        return child;
    }

    private DefaultMutableTreeNode getClassFunctions(DefaultMutableTreeNode functionNode, int line) {
        return getChildNode(functionNode, "functions", line);
    }

    private DefaultMutableTreeNode getVariablesNode(DefaultMutableTreeNode node, int line) {
        return getChildNode(node, "variables", line);
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

        Pattern assignmentPattern = Pattern.compile("([^(!=]+)=([^=]+)");
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
                    type.delete(0, type.length());
                    type.append(" : " + PyEvaluator.expressionVarType(importStatements, assignFrom));
                } else {
                    variablesNode = getVariablesNode(lastFunctionNode, line);
                    type.delete(0, type.length());
                    type.append( " : " + PyEvaluator.expressionVarType(importStatements, assignFrom) );
                }
            } else if (funcCtx != null) {
                variablesNode = getVariablesNode(lastFunctionNode, line);
                type.delete(0, type.length());
                type.append( " : " + PyEvaluator.expressionVarType(importStatements, assignFrom) );
            }  else {
                if (data.variables == null) {
                    SourceAsset variables = new SourceAsset("variables", line, begin(line, buffer));
                    data.variables = new DefaultMutableTreeNode(variables);
                    data.root.add(data.variables);
                }
                variablesNode = data.variables;
                pythonBlock = getRawText(bufferIntervalStart, ctx);
                try {
                    PyEvaluator.JepEval jepEval = PyEvaluator.addBlock(pythonBlock);

                    String kind = PyEvaluator.moduleLevelVarType(jepEval, assignTo);
                    if ( "None".equals(kind) ) {
                        kind = PyEvaluator.expressionVarType(importStatements, assignFrom);
                    }
                    type.delete(0, type.length());
                    type.append(" : " + kind );
                    jepEval.jep.close();
                } catch (JepException e) {
                    e.printStackTrace();
                }
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
        lastFunctionNode = null;

    }

    @Override
    public void enterFile_input(@NotNull Python3Parser.File_inputContext ctx) {
        bufferIntervalStart = ctx.start.getStartIndex();
    }
}

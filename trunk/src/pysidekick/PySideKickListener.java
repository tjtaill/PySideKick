package pysidekick;

import org.antlr.v4.runtime.misc.NotNull;
import org.gjt.sp.jedit.Buffer;
import pyparser.Python3BaseListener;
import pyparser.Python3Parser;
import sidekick.enhanced.SourceAsset;

import javax.swing.text.Position;
import javax.swing.tree.DefaultMutableTreeNode;


public class PySideKickListener extends Python3BaseListener {
    private PySideKickParsedData data;
    private Buffer buffer;

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

    @Override
    public void enterFuncdef(@NotNull Python3Parser.FuncdefContext ctx) {
        int line = ctx.getStart().getLine() - 1;
        String functionName = ctx.NAME().getText();
        if (data.functions == null) {
            SourceAsset functions = new SourceAsset("functions", line, begin(line, buffer) );

            data.functions = new DefaultMutableTreeNode(functions);
            data.root.add( data.functions );
        }
        SourceAsset function = new SourceAsset(functionName, line, begin(line, buffer));
        data.functions.add(new DefaultMutableTreeNode(function));
    }

    @Override
    public void enterExpr_stmt(@NotNull Python3Parser.Expr_stmtContext ctx) {
        ctx.getText();
    }
}

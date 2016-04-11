package pysidekick;

import sidekick.SideKickParsedData;

import javax.swing.tree.DefaultMutableTreeNode;

public class PySideKickParsedData extends SideKickParsedData {

    public DefaultMutableTreeNode imports = null;
    public DefaultMutableTreeNode functions = null;
    public DefaultMutableTreeNode classes = null;
    public DefaultMutableTreeNode variables = null;

    public PySideKickParsedData(String fileName) {
        super(fileName);
    }
}

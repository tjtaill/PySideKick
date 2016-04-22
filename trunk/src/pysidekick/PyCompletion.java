package pysidekick;


import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.textarea.Selection;
import sidekick.SideKickActions;
import sidekick.SideKickCompletion;
import sidekick.SideKickCompletionPopup;

import javax.swing.*;
import java.util.List;

public class PyCompletion extends SideKickCompletion {
    private SideKickCompletionPopup completionPopup;
    private StringBuilder typedChars = new StringBuilder();

    public PyCompletion(View view, String text, List items) {
        super(view, text, items);
    }

    @Override
    public boolean handleKeystroke(int selectedIndex, char keyChar)
    {
        // if(keyChar == '\t' || keyChar == '\n')
        if(SideKickActions.acceptChars.indexOf(keyChar) > -1)
        {
            insert(selectedIndex);
            if(SideKickActions.insertChars.indexOf(keyChar) > -1)
                textArea.userInput(keyChar);
            return false;
        }
        else
        {
            typedChars.append(keyChar);
            String prefix = typedChars.toString();
            for(int i = 0; i < items.size(); i++) {
                String description = (String)items.get(i);
                if ( description.startsWith(prefix) ) {
                    final int toSelect = i;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            completionPopup.setSelectedIndex(toSelect);
                        }
                    });
                    return true;
                }
            }
            // don't bother handling backspace yet just clear and start over
            typedChars.setLength(0);
            return true;
        }
    }

    public void insert(int index)
    {
        String selected = String.valueOf(get(index));
        String[] parts = selected.split("\\s", 2);
        selected = parts[0];
        int caret = textArea.getCaretPosition();
        Selection s = textArea.getSelectionAtOffset(caret);
        int start = (s == null ? caret : s.getStart());
        int end = (s == null ? caret : s.getEnd());
        JEditBuffer buffer = textArea.getBuffer();
        try
        {
            buffer.beginCompoundEdit();
            buffer.remove(start - text.length(),text.length());
            buffer.insert(start - text.length(),selected);
        }
        finally
        {
            buffer.endCompoundEdit();
        }
    }

    @Override
    public boolean updateInPlace(EditPane editPane, int caret)
    {
        return true;
    }

    public void setCompletionPopup(SideKickCompletionPopup completionPopup) {
        this.completionPopup = completionPopup;
    }
}

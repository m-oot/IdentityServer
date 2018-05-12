package Identity.DebugServer;

import javax.swing.*;
import java.awt.*;

public class ConsolePanel extends JPanel {
    JTextArea console;
    JScrollPane jScrollPane1;
    String content;

    public ConsolePanel() {
        console = new JTextArea();
        console.setLineWrap(true);
        console.setWrapStyleWord(true);
        jScrollPane1 = new JScrollPane(console);
        content = "";
        console.setText(content);
        this.setLayout(new GridLayout(1,1));
        this.add(jScrollPane1);
        this.setBorder(BorderFactory.createEmptyBorder(0,10,10,10));
    }

    public void write(String log) {
        content += "-------------------------------------------------------------------\n";
        content += log + "\n";
        content += "-------------------------------------------------------------------\n";
        console.setText(content);
        JScrollBar verticalScrollBar = jScrollPane1.getVerticalScrollBar();
        verticalScrollBar.setValue(verticalScrollBar.getMaximum());
    }
}

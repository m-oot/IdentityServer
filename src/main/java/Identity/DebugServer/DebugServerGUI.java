package Identity.DebugServer;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class DebugServerGUI {
    ArrayList<ConsolePanel> consoles;

    public DebugServerGUI() {
        consoles = new ArrayList<>();
        JFrame frame = new JFrame();
        JPanel panel = new JPanel();
        int rows = 2;
        int cols = 3;
        panel.setLayout(new GridLayout(rows,cols));
        for(int i = 0; i < rows; i++) {
            for(int j = 0; j < cols; j++) {
                ConsolePanel console = new ConsolePanel();
                consoles.add(console);
                panel.add(console,i,j);
            }
        }
        frame.add(panel);
        frame.setSize(1300, 1000);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        for(ConsolePanel c : consoles) {
            c.write("Hello");
        }
    }

    public void logToConsole(int consoleId, String message) {
        System.out.println(consoleId);
        consoles.get(consoleId).write(message);
    }

    public void logClient(String message) {
        consoles.get(consoles.size() - 1).write(message);
    }
}

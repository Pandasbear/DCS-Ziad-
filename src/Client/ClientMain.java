package Client;

import javax.swing.SwingUtilities;

public class ClientMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CrestGuiApp().setVisible(true));
    }
}

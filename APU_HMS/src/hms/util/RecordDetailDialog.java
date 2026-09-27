package hms.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class RecordDetailDialog {
    public static void showDetails(Component parent, String title, String[] labels, String[] values) {
        JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(parent), title, true);
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(labels.length, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        for (int i = 0; i < labels.length; i++) {
            formPanel.add(new JLabel(labels[i] + ":"));
            
            String val = (i < values.length && values[i] != null) ? values[i] : "";
            if (val.length() > 30 || labels[i].contains("NOTES") || labels[i].contains("DIAGNOSIS")) {
                JTextArea textArea = new JTextArea(val);
                textArea.setEditable(false);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                formPanel.add(new JScrollPane(textArea));
            } else {
                JTextField textField = new JTextField(val);
                textField.setEditable(false);
                formPanel.add(textField);
            }
        }

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(closeButton);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(bottomPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
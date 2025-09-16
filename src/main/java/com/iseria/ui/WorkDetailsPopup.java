package com.iseria.ui;

import javax.swing.*;
import java.awt.*;

public class WorkDetailsPopup extends JPanel {
    private JTextPane contentPane;
    private JScrollPane scrollPane;
    private boolean isVisible = false;

    public WorkDetailsPopup() {
        setLayout(new BorderLayout());
        setBackground(new Color(188, 188, 179, 240));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 2),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // Remplace le JLabel par un JTextPane
        contentPane = new JTextPane();
        contentPane.setContentType("text/html");
        contentPane.setEditable(false);
        contentPane.setBackground(new Color(188, 188, 179, 240));
        contentPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        contentPane.setFont(new Font("Arial", Font.PLAIN, 12));

        // Place le JTextPane dans un JScrollPane
        scrollPane = new JScrollPane(contentPane,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        UI.styleScrollPane(scrollPane);
        UI.configureScrollSpeed(scrollPane,20,80);
        scrollPane.setOpaque(true);
        scrollPane.getViewport().setOpaque(true);
        add(scrollPane, BorderLayout.CENTER);

        // Dimension fixe du popup
        setPreferredSize(new Dimension(300, 200));
        setVisible(false);
    }

    public void showDetails(String htmlContent, Component anchorComponent) {
        contentPane.setText(htmlContent);
        contentPane.setCaretPosition(0);

        // Make sure the popup has a parent before positioning
        Container parent = getParent();
        if (parent == null) {
            System.err.println("Warning: WorkDetailsPopup has no parent. Cannot show popup.");
            return;
        }

        try {
            // Ensure the popup is visible first
            setVisible(true);
            isVisible = true;

            // Force layout update
            revalidate();

            // Get the anchor component's location relative to the parent
            Point anchorLocationInParent = SwingUtilities.convertPoint(
                    anchorComponent.getParent(),
                    anchorComponent.getLocation(),
                    parent
            );

            // Position the popup next to the anchor component
            setSize(getPreferredSize());

            // Calculate position (to the right of the anchor)
            int popupX = anchorLocationInParent.x + anchorComponent.getWidth() + 5;
            int popupY = anchorLocationInParent.y;

            // Make sure popup stays within parent bounds
            int maxX = parent.getWidth() - getWidth();
            int maxY = parent.getHeight() - getHeight();

            popupX = Math.max(0, Math.min(popupX, maxX));
            popupY = Math.max(0, Math.min(popupY, maxY));

            setLocation(popupX, popupY);

            // Force repaint
            parent.repaint();

        } catch (Exception e) {
            System.err.println("Error positioning popup: " + e.getMessage());
            e.printStackTrace();

            // Fallback: position at a default location
            setSize(getPreferredSize());
            setLocation(100, 100);
            setVisible(true);
            isVisible = true;
        }
    }

    public void hideDetails() {
        setVisible(false);
        isVisible = false;
    }

    @Override
    public Point getMousePosition() {
        return isVisible ? super.getMousePosition() : null;
    }
}

package com.swing;

import com.swing.view.OrderClientFrame;

public class App {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            new OrderClientFrame().setVisible(true);
        });
    }
}

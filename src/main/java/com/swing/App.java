package com.swing;

import com.swing.view.OrderClientFrame;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        javax.swing.SwingUtilities.invokeLater( () -> {
            new OrderClientFrame().setVisible( true );
        } );
    }
}

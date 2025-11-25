package org.picopark;

import org.connection.Connection;

import javax.swing.*;
import java.net.URI;
import java.net.URISyntaxException;

public class Main extends JFrame {
    Connection connection ;
    NavigationManager navigationManager = new NavigationManager(this);

    public Main() throws URISyntaxException {
        //URI serverUri = new URI("ws://8.tcp.us-cal-1.ngrok.io:15069");
        URI serverUri = new URI("ws://148.113.207.55/ws");
        initJPanels();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setSize(1280, 720);
        setLocationRelativeTo(null);

        connection = new Connection(serverUri, navigationManager);
        // INICIAR CONEXIÓN EN HILO
        new Thread(() -> {
            try {
                connection.connectBlocking();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        navigationManager.navigateTo("login","Picopark - Login");
        setVisible(true);
    }

    private void initJPanels(){

        navigationManager.registerPanel("rooms", ()-> new RoomsPanel(connection));
        navigationManager.registerPanel("login", ()->new LoginPanel(this.connection));
        navigationManager.registerPanel("gamePanel",
                ()-> new GamePanel(navigationManager,connection), false);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new Main();
            } catch (URISyntaxException e) {
               e.printStackTrace();
            }
        });
    }
}
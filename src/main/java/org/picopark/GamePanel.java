package org.picopark;

import org.connection.Connection;
import org.connection.PlayerData;

import org.tiles.TilesEvents;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class GamePanel extends JPanel{
    private GameState gameState = GameState.RUNNING;
    final int originalSizeTile = 16;
    final int scale = 3;
    final int sizeTile = originalSizeTile * scale;

    final int maxRowsScreen = 10;
    final int maxColsScreen = 20;

    //Estas medidas son dinamicas provienen del mapa del servidor.
    private int maxRenWorld;
    private int maxColWorld;

    private final int widthScreen = sizeTile * maxColsScreen;
    private final int heightScreen = sizeTile * maxRowsScreen;

    int widthWorld;
    int heightWorld;

    public int getSizeTile(){
        return this.sizeTile;
    }

    public int getHeightScreen(){
        return this.heightScreen;
    }

    public int getWidthScreen(){
        return this.widthScreen;
    }

    public int getMaxRenWorld(){
        return this.maxRenWorld;
    }

    public int getMaxColWorld(){
        return this.maxColWorld;
    }

    private final NavigationManager navigationManager;
    private final Connection connection;
    TilesEvents tilesEvents;

    public GamePanel(NavigationManager navigationManager, Connection connection) {
        this.navigationManager = navigationManager;
        this.connection = connection;

        int[][] mapa = connection.getCurrentWorld();
        this.maxRenWorld = mapa.length;
        this.maxColWorld = mapa[0].length;

        widthWorld = sizeTile * maxColWorld;
        heightWorld = sizeTile * maxRenWorld;

        tilesEvents = new TilesEvents(this, mapa);

        this.navigationManager.resizeWindow(widthScreen, heightScreen);

        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);

        // Configurar el InputMap y ActionMap
        InputMap inputMap = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = this.getActionMap();

        // Variables para controlar el estado de las teclas
        final boolean[] leftPressed = {false};
        final boolean[] rightPressed = {false};

        // ========== MOVIMIENTO IZQUIERDA ==========
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false), "leftPressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, false), "leftPressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, true), "leftReleased");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, true), "leftReleased");

        actionMap.put("leftPressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!leftPressed[0]) {
                    GamePanel.this.connection.move("left");
                    leftPressed[0] = true;
                }
            }
        });

        actionMap.put("leftReleased", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                leftPressed[0] = false;
                GamePanel.this.connection.move("stop");
            }
        });

        // ========== MOVIMIENTO DERECHA ==========
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), "rightPressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, false), "rightPressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, true), "rightReleased");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, true), "rightReleased");

        actionMap.put("rightPressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!rightPressed[0]) {
                    GamePanel.this.connection.move("right");
                    rightPressed[0] = true;
                }
            }
        });

        actionMap.put("rightReleased", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rightPressed[0] = false;
                GamePanel.this.connection.move("stop");
            }
        });

        // ========== SALTO ==========
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "jump");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false), "jump");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, false), "jump");

        actionMap.put("jump", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GamePanel.this.connection.jump();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "pausePressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0, false), "pausePressed");
        actionMap.put("pausePressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GamePanel.this.setStateGame(GameState.MENU);
                repaint();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, false), "exitPressed");
        actionMap.put("exitPressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(GamePanel.this.gameState != GameState.MENU) return;
                GamePanel.this.connection.leaveRoom();
                GamePanel.this.navigationManager.navigateBack();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0, false), "resumePressed");
        actionMap.put("resumePressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(GamePanel.this.gameState != GameState.MENU) return;
                GamePanel.this.setStateGame(GameState.RUNNING);
                repaint();
            }
        });
    }



    public void setStateGame(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        PlayerData player = this.connection.getCurrentPlayer();
        float cameraX = player.getWorldX() - (widthScreen /2 - (sizeTile /2));
        float cameraY = player.getWorldY() - (heightScreen /2 - (sizeTile /2));

        if(cameraX < 0 ) cameraX = 0;
        if(cameraY < 0 ) cameraY = 0;

        if(cameraX > this.widthWorld - this.widthScreen) cameraX = this.widthWorld - this.widthScreen;
        if(cameraY > this.heightWorld - this.heightScreen) cameraY = this.heightWorld - this.heightScreen;

        tilesEvents.draw(g2d, (int)cameraX, (int)cameraY);

        for (PlayerData otherPlayers : this.connection.getPlayers()){
            BufferedImage sprite = otherPlayers.getDirectionImage();

            float worldX = otherPlayers.getWorldX();
            float worldY = otherPlayers.getWorldY();

            float screenX = worldX - cameraX;
            float screenY = worldY - cameraY;

            if(
                    worldX + sizeTile > cameraX &&
                    worldX - sizeTile < cameraX + widthScreen &&
                    worldY + sizeTile > cameraY &&
                    worldY - sizeTile < cameraY + heightScreen
            ) {
                g2d.drawImage(sprite, (int)screenX,(int)screenY, sizeTile, sizeTile, null);
                Font originalFont = g.getFont();
                g2d.setFont(new Font("Arial", Font.BOLD, 12));

                // Medir el texto para centrarlo
                FontMetrics fm = g.getFontMetrics();
                int textWidth = fm.stringWidth(otherPlayers.getUsername());

                // Calcular posición centrada arriba del sprite
                float textX = screenX + (sizeTile - textWidth) / 2;
                float textY = screenY - 5; // 5 píxeles arriba del sprite

                // Dibujar sombra/contorno para mejor legibilidad
                g2d.setColor(Color.BLACK);
                g2d.drawString(otherPlayers.getUsername(), (int)textX + 1, (int)textY + 1); // Sombra

                // Dibujar texto principal
                g2d.setColor(Color.WHITE);
                g2d.drawString(otherPlayers.getUsername(), (int)textX, (int)textY);
                // Restaurar fuente original
                g2d.setFont(originalFont);
            }
        }

        drawOverlay(g2d);

        // Overlay menú
        if (gameState == GameState.MENU) drawMenuOverlay(g2d);

        if(gameState == GameState.WINNER) drawWinOverlay(g2d);

    }

    private void drawMenuOverlay(Graphics2D g2d) {
        // Fondo semitransparente
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Título del menú
        g2d.setFont(new Font("Arial", Font.BOLD, 32));
        g2d.setColor(Color.WHITE);
        g2d.drawString("MENÚ PAUSA", getWidth()/2 - 120, getHeight()/2 - 80);

        // Botones (visuales)
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.drawString("[R] Reanudar", getWidth()/2 - 80, getHeight()/2 - 20);
        g2d.drawString("[S] Salir", getWidth()/2 - 80, getHeight()/2 + 20);
    }

    private void drawWinOverlay(Graphics2D g2d) {
        // Fondo negro semitransparente
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Texto principal "¡GANASTE!"
        String title = "¡GANASTE!";
        g2d.setFont(new Font("Arial", Font.BOLD, 64));

        // Sombra
        g2d.setColor(Color.BLACK);
        int titleWidth = g2d.getFontMetrics().stringWidth(title);
        g2d.drawString(
                title,
                (getWidth() - titleWidth) / 2 + 4,
                getHeight() / 2 - 20 + 4
        );

        // Texto principal blanco
        g2d.setColor(Color.WHITE);
        g2d.drawString(
                title,
                (getWidth() - titleWidth) / 2,
                getHeight() / 2 - 20
        );

        // Texto secundario
        String sub = "Presiona [R] para reiniciar";
        g2d.setFont(new Font("Arial", Font.PLAIN, 28));
        int subWidth = g2d.getFontMetrics().stringWidth(sub);

        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawString(
                sub,
                (getWidth() - subWidth) / 2,
                getHeight() / 2 + 40
        );
    }


    private void drawOverlay(Graphics2D g2d) {
        Font originalFont = g2d.getFont();
        g2d.setFont(new Font("Arial", Font.BOLD, 13));

        // Medir el texto para centrarlo
        FontMetrics fm = g2d.getFontMetrics();
        // Dibujar sombra/contorno para mejor legibilidad
        g2d.setColor(Color.BLACK);
        g2d.drawString("Usuario conectado : " + this.connection.getUsername(), 11, 21); // Sombra

        // Dibujar texto principal
        g2d.setColor(Color.WHITE);
        g2d.drawString("Usuario conectado : " + connection.getUsername(), 10, 20);

        String labelResume = "Presiona 'M' o 'ESCAPE' para pausar  el juego.";
        int textWidth = fm.stringWidth(labelResume);

        int x = this.widthScreen - textWidth - 10; // margen de 10px
        int y = 20;

        // Sombra
        g2d.setColor(Color.BLACK);
        g2d.drawString(labelResume, x + 1, y + 1);

        // Texto principal
        g2d.setColor(Color.WHITE);
        g2d.drawString(labelResume, x, y);

        // Restaurar fuente original
        g2d.setFont(originalFont);
    }

    public void addChatMessage(String user, String message) {}
}

enum GameState {
    RUNNING,
    MENU,
    WINNER,
    GAME_OVER
}

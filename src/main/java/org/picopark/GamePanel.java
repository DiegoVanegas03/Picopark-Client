package org.picopark;

import org.connection.Connection;
import org.connection.KeyData;
import org.connection.PlayerData;

import org.tiles.TilesEvents;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

public class GamePanel extends JPanel{
    private GameState gameState = GameState.RUNNING;
    final int originalSizeTile = 16;
    final int scale = 3;
    final int sizeTile = originalSizeTile * scale;

    final int maxRowsScreen = 10;
    final int maxColsScreen = 20;

    private String gameOverBy = "";

    //Estas medidas son dinamicas provienen del mapa del servidor.
    private int maxRenWorld;
    private int maxColWorld;

    private final int widthScreen = sizeTile * maxColsScreen;
    private final int heightScreen = sizeTile * maxRowsScreen;
    private final int chatWidth = 280;

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
    TilesEvents tilesEvents = new TilesEvents(this);;

    // Chat components
    private JPanel chatPanel;
    private JTextArea chatArea;
    private JTextField chatInput;
    private JButton sendChatButton;
    private boolean isTypingInChat = false;

    public GamePanel(NavigationManager navigationManager, Connection connection) {
        this.navigationManager = navigationManager;
        this.connection = connection;

        int[][] mapa = connection.getCurrentWorld();
        this.maxRenWorld = mapa.length;
        this.maxColWorld = mapa[0].length;

        widthWorld = sizeTile * maxColWorld;
        heightWorld = sizeTile * maxRenWorld;

        // Ajustar ventana para incluir el área de juego + chat
        this.navigationManager.resizeWindow(widthScreen + chatWidth, heightScreen);

        this.setBackground(new Color(247, 247, 247));
        this.setDoubleBuffered(true);
        this.setLayout(null);

        // Crear panel de chat
        createChatPanel();

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
                if (isTypingInChat) return; // Bloquear si está escribiendo en el chat
                if (!leftPressed[0]) {
                    GamePanel.this.connection.move("left");
                    GamePanel.this.connection.predictMove("left");
                    leftPressed[0] = true;
                }
            }
        });

        actionMap.put("leftReleased", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isTypingInChat) return;
                leftPressed[0] = false;
                GamePanel.this.connection.move("stop");
                GamePanel.this.connection.predictMove("stop");
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
                if (isTypingInChat) return;
                if (!rightPressed[0]) {
                    GamePanel.this.connection.move("right");
                    GamePanel.this.connection.predictMove("right");
                    rightPressed[0] = true;
                }
            }
        });

        actionMap.put("rightReleased", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isTypingInChat) return;
                rightPressed[0] = false;
                GamePanel.this.connection.move("stop");
                GamePanel.this.connection.predictMove("stop");
            }
        });

        // ========== SALTO ==========
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "jump");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false), "jump");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, false), "jump");

        actionMap.put("jump", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isTypingInChat) return;
                GamePanel.this.connection.jump();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "pausePressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0, false), "pausePressed");
        actionMap.put("pausePressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isTypingInChat) return;
                GamePanel.this.setStateGame(GameState.MENU);
                repaint();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, false), "exitPressed");
        actionMap.put("exitPressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isTypingInChat) return;
                if(GamePanel.this.gameState != GameState.MENU && GamePanel.this.gameState != GameState.WINNER) return;
                GamePanel.this.connection.leaveRoom();
                GamePanel.this.navigationManager.navigateBack();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0, false), "resumePressed");
        actionMap.put("resumePressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isTypingInChat) return;
                if(GamePanel.this.gameState != GameState.MENU) return;
                GamePanel.this.setStateGame(GameState.RUNNING);
                repaint();
            }
        });
    }

    public void setNewWorldDimensions(int ren, int col) {
        this.maxRenWorld = ren;
        this.maxColWorld = col;
        widthWorld = sizeTile * maxColWorld;
        heightWorld = sizeTile * maxRenWorld;
    }

    public void setStateGame(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        if(gameState == GameState.WINNER) {
            drawWinOverlay(g2d);
            return;
        }

        if(gameState == GameState.GAME_OVER){
            try{
                BufferedImage image = ImageIO
                        .read((Objects
                                .requireNonNull
                                        (this.getClass().getResourceAsStream("/assets-entities/dead.png"))));
                this.drawGameOver(g2d, this.gameOverBy, image);
            }catch (Exception e){
                e.printStackTrace();
            }
            return;
        }

        PlayerData player = this.connection.getCurrentPlayer();
        if (player == null) {
            // Si no hay jugador actual, mostrar overlays
            drawOverlay(g2d);
            if (gameState == GameState.MENU) drawMenuOverlay(g2d);
            return;
        }

        float cameraX = player.getWorldX() - (widthScreen /2 - (sizeTile /2));
        float cameraY = player.getWorldY() - (heightScreen /2 - (sizeTile /2));

        if(cameraX < 0 ) cameraX = 0;
        if(cameraY < 0 ) cameraY = 0;

        if(cameraX > this.widthWorld - this.widthScreen) cameraX = this.widthWorld - this.widthScreen;
        if(cameraY > this.heightWorld - this.heightScreen) cameraY = this.heightWorld - this.heightScreen;

        tilesEvents.draw(g2d, (int)cameraX, (int)cameraY, this.connection.getCurrentWorld(),
                this.connection.requiresKey(),
                this.connection.getCurrentPlayer() != null && this.connection.getCurrentPlayer().hasKey,
                this.connection.isDoorOpen());

        // Dibujar plataformas móviles
        for (org.connection.PlatformData platform : this.connection.getPlatforms()) {
            float screenX = platform.x - cameraX;
            float screenY = platform.y - cameraY;

            // Verificar si está en viewport
            if (platform.x + platform.width > cameraX &&
                    platform.x - platform.width < cameraX + widthScreen &&
                    platform.y + platform.height > cameraY &&
                    platform.y - platform.height < cameraY + heightScreen) {

                // Dibujar plataforma (rectángulo de color según tipo)
                Color platformColor = new Color(100, 150, 255, 200); // Azul semi-transparente
                g2d.setColor(platformColor);
                g2d.fillRect((int)screenX, (int)screenY, (int)platform.width, (int)platform.height);

                // Borde para mejor visibilidad
                g2d.setColor(Color.BLUE);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRect((int)screenX, (int)screenY, (int)platform.width, (int)platform.height);

                // Dibujar contador de jugadores necesarios al centro
                String counterText = String.valueOf(Math.max(0, platform.playersNeeded));
                Font counterFont = new Font("Arial", Font.BOLD, 20);
                FontMetrics fm = g2d.getFontMetrics(counterFont);

                int textX = (int)(screenX + (platform.width - fm.stringWidth(counterText)) / 2);
                int textY = (int)(screenY + ((platform.height - fm.getHeight()) / 2) + fm.getAscent());

                // Sombra del texto para mejor legibilidad
                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.drawString(counterText, textX + 1, textY + 1);
            }
        }

        // Dibujar llave
        KeyData key = this.connection.getKey();
        if (key != null) {
            float screenX = key.x - cameraX;
            float screenY = key.y - cameraY;

            // Hacer la llave mucho más grande (64x64 para que se vea bien)
            int keySize = 64;

            // Verificar si está en viewport
            if (key.x + keySize > cameraX &&
                    key.x - keySize < cameraX + widthScreen &&
                    key.y + keySize > cameraY &&
                    key.y - keySize < cameraY + heightScreen) {

                try {
                    BufferedImage keySprite = ImageIO.read(Objects.requireNonNull(this.getClass().
                            getResourceAsStream("/assets-tiles/llave.png")));

                    // Aplicar offset flotante
                    float drawY = screenY + key.floatOffset;

                    // Animación de apertura de puerta (brillo)
                    if (key.isOpeningDoor) {
                        // Efecto de brillo pulsante
                        long timeSinceOpen = System.currentTimeMillis() % 1000;
                        float brightness = (float) Math.sin(timeSinceOpen * 0.00628) * 0.5f + 0.5f;
                        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f + brightness * 0.7f));
                    }

                    g2d.drawImage(keySprite, (int)screenX, (int)drawY, keySize, keySize, null);

                    // Resetear composite
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

                } catch (IOException e) {
                    // Fallback: dibujar un rectángulo dorado más grande
                    g2d.setColor(Color.YELLOW);
                    g2d.fillRect((int)screenX, (int)screenY, keySize, keySize);
                    g2d.setColor(Color.ORANGE);
                    g2d.drawRect((int)screenX, (int)screenY, keySize, keySize);
                }
            }
        }

        for (PlayerData otherPlayers : this.connection.getPlayers()){
            if(!otherPlayers.isVisible) continue;
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

    private void drawGameOver(Graphics2D g2d, String loserName, BufferedImage deadSprite) {

        // Fondo blanco completo
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // === GAME OVER gigante centrado ===
        g2d.setFont(new Font("Arial", Font.BOLD, 72));
        String title = "GAME OVER";

        FontMetrics fm = g2d.getFontMetrics();
        int titleX = (getWidth() - fm.stringWidth(title)) / 2;
        int titleY = 120;

        // Sombra ligera para que se vea bonito
        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.drawString(title, titleX + 4, titleY + 4);

        // Texto principal negro
        g2d.setColor(Color.BLACK);
        g2d.drawString(title, titleX, titleY);

        // === SPRITE MUERTO CENTRADO ===
        if (deadSprite != null) {
            int spriteWidth = deadSprite.getWidth() * 4;   // agrandar 4x
            int spriteHeight = deadSprite.getHeight() * 4;

            int spriteX = (getWidth() - spriteWidth) / 2;
            int spriteY = titleY + 40;

            g2d.drawImage(deadSprite, spriteX, spriteY, spriteWidth, spriteHeight, null);
        }

        // === Nombre del culpable ===
        g2d.setFont(new Font("Arial", Font.BOLD, 32));
        String caused = loserName + " provocó el Game Over";

        FontMetrics fm2 = g2d.getFontMetrics();
        int textX = (getWidth() - fm2.stringWidth(caused)) / 2;
        int textY = getHeight() - 120;

        g2d.setColor(Color.BLACK);
        g2d.drawString(caused, textX, textY);
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
        String sub = "Presiona [S] para salir";
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

        FontMetrics fm = g2d.getFontMetrics();

        // ==== COLOR BASE PARA FONDOS BLANCOS ====
        Color textColor = new Color(20, 20, 20);      // Negro suave
        Color shadowColor = new Color(255, 255, 255); // Blanco puro como sombra

        // ---- Texto izquierda: Usuario conectado ----
        String overlayLeftText = "Usuario conectado : " + connection.getUsername();
        if(!this.connection.observerPlayerName.isEmpty()){
            overlayLeftText = "Observando a usuario :" + this.connection.observerPlayerName;
        }


        // Sombra
        g2d.setColor(shadowColor);
        g2d.drawString(overlayLeftText, 11, 21);

        // Texto
        g2d.setColor(textColor);
        g2d.drawString(overlayLeftText, 10, 20);

        // ---- Texto derecha: Info de controles ----
        String labelResume = "Presiona 'M' o 'ESCAPE' para pausar el juego.";
        int textWidth = fm.stringWidth(labelResume);

        int x = this.widthScreen - textWidth - 10;
        int y = 20;

        // Sombra
        g2d.setColor(shadowColor);
        g2d.drawString(labelResume, x + 1, y + 1);

        // Texto principal
        g2d.setColor(textColor);
        g2d.drawString(labelResume, x, y);

        g2d.setFont(originalFont);
    }

    public void addChatMessage(String user, String message) {
        if (chatArea != null) {
            SwingUtilities.invokeLater(() -> {
                chatArea.append(user + ": " + message + "\n");
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            });
        }
    }

    private void createChatPanel() {
        // Panel de chat al lado derecho del área de juego
        int chatHeight = heightScreen - 30; // Margen mayor para evitar desbordamiento
        int chatX = widthScreen; // Justo después del área de juego
        int chatY = 10; // Margen superior

        chatPanel = new JPanel();
        chatPanel.setLayout(new BorderLayout(5, 5));
        chatPanel.setBounds(chatX, chatY, chatWidth, chatHeight);
        chatPanel.setBorder(BorderFactory.createTitledBorder("Chat"));
        chatPanel.setBackground(new Color(240, 240, 240));
        chatPanel.setVisible(true); // Siempre visible

        // Área de mensajes con scroll
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 12));
        chatArea.setMargin(new Insets(5, 5, 5, 5));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatPanel.add(scrollPane, BorderLayout.CENTER);

        // Panel inferior con input y botón
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setBackground(new Color(240, 240, 240));

        chatInput = new JTextField();
        chatInput.setFont(new Font("Arial", Font.PLAIN, 12));

        // Listeners para detectar cuando se está escribiendo en el chat
        chatInput.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                isTypingInChat = true;
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                isTypingInChat = false;
            }
        });

        sendChatButton = new JButton("►");
        sendChatButton.setFocusable(false);
        sendChatButton.setPreferredSize(new Dimension(45, 25));
        sendChatButton.setToolTipText("Enviar mensaje");

        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(sendChatButton, BorderLayout.EAST);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));

        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        // Eventos
        sendChatButton.addActionListener(e -> sendChatMessage());
        chatInput.addActionListener(e -> sendChatMessage());

        this.add(chatPanel);
    }

    private void sendChatMessage() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            connection.sendChat(message);
            chatInput.setText("");
            // Devolver el foco al panel principal para que funcionen los controles
            GamePanel.this.requestFocusInWindow();
        }
    }

    public void gameOver(String gameOverBy) {
        this.gameState = GameState.GAME_OVER;
        this.gameOverBy = gameOverBy;
    }

    public void restartGame(){
        this.setStateGame(GameState.RUNNING);
    }

    public void gameWin(){
        this.setStateGame(GameState.WINNER);
    }
}

enum GameState {
    RUNNING,
    MENU,
    WINNER,
    GAME_OVER
}
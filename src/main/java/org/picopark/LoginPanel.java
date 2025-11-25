package org.picopark;

import org.connection.Connection;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;
import java.util.function.BiConsumer;

public class LoginPanel extends JPanel{

    private final JTextField txtUser;
    private final JPasswordField txtPass;

    private BufferedImage bg;


    public LoginPanel(Connection connection) {
        try{
            this.bg = ImageIO.read(
                    Objects.requireNonNull(
                            getClass().getResource("/assets-login/retro-game-background.jpg")
                    )
            );
        }catch(IOException e){
            e.printStackTrace();
        }

        // Panel de fondo con imagen
        setLayout(new GridBagLayout());

        // Card flotante tipo glassmorphism
        GlassPanel card = new GlassPanel();

        // Inputs
        JLabel lblUser = new JLabel("Usuario:");
        lblUser.setForeground(Color.WHITE);
        txtUser = new JTextField(15);

        JLabel lblPass = new JLabel("Contraseña:");
        lblPass.setForeground(Color.WHITE);
        txtPass = new JPasswordField(15);

        JButton btnLogin = new JButton("Ingresar");
        btnLogin.addActionListener(e -> {
            String user = txtUser.getText();
            String pass = txtPass.getText();
            if(user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Ingrese sus datos");
                return;
            }
            if (!connection.isConnected()) {
                JOptionPane.showMessageDialog(null, "Conectando al servidor, espere un momento...");
                return;
            }
            connection.authenticate(user,pass);
        });

        // Layout dentro de la card
        GridBagConstraints cardGbc = new GridBagConstraints();
        cardGbc.insets = new Insets(10, 10, 10, 10);
        cardGbc.gridx = 0; cardGbc.gridy = 0; card.add(lblUser, cardGbc);
        cardGbc.gridx = 1; card.add(txtUser, cardGbc);
        cardGbc.gridx = 0; cardGbc.gridy = 1; card.add(lblPass, cardGbc);
        cardGbc.gridx = 1; card.add(txtPass, cardGbc);
        cardGbc.gridx = 0; cardGbc.gridy = 2; cardGbc.gridwidth = 2;
        card.add(btnLogin, cardGbc);

        // Añadimos la card al panel de fondo
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        add(card, gbc);
    }

    public void updateUI(String message, Color color){
        System.out.println(message);
        JOptionPane.showMessageDialog(this, message, "Mensaje", JOptionPane.INFORMATION_MESSAGE);
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bg != null) {
            g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
        }
    }

    // ================= Panel tipo glass =================
    class GlassPanel extends JPanel {
        public GlassPanel() {
            setOpaque(false);
            setBackground(new Color(255, 255, 255, 120)); // blanco semitransparente
            setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            setLayout(new GridBagLayout());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            super.paintComponent(g2);
            g2.dispose();
        }
    }
}


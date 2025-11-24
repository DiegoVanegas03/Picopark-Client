package org.picopark;

import org.connection.Connection;
import org.connection.RoomInfo;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class RoomsPanel extends JPanel {

    private BufferedImage bg;


    public RoomsPanel(Connection connection) {

        try{
            this.bg = ImageIO.read(
                    Objects.requireNonNull(
                            getClass().getResource("/assets-login/arcade-rooms.jpg")
                    )
            );
        }catch(IOException e){
            e.printStackTrace();
        }

        setLayout(new BorderLayout());
        setBackground(new Color(30, 30, 40)); // fondo oscuro elegante

        JLabel title = new JLabel("Selecciona un nivel para jugar", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        add(title, BorderLayout.NORTH);

        // Panel interno de salas
        RoomsGridPanel roomsGridPanel = new RoomsGridPanel(connection.getRooms(), roomId -> {
            connection.joinRoom(roomId);
        });

        add(roomsGridPanel, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bg != null) {
            g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
        }
    }
}

class RoomsGridPanel extends JPanel {

    public RoomsGridPanel(List<RoomInfo> rooms, Consumer<String> onRoomSelected) {

        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel roomsContainer = new JPanel();
        roomsContainer.setLayout(new WrapLayout(FlowLayout.CENTER, 25, 25));
        roomsContainer.setOpaque(false);
        roomsContainer.setBorder(BorderFactory.createEmptyBorder(20, 20, 40, 20));

        for (RoomInfo room : rooms) {

            JPanel card = new JPanel(new BorderLayout()) {

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // Glass effect
                    g2.setColor(new Color(255, 255, 255, 60));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);

                    // Border glow
                    g2.setStroke(new BasicStroke(2f));
                    g2.setColor(new Color(255, 255, 255, 90));
                    g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);

                    g2.dispose();
                    super.paintComponent(g);
                }
            };

            card.setOpaque(false);
            card.setPreferredSize(new Dimension(160, 140));
            card.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // Hover animation
            card.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    card.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(255, 255, 255, 150), 2),
                            BorderFactory.createEmptyBorder(8, 8, 8, 8)
                    ));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    card.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                }
            });

            // Nombre
            JLabel nameLabel = new JLabel(room.getName(), SwingConstants.CENTER);
            nameLabel.setFont(new Font("Poppins", Font.BOLD, 15));
            nameLabel.setForeground(Color.WHITE);
            card.add(nameLabel, BorderLayout.NORTH);

            // Jugadores
            JLabel userCount = new JLabel(room.getPlayerCount() + " jugadores", SwingConstants.CENTER);
            userCount.setFont(new Font("Poppins", Font.PLAIN, 13));
            userCount.setForeground(new Color(230, 230, 230));
            card.add(userCount, BorderLayout.CENTER);

            // Botón
            JButton enterButton = new JButton("Entrar");
            enterButton.setFont(new Font("Poppins", Font.BOLD, 13));
            enterButton.setFocusPainted(false);
            enterButton.setBackground(new Color(255, 255, 255, 140));
            enterButton.setForeground(new Color(30, 30, 40));
            enterButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            enterButton.addActionListener(e -> onRoomSelected.accept(room.getId()));
            card.add(enterButton, BorderLayout.SOUTH);

            roomsContainer.add(card);
        }

        JScrollPane scrollPane = new JScrollPane(roomsContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        add(scrollPane, BorderLayout.CENTER);
    }
}

/**
 * FlowLayout que permite "wrap" de componentes como un FlowLayout normal,
 * pero ajustando automáticamente a la siguiente fila cuando se excede el ancho.
 */
class WrapLayout extends FlowLayout {

    public WrapLayout() {
        super();
    }

    public WrapLayout(int align) {
        super(align);
    }

    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= (getHgap() + 1);
        return minimum;
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getSize().width;

            if (targetWidth == 0)
                targetWidth = Integer.MAX_VALUE;

            int hgap = getHgap();
            int vgap = getVgap();
            Insets insets = target.getInsets();
            int maxWidth = targetWidth - (insets.left + insets.right + hgap * 2);

            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;

            int nmembers = target.getComponentCount();

            for (int i = 0; i < nmembers; i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();

                    if (rowWidth + d.width > maxWidth) {
                        dim.width = Math.max(dim.width, rowWidth);
                        dim.height += rowHeight + vgap;
                        rowWidth = 0;
                        rowHeight = 0;
                    }

                    if (rowWidth != 0) {
                        rowWidth += hgap;
                    }

                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }
            }

            dim.width = Math.max(dim.width, rowWidth);
            dim.height += rowHeight;

            dim.width += insets.left + insets.right;
            dim.height += insets.top + insets.bottom;

            return dim;
        }
    }
}

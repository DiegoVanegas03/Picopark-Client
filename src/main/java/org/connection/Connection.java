package org.connection;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.picopark.GamePanel;
import org.picopark.LoginPanel;
import org.picopark.NavigationManager;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Connection extends WebSocketClient {
    private NavigationManager navigationManager;
    private final Gson gson = new Gson();
    private String userId;
    private String username;
    private String currentRoom;
    private Map<String, PlayerData> players = new HashMap<>();
    private int[][] currentWorld;
    private List<RoomInfo> rooms = new ArrayList<>();

    public String observerPlayerName = "";

    public Connection(URI serverUri, NavigationManager navigationManager) {
        super(serverUri);
        this.navigationManager = navigationManager;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Conectado al servidor");
        /*if (gui != null) {
            gui.updateStatus("Conectado al servidor", Color.GREEN);
        }*/
    }

    @Override
    public void onMessage(String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            String type = json.get("type").getAsString();
            JsonObject data = json.has("data") ? json.getAsJsonObject("data") : new JsonObject();

            switch (type) {
                case "authSuccess":
                    handleAuthSuccess(data);
                    break;
                case "authFailed":
                    handleAuthFailed(data);
                    break;
                case "roomJoined":
                    handleRoomJoined(data);
                    break;
                case "startGame":
                    handleStartGame(data);
                    break;
                case "gameOver":
                    handleGameOver(data);
                    break;
                case "gameWin":
                    handleGameWin();
                    break;
                case "restartGame":
                    handleRestartGame();
                    break;
                case "playerJoined":
                    handlePlayerJoined(data);
                    break;
                case "playerLeft":
                    handlePlayerLeft(data);
                    break;
                case "gameUpdate":
                    handleGameUpdate(data);
                    break;
                case "chat":
                    handleChat(data);
                    break;
                case "error":
                    handleError(data);
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error procesando mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleGameWin() {
        JPanel panel  = navigationManager.getPanel("gamePanel");
        if(panel instanceof GamePanel)
            ((GamePanel) panel).gameWin();
    }

    private void handleRestartGame() {
        JPanel panel  = navigationManager.getPanel("gamePanel");
        if(panel instanceof GamePanel)
            ((GamePanel) panel).restartGame();
    }

    private void handleGameOver(JsonObject data) {
        String userName = data.get("userName").getAsString();
        JPanel panel  = navigationManager.getPanel("gamePanel");
        if(panel instanceof GamePanel)
            ((GamePanel) panel).gameOver(userName);
    }

    private void handleStartGame(JsonObject data) {
        int[][] world = gson.fromJson(data.get("world"), int[][].class);
        this.currentWorld = world;

        JPanel panel  = navigationManager.getPanel("gamePanel");
        if(panel instanceof GamePanel) {
            ((GamePanel) panel).setNewWorldDimensions(world.length, world[0].length);
            panel.repaint();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Desconectado del servidor: " + reason);
        /*if (gui != null) {
            gui.updateStatus("Desconectado: " + reason, Color.RED);
        }*/
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("Error: " + ex.getMessage());
        ex.printStackTrace();
    }


    private void handleAuthSuccess(JsonObject data) {
        this.userId = data.get("userId").getAsString();
        this.username = data.get("username").getAsString();

        rooms.clear();
        data.getAsJsonArray("rooms").forEach(room -> {
            JsonObject roomObj = room.getAsJsonObject();
            rooms.add(new RoomInfo(
                    roomObj.get("id").getAsString(),
                    roomObj.get("name").getAsString(),
                    roomObj.get("players").getAsString()
            ));
        });

        this.navigationManager.navigateTo("rooms", "Picopark - Rooms");
    }

    private void handleAuthFailed(JsonObject data) {
        String reason = data.get("reason").getAsString();

        JPanel panel  = navigationManager.getPanel("login");
        if(panel instanceof LoginPanel) {
            ((LoginPanel) panel).updateUI("Autenticación fallida: " + reason, Color.RED);
        }
    }

    private void handleRoomJoined(JsonObject data) {
        this.currentRoom = data.get("roomId").getAsString();
        String roomName = data.get("roomName").getAsString();

        int[][] world = gson.fromJson(data.get("world"), int[][].class);

        this.currentWorld = world;

        players.clear();
        data.getAsJsonArray("players").forEach(p -> {
            JsonObject playerObj = p.getAsJsonObject();
            PlayerData player = new PlayerData(
                    playerObj.get("id").getAsString(),
                    playerObj.get("username").getAsString(),
                    playerObj.get("x").getAsFloat(),
                    playerObj.get("y").getAsFloat()
            );
            players.put(player.id, player);
        });
        this.navigationManager.navigateTo("gamePanel", "Picopark - " + roomName);

        JPanel panel  = navigationManager.getPanel("gamePanel");
        if(panel instanceof GamePanel) {
            ((GamePanel) panel).addChatMessage("Sistema", "Te uniste a " + roomName);
        }
    }

    private void handlePlayerJoined(JsonObject data) {
        String playerId = data.get("userId").getAsString();
        String playerUsername = data.get("username").getAsString();
        JsonObject playerData = data.getAsJsonObject("player");

        PlayerData player = new PlayerData(
                playerId,
                playerUsername,
                playerData.get("x").getAsFloat(),
                playerData.get("y").getAsFloat()
        );
        players.put(playerId, player);

        JPanel panel  = navigationManager.getPanel("gamePanel");
        if(panel instanceof GamePanel) {
            ((GamePanel) panel).addChatMessage("Sistema", playerUsername + " salió del juego");
        }
    }

    private void handlePlayerLeft(JsonObject data) {
        System.out.println("Desconectado del servidor: " + data.toString());
        String playerId = data.get("userId").getAsString();
        String playerUsername = data.get("username").getAsString();

        players.remove(playerId);

        JPanel panel  = navigationManager.getPanel("gamePanel");
        if(panel instanceof GamePanel) {
            ((GamePanel) panel).addChatMessage("Sistema", playerUsername + " salió del juego");
        }
    }

    private void handleGameUpdate(JsonObject data) {
        data.getAsJsonArray("players").forEach(p -> {
            JsonObject playerObj = p.getAsJsonObject();
            String id = playerObj.get("id").getAsString();
            PlayerData player = players.get(id);
            if (player != null) {
                player.x = playerObj.get("x").getAsFloat();
                player.y = playerObj.get("y").getAsFloat();
                player.direction = playerObj.get("direction").getAsString();
                player.isVisible = playerObj.get("isVisible").getAsBoolean();
            }
        });

        JPanel panel  = navigationManager.getPanel("gamePanel");
        if(panel instanceof GamePanel) {
            players.values().forEach(PlayerData::updateSprite);
            panel.repaint();
        }
    }

    private void handleChat(JsonObject data) {
        String playerUsername = data.get("username").getAsString();
        String message = data.get("message").getAsString();

        JPanel panel  = navigationManager.getPanel("gamePanel");
        if(panel != null && panel instanceof GamePanel) {
            ((GamePanel) panel).addChatMessage(playerUsername, message);
        }

    }

    private void handleError(JsonObject data) {
        String error = data.get("message").getAsString();
        JPanel panel  = navigationManager.getPanel("login");
        if(panel instanceof LoginPanel) {
            ((LoginPanel) panel).updateUI(error, Color.RED);
        }
    }

    public int[][] getCurrentWorld() {
        return currentWorld;
    }

    public String getUsername(){
        return username;
    }

    public PlayerData getCurrentPlayer() {
        PlayerData currentPlayer = players.get(this.userId);

        // Si el jugador actual es visible, retornarlo
        if (currentPlayer != null && currentPlayer.isVisible) {
            return currentPlayer;
        }

        // Si no es visible, buscar el jugador visible más cercano
        PlayerData closestVisiblePlayer = null;
        float minDistance = Float.MAX_VALUE;

        for (PlayerData player : players.values()) {
            // Saltar jugadores no visibles o el mismo jugador
            if (!player.isVisible || player.id.equals(this.userId)) {
                continue;
            }

            // Calcular distancia al jugador actual
            float distance;
            if (currentPlayer != null) {
                float dx = player.x - currentPlayer.x;
                float dy = player.y - currentPlayer.y;
                distance = (float) Math.sqrt(dx * dx + dy * dy);
            } else {
                // Si currentPlayer es null, usar distancia al origen
                distance = (float) Math.sqrt(player.x * player.x + player.y * player.y);
            }

            // Actualizar el más cercano
            if (distance < minDistance) {
                minDistance = distance;
                closestVisiblePlayer = player;
            }
        }

        if(closestVisiblePlayer != null) {
            observerPlayerName =  closestVisiblePlayer.username;
            return closestVisiblePlayer;
        }

        return  currentPlayer;
    }

    public List<PlayerData> getPlayers() {
        /*return players.values()
                .stream()
                .filter(p -> !p.getId().equals(this.userId))
                .collect(Collectors.toList());*/
        return new ArrayList<>(players.values());
    }

    public List<RoomInfo> getRooms(){
        return rooms;
    }


    public void authenticate(String username, String password) {
        Map<String, String> data = new HashMap<>();
        data.put("username", username);
        data.put("password", password);
        sendMessage("auth", data);
    }


    public void joinRoom(String roomId) {
        Map<String, String> data = new HashMap<>();
        data.put("roomId", roomId);
        sendMessage("joinRoom", data);
    }

    public void move(String direction) {
        Map<String, String> data = new HashMap<>();
        data.put("direction", direction);
        sendMessage("move", data);
    }

    public void jump() {
        sendMessage("jump", new HashMap<>());
    }

    public void leaveRoom(){
        sendMessage("leaveRoom", new HashMap<>());
    }

    public void sendChat(String message) {
        Map<String, String> data = new HashMap<>();
        data.put("message", message);
        sendMessage("chat", data);
    }

    private void sendMessage(String type, Map<String, ?> data) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        if (data != null && !data.isEmpty()) {
            message.put("data", data);
        }
        send(gson.toJson(message));
    }

}

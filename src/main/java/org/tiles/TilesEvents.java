package org.tiles;

import org.picopark.GamePanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

public class TilesEvents {
    private final int maxTiles = 51;
    private BufferedImage[] tiles = new BufferedImage[maxTiles];

    private final GamePanel gp;

    public TilesEvents(GamePanel gamePanel){
        this.gp = gamePanel;
        getTilesImages();
    }

    public void getTilesImages(){
        try{
            this.tiles[0] = ImageIO.read((Objects.requireNonNull(this.getClass().getResourceAsStream("/assets-tiles/agua.png"))));
            this.tiles[1] = ImageIO.read((Objects.requireNonNull(this.getClass().getResourceAsStream("/assets-tiles/arbol.png"))));
            this.tiles[2]= ImageIO.read((Objects.requireNonNull(this.getClass().getResourceAsStream("/assets-tiles/arena.png"))));
            this.tiles[3] =ImageIO.read((Objects.requireNonNull(this.getClass().getResourceAsStream("/assets-tiles/muro.png"))));
            this.tiles[4] = ImageIO.read((Objects.requireNonNull(this.getClass().getResourceAsStream("/assets-tiles/pasto.png"))));
            this.tiles[5] = ImageIO.read((Objects.requireNonNull(this.getClass().getResourceAsStream("/assets-tiles/default-picopark-tile.png"))));
            this.tiles[6] = ImageIO.read((Objects.requireNonNull(this.getClass().getResourceAsStream("/assets-tiles/boton.png"))));
            this.tiles[7] = ImageIO.read((Objects.requireNonNull(this.getClass().getResourceAsStream("/assets-tiles/botonPresionado.png"))));
            this.tiles[8] = ImageIO.read((Objects.requireNonNull(this.getClass().getResourceAsStream("/assets-tiles/Puerta1.png"))));
            this.tiles[9] = ImageIO.read((Objects.requireNonNull(this.getClass().getResourceAsStream("/assets-tiles/Puerta2.png"))));
            this.tiles[10] = ImageIO.read((Objects.requireNonNull(this.getClass().getResourceAsStream("/assets-tiles/Puerta3.png"))));
            this.tiles[11] = ImageIO.read((Objects.requireNonNull(this.getClass().getResourceAsStream("/assets-tiles/Puerta4.png"))));
            this.tiles[12]  = ImageIO.read((Objects.requireNonNull(this.getClass().getResourceAsStream("/assets-tiles/PuertaAbierta1.png"))));
            this.tiles[13] = ImageIO.read((Objects.requireNonNull(this.getClass().getResourceAsStream("/assets-tiles/PuertaAbierta3.png"))));
            this.tiles[14] = ImageIO.read((Objects.requireNonNull(this.getClass().getResourceAsStream("/assets-tiles/PuertaAbierta4.png"))));
            // Nuevos tiles: 31-39 como suelo (usando pasto.png)
            this.tiles[31] = ImageIO.read((Objects.requireNonNull(this.getClass().getResourceAsStream("/assets-tiles/pasto.png"))));
            // Para 32-39, usar el mismo si no hay imágenes específicas
            for(int i = 32; i <= 39; i++) {
                this.tiles[i] = this.tiles[31];
            }
            this.tiles[50] = ImageIO.read((Objects.requireNonNull(this.getClass().getResourceAsStream("/assets-tiles/llave.png"))));
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public void draw(Graphics2D g, int cameraX, int cameraY, int[][]codeMapsTiles){
        draw(g, cameraX, cameraY, codeMapsTiles, false, false, false);
    }

    public void draw(Graphics2D g, int cameraX, int cameraY, int[][]codeMapsTiles, boolean requiresKey, boolean playerHasKey, boolean doorOpen){
        if (codeMapsTiles == null || codeMapsTiles.length == 0) return;
        
        int maxRen = codeMapsTiles.length;
        int maxCol = codeMapsTiles[0].length;

        // Calcular bounding box de la puerta si requiere llave
        int doorMinRen = Integer.MAX_VALUE;
        int doorMaxRen = Integer.MIN_VALUE;
        int doorMinCol = Integer.MAX_VALUE;
        int doorMaxCol = Integer.MIN_VALUE;
        if (requiresKey && !doorOpen) {
            for (int r = 0; r < maxRen; r++) {
                for (int c = 0; c < maxCol; c++) {
                    int tile = codeMapsTiles[r][c];
                    if (tile >= 12 && tile <= 14) {
                        doorMinRen = Math.min(doorMinRen, r);
                        doorMaxRen = Math.max(doorMaxRen, r);
                        doorMinCol = Math.min(doorMinCol, c);
                        doorMaxCol = Math.max(doorMaxCol, c);
                    }
                }
            }
        }
        
        int ren = 0, col = 0;
        int screenX, screenY;
        int worldX, worldY;
        int indexTile;

        while(ren < maxRen && col < maxCol){
            indexTile = codeMapsTiles[ren][col];

            // NO renderizar tiles 31-39 (son plataformas dinámicas, no tiles estáticos)
            // tampoco 30 (destino invisible), 40, 50 (llave, se dibuja como objeto)
            if(indexTile != 0 && indexTile != 30 && indexTile != 40 && indexTile != 50 &&
               !(indexTile >= 31 && indexTile <= 39)) {
                
                int actualTile = indexTile;
                if (requiresKey && !doorOpen && indexTile >= 12 && indexTile <= 14) {
                    // Determinar tile cerrado basado en posición
                    if (ren == doorMinRen && col == doorMinCol) {
                        actualTile = 10; // Puerta3 - Superior izquierda
                    } else if (ren == doorMinRen && col == doorMaxCol) {
                        actualTile = 11; // Puerta4 - Superior derecha
                    } else if (ren == doorMaxRen && col == doorMinCol) {
                        actualTile = 9; // Puerta2 - Abajo izquierda
                    } else if (ren == doorMaxRen && col == doorMaxCol) {
                        actualTile = 8; // Puerta1 - Abajo derecha
                    } else if (ren == doorMaxRen) {
                        actualTile = 8; // Puerta1 - Abajo
                    } else {
                        actualTile = 8; // Default Puerta1
                    }
                }
                
                worldX = col * gp.getSizeTile();
                worldY = ren * gp.getSizeTile();

                screenX = worldX - cameraX;
                screenY = worldY - cameraY;

                if(
                    worldX + gp.getSizeTile() > cameraX &&
                    worldX - gp.getSizeTile() < cameraX + gp.getWidthScreen() &&
                    worldY + gp.getSizeTile() > cameraY &&
                    worldY - gp.getSizeTile() < cameraY + gp.getHeightScreen()
                ) {
                    g.drawImage(tiles[actualTile], screenX, screenY, gp.getSizeTile(),
                            gp.getSizeTile(), null);
                }
            }

            col++;
            if(col == maxCol){
                col = 0;
                ren++;
            }
        }
    }
}
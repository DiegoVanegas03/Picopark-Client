package org.tiles;

import org.picopark.GamePanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

public class TilesEvents {
    private final int maxTiles = 15;
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
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public void draw(Graphics2D g, int cameraX, int cameraY, int[][]codeMapsTiles){
        int ren = 0, col = 0;
        int screenX, screenY;
        int worldX, worldY;
        int indexTile;

        while(ren < gp.getMaxRenWorld() && col < gp.getMaxColWorld()){
            indexTile = codeMapsTiles[ren][col];

            if(indexTile != 0) {
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
                    g.drawImage(tiles[indexTile], screenX, screenY, gp.getSizeTile(),
                            gp.getSizeTile(), null);
                }
            }

            col++;
            if(col == gp.getMaxColWorld()){
                col = 0;
                ren++;
            }
        }
    }
}
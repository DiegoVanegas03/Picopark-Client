package org.connection;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;
import java.util.Random;

public class PlayerData {
    Random random = new Random();
    private final int numero = random.nextInt(12)+1;

    private BufferedImage spriteStop, spriteLeftFirst, spriteRightFirst,
            spriteLeftSecond, spriteRightSecond;

    String id;
    String username;
    String direction = "stop";
    public boolean isVisible = true;
    float x;
    float y;
    int indexSprite = 1;


    private long lastSpriteUpdate = 0; // última vez que cambió el sprite
    private long spriteDelay = 200; // tiempo en ms entre cambios de sprite

    PlayerData(String id, String username, float x, float y) {
        this.id = id;
        this.username = username;
        this.x = x;
        this.y = y;
        this.getSpritesImages();
    }

    public String getUsername(){return this.username;}

    public float getWorldX(){return this.x;}
    public float getWorldY(){return this.y;}

    public void getSpritesImages(){
        try{
            this.spriteStop = ImageIO.read(Objects.requireNonNull(this.getClass().
                    getResourceAsStream("/assets-entities/Raro_der"+numero+".png")));

            this.spriteLeftFirst = ImageIO.read(Objects.requireNonNull(this.getClass().
                    getResourceAsStream("/assets-entities/Parados_izq"+numero+".png")));

            this.spriteLeftSecond = ImageIO.read(Objects.requireNonNull(this.getClass().
                    getResourceAsStream("/assets-entities/normal_izq"+numero+".png")));

            this.spriteRightFirst = ImageIO.read(Objects.requireNonNull(this.getClass().
                    getResourceAsStream("/assets-entities/Parados_der"+numero+".png")));

            this.spriteRightSecond = ImageIO.read(Objects.requireNonNull(this.getClass().
                    getResourceAsStream("/assets-entities/normal_der"+numero+".png")));
        }catch (IOException e){
            e.getStackTrace();
        }
    }

    public BufferedImage getDirectionImage() {
        return switch (direction) {
            case "left" -> switch (this.indexSprite){ case 1 -> spriteLeftFirst; case 2 ->spriteLeftSecond;
                default -> throw new IllegalStateException("Unexpected value: " + this.indexSprite);
            };
            case "right" -> switch (this.indexSprite){ case 1 -> spriteRightFirst; case 2 ->spriteRightSecond;
                default -> throw new IllegalStateException("Unexpected value: " + this.indexSprite);
            };
            default ->spriteStop;
        };
    }

    public void updateSprite() {
        long now = System.currentTimeMillis();
        if (now - lastSpriteUpdate >= spriteDelay) {
            indexSprite = indexSprite == 1 ? 2 : 1; // alterna sprites
            lastSpriteUpdate = now;
        }
    }
}

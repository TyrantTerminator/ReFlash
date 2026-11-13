package net.minecraft.client.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Test-only software renderer; never included in the real mod JAR. */
public class GuiGraphics {
  public int paintCount;
  private final BufferedImage image;
  public GuiGraphics(){this(1280,720);}
  public GuiGraphics(int width,int height){
    image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
    Graphics2D g=image.createGraphics();
    g.setColor(new Color(0xFF182432,true));
    g.fillRect(0,0,width,height);
    // Explicitly a test placeholder. A real replay's 3D world occupies this area.
    g.setColor(new Color(0xFF203548,true));
    for(int x=0;x<width;x+=24)g.drawLine(x,0,x,height);
    for(int y=0;y<height;y+=24)g.drawLine(0,y,width,y);
    g.dispose();
  }
  public void fill(int x0,int y0,int x1,int y1,int color){
    paintCount++;
    if(x1<=x0||y1<=y0)return;
    Graphics2D g=image.createGraphics();
    g.setColor(new Color(color,true));
    g.fillRect(x0,y0,x1-x0,y1-y0);
    g.dispose();
  }
  public void drawString(Font f,String text,int x,int y,int color,boolean shadow){
    paintCount++;
    Graphics2D g=image.createGraphics();
    g.setFont(new java.awt.Font("Monospaced",java.awt.Font.BOLD,11));
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g.setColor(new Color(color,true));
    g.drawString(text,x,y+10);
    g.dispose();
  }
  public void drawCenteredString(Font f,String text,int x,int y,int color){
    drawString(f,text,x-f.width(text)/2,y,color,false);
  }
  private static BufferedImage studioArt;
  public void blit(Object renderPipeline, net.minecraft.resources.Identifier texture,
                   int x,int y,float u,float v,int w,int h,int tw,int th){
      paintCount++;
      try {
        String which=String.valueOf(texture);
        BufferedImage art;
        if(which.contains("reflash_button")) art=ImageIO.read(Path.of("src/main/resources/assets/reflash/textures/gui/reflash_button.png").toFile());
        else if(which.contains("reflash_mark"))art=ImageIO.read(Path.of("src/main/resources/assets/reflash/textures/gui/reflash_mark.png").toFile());
        else { if(studioArt==null)studioArt=ImageIO.read(Path.of("src/main/resources/assets/reflash/textures/gui/studio_landscape.png").toFile()); art=studioArt; }
        Graphics2D gg=image.createGraphics();
        gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        gg.drawImage(art,x,y,w,h,null);
        gg.dispose();
      } catch(Exception ex){ fill(x,y,x+w,y+h,0xFF282540); }
  }
  /** Test-only illustrative world backdrop; the real mod uses Minecraft's live world. */
  public void previewWorld(int x,int y,int w,int h){
      try {
        if(studioArt==null)studioArt=ImageIO.read(Path.of("src/main/resources/assets/reflash/textures/gui/studio_landscape.png").toFile());
        Graphics2D gg=image.createGraphics();
        gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        gg.drawImage(studioArt,x,y,w,h,null);
        gg.dispose();
      } catch(Exception e){ throw new RuntimeException(e); }
  }
  public int guiWidth(){ return image.getWidth(); }
  public int guiHeight(){ return image.getHeight(); }
  public int getPixel(int x,int y){return image.getRGB(x,y);}
  public void save(Path file) throws Exception {ImageIO.write(image,"png",file.toFile());}
}

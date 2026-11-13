package dev.reflash.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Primitive-only cinematic GUI rendering.  All decoration is rectangles and small vector icons;
 * no framebuffers, blur, external image loads, or animated shader effects are required.
 * The only texture displayed inside the live editor is Minecraft's already-rendered world.
 */
public final class StudioTheme {
    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("reflash", "textures/gui/reflash_mark_24.png");
    private static final Identifier MINI_LOGO = Identifier.fromNamespaceAndPath("reflash", "textures/gui/reflash_button.png");
    private static final Identifier LANDSCAPE = Identifier.fromNamespaceAndPath("reflash", "textures/gui/studio_landscape.png");
    private StudioTheme() { }

    public static final int BASE = 0xFF070B15;
    public static final int HEADER = 0xFF0B101E;
    public static final int SIDEBAR = 0xFF0C1221;
    public static final int PANEL = 0xFF121A2C;
    public static final int PANEL_ALT = 0xFF172139;
    public static final int PANEL_HOVER = 0xFF263153;
    public static final int BORDER = 0xFF293853;
    public static final int TEXT = 0xFFF2F5FF;
    public static final int MUTED = 0xFF9DAFC8;
    public static final int SOFT = 0xFF60758E;
    public static final int CYAN = 0xFF3BDDF5;
    public static final int PURPLE = 0xFFAC73FF;
    public static final int PURPLE_DARK = 0xFF5427AD;
    public static final int AMBER = 0xFFFFBB65;
    public static final int GREEN = 0xFF70EFBB;
    public static final int RED = 0xFFFF668D;

    public static void rect(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w > 0 && h > 0) g.fill(x, y, x + w, y + h, color);
    }
    public static void rounded(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        if (w < 5 || h < 5) { rect(g, x,y,w,h,color); return; }
        rect(g, x + 3, y, w - 6, h, color);
        rect(g, x + 1, y + 1, w - 2, h - 2, color);
        rect(g, x, y + 3, w, h - 6, color);
    }
    public static void border(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w < 4 || h < 4) return;
        rect(g, x + 3, y, w - 6, 1, color);
        rect(g, x + 3, y + h - 1, w - 6, 1, color);
        rect(g, x, y + 3, 1, h - 6, color);
        rect(g, x + w - 1, y + 3, 1, h - 6, color);
        rect(g, x + 1, y + 1, 2, 1, color);
        rect(g, x + w - 3, y + 1, 2, 1, color);
        rect(g, x + 1, y + h - 2, 2, 1, color);
        rect(g, x + w - 3, y + h - 2, 2, 1, color);
    }
    public static void panel(GuiGraphics g, int x,int y,int w,int h) {
        rounded(g,x,y,w,h,PANEL);
        border(g,x,y,w,h,BORDER);
    }
    public static void softPanel(GuiGraphics g, int x,int y,int w,int h) {
        rounded(g,x+2,y+3,w,h,0x42000000);
        rounded(g,x,y,w,h,PANEL);
        border(g,x,y,w,h,0xFF2A3D5C);
    }
    public static void lightStrip(GuiGraphics g,int x,int y,int w,int color) {
        rect(g,x,y,w,1,color);
        if (w >= 14) rect(g,x+5,y+1,w-10,1,(color & 0x00FFFFFF) | 0x44000000);
    }
    public static void bar(GuiGraphics g,int x,int y,int w,int h,int fill,int total,int color) {
        rounded(g,x,y,w,h,0xFF293650);
        int amount = Math.min(w, Math.max(0, (int)((long)w * fill / Math.max(1,total))));
        if(amount>0) rounded(g,x,y,amount,h,color);
    }
    public static void gradient(GuiGraphics g,int x,int y,int w,int h,int rgb1,int rgb2,int strips) {
        strips = Math.max(1,Math.min(64,strips));
        int r1=(rgb1>>>16)&255,g1=(rgb1>>>8)&255,b1=rgb1&255;
        int r2=(rgb2>>>16)&255,g2=(rgb2>>>8)&255,b2=rgb2&255;
        for(int i=0;i<strips;i++) {
            float t= strips==1?0:(float)i/(strips-1);
            int r=(int)(r1+(r2-r1)*t),gg=(int)(g1+(g2-g1)*t),b=(int)(b1+(b2-b1)*t);
            int a=x+w*i/strips, end=x+w*(i+1)/strips;
            rect(g,a,y,Math.max(0,end-a),h,0xFF000000|(r<<16)|(gg<<8)|b);
        }
    }
    public static void label(GuiGraphics g,Font f,String s,int x,int y,int color){g.drawString(f,s,x,y,color,false);}
    public static void centered(GuiGraphics g,Font f,String s,int x,int y,int color){g.drawCenteredString(f,s,x,y,color);}
    public static void diamond(GuiGraphics g, int x,int y,int r,int color){
        for(int i=-r;i<=r;i++){int half=r-Math.abs(i); rect(g,x-half,y+i,half*2+1,1,color);}
    }
    public static void chip(GuiGraphics g,Font f,String s,int x,int y,int w,int color){
        rounded(g,x,y,w,19,0xFF1C2942); border(g,x,y,w,19,0xFF385171);
        rect(g,x+7,y+7,4,4,color);
        label(g,f,s,x+17,y+5,TEXT);
    }
    /** Tiny scalable icons constructed only from GuiGraphics.fill. */
    public static void icon(GuiGraphics g,String name,int x,int y,int color){
        switch(name){
            case "home" -> { rect(g,x+3,y+6,10,8,color);rect(g,x+1,y+5,3,3,color);rect(g,x+12,y+5,3,3,color);rect(g,x+6,y+10,4,5,BASE);}
            case "record" -> {rounded(g,x+3,y+3,10,10,color);rounded(g,x+6,y+6,4,4,RED);}
            case "library" -> {rect(g,x+2,y+5,12,10,color);rect(g,x+4,y+3,5,3,color);rect(g,x+4,y+8,8,1,BASE);}
            case "editor" -> {rect(g,x+1,y+4,14,10,color);for(int a=0;a<4;a++)rect(g,x+3+a*3,y+4,1,3,BASE);rect(g,x+5,y+9,2,3,BASE);rect(g,x+9,y+9,2,3,BASE);}
            case "settings" -> {rect(g,x+6,y+1,4,14,color);rect(g,x+1,y+6,14,4,color);rounded(g,x+4,y+4,8,8,color);rounded(g,x+6,y+6,4,4,BASE);}
            case "performance" -> {rect(g,x+1,y+10,3,4,color);rect(g,x+6,y+6,3,8,color);rect(g,x+11,y+3,3,11,color);}
            case "export" -> {rect(g,x+2,y+12,12,3,color);rect(g,x+7,y+3,2,10,color);rect(g,x+4,y+8,3,2,color);rect(g,x+9,y+8,3,2,color);}
            case "play" -> {for(int i=0;i<9;i++)rect(g,x+4+i/3,y+3+i,2,1,color);}
            case "camera" -> {rect(g,x+1,y+5,14,9,color);rect(g,x+4,y+3,6,2,color);rounded(g,x+5,y+7,6,5,BASE);}
            case "spark" -> {rect(g,x+7,y+1,2,14,color);rect(g,x+1,y+7,14,2,color);diamond(g,x+8,y+8,4,color);}
            case "folder" -> {rect(g,x+1,y+5,14,9,color);rect(g,x+3,y+3,6,3,color);}
            case "close" -> {for(int i=0;i<10;i++){rect(g,x+3+i,y+3+i,1,1,color);rect(g,x+12-i,y+3+i,1,1,color);}}
            default -> diamond(g,x+8,y+8,5,color);
        }
    }
    /** Branding is a bundled 256px texture. Drawn only when a GUI is open. */
    public static void logo(GuiGraphics g, int x, int y, int size) {
        if (size > 0) g.blit(RenderPipelines.GUI_TEXTURED, LOGO, x, y, 0, 0, size, size, 24, 24);
    }
    public static void tinyLogo(GuiGraphics g, int x, int y) {
        g.blit(RenderPipelines.GUI_TEXTURED, MINI_LOGO, x, y, 0, 0, 16, 16, 16, 16);
    }
    /** Decorative image bundled with the mod, NOT a screenshot of a recorded replay.
     *  The same fixed 1024x512 texture is only rendered while Studio's menu is open.
     */
    public static void sceneArt(GuiGraphics g,int x,int y,int w,int h,boolean showCaption){
        if(w<30||h<25)return;
        g.blit(RenderPipelines.GUI_TEXTURED,LANDSCAPE,x,y,0,0,w,h,1024,512);
        if(showCaption) rect(g,x,y+h-26,w,26,0xC5081021);
    }
}

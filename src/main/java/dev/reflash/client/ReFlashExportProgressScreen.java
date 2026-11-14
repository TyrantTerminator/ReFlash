package dev.reflash.client;

import dev.reflash.core.VideoExportLibrary;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Desktop;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ReFlash's GUI-only replacement for Momenton's native finishing/error pages.
 * The actual replay rendering engine and audio muxer are NOT modified.
 * This screen is installed only for exports the user started through ReFlash.
 */
public final class ReFlashExportProgressScreen extends Screen {
    private enum Phase { FINISHING, COMPLETE, FAILED, CANCELLED }
    private final Screen parent;
    private final Object finisher;
    private Phase phase;
    private String detail;
    private int screenTicks;
    private int displayedPercent;
    private Button mainButton, folderButton;

    private ReFlashExportProgressScreen(Screen parent,Object finisher,Phase phase,String detail) {
        super(Component.literal("ReFlash Export Studio"));
        this.parent=parent;
        this.finisher=finisher;
        this.phase=phase;
        this.detail=detail;
    }
    public static ReFlashExportProgressScreen finishing(Screen parent,Object finisher) {
        return new ReFlashExportProgressScreen(parent,finisher,Phase.FINISHING,"Finishing video and audio in the background");
    }
    public static ReFlashExportProgressScreen failed(Screen parent,String message) {
        return new ReFlashExportProgressScreen(parent,null,Phase.FAILED,message);
    }
    private Object get(String name) throws ReflectiveOperationException {
        return finisher.getClass().getMethod(name).invoke(finisher);
    }
    private void cancel() {
        if(phase==Phase.FINISHING && finisher!=null) {
            try { finisher.getClass().getMethod("cancel").invoke(finisher);detail="Cancelling export safely..."; }
            catch(ReflectiveOperationException | RuntimeException e) {phase=Phase.FAILED;detail="Could not cancel export: "+e.getMessage();}
        } else returnToStudio();
    }
    private void returnToStudio() { minecraft.setScreen(parent==null?new ReFlashScreen(null):parent); }
    private void openVideoLibrary(){
        if(phase==Phase.FINISHING){detail="Finish rendering before opening videos";return;}
        minecraft.setScreen(ReFlashScreen.videos(parent));
    }
    private void openFolder() {
        try {
            Path folder=VideoExportLibrary.folder(FabricLoader.getInstance().getGameDir());
            Files.createDirectories(folder);
            if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
                Desktop.getDesktop().open(folder.toFile());
            else detail="Video folder: "+folder;
        } catch (Exception e) {detail="Cannot open folder. Find videos in reflash/exports.";}
    }
    @Override protected void init() {
        int w=Math.min(575,Math.max(235,width-28)),cx=width/2, y=height/2+73;
        int bw=Math.max(94,(w-42)/2);
        mainButton=addRenderableWidget(Button.builder(Component.literal("Back to ReFlash"),b->cancel())
            .bounds(cx-bw-4,y,bw,26).build());
        folderButton=addRenderableWidget(Button.builder(Component.literal("Open Videos"),b->openVideoLibrary())
            .bounds(cx+4,y,bw,26).build());
        mainButton.setAlpha(0f);
        folderButton.setAlpha(0f);
    }
    @Override public void tick() {
        screenTicks++;
        if(phase!=Phase.FINISHING || finisher==null)return;
        try {
            boolean done=Boolean.TRUE.equals(get("done"));
            Object p=get("progress");
            if(p instanceof Number n) displayedPercent=Math.clamp((int)Math.round(n.doubleValue()*100),0,100);
            if(done) {
                Object err=get("error");
                boolean cancelled=Boolean.TRUE.equals(get("cancelled"));
                phase=cancelled?Phase.CANCELLED:err!=null?Phase.FAILED:Phase.COMPLETE;
                displayedPercent=phase==Phase.COMPLETE?100:displayedPercent;
                detail=cancelled?"Export cancelled. Your original replay is safe.":
                    err!=null?"Video encoding failed: "+((Throwable)err).getMessage():
                    "Export complete. Find your video in the Videos folder.";
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            phase=Phase.FAILED;
            detail="Cannot read the export status. Check reflash/export.log.";
        }
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        // Only simple rectangles and text: no blur, shader effects, or renderer interception.
        StudioTheme.rect(g,0,0,width,height,0xED070B15);
        int w=Math.min(575,Math.max(235,width-28)), x=(width-w)/2, y=height/2-115;
        StudioTheme.rounded(g,x+3,y+5,w,223,0x6B000000);
        StudioTheme.rounded(g,x,y,w,223,StudioTheme.BASE);
        StudioTheme.border(g,x,y,w,223,StudioTheme.BORDER);
        StudioTheme.gradient(g,x+1,y+1,w-2,3,StudioTheme.CYAN,StudioTheme.PURPLE,36);
        StudioTheme.logo(g,x+14,y+10,35);
        StudioTheme.label(g,font,"ReFlash Export Studio",x+57,y+16,StudioTheme.TEXT);
        StudioTheme.label(g,font,"VIDEO FINISHING  /  ENCODING  /  SAVE",x+57,y+35,StudioTheme.MUTED);
        int panelY=y+62;
        StudioTheme.softPanel(g,x+14,panelY,w-28,111);
        String heading=switch(phase) {
            case FINISHING -> "FINISHING YOUR VIDEO";
            case COMPLETE -> "EXPORT COMPLETE";
            case FAILED -> "EXPORT COULD NOT FINISH";
            case CANCELLED -> "EXPORT CANCELLED";
        };
        int accent=phase==Phase.FAILED?StudioTheme.RED:phase==Phase.COMPLETE?StudioTheme.GREEN:StudioTheme.PURPLE;
        StudioTheme.label(g,font,heading,x+27,panelY+16,accent);
        String text=detail==null?"Working...":detail;
        if(font.width(text)>w-65) {
            while(text.length()>5 && font.width(text+"...")>w-65)text=text.substring(0,text.length()-1);
            text+="...";
        }
        StudioTheme.label(g,font,text,x+27,panelY+42,StudioTheme.TEXT);
        int barW=Math.max(80,w-64),barY=panelY+75;
        StudioTheme.rounded(g,x+27,barY,barW,9,StudioTheme.PANEL_ALT);
        int barFill=Math.max(0,barW*displayedPercent/100);
        if(phase==Phase.FINISHING && displayedPercent==0)barFill=Math.max(5,(screenTicks*3)%(Math.max(8,barW/4)));
        if(barFill>0)StudioTheme.rounded(g,x+27,barY,barFill,9,accent);
        StudioTheme.label(g,font,phase==Phase.FINISHING?displayedPercent+"%":phase==Phase.COMPLETE?"100%":"—",x+27,panelY+90,StudioTheme.MUTED);
        // Draw native clickable widgets invisibly, then draw matching ReFlash skins.
        super.render(g,mouseX,mouseY,partial);
        drawAction(g,mainButton,phase==Phase.FINISHING?"Cancel Export":"Back to ReFlash",false);
        drawAction(g,folderButton,"Open Videos",true);
    }
    private void drawAction(GuiGraphics g,Button b,String label,boolean accent) {
        if(b==null)return;
        int x=b.getX(),y=b.getY(),w=b.getWidth(),h=b.getHeight();
        boolean hovered=b.isHoveredOrFocused();
        StudioTheme.rounded(g,x,y,w,h,accent?StudioTheme.PURPLE_DARK:StudioTheme.PANEL_ALT);
        StudioTheme.border(g,x,y,w,h,hovered?StudioTheme.CYAN:accent?StudioTheme.PURPLE:StudioTheme.BORDER);
        StudioTheme.centered(g,font,label,x+w/2,y+(h-9)/2,StudioTheme.TEXT);
    }
    @Override public void onClose() { if(phase==Phase.FINISHING)cancel(); else returnToStudio(); }
    @Override public boolean isPauseScreen() { return false; }
}

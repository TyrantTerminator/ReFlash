package dev.reflash.client;

import net.minecraft.client.gui.GuiGraphics;

/** Checks design assets and transparency against the actual Java GUI drawing code.
 *  This is a software-rendered visual contract, not a Minecraft rendering test.
 */
public final class VisualContractTest {
    private static void check(boolean valid,String message){if(!valid)throw new AssertionError(message);}
    public static void main(String[] args) {
        var studio=new ReFlashScreen(null);
        studio.width=1280;studio.height=720;studio.init();
        GuiGraphics sg=new GuiGraphics(1280,720);
        studio.render(sg,0,0,0);
        // The real menu must draw the bundled landscape and both cyan/purple chrome.
        check(sg.paintCount>85,"Studio missed most of its graphical elements");
        int artA=sg.getPixel(950,200),artB=sg.getPixel(1040,200);
        check(artA!=artB,"Studio landscape texture did not render");
        check(sg.getPixel(80,150)!=0,"Navigation not drawn");
        var editor=new ReFlashEditorScreen(null);
        editor.width=1280;editor.height=720;editor.init();
        GuiGraphics eg=new GuiGraphics(1280,720);
        int worldPixelBefore=eg.getPixel(325,207);
        editor.render(eg,0,0,0);
        // The active editor deliberately leaves most of the game's world transparent.
        check(eg.getPixel(325,207)==worldPixelBefore,"Editor obscured live world with fake footage");
        check(eg.paintCount>75,"Editor missing its panels, inspector or tracks");
        check(eg.getPixel(600,530)!=eg.getPixel(600,580),"Timeline has no distinct tracks");
        System.out.println("PASS: v0.8 Studio asset, cyan/purple chrome, transparent live viewport and timeline lanes");
    }
}

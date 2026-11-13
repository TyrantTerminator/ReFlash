package net.fabricmc.fabric.api.client.rendering.v1.hud;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
public interface HudElementRegistry {
    @FunctionalInterface interface Element { void render(GuiGraphics graphics,Object tracker); }
    final class Holder { public static Element last; }
    static void addLast(Identifier id, Element renderer) { Holder.last = renderer; }
}

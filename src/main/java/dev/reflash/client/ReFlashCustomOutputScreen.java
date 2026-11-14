package dev.reflash.client;

import dev.reflash.core.Preferences;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** User-defined final video output parameters; the replay capture stays lightweight. */
public final class ReFlashCustomOutputScreen extends Screen {
    private final Screen parent;
    private EditBox widthBox, heightBox, fpsBox, qualityBox;
    private String error = "";

    public ReFlashCustomOutputScreen(Screen parent) {
        super(Component.literal("ReFlash Custom Output"));
        this.parent = parent;
    }

    private EditBox field(int x, int y, int w, String label, int initial) {
        EditBox box = new EditBox(font, x, y, w, 22, Component.literal(label));
        box.setMaxLength(5);
        box.setValue(Integer.toString(initial));
        return addRenderableWidget(box);
    }

    @Override protected void init() {
        Preferences p = ReFlashClient.preferences();
        int w = Math.min(420, width - 26), x = (width - w) / 2, y = height / 2 - 113;
        int fw = Math.max(60, (w - 36) / 2);
        widthBox = field(x + 12, y + 49, fw, "Width", p.outputWidth());
        heightBox = field(x + 24 + fw, y + 49, fw, "Height", p.outputHeight());
        fpsBox = field(x + 12, y + 106, fw, "FPS", p.outputFps());
        qualityBox = field(x + 24 + fw, y + 106, fw, "Quality CRF", p.qualityCrf());
        addRenderableWidget(Button.builder(Component.literal("Save Custom Output"), b -> save())
                .bounds(x + 12, y + 166, fw, 26).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(x + 24 + fw, y + 166, fw, 26).build());
    }

    private void save() {
        try {
            int w = Integer.parseInt(widthBox.getValue().trim());
            int h = Integer.parseInt(heightBox.getValue().trim());
            int fps = Integer.parseInt(fpsBox.getValue().trim());
            int crf = Integer.parseInt(qualityBox.getValue().trim());
            Preferences p = ReFlashClient.preferences();
            // Both validators run before we save any preferences.
            if (crf < 14 || crf > 36) throw new IllegalArgumentException("Quality CRF: 14 to 36");
            if (fps < 30 || fps > 240) throw new IllegalArgumentException("FPS must be 30 to 240");
            p.setOutputPreset(w, h, fps);
            p.setCustomCrf(crf);
            ReFlashClient.savePreferences();
            minecraft.setScreen(parent);
        } catch (NumberFormatException e) {
            error = "Enter whole numbers for size, FPS, and quality";
        } catch (IllegalArgumentException e) {
            error = e.getMessage();
        }
    }

    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        int w = Math.min(420, width - 26), x = (width - w) / 2, y = height / 2 - 113;
        StudioTheme.rounded(g, x, y, w, 225, StudioTheme.BASE);
        StudioTheme.border(g, x, y, w, 225, StudioTheme.PURPLE);
        StudioTheme.label(g, font, "CUSTOM EXPORT VIDEO SETTINGS", x + 12, y + 14, StudioTheme.TEXT);
        StudioTheme.label(g, font, "WIDTH (px)", x + 12, y + 34, StudioTheme.CYAN);
        StudioTheme.label(g, font, "HEIGHT (px)", x + 24 + Math.max(60, (w - 36) / 2), y + 34, StudioTheme.CYAN);
        StudioTheme.label(g, font, "FRAME RATE 30-240", x + 12, y + 91, StudioTheme.CYAN);
        StudioTheme.label(g, font, "CRF 14-36 (lower = better)", x + 24 + Math.max(60, (w - 36) / 2), y + 91, StudioTheme.CYAN);
        StudioTheme.label(g, font, "Native replay uses current window size on Vulkan.", x + 12, y + 143, StudioTheme.MUTED);
        StudioTheme.label(g, font, "Replay render uses window size; resize only if requested.", x + 12, y + 155, StudioTheme.MUTED);
        if (!error.isBlank()) StudioTheme.label(g, font, error, x + 12, y + 207, StudioTheme.RED);
        super.render(g, mx, my, delta);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}

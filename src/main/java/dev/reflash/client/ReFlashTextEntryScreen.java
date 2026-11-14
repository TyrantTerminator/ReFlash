package dev.reflash.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

/** Native single-line text entry for recording and video renaming. */
public final class ReFlashTextEntryScreen extends Screen {
    private final Screen parent;
    private final String heading;
    private final String initial;
    private final Function<String, String> submit;
    private EditBox input;
    private String error = "";

    public ReFlashTextEntryScreen(Screen parent, String heading, String initial,
                                 Function<String, String> submit) {
        super(Component.literal(heading));
        this.parent = parent;
        this.heading = heading;
        this.initial = initial;
        this.submit = submit;
    }

    @Override protected void init() {
        int w = Math.min(310, width - 26), x = (width - w) / 2, y = height / 2 - 52;
        input = new EditBox(font, x + 10, y + 45, w - 20, 22, Component.literal("New name"));
        input.setMaxLength(90);
        input.setValue(initial);
        addRenderableWidget(input);
        addRenderableWidget(Button.builder(Component.literal("Rename"), b -> {
            String result = submit.apply(input.getValue());
            if (result == null) minecraft.setScreen(parent);
            else error = result;
        }).bounds(x + 10, y + 84, Math.max(65, (w - 30) / 2), 24).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(x + w / 2 + 5, y + 84, Math.max(65, (w - 30) / 2), 24).build());
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int w = Math.min(310, width - 26), x = (width - w) / 2, y = height / 2 - 52;
        StudioTheme.rounded(g, x, y, w, 130, StudioTheme.BASE);
        StudioTheme.border(g, x, y, w, 130, StudioTheme.PURPLE);
        StudioTheme.label(g, font, heading, x + 12, y + 12, StudioTheme.TEXT);
        if (!error.isBlank()) StudioTheme.label(g, font, error, x + 12, y + 115, StudioTheme.RED);
        super.render(g, mouseX, mouseY, delta);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}

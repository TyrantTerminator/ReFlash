package dev.reflash.client;

import dev.reflash.core.PauseMenuLayout;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;

/** One native button in the top-right of pause menu. Gameplay HUD is untouched. */
public final class ReFlashPauseMenu {
    private ReFlashPauseMenu() { }

    public static void install() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof PauseScreen)) return;
            PauseMenuLayout pos = PauseMenuLayout.forViewport(scaledWidth, scaledHeight);
            Screens.getButtons(screen).add(Button.builder(
                    Component.literal("◆ ReFlash").withStyle(ChatFormatting.AQUA),
                    b -> client.setScreen(new ReFlashScreen(screen)))
                    .bounds(pos.x(), pos.y(), pos.width(), pos.buttonHeight()).build());
        });
    }
}

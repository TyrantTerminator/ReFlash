package dev.reflash.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** One 22x22 clickable branded icon in each menu. Never draws an always-on gameplay overlay. */
public final class ReFlashPauseMenu {
    private ReFlashPauseMenu() { }
    private static final Map<Screen, WeakReference<Button>> ACTIVE = new WeakHashMap<>();
    private static final Set<Screen> HOOKED = Collections.newSetFromMap(new WeakHashMap<>());

    public static void install() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            boolean isPause = screen instanceof PauseScreen;
            if (!isPause && !(screen instanceof TitleScreen)) return;
            int x = isPause ? width - 30 : 8;
            int y = isPause ? 8 : 35;
            Button button = Button.builder(Component.empty(), ignored ->
                    client.setScreen(new ReFlashScreen(screen)))
                    .bounds(x, y, 22, 22).build();
            button.setTooltip(Tooltip.create(Component.literal(
                    "ReFlash Manager - " + ReFlashClient.managerKeyName())));
            Screens.getButtons(screen).add(button);
            ACTIVE.put(screen, new WeakReference<>(button));
            if (HOOKED.add(screen)) {
                ScreenEvents.afterRender(screen).register((rendered, graphics, mouseX, mouseY, delta) -> {
                    WeakReference<Button> ref = ACTIVE.get(rendered);
                    Button current = ref == null ? null : ref.get();
                    if (current != null && current.visible) {
                        StudioTheme.tinyLogo(graphics, current.getX() + 3, current.getY() + 3);
                    }
                });
            }
        });
    }
}

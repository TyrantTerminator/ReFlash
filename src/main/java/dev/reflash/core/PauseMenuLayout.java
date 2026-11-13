package dev.reflash.core;

/** Compact, single pause-menu launcher anchored outside the vanilla central grid. */
public record PauseMenuLayout(int x, int y, int width, int buttonHeight) {
    public static PauseMenuLayout forViewport(int screenWidth, int screenHeight) {
        int w = Math.min(92, Math.max(60, screenWidth / 4));
        int x = Math.max(3, screenWidth - w - 6);
        int y = screenHeight >= 150 ? 8 : 3;
        return new PauseMenuLayout(x, y, w, 20);
    }
}

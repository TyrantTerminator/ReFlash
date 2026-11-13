package dev.reflash.core;

/**
 * Immutable responsive placement for the lightweight in-game studio.
 * No Minecraft classes, allocations in the rendering hot path, or scaling assumptions.
 */
public record StudioLayout(int x, int y, int width, int height, boolean sideNav,
                           int contentX, int contentY, int contentWidth, int contentHeight) {
    public static StudioLayout forViewport(int viewWidth, int viewHeight) {
        int w = Math.max(1, Math.min(1160, viewWidth - 12));
        int h = Math.max(1, Math.min(660, viewHeight - 12));
        int x = (viewWidth - w) / 2;
        int y = (viewHeight - h) / 2;
        boolean side = w >= 612 && h >= 310;
        int cx = x + (side ? (w >= 760 ? 204 : 164) : 14);
        int cy = y + (side ? 78 : 91);
        int cw = w - (side ? (w >= 760 ? 226 : 184) : 28);
        int ch = h - (side ? 102 : 105);
        return new StudioLayout(x, y, w, h, side, cx, cy,
                Math.max(1, cw), Math.max(1, ch));
    }

    public boolean roomy() { return contentHeight >= 225; }
    public boolean ultraCompact() { return contentHeight < 158; }
}

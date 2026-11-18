package dev.reflash.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Function;

/** Exact export FPS input in the existing export-screen flow. No recording FPS changes. */
public final class ReFlashFps74 {
    private ReFlashFps74() {}
    static int parse(String input) {
        if (input == null || !input.trim().matches("[0-9]{1,3}"))
            throw new IllegalArgumentException("Enter a whole number from 1 to 240");
        int fps = Integer.parseInt(input.trim());
        if (fps < 1 || fps > 240)
            throw new IllegalArgumentException("Export FPS must be 1 to 240");
        return fps;
    }
    static String actionLabel(Object dialog) {
        try { if (String.valueOf(ReFlashUI38.val(dialog, "heading")).startsWith("Export frame rate")) return "APPLY FPS"; }
        catch (ReflectiveOperationException ignored) {}
        return "Rename";
    }
    static String inputLabel(Object dialog) {
        try { if (String.valueOf(ReFlashUI38.val(dialog, "heading")).startsWith("Export frame rate")) return "FPS"; }
        catch (ReflectiveOperationException ignored) {}
        return "New name";
    }
    public static void install(Object screen) {
        try {
            var f = ReFlashExportPanel60.class.getDeclaredField("sections");
            f.setAccessible(true);
            Object v = ((Map<?, ?>) f.get(null)).get(screen);
            if (v == null) v = "FPS";
            if (!"FPS".equals(v)) return;
            int[] d = ReFlashUI38.box(screen);
            if (d[2] < 160 || d[3] < 310) return; // Do not add a row over export buttons on tiny screens.
            int width = Math.min(202, d[2] - 20);
            int x = d[0] + 6;
            int y = d[1] + 220;
            int current = currentFps();
            ReFlashPanel61.button(screen,x,y,width,25,"CUSTOM FPS ("+current+")","SECONDARY",()->open(screen));
        } catch (Throwable failure) { System.err.println("[ReFlash 74] FPS input unavailable: " + failure); }
    }
    private static int currentFps() throws ReflectiveOperationException {
        Object p=ReFlashUI38.prefs();
        return ((Number)ReFlashUI38.call(p,"outputFps")).intValue();
    }
    private static String submit(Object screen,String text) {
        final int fps;
        try { fps = parse(text); }
        catch (IllegalArgumentException e) { return e.getMessage(); }
        try {
            Object p=ReFlashUI38.prefs();
            int w=((Number)ReFlashUI38.call(p,"outputWidth")).intValue();
            int h=((Number)ReFlashUI38.call(p,"outputHeight")).intValue();
            p.getClass().getMethod("setOutputPreset",int.class,int.class,int.class).invoke(p,w,h,fps);
            ReFlashUI38.savePrefs();
            return null;
        } catch (ReflectiveOperationException e) {return "Unable to save FPS: " + e.getClass().getSimpleName();}
    }
    private static void open(Object screen) {
        try {
            Class<?> screenClass=Class.forName("net.minecraft.class_437");
            Class<?> inputScreen=Class.forName("dev.reflash.client.ReFlashTextEntryScreen");
            Constructor<?> ctor=inputScreen.getConstructor(screenClass,String.class,String.class,Function.class);
            Object dialog=ctor.newInstance(screen,"Export frame rate (1-240)",Integer.toString(currentFps()),(Function<String,String>)(s)->submit(screen,s));
            Object mc=ReFlashUI38.val(screen,"field_22787");
            Method setScreen=mc.getClass().getMethod("method_1507",screenClass);
            setScreen.invoke(mc,dialog);
        } catch (Throwable failure) { System.err.println("[ReFlash 74] Could not open custom FPS: "+failure); }
    }
}

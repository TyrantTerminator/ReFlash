package dev.reflash.client;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/** Only hides directly overlapping secondary buttons in very narrow viewports. */
public final class ReFlashResponsive74 {
    private ReFlashResponsive74() {}
    static boolean overlaps(int x,int y,int w,int h,int ax,int ay,int aw,int ah) {
        return x<ax+aw && x+w>ax && y<ay+ah && y+h>ay;
    }
    public static void apply(Object screen) {
        try {
            int sw=((Number)ReFlashUI38.val(screen,"field_22789")).intValue();
            int sh=((Number)ReFlashUI38.val(screen,"field_22790")).intValue();
            if(sw>=760 && sh>=460) return;
            Object o=ReFlashUI38.val(screen,"controls");
            if(!(o instanceof List<?> controls)) return;
            List<int[]> seen = new ArrayList<>();
            for(Object entry:controls) {
                Object button=ReFlashUI38.call(entry,"widget");
                int x=((Number)ReFlashUI38.call(button,"method_46426")).intValue();
                int y=((Number)ReFlashUI38.call(button,"method_46427")).intValue();
                int w=((Number)ReFlashUI38.call(button,"method_25368")).intValue();
                int h=((Number)ReFlashUI38.call(button,"method_25364")).intValue();
                if (w<=0 || h<=0) continue;
                boolean collide = x<0||y<0||x+w>sw||y+h>sh;
                for (int[] r:seen) if (overlaps(x,y,w,h,r[0],r[1],r[2],r[3])) { collide = true; break; }
                Field visible=ReFlashUI38.field(button.getClass(),"field_22764");
                if (collide) visible.setBoolean(button,false);
                else seen.add(new int[]{x,y,w,h});
            }
        } catch (Throwable error) { System.err.println("[ReFlash 74] Compact layout guard failed: "+error); }
    }
}

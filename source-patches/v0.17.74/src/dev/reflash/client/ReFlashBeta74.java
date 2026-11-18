package dev.reflash.client;

/** One-time session notice. The X uses the same mouse coordinates as existing UI widgets. */
public final class ReFlashBeta74 {
    private static boolean dismissed;
    private ReFlashBeta74() {}
    private static int[] bounds(Object screen) throws ReflectiveOperationException {
        int w=((Number)ReFlashUI38.val(screen,"field_22789")).intValue();
        int h=((Number)ReFlashUI38.val(screen,"field_22790")).intValue();
        int bw=Math.min(360,Math.max(100,w-16));
        int bh=Math.min(108,Math.max(64,h-16));
        return new int[]{(w-bw)/2,(h-bh)/2,bw,bh};
    }
    public static void draw(Object screen,Object ctx) {
        if (dismissed) return;
        try {
            int[] b=bounds(screen);
            ReFlashUI38.rect(ctx,b[0]-2,b[1]-2,b[2]+4,b[3]+4,0xFF6B5EA8);
            ReFlashUI38.rect(ctx,b[0],b[1],b[2],b[3],0xFF111522);
            ReFlashUI38.rect(ctx,b[0]+b[2]-26,b[1]+8,18,18,0xFF302B48);
            Object font=ReFlashUI38.font(screen);
            ReFlashUI38.label(ctx,font,"X",b[0]+b[2]-20,b[1]+13,0xFFFFFFFF);
            ReFlashUI38.label(ctx,font,"REFLASH v0.17.74",b[0]+12,b[1]+13,0xFFEEF0FF);
            ReFlashUI38.label(ctx,font,"PRECISION FPS  |  PRIVATE BETA",b[0]+12,b[1]+35,0xFFF1C778);
            if(b[3]>=94) {
                ReFlashUI38.label(ctx,font,"Experimental: report bugs before sharing.",b[0]+12,b[1]+59,0xFFBFC1D0);
                ReFlashUI38.label(ctx,font,"1-240 FPS export settings. Close with X.",b[0]+12,b[1]+75,0xFFBFC1D0);
            }
        } catch (Throwable failure) { dismissed=true; System.err.println("[ReFlash 74] Beta notice disabled: "+failure); }
    }
    public static boolean mouse(Object screen,Object event) {
        if (dismissed) return false;
        try {
            int button=((Number)ReFlashUI38.call(event,"method_74245")).intValue();
            if(button!=0) return true;
            double mx=((Number)ReFlashUI38.call(event,"comp_4798")).doubleValue();
            double my=((Number)ReFlashUI38.call(event,"comp_4799")).doubleValue();
            int[] b=bounds(screen);
            if(mx>=b[0]+b[2]-30 && mx < b[0]+b[2]-3 && my>=b[1]+3 && my < b[1]+32) dismissed=true;
            return true; // Modal: do not let clicks through to controls behind the popup.
        } catch (ReflectiveOperationException failure) { return false; }
    }
}

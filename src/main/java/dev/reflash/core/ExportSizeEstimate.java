package dev.reflash.core;

import java.util.Locale;

/** Advisory compressed-video size band, never an exact promise or a native Vulkan render estimate. */
public final class ExportSizeEstimate {
    private ExportSizeEstimate(){}
    public record Result(double seconds,double lowMB,double highMB,long frames) {
        public boolean available(){return seconds>0;}
        public String summary(){
            if(!available())return "Choose a replay";
            return durationLabel()+"  •  approx "+fileSize(lowMB)+"–"+fileSize(highMB)+"  •  "+frames+" frames";
        }
        public String durationLabel(){
            if(seconds>=60)return String.format(Locale.ROOT,"%dm %02ds",(int)seconds/60,(int)seconds%60);
            return String.format(Locale.ROOT,"%.1fs",seconds);
        }
    }
    public static Result calculate(int ticks,int width,int height,int fps,UpscaleProfile profile){
        if(ticks<=0)return new Result(0,0,0,0);
        if(width<640||height<360||width>3840||height>2160||fps<24||fps>240||profile==null)
            throw new IllegalArgumentException("Invalid export configuration");
        double seconds=ticks/20.0;
        // Baseline 0.095 bits/pixel/frame at Balanced. H.264 CRF is variable-rate,
        // so give a deliberately broad 0.5x - 2.0x band rather than an exact size.
        double factor=switch(profile){case FAST->0.65;case BALANCED->1.0;case SHARP->1.55;case ARCHIVE->2.25;};
        double mbitsPerSecond=width*(double)height*fps*0.095*factor/1_000_000.0;
        double audioMbitsPerSecond=0.16;
        double lowMB=seconds*(mbitsPerSecond*0.5+audioMbitsPerSecond)/8.0;
        double highMB=seconds*(mbitsPerSecond*2.0+audioMbitsPerSecond)/8.0;
        return new Result(seconds,lowMB,highMB,Math.round(seconds*fps));
    }
    public static String fileSize(double mb){
        if(mb>=1000)return String.format(Locale.ROOT,"%.1f GB",mb/1000);
        if(mb>=10)return String.format(Locale.ROOT,"%.0f MB",mb);
        return String.format(Locale.ROOT,"%.1f MB",mb);
    }
}

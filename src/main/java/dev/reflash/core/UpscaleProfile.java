package dev.reflash.core;

/** Post-process profiles. No setting here changes Momenton's native Vulkan resolution. */
public enum UpscaleProfile {
    FAST("Fast / small file", "bilinear", "veryfast", 24),
    BALANCED("Balanced / recommended", "lanczos", "fast", 20),
    SHARP("Sharp / high quality", "lanczos", "medium", 17),
    ARCHIVE("Archive / slowest", "spline", "slow", 15);
    private final String label, scale, encoder;
    private final int crf;
    UpscaleProfile(String label, String scale, String encoder, int crf){
        this.label=label;this.scale=scale;this.encoder=encoder;this.crf=crf;
    }
    public String label(){return label;}
    public String scale(){return scale;}
    public String encoder(){return encoder;}
    public int crf(){return crf;}
    public UpscaleProfile next(){return values()[(ordinal()+1)%values().length];}
    public static UpscaleProfile byName(String name){
        if(name!=null)for(var p:values())if(p.name().equals(name))return p;
        return BALANCED;
    }
    public String explanation(){return switch(this){
        case FAST -> "Quick upscale, less detail, smaller file";
        case BALANCED -> "Lanczos upscale, good tradeoff";
        case SHARP -> "Higher bitrate, longer export";
        case ARCHIVE -> "Highest quality setting, slow CPU export";
    };}
}

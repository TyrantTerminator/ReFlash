package dev.reflash.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Optional OFFLINE video processing, never active during gameplay unless invoked by the user.
 * It scales/pads saved video and sets a constant output FPS. It does not invent scene detail
 * or interpolate missing animation. Frame duplication remains explicit in the UI.
 */
public final class VideoPostProcessor {
    private static final AtomicBoolean RUNNING=new AtomicBoolean();
    private static final AtomicBoolean CANCELLED=new AtomicBoolean();
    private static volatile Process active;
    private static volatile String status="Ready";
    private static volatile Path lastOutput;
    private static volatile int jobsDone,jobsTotal;
    private VideoPostProcessor(){}
    public static boolean running(){return RUNNING.get();}
    public static String status(){return status;}
    public static Path lastOutput(){return lastOutput;}
    public static int jobsDone(){return jobsDone;}
    public static int jobsTotal(){return jobsTotal;}

    public static Path latestExport(Path gameDir)throws IOException{
        try(var files=Files.list(VideoExportLibrary.folder(gameDir))){
            return files.filter(Files::isRegularFile).filter(VideoExportLibrary::valid)
                .filter(p->!p.getFileName().toString().toLowerCase(Locale.ROOT).contains("-reflash-"))
                .max(Comparator.comparingLong(p->{try{return Files.getLastModifiedTime(p).toMillis();}
                    catch(IOException ex){return 0;}})).orElse(null);
        }catch(java.nio.file.NoSuchFileException ex){return null;}
    }
    public static String filter(int width,int height,int fps){
        if(width<640||height<360||width>3840||height>2160||width%2!=0||height%2!=0||fps<24||fps>240)
            throw new IllegalArgumentException("Output limited to even dimensions <=4K and 24-240 FPS");
        return "scale="+width+":"+height+":flags=lanczos:force_original_aspect_ratio=decrease,"+
            "pad="+width+":"+height+":(ow-iw)/2:(oh-ih)/2,fps="+fps;
    }
    public static String filter(int width,int height,int fps,UpscaleProfile profile){
        return filter(width,height,fps).replace("flags=lanczos", "flags="+profile.scale());
    }
    public static synchronized boolean startLatest(Path gameDir,Preferences prefs){
        try{return startSelected(gameDir,latestExport(gameDir),prefs);}
        catch(IOException e){status="No video found: "+e.getMessage();return false;}
    }
    public static synchronized boolean startSelected(Path gameDir,Path selected,Preferences prefs){
        return startBatch(gameDir,selected==null?List.of():List.of(selected),prefs);
    }
    /** Runs a limited batch serially; can be cancelled. No concurrent encoders on an older laptop. */
    public static synchronized boolean startBatch(Path gameDir,List<Path> selected,Preferences prefs){
        if(!RUNNING.compareAndSet(false,true))return false;
        try{
            if(selected==null||selected.isEmpty()||selected.size()>25){
                status="Select 1-25 exported videos first";RUNNING.set(false);return false;
            }
            List<Path> inputs=new ArrayList<>();
            for(Path path:selected){
                if(!VideoExportLibrary.allowed(gameDir,path)){
                    status="Select a valid exported video (file missing or outside the exports folder)";RUNNING.set(false);return false;
                }
                if(!inputs.contains(path))inputs.add(path);
            }
            final UpscaleProfile profile=prefs.upscaleProfile();
            final String vf=filter(prefs.outputWidth(),prefs.outputHeight(),prefs.outputFps(),profile);
            final int width=prefs.outputWidth(),height=prefs.outputHeight(),fps=prefs.outputFps(),crf=prefs.qualityCrf();
            CANCELLED.set(false);jobsDone=0;jobsTotal=inputs.size();
            status="Queued "+jobsTotal+" videos";
            Thread thread=new Thread(()->processQueue(gameDir,inputs,vf,width,height,fps,profile,crf),"ReFlash-video-export-queue");
            thread.setDaemon(true);thread.start();return true;
        }catch(IOException|IllegalArgumentException e){
            status="Video batch setup failed: "+e.getMessage();RUNNING.set(false);return false;
        }
    }
    private static void processQueue(Path gameDir,List<Path> files,String vf,int width,int height,int fps,UpscaleProfile profile,int crf){
        try{
            for(int i=0;i<files.size();i++){
                if(CANCELLED.get())break;
                Path input=files.get(i);
                String name=input.getFileName().toString().replaceFirst("(?i)\\.(mp4|mkv|mov)$","");
                String stem=name+"-ReFlash-"+width+"x"+height+"-"+fps+"fps";
                Path out=input.resolveSibling(stem+".mp4");
                for(int j=1;Files.exists(out)&&j<=999;j++)out=input.resolveSibling(stem+"-"+j+".mp4");
                if(Files.exists(out)){status="Too many existing exports of "+name;break;}
                status="Video "+(i+1)+"/"+files.size()+" — "+input.getFileName();
                if(!transcode(gameDir,input,out,vf,i+1,files.size(),profile,crf))break;
                jobsDone++;
            }
            if(CANCELLED.get())status="Export queue cancelled";
            else if(jobsDone==files.size())status="Completed "+jobsDone+" video(s). Last: "+lastOutput.getFileName();
        }finally{active=null;RUNNING.set(false);}
    }
    private static boolean transcode(Path gameDir,Path input,Path out,String vf,int index,int count,UpscaleProfile profile,int crf){
        Process process=null;
        try{
            // ProcessBuilder arguments are a list: paths are never passed through cmd.exe or a shell.
            var builder=new ProcessBuilder(FfmpegCheck.candidate(gameDir),"-hide_banner","-nostdin","-y","-i",input.toString(),
                "-vf",vf,"-fps_mode","cfr","-c:v","libx264","-preset",profile.encoder(),"-crf",String.valueOf(crf),
                "-threads","2","-pix_fmt","yuv420p","-c:a","aac","-b:a","160k", "-movflags","+faststart",out.toString());
            process=builder.redirectErrorStream(true).start();active=process;
            String error="No encoder details";
            try(var reader=new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8))){
                String line;while((line=reader.readLine())!=null){
                    if(line.contains("time=")){
                        int n=line.indexOf("time=");
                        status="Video "+index+"/"+count+"  "+line.substring(n,Math.min(line.length(),n+19)).trim();
                    }else if(line.contains("Error")||line.contains("error")||line.contains("not found")){
                        error=line.length()>175?line.substring(0,175):line;
                    }
                }
            }
            int code=process.waitFor();
            if(CANCELLED.get()){
                Files.deleteIfExists(out);return false;
            }
            if(code==0 && Files.isRegularFile(out) && Files.size(out)>0){lastOutput=out;return true;}
            status="FFmpeg failed (code "+code+"): "+error;
            Files.deleteIfExists(out);return false;
        }catch(IOException e){status="Cannot run FFmpeg: "+e.getMessage();return false;}
        catch(InterruptedException e){Thread.currentThread().interrupt();status="Export interrupted";return false;}
        finally{active=null;}
    }
    public static void cancel(){
        if(RUNNING.get()){
            CANCELLED.set(true);
            Process current=active;if(current!=null)current.destroyForcibly();
            status="Cancelling export queue...";
        }
    }
}

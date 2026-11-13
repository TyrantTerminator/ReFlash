package dev.reflash.core;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Opt-in, short microphone level test. Never runs during gameplay without a button click.
 * Audio is not written to disk, transmitted, or retained after the test.
 */
public final class MicrophoneProbe {
    private static final AudioFormat FORMAT=new AudioFormat(48000f,16,2,true,false);
    private static final AtomicBoolean RUNNING=new AtomicBoolean();
    private static volatile String status="Not tested (click Test Mic)";
    private MicrophoneProbe(){}
    public static String status(){return status;}
    public static boolean running(){return RUNNING.get();}
    public static boolean start(String device){
        if(!RUNNING.compareAndSet(false,true))return false;
        status="Testing microphone for 0.6s (audio not saved)...";
        Thread t=new Thread(()->probe(device),"ReFlash-microphone-test");t.setDaemon(true);t.start();return true;
    }
    private static void probe(String device){
        try{
            DataLine.Info info=new DataLine.Info(TargetDataLine.class,FORMAT);
            Mixer.Info selected=null;
            if(device!=null&&!device.isBlank()&&!device.equals("System default")){
                for(Mixer.Info candidate:AudioSystem.getMixerInfo())
                    if(candidate.getName().equals(device) && AudioSystem.getMixer(candidate).isLineSupported(info)){selected=candidate;break;}
            }
            try(TargetDataLine line=selected==null?AudioSystem.getTargetDataLine(FORMAT):AudioSystem.getTargetDataLine(FORMAT,selected)){
                line.open(FORMAT);line.start();
                byte[] buffer=new byte[4096];long sum=0;long count=0;
                long target=48000L*2*60/100; // 0.6s of stereo 16-bit sample pairs
                while(count<target){
                    int got=line.read(buffer,0,Math.min(buffer.length,(int)Math.min(Integer.MAX_VALUE,(target-count)*2))&~3);
                    if(got<=0)break;
                    for(int i=0;i+1<got;i+=2){int sample=(short)((buffer[i+1]<<8)|(buffer[i]&255));sum+=(long)sample*sample;count++;}
                }
                double rms=count>0?Math.sqrt(sum/(double)count)/32768.0:0;
                double db=rms>0?20*Math.log10(rms): -90;
                status=count==0?"No samples captured. Check microphone permissions":
                    String.format(java.util.Locale.ROOT,"Mic detected  %.0f dBFS (test audio discarded)",Math.max(-90,db));
            }
        }catch(Exception ex){status="Microphone unavailable: "+ex.getClass().getSimpleName();}
        finally{RUNNING.set(false);}
    }
    /** Pure, deterministic helper for verifying the audio-level math without recording anyone. */
    public static double rmsDbfs(short[] samples){
        if(samples==null||samples.length==0)return -90;
        double sum=0;for(short sample:samples)sum+=(double)sample*sample;
        double rms=Math.sqrt(sum/samples.length)/32768.0;
        return rms==0?-90:20*Math.log10(rms);
    }
}

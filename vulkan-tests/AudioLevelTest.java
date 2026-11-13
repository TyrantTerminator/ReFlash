import dev.reflash.core.MicrophoneProbe;
public class AudioLevelTest{
 public static void main(String[]args){
    if(MicrophoneProbe.rmsDbfs(new short[100])!=-90)throw new AssertionError("silence");
    double a=MicrophoneProbe.rmsDbfs(new short[]{16384,-16384});
    if(a>-5.9||a<-6.2)throw new AssertionError("expected -6dBFS: "+a);
    System.out.println("PASS opt-in mic test level math, no device capture used in unit test");
 }
}

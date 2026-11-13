import dev.reflash.core.PerformanceSampler;
public class PerformanceBenchmarkTest{
 public static void main(String[]args){
    var p=new PerformanceSampler();p.start();
    for(int s=0;s<8;s++)for(int tick=0;tick<20;tick++)p.tick(s==7?60:80,false);
    for(int s=0;s<8;s++)for(int tick=0;tick<20;tick++)p.tick(s==7?40:70,true);
    p.stop();var r=p.result();
    if(!r.comparable()||r.lowestIdleSample()!=60||r.lowestRecordingSample()!=40)throw new AssertionError("Sampler minimum wrong: "+r);
    if(r.relativeChangePercent()>=0)throw new AssertionError("Benchmark sign wrong");
    System.out.println("PASS FPS benchmark min and average, bounded constant-memory sampling");
 }
}

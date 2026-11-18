import dev.reflash.client.ReFlashResponsive74;
import dev.reflash.client.ReFlashBeta74;
import java.util.*;
public final class TestUi74 {
  static class FakeScreen {
    public int field_22789,field_22790;
    public List<FakeControl> controls=new ArrayList<>();
    FakeScreen(int w,int h){field_22789=w;field_22790=h;}
  }
  static class FakeControl {FakeWidget b;FakeControl(FakeWidget w){b=w;}public FakeWidget widget(){return b;}}
  static class FakeWidget {
    public boolean field_22764=true;
    int x,y,w,h;FakeWidget(int a,int b,int c,int d){x=a;y=b;w=c;h=d;}
    public int method_46426(){return x;}
    public int method_46427(){return y;}
    public int method_25368(){return w;}
    public int method_25364(){return h;}
  }
  static class FakeEvent {
    int button;double x,y;FakeEvent(double x,double y){this.x=x;this.y=y;}
    public int method_74245(){return button;}
    public double comp_4798(){return x;}
    public double comp_4799(){return y;}
  }
  static void check(boolean b,String name){if(!b)throw new AssertionError(name);}
  public static void main(String[] args){
    FakeScreen tiny=new FakeScreen(540,320);
    FakeWidget first=new FakeWidget(10,10,90,25);FakeWidget second=new FakeWidget(80,10,90,25);FakeWidget third=new FakeWidget(180,10,90,25);
    tiny.controls.add(new FakeControl(first));tiny.controls.add(new FakeControl(second));tiny.controls.add(new FakeControl(third));
    ReFlashResponsive74.apply(tiny);
    check(first.field_22764,"first button stays");
    check(!second.field_22764,"overlapping later button must hide");
    check(third.field_22764,"nonoverlapping button stays");
    FakeScreen normal=new FakeScreen(1200,800);FakeWidget a=new FakeWidget(10,10,90,20);FakeWidget b=new FakeWidget(80,10,90,20);
    normal.controls.add(new FakeControl(a));normal.controls.add(new FakeControl(b));
    ReFlashResponsive74.apply(normal);
    check(a.field_22764&&b.field_22764,"normal screen untouched");
    FakeScreen popup=new FakeScreen(800,600);
    check(ReFlashBeta74.mouse(popup,new FakeEvent(10,10)),"background click consumed");
    check(ReFlashBeta74.mouse(popup,new FakeEvent(554,265)),"close consumed");
    check(!ReFlashBeta74.mouse(popup,new FakeEvent(10,10)),"popup stays dismissed");
    System.out.println("PASS 7 UI overlay/layout checks");
  }
}

import java.lang.reflect.Method;
public final class TestDialog74 {
  static final class FakeDialog { public String heading; FakeDialog(String s){heading=s;} }
  static void eq(String a,String b){if(!a.equals(b))throw new AssertionError(a+" != "+b);}
  public static void main(String[]a)throws Exception {
    Class<?> cls=Class.forName("dev.reflash.client.ReFlashFps74");
    Method action=cls.getDeclaredMethod("actionLabel",Object.class);action.setAccessible(true);
    Method input=cls.getDeclaredMethod("inputLabel",Object.class);input.setAccessible(true);
    eq((String)action.invoke(null,new FakeDialog("Export frame rate (1-240)")),"APPLY FPS");
    eq((String)input.invoke(null,new FakeDialog("Export frame rate (1-240)")),"FPS");
    eq((String)action.invoke(null,new FakeDialog("Rename recording")),"Rename");
    eq((String)input.invoke(null,new FakeDialog("Rename recording")),"New name");
    System.out.println("PASS 4 dialog label checks");
  }
}

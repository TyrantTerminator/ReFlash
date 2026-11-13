package dev.reflash.client;
import java.lang.reflect.Method;
public class InteractiveBoundsTest{
 @SuppressWarnings({"unchecked","rawtypes"})
 public static void main(String[]args)throws Exception{
   var p=(Class<? extends Enum>)Class.forName("dev.reflash.client.ReFlashScreen$Page");
   var choose=ReFlashScreen.class.getDeclaredMethod("selectPage",p);choose.setAccessible(true);
   var t=(Class<? extends Enum>)Class.forName("dev.reflash.client.ReFlashScreen$SettingsTab");
   var tab=ReFlashScreen.class.getDeclaredMethod("chooseSettings",t);tab.setAccessible(true);
   int total=0;
   new ReFlashClient().onInitializeClient();
   for(int[]dim:new int[][]{{320,240},{480,270},{640,360},{854,480},{1120,630},{1280,720}}){
     for(String page:new String[]{"CAPTURE","LIBRARY","EDITOR","EXPORT","SETTINGS"}){
       var screen=new ReFlashScreen(null);screen.width=dim[0];screen.height=dim[1];screen.init();
       choose.invoke(screen,Enum.valueOf(p,page));
       for(String s:page.equals("SETTINGS")?new String[]{"RECORDING","AUDIO","KEYS"}:new String[]{"RECORDING"}){
         if(page.equals("SETTINGS"))tab.invoke(screen,Enum.valueOf(t,s));
         for(var button:screen.widgets){
           if(button.getX()<0||button.getY()<0||button.getX()+button.getWidth()>dim[0]||button.getY()+button.getHeight()>dim[1])
             throw new AssertionError("Offscreen button "+page+" "+s+" @"+java.util.Arrays.toString(dim));
           total++;
         }
       }
     }
   }
   System.out.println("PASS "+total+" interactive button layouts within six Minecraft window sizes");
 }
}

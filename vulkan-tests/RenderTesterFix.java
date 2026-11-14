package dev.reflash.client;
import net.minecraft.client.gui.GuiGraphics;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
public class RenderTesterFix {
@SuppressWarnings({"unchecked","rawtypes"})
public static void main(String[] args) throws Exception {
  new ReFlashClient().onInitializeClient();
  Class p=Class.forName("dev.reflash.client.ReFlashScreen$Page");
  Method select=ReFlashScreen.class.getDeclaredMethod("selectPage",p);select.setAccessible(true);
  Path dir=Path.of("/mnt/data/reflash_011_review");Files.createDirectories(dir);
  for(String page:new String[]{"CAPTURE","LIBRARY","EDITOR","EXPORT"}){
    ReFlashScreen screen=new ReFlashScreen(null);screen.width=1000;screen.height=560;screen.init();
    select.invoke(screen,Enum.valueOf(p,page));
    for(int i=0;i<15;i++)screen.tick();
    GuiGraphics g=new GuiGraphics(1000,560);screen.render(g,0,0,0);
    g.save(dir.resolve("fixed-"+page.toLowerCase()+".png"));
  }
  System.out.println("PASS four source-rendered views after tester fixes (not Minecraft screenshots)");
}}

package dev.reflash.client;

import dev.reflash.core.ExportPreset;
import dev.reflash.core.PerformanceSampler;
import dev.reflash.core.ReplayIndex;
import dev.reflash.core.StudioLayout;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Responsive, native ReFlash Studio. All interaction goes through real Minecraft Button
 * widgets. The custom drawing code runs only while this menu is open: no gameplay HUD cost.
 */
public final class ReFlashScreen extends Screen {
    private enum Page { CAPTURE, LIBRARY, EDITOR, PERFORMANCE, SETTINGS }
    private enum Skin { PRIMARY, SECONDARY, QUIET, DANGER, NAV, ROW, TOGGLE }
    private record Control(Button widget, Supplier<String> caption, Skin skin, boolean selected, String icon) { }
    private static final String[] NAV = { "Home", "Library", "Editor", "Performance", "Settings" };
    private static final String[] NAV_ICONS = { "home", "library", "editor", "performance", "settings" };
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());
    private final Screen parent;
    private StudioLayout layout;
    private Page page = Page.CAPTURE;
    private final List<Control> controls = new ArrayList<>();
    private List<ReplayIndex.Replay> replayList = List.of();
    private int selectedReplay = -1, replayOffset, screenTicks;
    private String libraryError = "", notice = "";
    private int noticeTicks;

    public ReFlashScreen(Screen parent){super(Component.literal("ReFlash Studio"));this.parent=parent;}
    public static ReFlashScreen library(Screen parent){var s=new ReFlashScreen(parent);s.page=Page.LIBRARY;return s;}
    private Path replayFolder(){return MomentonBridge.replayFolder(FabricLoader.getInstance().getGameDir());}

    @Override protected void init(){
        layout=StudioLayout.forViewport(width,height);
        controls.clear();
        if(replayList.isEmpty() && libraryError.isEmpty())refreshLibrary();
        int sx=layout.x(), sy=layout.y(), sw=layout.width();
        add(sx+sw-36,sy+17,24,24,()->"",Skin.QUIET,false,"close",this::onClose);
        if(layout.sideNav()){
            int navW=layout.contentX()-sx-24;
            int nY=sy+108;
            for(Page p: Page.values()){
                final Page target=p;
                int yy=nY+p.ordinal()*47;
                add(sx+13,yy,navW,39,()->NAV[target.ordinal()],Skin.NAV,p==page,NAV_ICONS[p.ordinal()],()->selectPage(target));
            }
        } else {
            int x=sx+12, tabW=(sw-24)/5, y=sy+53;
            for(Page p:Page.values()){
                final Page next=p;
                add(x+p.ordinal()*tabW,y,tabW-3,28,()->NAV[next.ordinal()],Skin.NAV,p==page,NAV_ICONS[p.ordinal()],()->selectPage(next));
            }
        }
        switch(page){
            case CAPTURE -> initCapture();
            case LIBRARY -> initLibrary();
            case EDITOR -> initEditor();
            case PERFORMANCE -> initPerformance();
            case SETTINGS -> initSettings();
        }
    }
    private void selectPage(Page p){if(p==Page.LIBRARY)refreshLibrary();page=p;rebuildWidgets();}
    private void add(int x,int y,int w,int h,Supplier<String> s,Skin skin,boolean selected,String icon,Runnable action){
        if(w<12||h<12)return;
        Button b=Button.builder(Component.literal(s.get()), ignored->action.run()).bounds(x,y,w,h).build();
        b.setAlpha(0f);
        addRenderableWidget(b);
        controls.add(new Control(b,s,skin,selected,icon));
    }

    private void openKeybinds() {
        minecraft.setScreen(new KeyBindsScreen(this, minecraft.options));
    }

    private void editSelectedReplay() {
        if (selectedReplay < 0 || selectedReplay >= replayList.size()) {
            notice("Choose a recording in Library first");
            return;
        }
        if (MomentonBridge.isRecording()) {
            notice("Stop recording before opening the editor");
            return;
        }
        if (!MomentonBridge.openReplayForEditing(minecraft, replayList.get(selectedReplay).path())) {
            notice("Could not open editor: check replay format");
        }
    }

    private void initCapture(){
        int x=layout.contentX(),y=layout.contentY(),w=layout.contentWidth(),h=layout.contentHeight();
        boolean compact=layout.ultraCompact();
        int heroY=y+(compact?20:44), heroH=compact?Math.max(50,h-29):Math.min(243,Math.max(100,h*48/100));
        int ay=heroY+heroH-39;
        int gap=8;
        int buttonCount=w>=435?3:2;
        int bw=Math.max(75,(w-34-gap*(buttonCount-1))/buttonCount);
        add(x+15,ay,bw,28,()->MomentonBridge.isRecording()?"Stop & Save":"Start Recording",
                MomentonBridge.isRecording()?Skin.DANGER:Skin.PRIMARY,false,"record",()->{
            boolean was=MomentonBridge.isRecording();
            if (MomentonBridge.toggleRecording(minecraft)) {
                notice(was?"Recording stopped / saving":"Recording started");
                rebuildWidgets();
            }
        });
        add(x+15+bw+gap,ay,bw,28,()->"Recordings",Skin.SECONDARY,false,"library",()->selectPage(Page.LIBRARY));
        if(buttonCount==3) add(x+15+(bw+gap)*2,ay,bw,28,()->"Editor",Skin.SECONDARY,false,"editor",()->selectPage(Page.EDITOR));
        if(!compact&&h>280){
            int cardY=heroY+heroH+14;
            int avail=h-(cardY-y)-6;
            if(avail>75){
                int rightW=Math.max(160,w/3);
                add(x+w-rightW+13,cardY+avail-37,rightW-25,25,()->"View Recordings",Skin.SECONDARY,false,"library",()->selectPage(Page.LIBRARY));
                int leftW=w-rightW-11;
                if(avail>183 && leftW>360){
                    int quickY=cardY+avail-40;
                    add(x+15,quickY,Math.min(160,(leftW-42)/2),27,()->"Open Editor",Skin.SECONDARY,false,"editor",()->selectPage(Page.EDITOR));
                    add(x+21+Math.min(160,(leftW-42)/2),quickY,Math.min(160,(leftW-42)/2),27,
                            ()->"Capture Settings",Skin.QUIET,false,"settings",()->selectPage(Page.SETTINGS));
                }
            }
        }
    }
    private int libRowH(){return layout.contentHeight()>=320?65:layout.contentHeight()>185?41:28;}
    private int libRows(){
        int avail=layout.contentHeight()-(layout.ultraCompact()?69:110);
        return Math.max(1,Math.min(6,avail/(libRowH()+7)));
    }
    private void initLibrary(){
        int x=layout.contentX(),y=layout.contentY(),w=layout.contentWidth(),h=layout.contentHeight();
        boolean wide=w>=435;
        int listW=wide?w*58/100:w;
        if(!layout.ultraCompact())add(x+Math.max(0,listW-87),y+6,83,24,()->"Refresh",Skin.QUIET,false,"spark",()->{refreshLibrary();rebuildWidgets();});
        int startY=y+(layout.ultraCompact()?23:46),rh=libRowH();
        for(int i=0;i<libRows();i++){
            int index=replayOffset+i;
            if(index>=replayList.size())break;
            final int n=index;
            add(x+6,startY+i*(rh+7),Math.max(25,listW-12),rh,()->replayList.get(n).fileName(),Skin.ROW,index==selectedReplay,"library",()->{
                selectedReplay=n;rebuildWidgets();
            });
        }
        int controlsY=y+h-32;
        add(x+6,controlsY,Math.max(69,Math.min(150,listW*47/100)),25,()->"Open Replay",Skin.PRIMARY,false,"play",this::openSelectedReplay);
        if(wide){
            add(x+listW+14,controlsY,Math.max(24,w-listW-27),25,()->"Edit Selected",Skin.SECONDARY,false,"editor",this::editSelectedReplay);
        }else if(w>=245){
            add(x+Math.min(155,listW*47/100)+12,controlsY,Math.max(32,w-Math.min(155,listW*47/100)-18),25,()->"Edit Selected",Skin.SECONDARY,false,"editor",this::editSelectedReplay);
        }
        if(replayList.size()>libRows()&&h>170){
            add(x+8,startY+libRows()*(rh+7)+2,60,18,()->"< Prev",Skin.QUIET,false,"",()->{
                replayOffset=Math.max(0,replayOffset-libRows());rebuildWidgets();
            });
            add(x+77,startY+libRows()*(rh+7)+2,60,18,()->"Next >",Skin.QUIET,false,"",()->{
                replayOffset=Math.min(Math.max(0,replayList.size()-1),replayOffset+libRows());rebuildWidgets();
            });
        }
    }
    private void initEditor(){
        int x=layout.contentX(),y=layout.contentY(),w=layout.contentWidth(),h=layout.contentHeight();
        int actionsY=y+Math.max(25,h-38);
        int primaryW=Math.min(210,Math.max(98,w/2));
        add(x+10,actionsY,primaryW,28,()->MomentonBridge.isInReplay()?"Open Replay Editor":"Choose Recording",Skin.PRIMARY,false,"editor",()->{
            if(MomentonBridge.isInReplay()) {
                if (!MomentonBridge.openNativeEditor(minecraft)) notice("Momenton editor unavailable");
            } else {
                selectPage(Page.LIBRARY);
                notice("Choose a replay, then click Edit Selected");
            }
        });
        if(w>=330)add(x+20+primaryW,actionsY,Math.min(172,w-primaryW-31),28,
                ()->MomentonBridge.isInReplay()?"Export Replay":"Export Limitations",Skin.SECONDARY,false,"export",()->{
                    if(MomentonBridge.isInReplay()) {
                        if(!MomentonBridge.openNativeExport(minecraft,this))notice("Momenton export window unavailable");
                    }else notice("Under VulkanMod, video exports are window-sized");
                });
    }
    private void initPerformance(){
        int x=layout.contentX(),y=layout.contentY(),w=layout.contentWidth(),h=layout.contentHeight();
        add(x+12,y+h-39,Math.min(235,w-24),29,
                ()->ReFlashClient.performanceSampler().active()?"End FPS Comparison":"Start FPS Comparison",Skin.PRIMARY,false,"performance",ReFlashClient::toggleBenchmark);
    }
    private void initSettings(){
        int x=layout.contentX(),y=layout.contentY(),w=layout.contentWidth(),h=layout.contentHeight();
        int base=y+(layout.ultraCompact()?18:43);
        int rowH=layout.ultraCompact()?31:Math.min(61,Math.max(34,(h-55)/3));
        int buttonH=Math.min(29,rowH-4);
        add(x+w-158,base+(rowH-buttonH)/2,143,buttonH,()->"Change Keybinds",Skin.PRIMARY,false,"settings",this::openKeybinds);
        add(x+w-108,base+rowH+(rowH-buttonH)/2,93,buttonH,
                ()->ReFlashClient.preferences().autoRecord()?"Enabled":"Disabled",Skin.TOGGLE,
                ReFlashClient.preferences().autoRecord(),"",()->{
            var p=ReFlashClient.preferences();p.setAutoRecord(!p.autoRecord());ReFlashClient.savePreferences();rebuildWidgets();
        });
        add(x+w-108,base+rowH*2+(rowH-buttonH)/2,93,buttonH,
                ()->ReFlashClient.preferences().notifications()?"Enabled":"Disabled",Skin.TOGGLE,
                ReFlashClient.preferences().notifications(),"",()->{
            var p=ReFlashClient.preferences();p.setNotifications(!p.notifications());ReFlashClient.savePreferences();rebuildWidgets();
        });
    }
    private void refreshLibrary(){
        try{
            replayList=ReplayIndex.recent(replayFolder(),128);libraryError="";
            if(selectedReplay>=replayList.size())selectedReplay=replayList.isEmpty()?-1:0;
            replayOffset=0;
        }catch(IOException e){replayList=List.of();libraryError="Could not read replay files";selectedReplay=-1;}
    }
    private void openSelectedReplay(){
        if(selectedReplay<0||selectedReplay>=replayList.size()){notice("Select a saved recording first");return;}
        if(MomentonBridge.isRecording()){notice("Stop recording before playback");return;}
        if(!MomentonBridge.openReplay(minecraft,replayList.get(selectedReplay).path()))notice("Could not load replay; try Momenton browser");
    }
    private void openFolder(){
        try{
            Path folder=replayFolder();Files.createDirectories(folder);
            if(Desktop.isDesktopSupported()&&Desktop.getDesktop().isSupported(Desktop.Action.OPEN))Desktop.getDesktop().open(folder.toFile());
            else notice("Folder opening is unsupported here");
        }catch(Exception e){notice("Could not open replay folder");}
    }
    private void notice(String message){notice=message;noticeTicks=100;}
    @Override public void tick(){screenTicks++;if(noticeTicks>0)noticeTicks--;}

    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        StudioTheme.rect(g,0,0,width,height,0xD8060A13);
        drawChrome(g);
        switch(page){
            case CAPTURE -> drawCapture(g);
            case LIBRARY -> drawLibrary(g);
            case EDITOR -> drawEditor(g);
            case PERFORMANCE -> drawPerformance(g);
            case SETTINGS -> drawSettings(g);
        }
        super.render(g,mouseX,mouseY,partial);
        for(Control c:controls)drawControl(g,c);
        if(noticeTicks>0){
            int w=Math.min(layout.width()-30,font.width(notice)+40),x=layout.x()+(layout.width()-w)/2,y=layout.y()+layout.height()-37;
            StudioTheme.rounded(g,x,y,w,25,0xFF1D2D45);
            StudioTheme.border(g,x,y,w,25,StudioTheme.CYAN);
            StudioTheme.centered(g,font,fit(notice,w-15),x+w/2,y+8,StudioTheme.TEXT);
        }
    }
    private void drawChrome(GuiGraphics g){
        int x=layout.x(),y=layout.y(),w=layout.width(),h=layout.height();
        StudioTheme.rounded(g,x+4,y+7,w,h,0x59000000);
        StudioTheme.rounded(g,x,y,w,h,StudioTheme.BASE);
        StudioTheme.border(g,x,y,w,h,StudioTheme.BORDER);
        StudioTheme.rounded(g,x+1,y+1,w-2,54,StudioTheme.HEADER);
        StudioTheme.gradient(g,x+2,y+2,w-4,2,StudioTheme.CYAN,StudioTheme.PURPLE,48);
        StudioTheme.logo(g,x+12,y+8,37);
        StudioTheme.label(g,font,"ReFlash",x+50,y+14,StudioTheme.TEXT);
        StudioTheme.label(g,font,"MANAGER",x+107,y+14,StudioTheme.PURPLE);
        StudioTheme.label(g,font,"VULKAN  /  REPLAY  /  CREATE",x+50,y+31,StudioTheme.MUTED);
        if(w>=660){
            String status=MomentonBridge.isRecording()?"RECORDING":MomentonBridge.isInReplay()?"REPLAY LOADED":"CAPTURE READY";
            StudioTheme.chip(g,font,status,x+w-172,y+19,119,MomentonBridge.isRecording()?StudioTheme.RED:StudioTheme.GREEN);
        }
        if(layout.sideNav()){
            int navW=layout.contentX()-x-12;
            StudioTheme.rect(g,x+1,y+55,navW,h-57,StudioTheme.SIDEBAR);
            StudioTheme.rect(g,x+navW,y+56,1,h-57,StudioTheme.BORDER);
            StudioTheme.label(g,font,"WORKSPACE",x+23,y+83,StudioTheme.SOFT);
            if(h>435){
                StudioTheme.rect(g,x+18,y+h-118,navW-34,1,StudioTheme.BORDER);
                StudioTheme.icon(g,"record",x+21,y+h-104,StudioTheme.CYAN);
                StudioTheme.label(g,font,"RECORD SMART",x+42,y+h-105,StudioTheme.TEXT);
                StudioTheme.label(g,font,"RENDER LATER",x+42,y+h-88,StudioTheme.MUTED);
                StudioTheme.label(g,font,"MANAGER: " + ReFlashClient.managerKeyName(),x+20,y+h-48,StudioTheme.SOFT);
            }
        }
        StudioTheme.label(g,font,"v0.9.1 VK / FABRIC 1.21.11",x+w-174,y+h-16,StudioTheme.SOFT);
    }
    private void heading(GuiGraphics g,String title,String subtitle,int x,int y){
        StudioTheme.label(g,font,title,x+2,y+5,StudioTheme.TEXT);
        if(!layout.ultraCompact())StudioTheme.label(g,font,fit(subtitle,layout.contentWidth()-6),x+2,y+22,StudioTheme.MUTED);
    }
    private void drawCapture(GuiGraphics g){
        int x=layout.contentX(),y=layout.contentY(),w=layout.contentWidth(),h=layout.contentHeight();
        boolean tiny=layout.ultraCompact();
        heading(g,"Welcome to ReFlash Studio","Your Minecraft moments, rendered on your terms.",x,y);
        int heroY=y+(tiny?20:44),heroH=tiny?Math.max(50,h-29):Math.min(243,Math.max(100,h*48/100));
        StudioTheme.softPanel(g,x,heroY,w,heroH);
        StudioTheme.sceneArt(g,x+2,heroY+2,w-4,heroH-4,false);
        StudioTheme.rect(g,x+2,heroY+2,w*52/100,heroH-4,0xCC10152A);
        StudioTheme.rect(g,x+2,heroY+2,4,heroH-4,StudioTheme.PURPLE);
        StudioTheme.label(g,font,MomentonBridge.isRecording()?"RECORDING IN PROGRESS":"LIGHTWEIGHT REPLAY CAPTURE",x+18,heroY+17,MomentonBridge.isRecording()?StudioTheme.RED:StudioTheme.CYAN);
        if(heroH>105){
            StudioTheme.label(g,font,"Record. Replay. Create.",x+18,heroY+46,StudioTheme.TEXT);
            StudioTheme.label(g,font,fit("State replay instead of gameplay video encoding",w/2-12),x+18,heroY+68,StudioTheme.MUTED);
            if(heroH>175){
                StudioTheme.icon(g,"spark",x+18,heroY+97,StudioTheme.PURPLE);
                StudioTheme.label(g,font,"NATIVE VULKAN REPLAY EDITOR",x+40,heroY+100,StudioTheme.TEXT);
                StudioTheme.icon(g,"export",x+18,heroY+122,StudioTheme.CYAN);
                StudioTheme.label(g,font,"EXPORT SETTINGS: WINDOW SIZE ON VULKAN",x+40,heroY+125,StudioTheme.TEXT);
            }
        }
        if(!tiny && h>280){
            int yy=heroY+heroH+14,remaining=Math.max(60,h-(yy-y)-5);
            int gap=11,cardW=(w-gap*2)*2/3,smallW=w-cardW-gap;
            StudioTheme.softPanel(g,x,yy,cardW,remaining);
            StudioTheme.label(g,font,"PERFORMANCE FIRST",x+16,yy+16,StudioTheme.CYAN);
            int sw=(cardW-52)/3;
            stat(g,x+16,yy+38,sw,Math.min(87,remaining-47),String.valueOf(minecraft.getFps()),"GAME FPS",StudioTheme.CYAN);
            stat(g,x+24+sw,yy+38,sw,Math.min(87,remaining-47),String.valueOf(replayList.size()),"REPLAYS",StudioTheme.PURPLE);
            stat(g,x+32+sw*2,yy+38,sw,Math.min(87,remaining-47),"OFFLINE","RENDER",StudioTheme.GREEN);
            if(remaining>178){
                int hy=yy+137;
                StudioTheme.label(g,font,"THREE STEPS TO A CINEMATIC REPLAY",x+16,hy,StudioTheme.PURPLE);
                int ww=(cardW-43)/3;
                String[] actions={"01  CAPTURE","02  EDIT","03  EXPORT"};
                String[] descriptions={"Record data","Replay tools","Video output"};
                for(int i=0;i<3;i++){
                    int xx=x+15+i*(ww+8);
                    StudioTheme.rounded(g,xx,hy+20,ww,31,0xFF182238);
                    StudioTheme.label(g,font,fit(actions[i],ww-10),xx+7,hy+24,
                            i==0?StudioTheme.CYAN:i==1?StudioTheme.PURPLE:StudioTheme.GREEN);
                    StudioTheme.label(g,font,fit(descriptions[i],ww-10),xx+7,hy+38,StudioTheme.MUTED);
                }
            }
            StudioTheme.softPanel(g,x+cardW+gap,yy,smallW,remaining);
            StudioTheme.icon(g,"library",x+cardW+26,yy+18,StudioTheme.PURPLE);
            StudioTheme.label(g,font,"YOUR RECORDINGS",x+cardW+49,yy+20,StudioTheme.TEXT);
            if(remaining>100)StudioTheme.label(g,font,fit("Browse and edit saved replay files.",smallW-30),x+cardW+24,yy+54,StudioTheme.MUTED);
        }
    }
    private void stat(GuiGraphics g,int x,int y,int w,int h,String val,String sub,int color){
        if(w<26||h<20)return;
        StudioTheme.rounded(g,x,y,w,h,StudioTheme.PANEL_ALT);
        StudioTheme.border(g,x,y,w,h,StudioTheme.BORDER);
        StudioTheme.label(g,font,fit(val,w-16),x+9,y+Math.max(7,h/3),color);
        if(h>37)StudioTheme.label(g,font,fit(sub,w-14),x+9,y+h-19,StudioTheme.MUTED);
    }
    private void drawLibrary(GuiGraphics g){
        int x=layout.contentX(),y=layout.contentY(),w=layout.contentWidth(),h=layout.contentHeight();
        boolean wide=w>=435;int listW=wide?w*58/100:w;
        heading(g,"Replay Library",replayList.size()+" saved recordings  /  local Vulkan-compatible replay files",x,y);
        int top=y+(layout.ultraCompact()?23:46),rh=libRowH();
        if(!libraryError.isEmpty())StudioTheme.label(g,font,libraryError,x+12,top+10,StudioTheme.RED);
        else if(replayList.isEmpty()){
            StudioTheme.softPanel(g,x+2,top,listW-4,Math.max(40,h-(top-y)-48));
            StudioTheme.icon(g,"library",x+18,top+20,StudioTheme.PURPLE);
            StudioTheme.label(g,font,"No recordings yet",x+42,top+23,StudioTheme.TEXT);
            if(h>150)StudioTheme.label(g,font,"Press F8 in a world to start capturing",x+18,top+52,StudioTheme.MUTED);
        }else{
            for(int i=0;i<libRows()&&i+replayOffset<replayList.size();i++){
                int index=i+replayOffset;var v=replayList.get(index);
                int yy=top+i*(rh+7);
                int tx=x+(rh>39?rh+14:28);
                if(rh>39){
                    StudioTheme.rounded(g,x+11,yy+10,rh-16,rh-20,0xFF32264D);
                    StudioTheme.icon(g,"editor",x+18,yy+(rh-16)/2,StudioTheme.PURPLE);
                }
                StudioTheme.label(g,font,fit(v.fileName(),Math.max(20,listW-(tx-x)-83)),tx,yy+Math.max(6,rh/3),StudioTheme.TEXT);
                if(rh>37)StudioTheme.label(g,font,DATE.format(v.modifiedAt()),tx,yy+rh-24,StudioTheme.MUTED);
                StudioTheme.label(g,font,humanBytes(v.byteSize()),x+listW-79,yy+rh/2-4,index==selectedReplay?StudioTheme.CYAN:StudioTheme.MUTED);
            }
        }
        if(wide){
            int rx=x+listW+11,rw=w-listW-11;
            StudioTheme.softPanel(g,rx,top,rw,Math.max(58,h-(top-y)-44));
            StudioTheme.sceneArt(g,rx+10,top+12,rw-20,Math.min(166,Math.max(30,h/3)),false);
            int textY=top+Math.min(166,Math.max(30,h/3))+24;
            StudioTheme.label(g,font,"PROJECT DETAILS",rx+14,textY,StudioTheme.CYAN);
            String name=selectedReplay>=0&&selectedReplay<replayList.size()?replayList.get(selectedReplay).fileName():"Select a recording";
            StudioTheme.label(g,font,fit(name,rw-30),rx+14,textY+20,StudioTheme.TEXT);
            StudioTheme.label(g,font,"Visual: decorative artwork",rx+14,textY+38,StudioTheme.SOFT);
            if(h>340)StudioTheme.label(g,font,"Use Open Replay to inspect actual footage",rx+14,textY+59,StudioTheme.MUTED);
        }
    }
    private void drawEditor(GuiGraphics g){
        int x=layout.contentX(),y=layout.contentY(),w=layout.contentWidth(),h=layout.contentHeight();
        heading(g,"Cinematic Editor","Momenton replay playback and native editor; Vulkan output is window-sized",x,y);
        int top=y+(layout.ultraCompact()?24:46);
        int hh=Math.max(60,Math.min(255,h-125));
        StudioTheme.softPanel(g,x,top,w,hh);
        StudioTheme.sceneArt(g,x+2,top+2,w-4,hh-4,false);
        StudioTheme.rect(g,x+2,top+2,w*50/100,hh-4,0xBB15132B);
        StudioTheme.label(g,font,"VULKAN REPLAY EDITOR",x+18,top+15,StudioTheme.PURPLE);
        if(hh>100){
            StudioTheme.label(g,font,"Record the moments, edit later.",x+18,top+45,StudioTheme.TEXT);
            StudioTheme.label(g,font,"Native timeline editing through Momenton",x+18,top+71,StudioTheme.MUTED);
            StudioTheme.label(g,font,"Freecam / 3rd person: not implemented",x+18,top+93,StudioTheme.CYAN);
        }
        int by=top+hh+13;
        if(by+40<y+h-46){
            StudioTheme.softPanel(g,x,by,w,Math.min(90,y+h-46-by));
            StudioTheme.label(g,font,"ACTUAL REPLAY TOOLS",x+14,by+14,StudioTheme.TEXT);
            if(w>380){
                StudioTheme.label(g,font,"Open a .momenton replay, then enter Momenton's native timeline editor.",x+14,by+37,StudioTheme.MUTED);
                StudioTheme.label(g,font,"Vulkan 1440p, camera paths and third-person export remain unimplemented.",x+14,by+57,StudioTheme.AMBER);
            }
        }
    }
    private void drawPerformance(GuiGraphics g){
        int x=layout.contentX(),y=layout.contentY(),w=layout.contentWidth(),h=layout.contentHeight();
        heading(g,"Performance Lab","Replay recording vs gameplay FPS. No frame capture in the benchmark.",x,y);
        int top=y+(layout.ultraCompact()?24:53),hh=Math.max(54,Math.min(158,h-94));
        var test=ReFlashClient.performanceSampler();var result=test.result();
        int gap=12, cw=(w-gap)/2;
        stat(g,x,top,cw,hh,result.idleSamples()>0?String.format(Locale.ROOT,"%.0f",result.idleFps()):"--","REPLAY OFF  /  FPS",StudioTheme.CYAN);
        stat(g,x+cw+gap,top,cw,hh,result.recordingSamples()>0?String.format(Locale.ROOT,"%.0f",result.recordingFps()):"--","REPLAY ON  /  FPS",StudioTheme.PURPLE);
        int bottom=top+hh+13;
        if(bottom+55<y+h){
            StudioTheme.softPanel(g,x,bottom,w,Math.min(100,y+h-54-bottom));
            StudioTheme.label(g,font,test.active()?"TEST IN PROGRESS":result.comparable()?"COMPARISON COMPLETE":"NO COMPARISON YET",x+16,bottom+19,StudioTheme.GREEN);
            if(result.comparable())StudioTheme.label(g,font,String.format(Locale.ROOT,"Relative FPS change: %+.1f%%",result.relativeChangePercent()),x+16,bottom+41,StudioTheme.TEXT);
            else StudioTheme.label(g,font,"Capture at least five samples with replay OFF, then ON.",x+16,bottom+41,StudioTheme.MUTED);
            if(bottom+77<y+h)StudioTheme.label(g,font,"Recording overhead varies by world, mods and hardware.",x+16,bottom+65,StudioTheme.SOFT);
        }
    }
    private void drawSettings(GuiGraphics g){
        int x=layout.contentX(),y=layout.contentY(),w=layout.contentWidth(),h=layout.contentHeight();
        heading(g,"Manager & Controls","One click or one custom key opens everything.",x,y);
        int base=y+(layout.ultraCompact()?18:43);
        int rh=layout.ultraCompact()?31:Math.min(61,Math.max(34,(h-55)/3));
        String[] labels={"MANAGER KEY","AUTO-RECORD","NOTIFICATIONS"};
        String[] details={"Current: " + ReFlashClient.managerKeyName(),
                "Record automatically after joining a world","Show Minecraft recording and save notifications"};
        for(int i=0;i<3;i++){
            int yy=base+i*rh;if(yy+rh-2>y+h)break;
            StudioTheme.softPanel(g,x,yy,w,rh-2);
            if(!layout.ultraCompact())StudioTheme.icon(g,i==0?"settings":i==1?"record":"spark",x+17,yy+16,i==0?StudioTheme.PURPLE:StudioTheme.CYAN);
            int textX=x+(layout.ultraCompact()?9:42);
            StudioTheme.label(g,font,fit(labels[i],Math.max(25,w-165)),textX,yy+7,StudioTheme.TEXT);
            if(rh>=55&&w>=340)StudioTheme.label(g,font,fit(details[i],Math.max(25,w-165)),textX,yy+29,StudioTheme.MUTED);
        }
        if(base+rh*3+30<y+h){
            StudioTheme.label(g,font,"CONTROLS -> KEY BINDS -> REFLASH",x+6,base+rh*3+10,StudioTheme.PURPLE);
        }
    }
    private void drawControl(GuiGraphics g,Control c){
        Button b=c.widget();if(!b.visible)return;
        int x=b.getX(),y=b.getY(),w=b.getWidth(),h=b.getHeight();boolean hot=b.isHoveredOrFocused();
        int fill,textColor,stroke=StudioTheme.BORDER;
        switch(c.skin()){
            case PRIMARY->{fill=hot?0xFFCE9DFF:StudioTheme.PURPLE_DARK;textColor=StudioTheme.TEXT;stroke=StudioTheme.PURPLE;}
            case DANGER->{fill=hot?0xFFC84875:0xFF792642;textColor=StudioTheme.TEXT;stroke=StudioTheme.RED;}
            case NAV->{fill=c.selected()?0xFF282044:hot?0xFF20243A:StudioTheme.SIDEBAR;textColor=c.selected()?StudioTheme.TEXT:StudioTheme.MUTED;stroke=c.selected()?StudioTheme.PURPLE:fill;}
            case TOGGLE->{fill=c.selected()?0xFF144A51:0xFF2A3146;textColor=c.selected()?StudioTheme.CYAN:StudioTheme.MUTED;stroke=c.selected()?StudioTheme.CYAN:StudioTheme.BORDER;}
            case ROW->{fill=c.selected()?0xFF202F49:hot?0xFF1D2A43:StudioTheme.PANEL;textColor=StudioTheme.TEXT;stroke=c.selected()?StudioTheme.CYAN:StudioTheme.BORDER;}
            case SECONDARY->{fill=hot?StudioTheme.PANEL_HOVER:StudioTheme.PANEL_ALT;textColor=StudioTheme.TEXT;stroke=hot?StudioTheme.CYAN:StudioTheme.BORDER;}
            default->{fill=hot?StudioTheme.PANEL_HOVER:StudioTheme.PANEL;textColor=StudioTheme.MUTED;stroke=hot?StudioTheme.CYAN:fill;}
        }
        StudioTheme.rounded(g,x,y,w,h,fill);
        if(stroke!=fill)StudioTheme.border(g,x,y,w,h,stroke);
        if(c.selected()&&c.skin()==Skin.NAV)StudioTheme.rect(g,x+1,y+6,3,h-12,StudioTheme.PURPLE);
        String label=fit(c.caption().get(),Math.max(8,w-13-(c.icon().isEmpty()?0:19)));
        if(!c.icon().isEmpty()){
            int iconY=y+(h-16)/2;
            StudioTheme.icon(g,c.icon(),x+12,iconY,c.selected()&&c.skin()==Skin.NAV?StudioTheme.PURPLE:c.skin()==Skin.PRIMARY?StudioTheme.TEXT:StudioTheme.CYAN);
            StudioTheme.label(g,font,label,x+34,y+(h-9)/2,textColor);
        } else if(c.skin()==Skin.NAV){StudioTheme.label(g,font,label,x+15,y+(h-9)/2,textColor);}
        else StudioTheme.centered(g,font,label,x+w/2,y+(h-9)/2,textColor);
    }
    private String fit(String s,int width){
        if(width<8)return"";if(font.width(s)<=width)return s;
        while(!s.isEmpty()&&font.width(s+"...")>width)s=s.substring(0,s.length()-1);
        return s.isEmpty()?"":s+"...";
    }
    private static String humanBytes(long size){
        if(size<1024)return size+"B";
        if(size<1024*1024)return size/1024+"KB";
        return String.format(Locale.ROOT,"%.1f MB",size/(1024.0*1024));
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}

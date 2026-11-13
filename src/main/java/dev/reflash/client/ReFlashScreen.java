package dev.reflash.client;

import dev.reflash.core.ExportPreset;
import dev.reflash.core.EditorTimeline;
import dev.reflash.core.PerformanceSampler;
import dev.reflash.core.ReplayIndex;
import dev.reflash.core.VideoPostProcessor;
import dev.reflash.core.VideoExportLibrary;
import dev.reflash.core.MicrophoneProbe;
import dev.reflash.core.DiagnosticsReport;
import dev.reflash.core.Preferences;
import dev.reflash.core.ReplayProject;
import dev.reflash.core.FfmpegCheck;
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
    private enum Page { CAPTURE, LIBRARY, EDITOR, EXPORT, PERFORMANCE, SETTINGS }
    private enum SettingsTab { RECORDING, AUDIO, KEYS }
    private SettingsTab settingsTab=SettingsTab.RECORDING;
    private enum Skin { PRIMARY, SECONDARY, QUIET, DANGER, NAV, ROW, TOGGLE, TIMELINE }
    private record Control(Button widget, Supplier<String> caption, Skin skin, boolean selected, String icon) { }
    private static final String[] NAV = { "Home", "Library", "Editor", "Export", "Performance", "Settings" };
    private static final String[] NAV_ICONS = { "home", "library", "editor", "export", "performance", "settings" };
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());
    private final Screen parent;
    private StudioLayout layout;
    private Page page = Page.CAPTURE;
    private final List<Control> controls = new ArrayList<>();
    private List<ReplayIndex.Replay> replayList = List.of();
    private int selectedReplay = -1, replayOffset, screenTicks;
    private ReplayProject editingProject;
    private int selectedClip;
    private int playheadSourceTick;
    private int editIdleTicks;
    private Boolean ffmpegReady;
    private List<VideoExportLibrary.Media> exportedVideos=List.of();
    private int selectedVideo=-1,videoScroll=0;
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
            int x=sx+12, tabW=(sw-24)/Page.values().length, y=sy+53;
            for(Page p:Page.values()){
                final Page next=p;
                add(x+p.ordinal()*tabW,y,tabW-3,28,()->NAV[next.ordinal()],Skin.NAV,p==page,NAV_ICONS[p.ordinal()],()->selectPage(next));
            }
        }
        switch(page){
            case CAPTURE -> initCapture();
            case LIBRARY -> initLibrary();
            case EDITOR -> initEditor();
            case EXPORT -> initExport();
            case PERFORMANCE -> initPerformance();
            case SETTINGS -> initSettings();
        }
    }
    private void selectPage(Page p){
        if(p==Page.LIBRARY)refreshLibrary();
        if(page==Page.EDITOR && p!=Page.EDITOR)savePendingEdits();
        if(p==Page.EDITOR && editingProject==null)loadProject();
        if(p==Page.EXPORT){ffmpegReady=FfmpegCheck.available(FabricLoader.getInstance().getGameDir());refreshVideos();}
        page=p;rebuildWidgets();
    }
    private void loadProject() {
        editingProject=null;selectedClip=0;playheadSourceTick=0;editIdleTicks=0;
        if(selectedReplay<0 && !replayList.isEmpty())selectedReplay=0;
        if(selectedReplay>=0 && selectedReplay<replayList.size()){
            try { editingProject=ReplayProject.open(replayList.get(selectedReplay).path());playheadSourceTick=editingProject.clips().getFirst().startTick(); }
            catch(IOException e){ libraryError="Cannot read replay metadata: "+e.getMessage(); }
        }
    }
    private void editClip(int what) {
        if(editingProject==null){selectPage(Page.LIBRARY);notice("Select a recording to edit first");return;}
        try {
            var c=editingProject.clips().get(selectedClip);
            switch(what){
                case 0 -> editingProject.trimStart(selectedClip,c.startTick()-20);
                case 1 -> editingProject.trimStart(selectedClip,c.startTick()+20);
                case 2 -> editingProject.trimEnd(selectedClip,c.endTick()-20);
                case 3 -> editingProject.trimEnd(selectedClip,c.endTick()+20);
                case 4 -> { if(!editingProject.splitAt(selectedClip,playheadSourceTick)) notice("Place the playhead inside a clip to split"); }
                case 5 -> {if(editingProject.delete(selectedClip)) selectedClip=Math.max(0,selectedClip-1);}
                case 6 -> selectedClip=(selectedClip+1)%editingProject.clips().size();
                case 7 -> { editingProject.save();notice("Cuts saved for replay playback and export"); }
                case 8 -> {if(!editingProject.undo())notice("Nothing to undo");}
                case 9 -> {if(!editingProject.redo())notice("Nothing to redo");}
                case 10 -> {if(editingProject.reset()){selectedClip=0;playheadSourceTick=0;}}
            }
            editIdleTicks=0;
            if(editingProject!=null && !editingProject.clips().isEmpty()) { selectedClip=Math.min(selectedClip,editingProject.clips().size()-1); var selected=editingProject.clips().get(selectedClip);playheadSourceTick=Math.clamp(playheadSourceTick,selected.startTick(),selected.endTick()-1); }
            rebuildWidgets();
        } catch(IllegalArgumentException|IndexOutOfBoundsException|IOException ex) {notice("Could not edit clip: "+ex.getMessage());}
    }

    private void add(int x,int y,int w,int h,Supplier<String> s,Skin skin,boolean selected,String icon,Runnable action){
        if(w<12||h<12||x<0||y<0||x+w>width||y+h>height)return;
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

    private void editLatestReplay() {
        refreshLibrary();
        if (replayList.isEmpty()) {
            selectPage(Page.LIBRARY);
            notice("No recordings yet. Finish a recording first.");
            return;
        }
        selectedReplay=0;
        editSelectedReplay();
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
            if (ReFlashClient.toggleRecording(minecraft)) {
                notice(was?"Recording stopped / saving":"Recording started");
                rebuildWidgets();
            }
        });
        add(x+15+bw+gap,ay,bw,28,()->"Recordings",Skin.SECONDARY,false,"library",()->selectPage(Page.LIBRARY));
        if(buttonCount==3) add(x+15+(bw+gap)*2,ay,bw,28,()->"Edit Latest",Skin.SECONDARY,false,"editor",this::editLatestReplay);
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
                selectedReplay=n;editingProject=null;rebuildWidgets();
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
        if(editingProject==null && selectedReplay>=0)loadProject();
        int top=y+(layout.ultraCompact()?24:46);
        int hero=Math.max(60,Math.min(166,h*43/100));
        int trimY=top+hero+68;
        if(editingProject!=null){
            int timelineY=top+hero+8;
            int areaW=w-30,segments=Math.max(4,Math.min(40,areaW/15));
            for(int i=0;i<segments;i++){
                final int segment=i, count=segments;
                int xx=x+15+segment*areaW/count;
                int ww=Math.max(2,x+15+(segment+1)*areaW/count-xx);
                add(xx,timelineY+23,ww,15,()->"",Skin.TIMELINE,false,"",()->{
                    var pos=EditorTimeline.at(editingProject.clips(),areaW,
                        (segment*areaW/count)+(areaW/(count*2)));
                    selectedClip=pos.clipIndex();playheadSourceTick=pos.sourceTick();editIdleTicks=0;rebuildWidgets();
                });
            }
        }
        if(editingProject!=null && trimY+73<y+h-40 && w>=285){
            int bw=Math.max(45,Math.min(100,(w-45)/4));
            add(x+12,trimY,bw,25,()->"Start -1s",Skin.SECONDARY,false,"",()->editClip(0));
            add(x+18+bw,trimY,bw,25,()->"Start +1s",Skin.SECONDARY,false,"",()->editClip(1));
            add(x+24+bw*2,trimY,bw,25,()->"End -1s",Skin.SECONDARY,false,"",()->editClip(2));
            add(x+30+bw*3,trimY,bw,25,()->"End +1s",Skin.SECONDARY,false,"",()->editClip(3));
            add(x+12,trimY+32,bw,24,()->"Next clip",Skin.SECONDARY,false,"",()->editClip(6));
            add(x+18+bw,trimY+32,bw,24,()->"Split",Skin.SECONDARY,false,"",()->editClip(4));
            add(x+24+bw*2,trimY+32,bw,24,()->"Delete",Skin.DANGER,false,"",()->editClip(5));
            add(x+30+bw*3,trimY+32,bw,24,()->"Save cuts",Skin.PRIMARY,false,"",()->editClip(7));
            if(trimY+63+24<y+h-40){
                add(x+12,trimY+63,bw,23,()->"Undo",Skin.QUIET,false,"",()->editClip(8));
                add(x+18+bw,trimY+63,bw,23,()->"Redo",Skin.QUIET,false,"",()->editClip(9));
                add(x+24+bw*2,trimY+63,bw,23,()->"Reset",Skin.QUIET,false,"",()->editClip(10));
            }
        }
        int actionsY=y+h-36;
        int bw=Math.max(80,Math.min(205,(w-30)/2));
        add(x+9,actionsY,bw,28,()->editingProject==null?"Choose Recording":"Preview Replay",Skin.PRIMARY,false,"play",()->{
            if(editingProject==null){selectPage(Page.LIBRARY);return;}
            try { editingProject.save(); }catch(IOException e){notice("Could not save project: "+e.getMessage());return;}
            editSelectedReplay();
        });
        if(w>=250)add(x+18+bw,actionsY,Math.min(bw,w-bw-27),28,()->"Export & Presets",Skin.SECONDARY,false,"export",()->selectPage(Page.EXPORT));
    }

    private static final int[][] SIZES = {{1280,720},{1920,1080},{2560,1440},{3840,2160}};
    private static final int[] RATES = {30,60,120,240};
    private void nextSize() {
        var p=ReFlashClient.preferences();int index=0;
        for(int i=0;i<SIZES.length;i++)if(SIZES[i][0]==p.outputWidth())index=i;
        int[] size=SIZES[(index+1)%SIZES.length];p.setOutputPreset(size[0],size[1],p.outputFps());
        ReFlashClient.savePreferences();rebuildWidgets();
    }
    private void nextRate() {
        var p=ReFlashClient.preferences();int index=0;
        for(int i=0;i<RATES.length;i++)if(RATES[i]==p.outputFps())index=i;
        p.setOutputPreset(p.outputWidth(),p.outputHeight(),RATES[(index+1)%RATES.length]);
        ReFlashClient.savePreferences();rebuildWidgets();
    }
    private void nextPreset() {
        var p = ReFlashClient.preferences();
        var next = p.exportPreset().next();
        p.setExportPreset(next);
        p.setOutputPreset(next.width(),next.height(),next.fps());
        ReFlashClient.savePreferences();
        rebuildWidgets();
    }
    private void refreshVideos(){
        try{
            exportedVideos=VideoExportLibrary.list(FabricLoader.getInstance().getGameDir(),64);
            if(selectedVideo>=exportedVideos.size())selectedVideo=-1;
        }catch(IOException ex){exportedVideos=List.of();selectedVideo=-1;notice("Could not scan exported videos");}
    }
    private void openExportFolder(){
        try{
            Path folder=VideoExportLibrary.folder(FabricLoader.getInstance().getGameDir());Files.createDirectories(folder);
            if(Desktop.isDesktopSupported()&&Desktop.getDesktop().isSupported(Desktop.Action.OPEN))Desktop.getDesktop().open(folder.toFile());
            else notice("Output folder: "+folder);
        }catch(Exception ex){notice("Could not open export folder");}
    }
    private void renderSelectedReplay(){
        if(NativeReplayExporter.isRendering()){NativeReplayExporter.cancel();notice("Cancelling native render");return;}
        if(editingProject==null)loadProject();
        if(editingProject==null){notice("Select a recording in the Library first");selectPage(Page.LIBRARY);return;}
        if(MomentonBridge.isRecording()){notice("Stop recording before rendering");return;}
        if(minecraft.level!=null&&!MomentonBridge.isInReplay()){
            notice("Open the replay first, or return to title screen to render");return;
        }
        savePendingEdits();
        boolean started=NativeReplayExporter.render(minecraft,this,FabricLoader.getInstance().getGameDir(),
                editingProject,ReFlashClient.preferences().outputFps());
        notice(NativeReplayExporter.status());
        if(started)ffmpegReady=true;
    }
    private void initExport() {
        int x=layout.contentX(),y=layout.contentY(),w=layout.contentWidth(),h=layout.contentHeight();
        int top=y+(layout.ultraCompact()?23:44),row=36;
        int labelWidth=w>=360?155:100,buttonX=x+w-Math.min(195,Math.max(102,w/3));
        int bw=x+w-buttonX-10;
        add(buttonX,top,bw,26,()->ReFlashClient.preferences().outputWidth()+" x "+ReFlashClient.preferences().outputHeight(),
                Skin.SECONDARY,false,"export",this::nextSize);
        add(buttonX,top+row,bw,26,()->ReFlashClient.preferences().outputFps()+" FPS",Skin.SECONDARY,false,"performance",this::nextRate);
        if(h>=220)add(buttonX,top+row*2,bw,26,()->"Preset: "+ReFlashClient.preferences().exportPreset().label(),
                Skin.PRIMARY,false,"export",this::nextPreset);
        int middle=top+(h>=220?137:88);
        int controlsY=y+h-34;
        int nW=Math.max(94,(w-34)/2);
        if(middle+30<controlsY-12){
            add(x+8,middle,nW,26,
                ()->NativeReplayExporter.isRendering()?"Cancel Native Render":"Render Replay (Window)",
                Skin.PRIMARY,false,"play",this::renderSelectedReplay);
            if(w>=270)add(x+15+nW,middle,Math.max(70,w-nW-23),26,()->"Refresh Videos",Skin.SECONDARY,false,"library",()->{
                refreshVideos();rebuildWidgets();
            });
        }
        int listY=middle+46;
        int maxRows=Math.max(0,Math.min(3,(controlsY-listY-8)/28));
        for(int i=0;i<maxRows;i++){
            int index=i+videoScroll;
            if(index>=exportedVideos.size())break;
            final int chosen=index;
            add(x+8,listY+i*28,w-16,24,()->exportedVideos.get(chosen).fileName(),
                Skin.ROW,index==selectedVideo,"play",()->{selectedVideo=chosen;rebuildWidgets();});
        }
        if(h>=300&&exportedVideos.size()>maxRows&&maxRows>0){
            add(x+w-74,listY+maxRows*28,66,19,()->"More >",Skin.QUIET,false,"",()->{
                videoScroll=(videoScroll+maxRows)%exportedVideos.size();rebuildWidgets();
            });
        }
        int lowerW=w>=600?Math.max(88,(w-42)/3):nW;
        add(x+8,controlsY,lowerW,27,
            ()->VideoPostProcessor.running()?"Cancel Enhance":"Enhance Selected MP4",Skin.SECONDARY,false,"export",()->{
                if(VideoPostProcessor.running()){VideoPostProcessor.cancel();return;}
                if(minecraft.level!=null&&!MomentonBridge.isInReplay()){
                    notice("Leave the live world before CPU-heavy offline video processing");return;
                }
                if(selectedVideo<0||selectedVideo>=exportedVideos.size()){notice("Select an exported video first");return;}
                if(!VideoPostProcessor.startSelected(FabricLoader.getInstance().getGameDir(),
                    exportedVideos.get(selectedVideo).path(),ReFlashClient.preferences()))notice(VideoPostProcessor.status());
                else notice("Offline video enhancement started");
            });
        if(w>=600){
            add(x+15+lowerW,controlsY,lowerW,27,()->"Queue First 6",Skin.SECONDARY,false,"export",()->{
                if(VideoPostProcessor.running()){notice("A video job is already running");return;}
                if(minecraft.level!=null&&!MomentonBridge.isInReplay()){
                    notice("Leave the live world to preserve gameplay FPS");return;
                }
                var paths=exportedVideos.stream().limit(6).map(VideoExportLibrary.Media::path).toList();
                if(!VideoPostProcessor.startBatch(FabricLoader.getInstance().getGameDir(),paths,ReFlashClient.preferences()))notice(VideoPostProcessor.status());
                else notice("Queued "+paths.size()+" videos (sequential processing)");
            });
            add(x+22+lowerW*2,controlsY,Math.max(66,w-lowerW*2-30),27,()->"Output Folder",Skin.QUIET,false,"folder",this::openExportFolder);
        }else if(w>=270){
            add(x+15+lowerW,controlsY,Math.max(70,w-lowerW-23),27,()->"Output Folder",Skin.QUIET,false,"folder",this::openExportFolder);
        }

    }
    private void drawExport(GuiGraphics g) {
        int x=layout.contentX(),y=layout.contentY(),w=layout.contentWidth(),h=layout.contentHeight();
        heading(g,"Export Studio","Render gameplay at window size, then select optional output enhancement",x,y);
        int top=y+(layout.ultraCompact()?23:44);
        int heroHeight=h>=220?110:78;
        StudioTheme.softPanel(g,x,top-5,w,heroHeight);
        StudioTheme.label(g,font,"OUTPUT RESOLUTION",x+12,top+7,StudioTheme.CYAN);
        StudioTheme.label(g,font,"RENDER / OUTPUT FPS",x+12,top+43,StudioTheme.PURPLE);
        if(h>=220)StudioTheme.label(g,font,"ONE-CLICK PRESET",x+12,top+79,StudioTheme.CYAN);
        int middle=top+(h>=220?137:88);
        if(middle+30<y+h-46){
            StudioTheme.label(g,font,fit("RENDER SOURCE    "+(editingProject==null?"Choose recording in Library":editingProject.replayPath().getFileName().toString()),w-16),
                x+9,middle-15,StudioTheme.MUTED);
        }
        int listY=middle+46;
        if(listY+28<y+h-41){
            StudioTheme.label(g,font,fit("EXPORTED FILES  /  "+exportedVideos.size()+"  •  Pick one to enhance",w-10),x+9,listY-12,StudioTheme.CYAN);
        }
        if(h>=310){
            StudioTheme.label(g,font,fit(Boolean.TRUE.equals(ffmpegReady)?VideoPostProcessor.status():
                "FFmpeg unavailable — install to enable native render and MP4 enhancement",w-14),x+9,y+h-61,Boolean.TRUE.equals(ffmpegReady)?StudioTheme.MUTED:StudioTheme.AMBER);
            if(VideoPostProcessor.jobsTotal()>0){
                int width=Math.max(40,w-16);
                StudioTheme.rect(g,x+8,y+h-49,width,5,StudioTheme.PANEL_ALT);
                StudioTheme.rect(g,x+8,y+h-49,width*VideoPostProcessor.jobsDone()/VideoPostProcessor.jobsTotal(),5,StudioTheme.PURPLE);
            }
        }
    }

    private void initPerformance(){
        int x=layout.contentX(),y=layout.contentY(),w=layout.contentWidth(),h=layout.contentHeight();
        add(x+12,y+h-39,Math.min(235,w-24),29,
                ()->ReFlashClient.performanceSampler().active()?"End FPS Comparison":"Start FPS Comparison",Skin.PRIMARY,false,"performance",ReFlashClient::toggleBenchmark);
    }
    private void chooseSettings(SettingsTab tab){settingsTab=tab;rebuildWidgets();}
    private int settingsStart(){return layout.contentY()+(layout.ultraCompact()?25:73);}
    private int settingsRowHeight(){return layout.ultraCompact()?28:Math.min(49,Math.max(29,(layout.contentHeight()-80)/5));}
    private void initSettings(){
        int x=layout.contentX(),y=layout.contentY(),w=layout.contentWidth(),h=layout.contentHeight();
        int tw=(w-16)/3,tabY=y+(layout.ultraCompact()?0:40);
        for(SettingsTab t:SettingsTab.values()){
            int index=t.ordinal();
            add(x+index*tw,tabY,tw-4,24,()->t==SettingsTab.RECORDING?"Recording":t==SettingsTab.AUDIO?"Audio":"Keybinds",
                Skin.NAV,t==settingsTab,"settings",()->chooseSettings(t));
        }
        int start=settingsStart(),rh=settingsRowHeight(),bw=Math.max(88,Math.min(176,w/3));
        int xx=x+w-bw-12;
        switch(settingsTab){
            case RECORDING->{
                add(xx,start+3,bw,24,()->"Manual Only",Skin.QUIET,false,"record",()->notice("Recording is started only by the button or hotkey"));
                add(xx,start+rh+3,bw,24,()->ReFlashClient.preferences().notifications()?"Notifications ON":"Notifications OFF",Skin.TOGGLE,
                    ReFlashClient.preferences().notifications(),"",()->{
                        var p=ReFlashClient.preferences();p.setNotifications(!p.notifications());ReFlashClient.savePreferences();rebuildWidgets();
                    });
                add(xx,start+rh*2+3,bw,24,()->ReFlashClient.preferences().hudPosition().name().replace('_',' '),Skin.SECONDARY,false,"record",()->{
                    var p=ReFlashClient.preferences();p.setHudPosition(p.hudPosition().next());ReFlashClient.savePreferences();rebuildWidgets();
                });
                add(xx,start+rh*3+3,bw,24,()->ReFlashClient.preferences().hudSize().name(),Skin.SECONDARY,false,"settings",()->{
                    var p=ReFlashClient.preferences();p.setHudSize(p.hudSize().next());ReFlashClient.savePreferences();rebuildWidgets();
                });
                add(xx,start+rh*4+3,bw,24,()->ReFlashClient.preferences().hudOpacity()+"% Opacity",Skin.SECONDARY,false,"settings",()->{
                    var p=ReFlashClient.preferences();p.setHudOpacity(p.hudOpacity()>=100?25:p.hudOpacity()+25);ReFlashClient.savePreferences();rebuildWidgets();
                });
            }
            case AUDIO->{
                add(xx,start+3,bw,24,()->MomentonBridge.micEnabled()?"Mic ON (next take)":"Mic OFF (next take)",Skin.TOGGLE,
                    MomentonBridge.micEnabled(),"",()->{
                        if(!MomentonBridge.setMicEnabled(!MomentonBridge.micEnabled()))notice("Microphone setting unavailable");rebuildWidgets();
                    });
                add(xx,start+rh+3,bw,24,()->"Device: "+MomentonBridge.micDevice(),Skin.SECONDARY,false,"record",()->{
                    if(!MomentonBridge.cycleMicDevice())notice("No compatible input devices detected");rebuildWidgets();
                });
                add(xx,start+rh*2+3,bw,24,()->MicrophoneProbe.running()?"Testing...":"Test Mic (0.6s)",Skin.PRIMARY,false,"record",()->{
                    if(!MicrophoneProbe.start(MomentonBridge.micDevice()))notice("Microphone test already running");
                    else notice("Mic test started: never saves audio");
                });
                add(xx,start+rh*3+3,bw,24,()->"Game audio: Replay",Skin.QUIET,false,"settings",()->notice("Minecraft sound is rendered during offline replay export"));
                add(xx,start+rh*4+3,bw,24,()->"Windows audio: OFF",Skin.QUIET,false,"settings",()->notice("Desktop/meeting audio capture is not implemented. No silent capture."));
            }
            case KEYS->{
                add(xx,start+3,bw,24,()->"Change Keybinds",Skin.PRIMARY,false,"settings",this::openKeybinds);
                add(xx,start+rh+3,bw,24,()->"Manager: "+ReFlashClient.managerKeyName(),Skin.QUIET,false,"settings",this::openKeybinds);
                add(xx,start+rh*2+3,bw,24,()->"Record: F8 default",Skin.QUIET,false,"record",this::openKeybinds);
                add(xx,start+rh*3+3,bw,24,()->"FPS test: F12 default",Skin.QUIET,false,"performance",this::openKeybinds);
                add(xx,start+rh*4+3,bw,24,()->"Unbind Momenton: "+ReFlashClient.momentonRecordKey(),Skin.SECONDARY,false,"settings",()->{
                    if(!ReFlashClient.unbindMomentonRecordingKey())notice("Momenton key unavailable");rebuildWidgets();
                });
                add(xx,start+rh*5+3,bw,24,()->"Save Diagnostics",Skin.SECONDARY,false,"settings",()->{
                    Path path=DiagnosticsReport.create(FabricLoader.getInstance().getGameDir(),ReFlashClient.preferences(),
                        MomentonBridge.backendPresent(),ReFlashClient.momentonRecordKey(),
                        ReFlashClient.performanceSampler().result(),FfmpegCheck.available(FabricLoader.getInstance().getGameDir()));
                    notice(path==null?"Could not save diagnostics":"Saved reflash/diagnostics.txt (no personal details)");
                });
            }
        }
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
    private void savePendingEdits(){
        if(editingProject==null||!editingProject.dirty())return;
        try{editingProject.save();}catch(IOException e){notice("Auto-save failed: "+e.getMessage());}
    }
    private void notice(String message){notice=message;noticeTicks=100;}
    @Override public void tick(){
        screenTicks++;if(noticeTicks>0)noticeTicks--;
        if(page==Page.EDITOR&&editingProject!=null&&editingProject.dirty()&&++editIdleTicks>=160){
            savePendingEdits();editIdleTicks=0;
        }
    }
    @Override public void onClose(){savePendingEdits();minecraft.setScreen(parent);}

    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        StudioTheme.rect(g,0,0,width,height,0xD8060A13);
        drawChrome(g);
        switch(page){
            case CAPTURE -> drawCapture(g);
            case LIBRARY -> drawLibrary(g);
            case EDITOR -> drawEditor(g);
            case EXPORT -> drawExport(g);
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
        StudioTheme.label(g,font,"v0.10.0 VK / FABRIC 1.21.11",x+w-174,y+h-16,StudioTheme.SOFT);
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
            StudioTheme.icon(g,"camera",rx+16,top+18,StudioTheme.PURPLE);
            StudioTheme.label(g,font,"REPLAY DETAILS",rx+41,top+20,StudioTheme.CYAN);
            String name=selectedReplay>=0&&selectedReplay<replayList.size()?replayList.get(selectedReplay).fileName():"No replay selected";
            StudioTheme.label(g,font,fit(name,rw-29),rx+14,top+52,StudioTheme.TEXT);
            if(h>210) {
                StudioTheme.label(g,font,fit("This is not a replay thumbnail.",rw-25),rx+14,top+80,StudioTheme.SOFT);
                StudioTheme.label(g,font,fit("Use Edit Selected for the cut timeline.",rw-25),rx+14,top+105,StudioTheme.MUTED);
                StudioTheme.label(g,font,fit("The world loads before editor opens.",rw-25),rx+14,top+129,StudioTheme.MUTED);
            }
        }
    }
    private void drawEditor(GuiGraphics g){
        int x=layout.contentX(),y=layout.contentY(),w=layout.contentWidth(),h=layout.contentHeight();
        heading(g,"ReFlash Clip Studio","Trim and split here; Preview loads the real Minecraft replay",x,y);
        int top=y+(layout.ultraCompact()?24:46);
        int hero=Math.max(60,Math.min(166,h*43/100));
        StudioTheme.softPanel(g,x,top,w,hero);
        StudioTheme.rounded(g,x+2,top+2,w-4,hero-4,0xFF111A2A);
        StudioTheme.rect(g,x+2,top+2,4,hero-4,StudioTheme.PURPLE);
        if(w>490){
            int px=x+w*62/100;
            StudioTheme.rounded(g,px,top+20,w*30/100,hero-40,0xFF20283B);
            StudioTheme.icon(g,"play",px+w*14/100,top+42,StudioTheme.CYAN);
            StudioTheme.label(g,font,"REPLAY VIEW",px+17,top+70,StudioTheme.TEXT);
            if(hero>105)StudioTheme.label(g,font,"Load the replay for live world preview",px+17,top+93,StudioTheme.SOFT);
        }
        StudioTheme.label(g,font,editingProject==null?"CHOOSE A RECORDING IN LIBRARY":"EDITING SAVED REPLAY",x+16,top+15,StudioTheme.CYAN);
        if(editingProject!=null){
            String title=replayList.get(selectedReplay).fileName();
            StudioTheme.label(g,font,fit(title,w*50/100-20),x+16,top+35,StudioTheme.TEXT);
            StudioTheme.label(g,font,"Length: "+(editingProject.lengthTicks()/20.0)+"s  •  Clips: "+editingProject.clips().size(),x+16,top+53,StudioTheme.MUTED);
            if(hero>85)StudioTheme.label(g,font,editingProject.hasMicrophoneTrack()?"Mic audio embedded":"No microphone track detected",x+16,top+72,
                    editingProject.hasMicrophoneTrack()?StudioTheme.GREEN:StudioTheme.AMBER);
        }
        int timelineY=top+hero+8;
        int timelineH=Math.min(151,Math.max(28,y+h-55-timelineY));
        if(editingProject!=null && timelineH>27){
            StudioTheme.softPanel(g,x,timelineY,w,timelineH);
            StudioTheme.label(g,font,"CUT TIMELINE",x+14,timelineY+8,StudioTheme.PURPLE);
            int yy=timelineY+25,areaX=x+15,areaW=w-30;
            int total=Math.max(1,editingProject.lengthTicks()),cursor=0;
            for(int i=0;i<editingProject.clips().size();i++){
                var c=editingProject.clips().get(i);
                int ww=Math.max(2,(int)((long)c.length()*areaW/total));
                StudioTheme.rect(g,areaX+cursor,yy,Math.min(ww,areaW-cursor),9,i==selectedClip?StudioTheme.PURPLE:StudioTheme.CYAN);
                cursor+=ww;if(cursor>=areaW)break;
            }
            int px=EditorTimeline.pixelAt(editingProject.clips(),areaW,selectedClip,playheadSourceTick);
            StudioTheme.rect(g,areaX+px,yy-6,2,22,StudioTheme.AMBER);
            if(timelineH>46){
                var c=editingProject.clips().get(selectedClip);
                String text="Clip "+(selectedClip+1)+" / "+editingProject.clips().size()+"  |  Playhead "+(playheadSourceTick/20.0)+"s  |  "+c.startTick()/20.0+"-"+c.endTick()/20.0+"s"+(editingProject.dirty()?"  *":"");
                StudioTheme.label(g,font,fit(text,w-25),x+14,timelineY+36,StudioTheme.TEXT);
            }
        }
    }

    private void drawPerformance(GuiGraphics g){
        int x=layout.contentX(),y=layout.contentY(),w=layout.contentWidth(),h=layout.contentHeight();
        heading(g,"FPS Comparison","F12 toggles this test on and off. The button below does the same.",x,y);
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
            if(bottom+77<y+h){
                StudioTheme.label(g,font,"Lowest observed sample: OFF "+result.lowestIdleSample()+" / ON "+result.lowestRecordingSample()+" FPS",x+16,bottom+65,StudioTheme.SOFT);
            }
        }
    }
    private void drawSettings(GuiGraphics g){
        int x=layout.contentX(),y=layout.contentY(),w=layout.contentWidth(),h=layout.contentHeight();
        heading(g,"Studio Settings","Recording, audio privacy, and controls",x,y);
        int start=settingsStart(),rh=settingsRowHeight(),bw=Math.max(88,Math.min(176,w/3));
        String[] labels,details;
        if(settingsTab==SettingsTab.RECORDING){
            labels=new String[]{"RECORDING MODE","NOTIFICATIONS","RECORDING INDICATOR","INDICATOR SIZE","INDICATOR OPACITY"};
            details=new String[]{"Button or remappable hotkey only","Status messages and save confirmation","Top-left, top-right, or hidden","Compact fits small screens","25% to 100%"};
        }else if(settingsTab==SettingsTab.AUDIO){
            labels=new String[]{"MICROPHONE CAPTURE","MICROPHONE DEVICE","OPT-IN MIC CHECK","MINECRAFT GAME AUDIO","WINDOWS DESKTOP AUDIO"};
            details=new String[]{"For recordings only when enabled","Select the proper microphone",MicrophoneProbe.status(),
                "Recreated during replay export","Not captured; requires separate opt-in system"};
        }else{
            labels=new String[]{"KEYBOARD SHORTCUTS","OPEN MANAGER","START / STOP RECORDING","FPS COMPARISON","MOMENTON F9 CONFLICT"};
            details=new String[]{"All ReFlash keys are remappable","Current: "+ReFlashClient.managerKeyName(),"Same action as record button",
                "Toggle test with F12","Click to disable the duplicate Momenton recording key"};
        }
        for(int i=0;i<labels.length;i++){
            int yy=start+i*rh;
            if(yy+rh-2>y+h-12)break;
            StudioTheme.softPanel(g,x,yy,w,rh-3);
            int labelX=x+(layout.ultraCompact()?9:29);
            if(!layout.ultraCompact())StudioTheme.icon(g,settingsTab==SettingsTab.AUDIO?"record":"settings",x+9,yy+9,StudioTheme.CYAN);
            StudioTheme.label(g,font,fit(labels[i],Math.max(15,w-bw-44)),labelX,yy+6,StudioTheme.TEXT);
            if(rh>=36)StudioTheme.label(g,font,fit(details[i],Math.max(15,w-bw-44)),labelX,yy+22,StudioTheme.MUTED);
        }
    }

    private void drawControl(GuiGraphics g,Control c){
        Button b=c.widget();if(!b.visible||c.skin()==Skin.TIMELINE)return;
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
    @Override public boolean isPauseScreen(){return false;}
}

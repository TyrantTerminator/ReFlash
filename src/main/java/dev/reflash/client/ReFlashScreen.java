package dev.reflash.client;

import dev.reflash.core.ExportPreset;
import dev.reflash.core.ExportSizeEstimate;
import dev.reflash.core.UpscaleProfile;
import dev.reflash.core.ReplayDuration;
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
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Responsive, native ReFlash Studio. All interaction goes through real Minecraft Button
 * widgets. The custom drawing code runs only while this menu is open: no gameplay HUD cost.
 */
public final class ReFlashScreen extends Screen {
    private enum Page { CAPTURE, LIBRARY, EDITOR, EXPORT, PERFORMANCE, SETTINGS }
    private enum SettingsTab { RECORDING, AUDIO, KEYS }
    /** Export setup and output videos live in two clearly labelled views. */
    private enum ExportView { PRESETS, VIDEOS }
    private enum CompactPresetGroup { RESOLUTION, FRAME_RATE, QUALITY }
    private ExportView exportView = ExportView.PRESETS;
    private CompactPresetGroup compactPresetGroup = CompactPresetGroup.FRAME_RATE;
    private int presetPulseTicks = 18;
    private Path requestedReplayPath;
    private SettingsTab settingsTab=SettingsTab.RECORDING;
    private enum Skin { PRIMARY, SECONDARY, QUIET, DANGER, NAV, ROW, TOGGLE, TIMELINE, SELECTOR }
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
    private ReplayDuration.Info selectedInfo;
    private List<VideoExportLibrary.Media> exportedVideos=List.of();
    private int selectedVideo=-1,videoScroll=0;
    private String libraryError = "", notice = "";
    private int noticeTicks;
    private dev.reflash.core.RecordingSaveMonitor.State lastSaveUiState;
    // Lightweight UI-only easing; no extra textures, shaders or gameplay hooks.
    private int pageRevealTicks=14;
    private final Map<Button,Float> hoverEase=new IdentityHashMap<>();

    public ReFlashScreen(Screen parent){super(Component.literal("ReFlash Studio"));this.parent=parent;}
    public static ReFlashScreen library(Screen parent){var s=new ReFlashScreen(parent);s.page=Page.LIBRARY;return s;}
    /** Always opens the branded exporter, even from an engine-owned replay screen. */
    public static ReFlashScreen exporting(Screen parent, Path replay){
        var s=new ReFlashScreen(parent);s.page=Page.EXPORT;s.requestedReplayPath=replay;
        return s;
    }
    /** Keeps native replay-editor Export actions inside ReFlash's own export workspace. */
    public void openExportTab(){ selectPage(Page.EXPORT); }
    private Path replayFolder(){return ReFlashReplayBridge.replayFolder(FabricLoader.getInstance().getGameDir());}

    @Override protected void init(){
        layout=StudioLayout.forViewport(width,height);
        controls.clear();
        hoverEase.clear();
        if(replayList.isEmpty() && libraryError.isEmpty())refreshLibrary();
        if(page==Page.EXPORT && editingProject==null && selectedReplay>=0)loadProject();
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
        if(p==Page.EXPORT){
            ffmpegReady=FfmpegCheck.available(FabricLoader.getInstance().getGameDir());
            refreshVideos();
            if(editingProject==null && selectedReplay>=0) loadProject();
        }
        page=p;pageRevealTicks=0;rebuildWidgets();
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
        // A recording cannot start from the title screen or an unloaded world.
        // Keep the shortcut visible while making its unavailable state obvious.
        if ("Saving...".equals(s.get()) ||
                ("Start Recording".equals(s.get()) && !ReFlashReplayBridge.isRecording() &&
                (minecraft == null || minecraft.level == null || minecraft.player == null))) {
            b.active = false;
        }
        addRenderableWidget(b);
        controls.add(new Control(b,s,skin,selected,icon));
    }

    private void openKeybinds() {
        minecraft.setScreen(new KeyBindsScreen(this, minecraft.options));
    }

    private void editSelectedReplay() {
        if(selectedReplay<0 || selectedReplay>=replayList.size()){
            selectPage(Page.LIBRARY);notice("Choose a saved recording first");return;
        }
        if(ReFlashReplayBridge.isRecording()){
            notice("Stop recording before editing");return;
        }
        loadProject();
        if(editingProject==null){notice("Cannot read this replay project");return;}
        // Editing is always ReFlash Studio. World playback is a separate Preview action.
        selectPage(Page.EDITOR);
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
        add(x+15,ay,bw,28,()->ReFlashReplayBridge.isRecording()?"Stop & Save":
                        ReFlashClient.saveState()==dev.reflash.core.RecordingSaveMonitor.State.SAVING?"Saving...":"Start Recording",
                ReFlashReplayBridge.isRecording()?Skin.DANGER:Skin.PRIMARY,false,"record",()->{
            boolean was=ReFlashReplayBridge.isRecording();
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
        // Reserve the library heading, separate action toolbar and footer.
        int avail=layout.contentHeight()-(layout.ultraCompact()?69:144);
        return Math.max(1,Math.min(6,avail/(libRowH()+7)));
    }
    private void initLibrary(){
        int x=layout.contentX(),y=layout.contentY(),w=layout.contentWidth(),h=layout.contentHeight();
        boolean wide=w>=435;
        int listW=wide?w*58/100:w;
        if(!layout.ultraCompact())add(x+Math.max(0,listW-87),y+40,83,24,()->"Refresh",Skin.QUIET,false,"spark",()->{refreshLibrary();rebuildWidgets();});
        if(!layout.ultraCompact()&&listW>=240)add(x+8,y+40,132,24,()->"Import Old",Skin.QUIET,false,"library",()->{
            try {
                var imported=dev.reflash.core.LegacyReplayImporter.importRecordings(FabricLoader.getInstance().getGameDir());
                notice("Imported "+imported.imported()+" old recordings ("+imported.invalid()+" invalid)");
                refreshLibrary();rebuildWidgets();
            }catch(IOException e){notice("Old recordings import failed: "+e.getMessage());}
        });
        int startY=y+(layout.ultraCompact()?23:74),rh=libRowH();
        for(int i=0;i<libRows();i++){
            int index=replayOffset+i;
            if(index>=replayList.size())break;
            final int n=index;
            add(x+6,startY+i*(rh+7),Math.max(25,listW-12),rh,()->replayList.get(n).fileName(),Skin.ROW,index==selectedReplay,"library",()->{
                selectedReplay=n;editingProject=null;inspectSelectedReplay();rebuildWidgets();
            });
        }
        int controlsY=y+h-32;
        add(x+6,controlsY,Math.max(69,Math.min(150,listW*47/100)),25,()->"Open Replay",Skin.PRIMARY,false,"play",this::openSelectedReplay);
        if(wide){
            add(x+listW+14,controlsY,Math.max(24,w-listW-27),25,()->"Edit Selected",Skin.SECONDARY,false,"editor",this::editSelectedReplay);
        }else if(w>=245){
            add(x+Math.min(155,listW*47/100)+12,controlsY,Math.max(32,w-Math.min(155,listW*47/100)-18),25,()->"Edit Selected",Skin.SECONDARY,false,"editor",this::editSelectedReplay);
        }
        if(wide && selectedInfo!=null && selectedInfo.shortened() && h>=250){
            int rx=x+listW+14;
            add(rx,controlsY-36,Math.max(55,w-listW-27),27,()->"Restore Full Recording",
                    Skin.PRIMARY,false,"editor",this::restoreFullRecording);
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
    private int editorTop(int y,int h){ return y+(h<158?23:h<260?32:46); }
    private int editorPreviewHeight(int h){
        if(h<158)return 31;
        if(h<260)return 48;
        return Math.max(64,Math.min(228,Math.min(h*46/100,h-265)));
    }
    private int editorTimelineHeight(int y,int h,int timelineY){
        return Math.min(179,Math.max(28,y+h-50-timelineY));
    }
    private void initEditor(){
        int x=layout.contentX(),y=layout.contentY(),w=layout.contentWidth(),h=layout.contentHeight();
        if(editingProject==null && selectedReplay>=0)loadProject();
        int top=editorTop(y,h);
        int hero=editorPreviewHeight(h);
        int trimY=top+hero+99;
        if(editingProject!=null){
            int timelineY=top+hero+8;
            int tH=editorTimelineHeight(y,h,timelineY);
            int areaW=w-30,segments=Math.max(4,Math.min(40,areaW/15));
            for(int i=0;i<segments;i++){
                final int segment=i, count=segments;
                int xx=x+15+segment*areaW/count;
                int ww=Math.max(2,x+15+(segment+1)*areaW/count-xx);
                add(xx,timelineY+(tH<65?25:42),ww,tH<65?15:21,()->"",Skin.TIMELINE,false,"",()->{
                    var pos=EditorTimeline.at(editingProject.clips(),areaW,
                        (segment*areaW/count)+(areaW/(count*2)));
                    selectedClip=pos.clipIndex();playheadSourceTick=pos.sourceTick();editIdleTicks=0;rebuildWidgets();
                });
            }
        }
        if(editingProject!=null && h<260 && w>=240){
            int quickY=top+Math.max(4,(hero-20)/2),quickW=Math.min(86,(w-145)/2);
            add(x+w-quickW*2-13,quickY,quickW,20,()->"Split",Skin.SECONDARY,false,"",()->editClip(4));
            add(x+w-quickW-7,quickY,quickW,20,()->"Save Cuts",Skin.PRIMARY,false,"",()->editClip(7));
        }
        if(editingProject!=null && trimY+58<y+h-38 && w>=285){
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
            if(!ReFlashReplayBridge.openReplay(minecraft,editingProject.replayPath()))
                notice("Cannot load replay preview; check this recording");
        });
        if(w>=250)add(x+18+bw,actionsY,Math.min(bw,w-bw-27),28,()->"Export / "+ReFlashClient.preferences().outputFps()+" FPS",Skin.SECONDARY,false,"export",()->selectPage(Page.EXPORT));
    }

    private static final int[][] SIZES = {{1280,720},{1920,1080},{2560,1440},{3840,2160}};
    private static final String[] SIZE_LABELS = {"720p", "1080p", "1440p", "4K"};
    private static final int[] RATES = {30,60,120,144,240};
    private void selectResolution(int width, int height) {
        var p=ReFlashClient.preferences();
        p.setOutputPreset(width,height,p.outputFps());presetPulseTicks=0;
        ReFlashClient.savePreferences();rebuildWidgets();
    }
    private void selectFrameRate(int fps) {
        var p=ReFlashClient.preferences();
        p.setOutputPreset(p.outputWidth(),p.outputHeight(),fps);presetPulseTicks=0;
        ReFlashClient.savePreferences();rebuildWidgets();
    }
    private void selectQuality(UpscaleProfile profile) {
        ReFlashClient.preferences().setUpscaleProfile(profile);presetPulseTicks=0;
        ReFlashClient.savePreferences();rebuildWidgets();
    }
    private void selectExportView(ExportView view) {
        exportView=view;pageRevealTicks=0;rebuildWidgets();
    }
    private void selectCompactGroup(CompactPresetGroup group) {
        compactPresetGroup=group;rebuildWidgets();
    }
    private int exportTabsY(int y,int h){return y+(h<205?21:42);}
    private int exportChooserTop(int y,int h){return y+(h<245?96:h<340?84:99);}
    private void addPresetRow(int x,int y,int w,int h,int kind) {
        int count=kind==0?SIZES.length:kind==1?RATES.length:UpscaleProfile.values().length;
        int gap=w>=400?8:4;
        int bw=Math.max(24,(w-gap*(count-1))/count);
        var prefs=ReFlashClient.preferences();
        for(int i=0;i<count;i++){
            int index=i, bx=x+i*(bw+gap), buttonWidth=i==count-1?Math.max(23,x+w-bx):bw;
            if(kind==0){
                int[] dim=SIZES[index];
                add(bx,y,buttonWidth,h,()->SIZE_LABELS[index],Skin.SELECTOR,
                    prefs.outputWidth()==dim[0] && prefs.outputHeight()==dim[1],"",()->selectResolution(dim[0],dim[1]));
            }else if(kind==1){
                int fps=RATES[index];
                add(bx,y,buttonWidth,h,()->fps+" FPS",Skin.SELECTOR,prefs.outputFps()==fps,"",()->selectFrameRate(fps));
            }else{
                UpscaleProfile profile=UpscaleProfile.values()[index];
                String name=switch(profile){case FAST->"FAST";case BALANCED->"BALANCED";case SHARP->"SHARP";case ARCHIVE->"ARCHIVE";};
                add(bx,y,buttonWidth,h,()->name,Skin.SELECTOR,prefs.upscaleProfile()==profile,"",()->selectQuality(profile));
            }
        }
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
        if(ReFlashReplayBridge.isRecording()){notice("Stop recording before rendering");return;}
        if(minecraft.level!=null&&!ReFlashReplayBridge.isInReplay()){
            notice("Open the replay first, or return to title screen to render");return;
        }
        savePendingEdits();
        boolean started=NativeReplayExporter.render(minecraft,this,FabricLoader.getInstance().getGameDir(),
                editingProject,ReFlashClient.preferences().outputFps());
        notice(NativeReplayExporter.status());
        if(started)ffmpegReady=true;
        else if(minecraft.screen != this && minecraft.screen != null) {
            NativeReplayExporter.routeExportScreen(minecraft);
        }
    }
    private ExportSizeEstimate.Result selectedSizeEstimate(){
        int ticks=editingProject==null?selectedInfo!=null&&selectedInfo.complete()?selectedInfo.editedTicks():0:editingProject.lengthTicks();
        var p=ReFlashClient.preferences();
        return ExportSizeEstimate.calculate(ticks,p.outputWidth(),p.outputHeight(),p.outputFps(),p.upscaleProfile());
    }
    private void initExport(){
        int x=layout.contentX(),y=layout.contentY(),w=layout.contentWidth(),h=layout.contentHeight();
        int tabsY=exportTabsY(y,h), tabsW=Math.max(45,(w-12)/2);
        add(x+2,tabsY,tabsW,24,()->"01  QUALITY & FPS",Skin.SELECTOR,exportView==ExportView.PRESETS,"",()->selectExportView(ExportView.PRESETS));
        add(x+10+tabsW,tabsY,Math.max(28,w-tabsW-12),24,()->"02  EXPORTED VIDEOS",Skin.SELECTOR,exportView==ExportView.VIDEOS,"",()->selectExportView(ExportView.VIDEOS));
        if(exportView==ExportView.PRESETS){
            // The most important setting, FPS, must be visible first on compact screens.
            if(h<245 && compactPresetGroup==null)compactPresetGroup=CompactPresetGroup.FRAME_RATE;
            int sy=exportChooserTop(y,h),buttonHeight=h>=340?28:Math.max(18,h>=245?24:21);
            if(h<245){
                int groupY=tabsY+30,categoryWidth=Math.max(30,(w-16)/3);
                for(var group:CompactPresetGroup.values()){
                    int i=group.ordinal();
                    add(x+4+i*(categoryWidth+2),groupY,categoryWidth,19,()->switch(group){
                        case RESOLUTION->"SIZE";case FRAME_RATE->"FPS";case QUALITY->"QUALITY";},Skin.SELECTOR,group==compactPresetGroup,"",()->selectCompactGroup(group));
                }
                addPresetRow(x+7,sy,Math.max(30,w-14),buttonHeight,compactPresetGroup.ordinal());
            }else{
                int step=h>=340?61:48;
                for(int kind=0;kind<3;kind++)addPresetRow(x+8,sy+kind*step,w-16,buttonHeight,kind);
            }
            int actionY=y+h-36;
            int firstWidth=Math.max(58,(w-24)/2);
            add(x+6,actionY,firstWidth,28,
                ()->NativeReplayExporter.isRendering()?"Cancel Render":"Render Replay",Skin.PRIMARY,false,"play",this::renderSelectedReplay);
            add(x+12+firstWidth,actionY,Math.max(40,w-firstWidth-18),28,
                ()->"View Videos",Skin.SECONDARY,false,"folder",()->{refreshVideos();selectExportView(ExportView.VIDEOS);});
        }else{
            int startY=tabsY+56,actionY=y+h-36;
            int maxRows=Math.max(0,Math.min(7,(actionY-startY-12)/30));
            for(int i=0;i<maxRows;i++){
                int index=i+videoScroll;
                if(index>=exportedVideos.size())break;
                final int chosen=index;
                add(x+8,startY+i*30,w-16,25,()->exportedVideos.get(chosen).fileName(),
                    Skin.ROW,index==selectedVideo,"play",()->{selectedVideo=chosen;rebuildWidgets();});
            }
            if(maxRows>0 && exportedVideos.size()>maxRows){
                add(x+w-100,actionY-29,92,19,()->"Next videos",Skin.QUIET,false,"",()->{
                    videoScroll=(videoScroll+maxRows)%exportedVideos.size();rebuildWidgets();});
            }
            int bw=w>=480?Math.max(65,(w-32)/3):Math.max(65,(w-24)/2);
            add(x+5,actionY,bw,28,()->VideoPostProcessor.running()?"Cancel Upscale":"Upscale Selected",Skin.PRIMARY,false,"spark",()->{
                if(VideoPostProcessor.running()){VideoPostProcessor.cancel();return;}
                if(minecraft.level!=null&&!ReFlashReplayBridge.isInReplay()){
                    notice("Leave the live world before CPU-heavy video processing");return;
                }
                if(selectedVideo<0||selectedVideo>=exportedVideos.size()){notice("Select an exported video first");return;}
                if(!VideoPostProcessor.startSelected(FabricLoader.getInstance().getGameDir(),
                    exportedVideos.get(selectedVideo).path(),ReFlashClient.preferences()))notice(VideoPostProcessor.status());
                else notice("Offline video enhancement started");
            });
            if(w>=480){
                add(x+11+bw,actionY,bw,28,()->"Queue First 6",Skin.SECONDARY,false,"export",()->{
                    if(VideoPostProcessor.running()){notice("A video job is already running");return;}
                    if(minecraft.level!=null&&!ReFlashReplayBridge.isInReplay()){
                        notice("Leave the live world to preserve gameplay FPS");return;
                    }
                    var paths=exportedVideos.stream().limit(6).map(VideoExportLibrary.Media::path).toList();
                    if(!VideoPostProcessor.startBatch(FabricLoader.getInstance().getGameDir(),paths,ReFlashClient.preferences()))
                        notice(VideoPostProcessor.status());
                    else notice("Queued "+paths.size()+" videos");
                });
                add(x+17+bw*2,actionY,Math.max(35,w-bw*2-22),28,()->"Open Folder",Skin.SECONDARY,false,"folder",this::openExportFolder);
            }else{
                add(x+11+bw,actionY,Math.max(35,w-bw-17),28,()->"Open Folder",Skin.SECONDARY,false,"folder",this::openExportFolder);
            }
        }
    }
    private void drawExport(GuiGraphics g){
        int x=layout.contentX(),y=layout.contentY(),w=layout.contentWidth(),h=layout.contentHeight();
        heading(g,"ReFlash Export Studio","30 / 60 / 120 / 144 / 240 FPS  •  choose your export quality",x,y);
        int tabsY=exportTabsY(y,h);
        if(exportView==ExportView.PRESETS){
            int sy=exportChooserTop(y,h);
            StudioTheme.softPanel(g,x,sy-(h<245?7:23),w,Math.max(28, h<245?37:h>=340?207:159));
            StudioTheme.gradient(g,x+6,sy-(h<245?6:22),Math.max(10,w-12),2,StudioTheme.PURPLE,StudioTheme.CYAN,40);
            if(h>=245){
                int step=h>=340?61:48;
                String[] names={"RESOLUTION", "FRAME RATE", "UPSCALE QUALITY"};
                for(int i=0;i<3;i++){
                    int yy=sy+i*step-16;
                    StudioTheme.label(g,font,names[i],x+11,yy,i==1?StudioTheme.CYAN:StudioTheme.PURPLE);
                }
            }
            ExportSizeEstimate.Result estimate=selectedSizeEstimate();
            int estimateY= h>=340?sy+190:h>=260?sy+146:sy+36;
            if(estimateY+48<y+h-44){
                StudioTheme.softPanel(g,x,estimateY,w,44);
                StudioTheme.icon(g,"export",x+13,estimateY+14,StudioTheme.PURPLE);
                StudioTheme.label(g,font,"ESTIMATED FILE SIZE",x+35,estimateY+7,StudioTheme.TEXT);
                StudioTheme.label(g,font,fit(estimate.available()?estimate.summary():"Choose a replay in Library to estimate size",w-55),
                    x+35,estimateY+25,estimate.available()?StudioTheme.GREEN:StudioTheme.MUTED);
            }else if(h>=230){
                StudioTheme.label(g,font,fit(estimate.available()?"Size estimate: "+estimate.summary():"Select a recording for size estimate",w-12),
                    x+10,Math.min(y+h-59,estimateY+4),StudioTheme.MUTED);
            }
            if(h>=410 && estimateY+111<y+h-80){
                var p=ReFlashClient.preferences();
                StudioTheme.softPanel(g,x,estimateY+52,w,55);
                StudioTheme.icon(g,"spark",x+14,estimateY+69,StudioTheme.CYAN);
                StudioTheme.label(g,font,"SELECTED QUALITY",x+38,estimateY+62,StudioTheme.CYAN);
                StudioTheme.label(g,font,fit(p.upscaleProfile().explanation(),w-50),x+38,estimateY+81,StudioTheme.MUTED);
            }
            if(h>=370){
                StudioTheme.label(g,font,fit("Approximate H.264 size • varies with movement, detail and bitrate",w-15),x+10,y+h-78,StudioTheme.SOFT);
                StudioTheme.label(g,font,fit("Vulkan renders at window size • upscaling does not recreate missing detail",w-15),x+10,y+h-61,StudioTheme.AMBER);
            }
        }else{
            int startY=tabsY+56;
            StudioTheme.softPanel(g,x,startY-5,w,Math.max(40,y+h-81-startY));
            StudioTheme.label(g,font,fit(exportedVideos.size()+" SAVED VIDEOS  •  Select a file to upscale",w-18),x+9,startY-17,StudioTheme.CYAN);
            if(exportedVideos.isEmpty()){
                StudioTheme.icon(g,"folder",x+18,startY+19,StudioTheme.PURPLE);
                StudioTheme.label(g,font,"No exported videos yet",x+41,startY+23,StudioTheme.TEXT);
                if(h>=280)StudioTheme.label(g,font,"Render a replay first, then choose its MP4 here.",x+18,startY+54,StudioTheme.MUTED);
            }
            if(h>=270){
                String path=VideoExportLibrary.folder(FabricLoader.getInstance().getGameDir()).toAbsolutePath().toString();
                StudioTheme.label(g,font,fit("FOLDER: "+path,w-16),x+8,y+h-71,StudioTheme.GREEN);
            }
            if(VideoPostProcessor.running()){
                StudioTheme.label(g,font,fit(VideoPostProcessor.status(),w-14),x+8,y+h-54,StudioTheme.SOFT);
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
                add(xx,start+3,bw,24,()->ReFlashReplayBridge.micEnabled()?"Mic ON (next take)":"Mic OFF (next take)",Skin.TOGGLE,
                    ReFlashReplayBridge.micEnabled(),"",()->{
                        if(!ReFlashReplayBridge.setMicEnabled(!ReFlashReplayBridge.micEnabled()))notice("Microphone setting unavailable");rebuildWidgets();
                    });
                add(xx,start+rh+3,bw,24,()->"Device: "+ReFlashReplayBridge.micDevice(),Skin.SECONDARY,false,"record",()->{
                    if(!ReFlashReplayBridge.cycleMicDevice())notice("No compatible input devices detected");rebuildWidgets();
                });
                add(xx,start+rh*2+3,bw,24,()->MicrophoneProbe.running()?"Testing...":"Test Mic (0.6s)",Skin.PRIMARY,false,"record",()->{
                    if(!MicrophoneProbe.start(ReFlashReplayBridge.micDevice()))notice("Microphone test already running");
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
                add(xx,start+rh*4+3,bw,24,()->"Record key: "+ReFlashClient.engineRecordKey(),Skin.SECONDARY,false,"settings",()->{
                    openKeybinds();
                });
                add(xx,start+rh*5+3,bw,24,()->"Save Diagnostics",Skin.SECONDARY,false,"settings",()->{
                    Path path=DiagnosticsReport.create(FabricLoader.getInstance().getGameDir(),ReFlashClient.preferences(),
                        ReFlashReplayBridge.backendPresent(),ReFlashClient.engineRecordKey(),
                        ReFlashClient.performanceSampler().result(),FfmpegCheck.available(FabricLoader.getInstance().getGameDir()));
                    notice(path==null?"Could not save diagnostics":"Saved reflash/diagnostics.txt (no personal details)");
                });
            }
        }
    }
    private void inspectSelectedReplay(){
        selectedInfo=selectedReplay>=0&&selectedReplay<replayList.size()?ReplayDuration.inspect(replayList.get(selectedReplay).path()):null;
    }
    private void refreshLibrary(){
        try{
            Path selectedPath=selectedReplay>=0&&selectedReplay<replayList.size()?replayList.get(selectedReplay).path():null;
            replayList=ReplayIndex.recent(replayFolder(),128);libraryError="";
            if(requestedReplayPath!=null){
                for(int i=0;i<replayList.size();i++)if(replayList.get(i).path().equals(requestedReplayPath)){
                    selectedReplay=i; break;
                }
                requestedReplayPath=null;
            }
            if(selectedPath!=null){
                int keep=-1;
                for(int i=0;i<replayList.size();i++)if(replayList.get(i).path().equals(selectedPath)){keep=i;break;}
                selectedReplay=keep>=0?keep:(replayList.isEmpty()?-1:0);
            }else if(selectedReplay>=replayList.size()||selectedReplay<0){
                selectedReplay=replayList.isEmpty()?-1:0;
            }
            if(replayOffset>=replayList.size())replayOffset=0;
            inspectSelectedReplay();
        }catch(IOException e){replayList=List.of();libraryError="Could not read replay files";selectedReplay=-1;selectedInfo=null;}
    }
    private void restoreFullRecording(){
        if(selectedReplay<0||selectedReplay>=replayList.size()){notice("Select a recording");return;}
        try {
            var project=ReplayProject.open(replayList.get(selectedReplay).path());
            project.reset();project.save();
            editingProject=null;inspectSelectedReplay();
            notice("Restored original recording duration: "+selectedInfo.sourceLabel());rebuildWidgets();
        }catch(IOException|RuntimeException ex){notice("Cannot restore cuts: "+ex.getMessage());}
    }
    private void openSelectedReplay(){
        if(selectedReplay<0||selectedReplay>=replayList.size()){notice("Select a saved recording first");return;}
        if(ReFlashReplayBridge.isRecording()){notice("Stop recording before playback");return;}
        if(!ReFlashReplayBridge.openReplay(minecraft,replayList.get(selectedReplay).path()))notice("Could not load replay; inspect this recording in ReFlash Library");
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
        screenTicks++;pageRevealTicks=Math.min(16,pageRevealTicks+1);presetPulseTicks=Math.min(18,presetPulseTicks+1);if(noticeTicks>0)noticeTicks--;
        // A completed replay is written asynchronously. A Studio opened before it
        // saved must refresh rather than keep showing 0 replays indefinitely.
        // At 1 check / 2 seconds this never touches the gameplay render path.
        var saveState=ReFlashClient.saveState();
        if(saveState!=lastSaveUiState){
            lastSaveUiState=saveState;
            if(page==Page.CAPTURE)rebuildWidgets();
        }
        if((page==Page.CAPTURE || page==Page.LIBRARY) && screenTicks%40==0 &&
                !ReFlashReplayBridge.isRecording()){
            List<String> previous=replayList.stream().map(ReplayIndex.Replay::fileName).toList();
            refreshLibrary();
            if(!previous.equals(replayList.stream().map(ReplayIndex.Replay::fileName).toList()))rebuildWidgets();
        }
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
        // Subtle top accent follows page navigation; fades out instead of covering content.
        if(pageRevealTicks<13){
            int px=layout.contentX(), py=layout.contentY()-5, ww=layout.contentWidth();
            int tw=Math.max(2,ww*(pageRevealTicks+1)/13);
            StudioTheme.gradient(g,px,py,tw,2,StudioTheme.PURPLE,StudioTheme.CYAN,24);
        }
        super.render(g,mouseX,mouseY,partial);
        for(Control c:controls)drawControl(g,c);
        // Gentle 10-tick content fade when navigating Studio pages. 100% GUI-only.
        if(pageRevealTicks<5){
            float t=(5f-pageRevealTicks)/5f;
            int alpha=Math.round(43f*t*t);
            StudioTheme.rect(g,layout.contentX(),layout.contentY(),layout.contentWidth(),layout.contentHeight(),
                (alpha<<24)|0x00060C18);
        }
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
            String status=ReFlashReplayBridge.isRecording()?"RECORDING":
                    ReFlashClient.saveState()==dev.reflash.core.RecordingSaveMonitor.State.SAVING?"SAVING REPLAY":
                    ReFlashReplayBridge.isInReplay()?"REPLAY LOADED":
                    minecraft==null||minecraft.level==null?"JOIN A WORLD":"CAPTURE READY";
            StudioTheme.chip(g,font,status,x+w-172,y+19,119,ReFlashReplayBridge.isRecording()?StudioTheme.RED:StudioTheme.GREEN);
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
        StudioTheme.label(g,font,"REFLASH 0.11.2 / FABRIC 1.21.11",x+w-162,y+h-16,StudioTheme.SOFT);
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
        boolean canCapture=minecraft!=null && minecraft.level!=null && minecraft.player!=null;
        String captureState=ReFlashReplayBridge.isRecording()?"RECORDING IN PROGRESS":
                canCapture?"LIGHTWEIGHT REPLAY CAPTURE":"JOIN A WORLD TO START RECORDING";
        StudioTheme.label(g,font,fit(captureState,w/2-10),x+18,heroY+17,
                ReFlashReplayBridge.isRecording()?StudioTheme.RED:canCapture?StudioTheme.CYAN:StudioTheme.AMBER);
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
        int top=y+(layout.ultraCompact()?23:74),rh=libRowH();
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
            if(selectedInfo!=null){
                String len=selectedInfo.complete()?"SOURCE  "+selectedInfo.sourceLabel()+"   •   CUTS  "+selectedInfo.editedLabel():selectedInfo.issue();
                StudioTheme.label(g,font,fit(len,rw-24),rx+14,top+73,
                    selectedInfo.shortened()?StudioTheme.AMBER:StudioTheme.GREEN);
            }
            if(h>210) {
                StudioTheme.label(g,font,fit("Source length is independent of cuts.",rw-25),rx+14,top+95,StudioTheme.SOFT);
                StudioTheme.label(g,font,fit("Use Edit Selected for the cut timeline.",rw-25),rx+14,top+119,StudioTheme.MUTED);
                StudioTheme.label(g,font,fit("The world loads before editor opens.",rw-25),rx+14,top+143,StudioTheme.MUTED);
            }
        }
    }
    private void drawEditor(GuiGraphics g){
        int x=layout.contentX(),y=layout.contentY(),w=layout.contentWidth(),h=layout.contentHeight();
        heading(g,"ReFlash Editor","Non-destructive editing  /  select a recording, cut, preview, export",x,y);
        int top=editorTop(y,h);
        int hero=editorPreviewHeight(h);
        // Editing workspace: primary playback viewport with a compact clip inspector.
        boolean inspector=w>=520 && hero>=102;
        int gap=10,previewW=inspector?Math.max(245,(w-gap)*67/100):w;
        int inspectorX=x+previewW+gap,inspectorW=w-previewW-gap;
        StudioTheme.softPanel(g,x,top,previewW,hero);
        StudioTheme.rounded(g,x+3,top+3,previewW-6,hero-6,0xFF080D19);
        StudioTheme.gradient(g,x+3,top+3,previewW-6,2,StudioTheme.PURPLE,StudioTheme.CYAN,30);
        StudioTheme.label(g,font,hero<64?"PREVIEW  /  OPEN WORLD":"PREVIEW  /  REPLAY WORLD",x+15,top+(hero<64?10:13),StudioTheme.CYAN);
        if(hero>=96){
            int pcx=x+previewW/2,pcy=top+hero/2-3;
            StudioTheme.rounded(g,pcx-26,pcy-25,52,50,0xFF1E2B45);
            StudioTheme.border(g,pcx-26,pcy-25,52,50,StudioTheme.BORDER);
            StudioTheme.icon(g,"play",pcx-8,pcy-8,StudioTheme.CYAN);
            StudioTheme.centered(g,font,editingProject==null?"Select a replay to begin":"OPEN REPLAY PREVIEW",pcx,pcy+34,StudioTheme.TEXT);
        }else if(hero>=64){
            StudioTheme.centered(g,font,editingProject==null?"No replay selected":"Preview opens in replay world",x+previewW/2,top+hero/2+3,StudioTheme.MUTED);
        }
        if(hero>=150){
            String source=editingProject==null?"No media loaded":replayList.get(selectedReplay).fileName();
            StudioTheme.label(g,font,fit(source,previewW-34),x+15,top+hero-35,StudioTheme.TEXT);
            StudioTheme.label(g,font,"Actual replay view opens with the Preview Replay button",x+15,top+hero-18,StudioTheme.SOFT);
        }
        if(inspector){
            StudioTheme.softPanel(g,inspectorX,top,inspectorW,hero);
            StudioTheme.label(g,font,"CLIP INSPECTOR",inspectorX+13,top+15,StudioTheme.PURPLE);
            StudioTheme.rect(g,inspectorX+13,top+34,inspectorW-26,1,StudioTheme.BORDER);
            if(editingProject==null){
                StudioTheme.label(g,font,"Nothing selected",inspectorX+13,top+53,StudioTheme.TEXT);
                StudioTheme.label(g,font,"Open a replay from Library",inspectorX+13,top+73,StudioTheme.MUTED);
            }else{
                String[] stats={
                    "SOURCE   "+String.format(Locale.ROOT,"%.2f s",editingProject.sourceTicks()/20.0),
                    "EDITED   "+String.format(Locale.ROOT,"%.2f s",editingProject.lengthTicks()/20.0),
                    "CLIPS    "+editingProject.clips().size(),
                    "ACTIVE   "+(selectedClip+1)+" / "+editingProject.clips().size(),
                    "PLAYHEAD "+String.format(Locale.ROOT,"%.2f s",playheadSourceTick/20.0),
                    editingProject.hasMicrophoneTrack()?"MIC TRACK  PRESENT":"MIC TRACK  NONE"
                };
                for(int i=0;i<stats.length;i++){
                    int yy=top+48+i*23;
                    if(yy+10<top+hero-5)StudioTheme.label(g,font,fit(stats[i],inspectorW-24),inspectorX+13,yy,i==3?StudioTheme.CYAN:StudioTheme.TEXT);
                }
            }
        }
        // NLE-like timeline: ruler, labeled track, clip regions, and movable playhead.
        int timelineY=top+hero+8;
        int timelineH=editorTimelineHeight(y,h,timelineY);
        if(timelineH>27){
            StudioTheme.softPanel(g,x,timelineY,w,timelineH);
            StudioTheme.label(g,font,"TIMELINE  /  CUTS",x+14,timelineY+8,StudioTheme.PURPLE);
            if(editingProject==null){
                if(timelineH>50)StudioTheme.label(g,font,"Choose a recording in Library to create an editable timeline",x+14,timelineY+30,StudioTheme.MUTED);
            }else{
                int areaX=x+15,areaW=w-30;
                int total=Math.max(1,editingProject.lengthTicks());
                int rulerY=timelineY+(timelineH<65?19:30);
                StudioTheme.rect(g,areaX,rulerY,areaW,1,StudioTheme.BORDER);
                for(int t=0;t<=4;t++){
                    int px=areaX+areaW*t/4;
                    StudioTheme.rect(g,px,rulerY,1,6,StudioTheme.SOFT);
                    if(w>=300 && timelineH>=65)StudioTheme.label(g,font,String.format(Locale.ROOT,"%.1fs",total*t/80.0),
                            Math.min(x+w-42,px+2),rulerY-12,StudioTheme.MUTED);
                }
                int barY=timelineY+(timelineH<65?25:42),clipAreaW=areaW,cursor=0;
                int barH=timelineH<65?Math.max(10,Math.min(16,timelineH-28)):22;
                StudioTheme.rect(g,areaX,barY,clipAreaW,barH,0xFF0E1B32);
                for(int i=0;i<editingProject.clips().size();i++){
                    var c=editingProject.clips().get(i);
                    int ww=Math.max(2,(int)((long)c.length()*clipAreaW/total));
                    int visible=Math.min(ww,clipAreaW-cursor);
                    if(visible<=0)break;
                    int shade=i==selectedClip?0xFF7446D3:0xFF235A83;
                    StudioTheme.rounded(g,areaX+cursor,barY,visible,barH,shade);
                    StudioTheme.border(g,areaX+cursor,barY,visible,barH,i==selectedClip?StudioTheme.PURPLE:StudioTheme.CYAN);
                    if(visible>42 && barH>=17)StudioTheme.label(g,font,"CLIP "+(i+1),areaX+cursor+7,barY+5,StudioTheme.TEXT);
                    cursor+=ww;
                }
                int head=EditorTimeline.pixelAt(editingProject.clips(),clipAreaW,selectedClip,playheadSourceTick);
                StudioTheme.rect(g,areaX+head,barY-(timelineH<65?4:11),2,barH+(timelineH<65?7:17),StudioTheme.AMBER);
                if(timelineH>=65)StudioTheme.diamond(g,areaX+head+1,barY-10,4,StudioTheme.AMBER);
                if(timelineH>=86){
                    String info="CLIP "+(selectedClip+1)+" / "+editingProject.clips().size()+
                        "    POSITION "+String.format(Locale.ROOT,"%.2fs",playheadSourceTick/20.0)+
                        (editingProject.dirty()?"    * UNSAVED":"    SAVED");
                    StudioTheme.label(g,font,fit(info,w-28),x+15,timelineY+74,StudioTheme.TEXT);
                }
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
                "Toggle test with F12","Recording uses the ReFlash keybind only (F8 by default)"};
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
        int x=b.getX(),y=b.getY(),w=b.getWidth(),h=b.getHeight();boolean hot=b.active&&b.isHoveredOrFocused();
        float eased=hoverEase.getOrDefault(b,0f);
        eased=Math.clamp(eased+(hot?0.22f:-0.22f),0f,1f);
        hoverEase.put(b,eased);
        int fill,textColor,stroke=StudioTheme.BORDER;
        switch(c.skin()){
            case PRIMARY->{fill=hot?0xFFCE9DFF:StudioTheme.PURPLE_DARK;textColor=StudioTheme.TEXT;stroke=StudioTheme.PURPLE;}
            case DANGER->{fill=hot?0xFFC84875:0xFF792642;textColor=StudioTheme.TEXT;stroke=StudioTheme.RED;}
            case NAV->{fill=c.selected()?0xFF282044:hot?0xFF20243A:StudioTheme.SIDEBAR;textColor=c.selected()?StudioTheme.TEXT:StudioTheme.MUTED;stroke=c.selected()?StudioTheme.PURPLE:fill;}
            case SELECTOR->{fill=c.selected()?0xFF4A286E:hot?0xFF2A3551:0xFF1B263C;textColor=c.selected()?StudioTheme.TEXT:StudioTheme.MUTED;stroke=c.selected()?StudioTheme.PURPLE:StudioTheme.BORDER;}
            case TOGGLE->{fill=c.selected()?0xFF144A51:0xFF2A3146;textColor=c.selected()?StudioTheme.CYAN:StudioTheme.MUTED;stroke=c.selected()?StudioTheme.CYAN:StudioTheme.BORDER;}
            case ROW->{fill=c.selected()?0xFF202F49:hot?0xFF1D2A43:StudioTheme.PANEL;textColor=StudioTheme.TEXT;stroke=c.selected()?StudioTheme.CYAN:StudioTheme.BORDER;}
            case SECONDARY->{fill=hot?StudioTheme.PANEL_HOVER:StudioTheme.PANEL_ALT;textColor=StudioTheme.TEXT;stroke=hot?StudioTheme.CYAN:StudioTheme.BORDER;}
            default->{fill=hot?StudioTheme.PANEL_HOVER:StudioTheme.PANEL;textColor=StudioTheme.MUTED;stroke=hot?StudioTheme.CYAN:fill;}
        }
        if(!b.active){
            fill=StudioTheme.PANEL;textColor=StudioTheme.SOFT;stroke=StudioTheme.BORDER;
        }
        if(c.skin()!=Skin.NAV && c.skin()!=Skin.TIMELINE && c.skin()!=Skin.DANGER && b.active) {
            fill=StudioTheme.mix(fill,StudioTheme.PANEL_HOVER,eased*0.34f);
        }
        StudioTheme.rounded(g,x,y,w,h,fill);
        if(eased>0f && c.skin()!=Skin.ROW && c.skin()!=Skin.NAV)
            StudioTheme.rect(g,x+6,y+h-2,Math.max(1,Math.round((w-12)*eased)),1,StudioTheme.CYAN);
        if(stroke!=fill)StudioTheme.border(g,x,y,w,h,stroke);
        if(c.selected()&&c.skin()==Skin.NAV)StudioTheme.rect(g,x+1,y+6,3,h-12,StudioTheme.PURPLE);
        if(c.selected()&&c.skin()==Skin.SELECTOR){
            float progress=Math.min(1f,(presetPulseTicks+1)/7f);
            int glow=Math.max(2,Math.round((w-12)*progress));
            StudioTheme.gradient(g,x+6,y+h-3,glow,2,StudioTheme.PURPLE,StudioTheme.CYAN,20);
        }
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

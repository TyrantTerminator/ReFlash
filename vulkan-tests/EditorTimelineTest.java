import dev.reflash.core.ReplayProject;
import dev.reflash.core.EditorTimeline;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
public class EditorTimelineTest {
    public static void main(String[] args)throws Exception{
        var tmp=Files.createTempDirectory("reflash-timeline-");var replay=tmp.resolve("source.momenton");
        try(var z=new ZipOutputStream(Files.newOutputStream(replay))){z.putNextEntry(new ZipEntry("metadata.json"));z.write("{\"durationTicks\":1000}".getBytes(StandardCharsets.UTF_8));z.closeEntry();}
        var p=ReplayProject.open(replay);
        if(!p.splitAt(0,333))throw new AssertionError("splitAt rejected");
        if(p.splitAt(0,0)||p.splitAt(0,333))throw new AssertionError("split endpoints accepted");
        if(p.clips().size()!=2||p.clips().get(0).length()!=333)throw new AssertionError("wrong split");
        if(!p.canUndo()||!p.undo()||p.clips().size()!=1)throw new AssertionError("undo");
        if(!p.redo()||p.clips().size()!=2)throw new AssertionError("redo");
        var a=EditorTimeline.at(p.clips(),1000,500);
        if(a.clipIndex()!=1||a.sourceTick()!=500)throw new AssertionError("timeline seek "+a);
        var b=EditorTimeline.at(p.clips(),1000,1000);
        if(b.sourceTick()!=999)throw new AssertionError("last tick");
        if(EditorTimeline.pixelAt(p.clips(),1000,1,500)!=500)throw new AssertionError("seek inverse");
        p.trimStart(1,470);p.trimEnd(1,700);p.save();
        if(p.dirty())throw new AssertionError("save dirty");
        var q=ReplayProject.open(replay);
        if(q.clips().size()!=2||q.clips().get(1).startTick()!=470||q.clips().get(1).endTick()!=700)throw new AssertionError("saved cuts");
        if(!p.move(1,0)||!p.undo())throw new AssertionError("reorder/undo");
        if(!p.reset()||p.clips().size()!=1)throw new AssertionError("reset");
        System.out.println("PASS interactive timeline mapping + explicit playhead split + undo/redo + cuts/save");
    }
}

import dev.reflash.client.ReplayEditingBridge;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.state.EditorStateManager;

public class BridgeContractTest {
 public static void main(String[] args){
   var a=ReplayEditingBridge.snapshot();
   assertThat(a.active() && a.tick()==60 && a.duration()==1200 && a.sceneCount()==2,"snapshot");
   // A recorded player can be followed by Flashback's third-person tracking camera.
   var targetId = java.util.UUID.randomUUID();
   net.minecraft.client.Minecraft.getInstance().level.testPlayers.add(
       new net.minecraft.world.entity.player.Player(targetId,"Runner"));
   assertThat(ReplayEditingBridge.availableFollowTargets().size()==1,"trackable player list");
   assertThat(ReplayEditingBridge.addThirdPersonFollowKeyframe(targetId),"third-person follow keyframe");
   assertThat(!ReplayEditingBridge.addThirdPersonFollowKeyframe(java.util.UUID.randomUUID()),"unknown target rejected");
   assertThat(ReplayEditingBridge.addCameraKeyframe(),"add keyframe");
   var b=ReplayEditingBridge.snapshot();
   assertThat(b.camera().size()==2 && b.camera().getFirst()==60,"keyframe at tick 60");
   assertThat(ReplayEditingBridge.addFovKeyframe(95f),"add FOV keyframe");
   assertThat(ReplayEditingBridge.addTimeKeyframe(18000),"add time keyframe");
   var extended=ReplayEditingBridge.snapshot();
   assertThat(extended.fov().size()==1 && extended.time().size()==1,"FOV/time on native timeline");
   assertThat(!ReplayEditingBridge.addFovKeyframe(400f) && !ReplayEditingBridge.addTimeKeyframe(-2),"invalid numeric values rejected");
   assertThat(ReplayEditingBridge.seek(7000),"seek");
   assertThat(Flashback.server.tick==1200,"seek clamped to replay duration");
   assertThat(ReplayEditingBridge.setPlaying(true) && !ReplayEditingBridge.snapshot().paused(),"resume");
   assertThat(ReplayEditingBridge.setPlaying(false) && ReplayEditingBridge.snapshot().paused(),"pause");
   assertThat(ReplayEditingBridge.markExportEdge(true),"export in");
   assertThat(ReplayEditingBridge.seek(400),"seek earlier");
   assertThat(ReplayEditingBridge.markExportEdge(false),"export out");
   var c=ReplayEditingBridge.snapshot();
   assertThat(c.exportStart()==1199 && c.exportEnd()==1200,"export points retained / correctly clamped");
   assertThat(ReplayEditingBridge.changeScene(1) && ReplayEditingBridge.snapshot().sceneNumber()==2,"scene change");
   assertThat(ReplayEditingBridge.undo() && ReplayEditingBridge.redo(),"undo redo");
   assertThat(ReplayEditingBridge.setPlaybackRate(0.5f) && Flashback.server.desiredSpeed==10f,
              "real Flashback playback speed API");
   assertThat(!ReplayEditingBridge.setPlaybackRate(5f), "reject invalid playback speed");
   assertThat(ReplayEditingBridge.createScene() && ReplayEditingBridge.snapshot().sceneCount()==3
              && ReplayEditingBridge.snapshot().sceneNumber()==3, "new editable Flashback scene");

   assertThat(EditorStateManager.editor.dirty>=5,"mark dirty on edits");
   Flashback.server=null;
   assertThat(!ReplayEditingBridge.snapshot().active() && !ReplayEditingBridge.seek(10)
     && !ReplayEditingBridge.addCameraKeyframe(),"no replay is safe");
   System.out.println("PASS: offline bridge contract incl. third-person follow camera: snapshot, real-keyframe path, playback, seek, export markers, scenes, new scene, playback rate, undo/redo, missing replay");
 }
 private static void assertThat(boolean condition,String desc){if(!condition)throw new AssertionError(desc);}
}

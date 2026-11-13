package com.moulberry.flashback.keyframe.impl;
import com.moulberry.flashback.keyframe.Keyframe;
import com.moulberry.flashback.combo_options.TrackingBodyPart;
import java.util.UUID;
import org.joml.Vector3d;
public class TrackEntityKeyframe extends Keyframe {
  public UUID target; public TrackingBodyPart body; public Vector3d view;
  public TrackEntityKeyframe(UUID id, TrackingBodyPart body, float yaw, float pitch, Vector3d position, Vector3d view, float roll) {
    this.target=id; this.body=body; this.view=view;
  }
}

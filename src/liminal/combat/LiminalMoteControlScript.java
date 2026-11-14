package liminal.combat;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.MoteControlScript;

public class LiminalMoteControlScript extends MoteControlScript {

  @Override
  public ShipAPI getLockTarget(ShipAPI from, Vector2f loc) {
    findSlots(from);
    if (attractor == null || loc == null) {
      return null;
    }
    return super.getLockTarget(from, loc);
  }

  @Override
  public Vector2f getTargetLoc(ShipAPI from) {
    findSlots(from);
    if (attractor == null) {
      return new Vector2f(from.getLocation());
    }
    return super.getTargetLoc(from);
  }

  @Override
  public Vector2f getTargetedLocation(ShipAPI from) {
    Vector2f loc = from.getSystem().getTargetLoc();
    if (loc != null) {
      return loc;
    }
    if (from.getMouseTarget() != null) {
      return new Vector2f(from.getMouseTarget());
    }
    ShipAPI target = from.getShipTarget();
    if (target != null) {
      return new Vector2f(target.getLocation());
    }
    return new Vector2f(from.getLocation());
  }
}

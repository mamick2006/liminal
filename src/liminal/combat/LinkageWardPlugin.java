package liminal.combat;

import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import liminal.crossmod.StandingLinkage;

public class LinkageWardPlugin extends BaseEveryFrameCombatPlugin {

  private static final String MOD_ID = "liminal_standing_linkage_ward";
  private static final float MIN_INTERVAL = 0.5f;
  private static final float MAX_INTERVAL = 1f;

  private static boolean warnedOnce;

  private final float damageTakenMult;
  private final IntervalUtil interval = new IntervalUtil(MIN_INTERVAL, MAX_INTERVAL);
  private CombatEngineAPI engine;

  public LinkageWardPlugin(float damageTakenMult) {
    this.damageTakenMult = damageTakenMult;
  }

  public static void addIfEarned(MissionDefinitionAPI api, boolean avatarPresent) {
    if (api == null || !avatarPresent) {
      return;
    }
    try {
      if (!StandingLinkage.isOwned()) {
        return;
      }
      float mult = StandingLinkage.wardDamageTakenMult();
      if (mult >= 1f) {
        return;
      }
      api.addPlugin(new LinkageWardPlugin(mult));
    } catch (Throwable t) {
      if (!warnedOnce) {
        warnedOnce = true;
        Global.getLogger(LinkageWardPlugin.class).warn("Liminal: could not install the Standing"
            + " Linkage's ward for this battle, most likely a Captain's Quarters version this one"
            + " does not know. The battle runs without the damage reduction; nothing else is"
            + " affected.", t);
      }
    }
  }

  @Override
  public void init(CombatEngineAPI engine) {
    this.engine = engine;
    interval.forceIntervalElapsed();
  }

  @Override
  public void advance(float amount, List<InputEventAPI> events) {
    if (engine == null || engine.isPaused()) {
      return;
    }
    interval.advance(amount);
    if (!interval.intervalElapsed()) {
      return;
    }
    for (ShipAPI ship : engine.getShips()) {
      if (ship == null || ship.getOwner() != 0 || ship.isHulk()) {
        continue;
      }
      ship.getMutableStats().getHullDamageTakenMult().modifyMult(MOD_ID, damageTakenMult);
      ship.getMutableStats().getArmorDamageTakenMult().modifyMult(MOD_ID, damageTakenMult);
    }
  }
}

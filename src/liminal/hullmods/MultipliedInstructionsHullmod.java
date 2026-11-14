package liminal.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import liminal.LGlobals;
import liminal.instructions.Instruction;
import liminal.instructions.Instruction.Group;
import liminal.instructions.Instructions;

public abstract class MultipliedInstructionsHullmod extends BaseHullMod {

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
    for (Instruction instruction : Instructions.getOwnInstructions()) {
      if (instruction.getGroup().equals(Group.NEGATIVE)) {
        continue;
      }
      float mult = instruction.getMaxLevelMultiplier() * effectMultiplier();
      instruction.applyEffectsBeforeShipCreation(hullSize, stats, id, mult);
    }
  }

  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    for (Instruction instruction : Instructions.getOwnInstructions()) {
      if (instruction.getGroup().equals(Group.NEGATIVE)) {
        continue;
      }
      if (instruction.hasApplyEffectsAfterShipCreation()) {
        float mult = instruction.getMaxLevelMultiplier() * effectMultiplier();
        instruction.applyEffectsAfterShipCreation(ship, id, mult);
      }
    }
  }

  @Override
  public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
    for (Instruction instruction : Instructions.getOwnInstructions()) {
      if (instruction.getGroup().equals(Group.NEGATIVE)) {
        continue;
      }
      if (instruction.hasApplyEffectsToFighterSpawnedByShip()) {
        float mult = instruction.getMaxLevelMultiplier() * effectMultiplier();
        instruction.applyEffectsToFighterSpawnedByShip(fighter, ship, id, mult);
      }
    }
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount) {

    if (Global.getCombatEngine().isPaused()) {
      return;
    }

    if (!ship.isAlive() || ship.isPiece()) {
      return;
    }
    for (Instruction instruction : Instructions.getOwnInstructions()) {
      if (instruction.getGroup().equals(Group.NEGATIVE)) {
        continue;
      }
      if (instruction.hasAdvanceInCombat()) {
        float mult = instruction.getMaxLevelMultiplier() * effectMultiplier();
        instruction.advanceInCombat(ship, amount, mult);
      }
    }
    ship.setJitter(this, LGlobals.jitterColor, 1f, 2, 0, 15f);
    ship.setJitterUnder(this, LGlobals.jitterUnderColor, 1f, 10, 0f, 45f);
  }

  public String getDescriptionParam(int index, HullSize hullSize) {
    if (index == 0)
      return "all";
    if (index == 1)
      return "" + (int) (effectMultiplier() * 100f) + "%";
    return null;
  }

  @Override
  public boolean isApplicableToShip(ShipAPI ship) {
    for (String hullmod : ship.getVariant().getHullMods()) {
      if (hullmod.startsWith("liminal_instructions") && !hullmod.equals(spec.getId())) {
        return false;
      }
      if (hullmod.startsWith("liminal_connection") && !hullmod.equals(spec.getId())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean affectsOPCosts() {
    return true;
  }

  protected abstract float effectMultiplier();
}

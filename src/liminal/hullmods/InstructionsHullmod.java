package liminal.hullmods;

import java.util.ArrayList;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import liminal.LGlobals;
import liminal.LiminalStrings;
import liminal.instructions.Instruction;
import liminal.instructions.InstructionsManager;

public abstract class InstructionsHullmod extends BaseHullMod {

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
    if (Global.LOADING_SAVE) {
      stats.getNumFighterBays().modifyFlat(id, 10);
    }
    InstructionsManager instructionsManager = InstructionsManager.getInstance();
    if (instructionsManager.getLoadout(spec.getId()).isOverCapacity()) {
      return;
    }

    for (Instruction instruction : getInstructions()) {
      int level = instructionsManager.getInstructionLevel(instruction.getName());
      float mult = instruction.getLevelMultiplier(level);
      instruction.applyEffectsBeforeShipCreation(hullSize, stats, id, mult);
    }
  }

  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    InstructionsManager instructionsManager = InstructionsManager.getInstance();
    if (instructionsManager.getLoadout(spec.getId()).isOverCapacity()) {
      return;
    }
    for (Instruction instruction : getInstructions()) {
      if (instruction.hasApplyEffectsAfterShipCreation()) {
        int level = instructionsManager.getInstructionLevel(instruction.getName());
        float mult = instruction.getLevelMultiplier(level);
        instruction.applyEffectsAfterShipCreation(ship, id, mult);
      }
    }
  }

  @Override
  public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
    InstructionsManager instructionsManager = InstructionsManager.getInstance();
    if (instructionsManager.getLoadout(spec.getId()).isOverCapacity()) {
      return;
    }
    for (Instruction instruction : getInstructions()) {
      if (instruction.hasApplyEffectsToFighterSpawnedByShip()) {
        int level = instructionsManager.getInstructionLevel(instruction.getName());
        float mult = instruction.getLevelMultiplier(level);
        instruction.applyEffectsToFighterSpawnedByShip(fighter, ship, id, mult);
      }
    }
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount) {
    InstructionsManager instructionsManager = InstructionsManager.getInstance();
    if (instructionsManager.getLoadout(spec.getId()).isOverCapacity()) {
      return;
    }
    for (Instruction instruction : getInstructions()) {
      if (instruction.hasAdvanceInCombat()) {
        int level = instructionsManager.getInstructionLevel(instruction.getName());
        float mult = instruction.getLevelMultiplier(level);
        instruction.advanceInCombat(ship, amount, mult);
      }
    }
  }

  @Override
  public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width,
      boolean isForModSpec) {
    final InstructionsManager instructionsManager = InstructionsManager.getInstance();
    ArrayList<Instruction> instructions = getInstructions();
    int used = instructionsManager.countUsedCapacity(spec.getId());
    if (ship != null && ship.getVariant() != null && ship.getVariant().hasHullMod(spec.getId())) {
      tooltip.addPara(LiminalStrings.mgmt("hullmod_capacity_line"), 5f, Misc.getHighlightColor(),
          instructionsManager.getHullmodName(spec.getId()), "" + used,
          "" + instructionsManager.getCapacity(spec.getId()),
          "" + InstructionsManager.sizeWeight(ship.getHullSize()));
    }
    if (used > instructionsManager.getCapacity(spec.getId())) {
      instructionsManager.addOverCapacityLine(tooltip, spec.getId(), used, 5f);
    }
    tooltip.addPara("", -10f);
    tooltip.addImage(Global.getSettings().getSpriteName("icons", "liminal_line"), 100f, 0f);
    tooltip.addPara("", -15f);
    tooltip.setParaFontColor(LGlobals.liminalityTextColor);
    if (instructions.size() == 0) {
      tooltip.addPara(LiminalStrings.mgmt("hullmod_no_instructions"), 5f, LGlobals.liminalityColor,
          "Instructions",
          liminal.intel.LiminalMessageMath.detailsPlace(liminal.topbar.LiminalMgmtInjector.isAlive()));
    }
    for (Instruction instruction : instructions) {
      if (hullSize == null) {
        instruction.getEffectDescription(tooltip, 5f,
            instructionsManager.getInstructionLevel(instruction.getName()));
      } else {
        instruction.getEffectDescription(tooltip, 5f, hullSize,
            instructionsManager.getInstructionLevel(instruction.getName()));
      }
    }
    tooltip.setParaFontColor(Misc.getTextColor());
  }

  @Override
  public boolean isApplicableToShip(ShipAPI ship) {
    if (carriesAnotherTemplate(ship)) {
      return false;
    }
    if (ship.getVariant().hasHullMod(spec.getId())) {
      return true;
    }
    return InstructionsManager.getInstance().fitsCapacity(spec.getId(), ship);
  }

  private boolean carriesAnotherTemplate(ShipAPI ship) {
    for (String hullmod : ship.getVariant().getHullMods()) {
      if (hullmod.startsWith("liminal_instructions") && !hullmod.equals(spec.getId())) {
        return true;
      }
      if (hullmod.startsWith("liminal_connection") && !hullmod.equals(spec.getId())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String getUnapplicableReason(ShipAPI ship) {
    if (ship == null || ship.getVariant() == null || ship.getVariant().hasHullMod(spec.getId())
        || carriesAnotherTemplate(ship)) {
      return null;
    }
    InstructionsManager instructionsManager = InstructionsManager.getInstance();
    int left = instructionsManager.capacityLeftFor(spec.getId(), ship);
    int needs = InstructionsManager.sizeWeight(ship.getHullSize());
    if (left >= needs) {
      return null;
    }
    return String.format(LiminalStrings.mgmt("err_capacity_left"),
        instructionsManager.getHullmodName(spec.getId()), Math.max(0, left), needs);
  }

  @Override
  public boolean affectsOPCosts() {
    InstructionsManager instructionsManager = InstructionsManager.getInstance();
    if (instructionsManager.getLoadout(spec.getId()).isOverCapacity()) {
      return false;
    }
    for (Instruction instruction : getInstructions()) {
      if (instruction.affectsOPCosts()) {
        return true;
      }
    }
    return false;
  }

  public ArrayList<Instruction> getInstructions() {
    return InstructionsManager.getInstance().getInstructionsForLoadout(spec.getId());
  }
}

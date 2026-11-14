package liminal.instructions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public abstract class Instruction {

  public enum Group {
    STANDARD, NEGATIVE, LOGISTIC, SPECIAL,
  }

  private String name;
  private final String templateKeyName;
  private final String displayName;
  private final int opCost;
  private final String description;
  private final int rpMult;
  private final Group group;
  private final InstructionSpec spec;

  public Instruction(String name, int opCost, int rpMult, Group group) {
    this.spec = null;
    this.name = name;
    this.templateKeyName = name;
    this.displayName = Global.getSettings().getString("liminal_instructions", name + "_name");
    this.opCost = opCost;
    this.description = isGachaOnly() ? "" : Global.getSettings().getString("liminal_instructions", name);
    this.rpMult = rpMult;
    this.group = group;
  }

  protected Instruction(InstructionSpec spec) {
    this.spec = spec;
    this.name = spec.id;
    this.templateKeyName = spec.id;
    this.displayName = spec.displayName;
    this.opCost = spec.opCost;
    this.description = spec.description;
    this.rpMult = spec.rpMult;
    this.group = spec.group;
  }

  public InstructionSpec getSpec() {
    return spec;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public int getOpCost() {
    return opCost;
  }

  public String getDescription() {
    return description;
  }

  public int getRpMult() {
    return rpMult;
  }

  public Group getGroup() {
    return group;
  }

  public void getEffectDescription(TooltipMakerAPI a, float padding, HullSize hullSize, int level) {
    float levelMultiplier = getLevelMultiplier(level);
    getEffectDescription(a, padding, hullSize, levelMultiplier);
  }

  public void getEffectDescription(TooltipMakerAPI a, float padding, int level) {
    float levelMultiplier = getLevelMultiplier(level);
    getEffectDescription(a, padding, levelMultiplier);
  }

  public abstract void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier);

  public void applyEffectsAfterShipCreation(ShipAPI ship, String id, float levelMultiplier) {
  }

  public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id, float levelMultiplier) {
  }

  public void advanceInCombat(ShipAPI ship, float amount, float levelMultiplier) {
  }

  public void getEffectDescription(TooltipMakerAPI a, float padding, HullSize hullSize, float levelMultiplier) {
    getEffectDescription(a, padding, levelMultiplier);
  }

  public abstract void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier);

  protected String effectTemplate() {
    if (spec != null) {
      return spec.effect;
    }
    return Global.getSettings().getString("liminal_instructions", templateKeyName + "_effect");
  }

  protected String effectSizesTemplate() {
    if (spec != null) {
      return spec.effect;
    }
    return Global.getSettings().getString("liminal_instructions", templateKeyName + "_effect_sizes");
  }

  public boolean hasApplyEffectsAfterShipCreation() {
    return false;
  }

  public boolean hasApplyEffectsToFighterSpawnedByShip() {
    return false;
  }

  public boolean hasAdvanceInCombat() {
    return false;
  }

  public boolean affectsOPCosts() {
    return false;
  }

  public boolean isIncompatible(Instruction i) {
    return false;
  }

  public boolean isGachaOnly() {
    return spec != null && !spec.template;
  }

  public String illustrationName() {
    if (spec != null) {
      return null;
    }
    return Global.getSettings().getSpriteName("illustrations", "liminal_" +
        name);
  }

  public String i(int value) {
    return "" + value;
  }

  public String t(float value) {
    return "" + (int) (value);
  }

  public String tP(float value) {
    return "" + (int) Math.round(value * 100f) + "%";
  }

  public String tX(float value) {
    return "" + (int) Math.round(value * 100f) / 100f + "x";
  }

  public int getMaxLevel() {
    switch (group) {
      case STANDARD:
        return 60;
      case NEGATIVE:
        return 1;
      case LOGISTIC:
        return 3;
      case SPECIAL:
        return 3;
    }
    return 0;
  }

  public float getLevelMultiplier(int level) {
    switch (group) {
      case STANDARD:
        if (level == 60) {
          return 3f;
        }
        return Math.min(3.0f, (float) (Math.log(4f + level - 1f) / Math.log(4f)));
      case NEGATIVE:
        return 1f;
      case LOGISTIC:
      case SPECIAL:
        return (level - 1) * 0.5f + 1f;
    }
    return 1f;
  }

  public float getMaxLevelMultiplier() {
    int maxLevel = getMaxLevel();
    return getLevelMultiplier(maxLevel);
  }
}

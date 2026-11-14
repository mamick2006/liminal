package liminal.instructions;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;

import liminal.LGlobals;
import liminal.LiminalStrings;
import liminal.instructions.Instruction.Group;

class BadMissile extends Instruction {
  private static final float ROF_MULT = 0.4f;
  private static final float FLUX_MULT = 1.50f;

  public BadMissile() {
    super("bad_missile", -1, 1, Group.NEGATIVE);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getMissileRoFMult().modifyMult(getName(), ROF_MULT);
    stats.getMissileWeaponFluxCostMod().modifyMult(getName(), FLUX_MULT);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), tX(ROF_MULT), tX(FLUX_MULT));
  }
}

class BadEnergy extends Instruction {
  private static final float ROF_MULT = 0.7f;
  private static final float FLUX_MULT = 1.30f;

  public BadEnergy() {
    super("bad_energy", -1, 1, Group.NEGATIVE);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getEnergyRoFMult().modifyMult(getName(), ROF_MULT);
    stats.getEnergyWeaponFluxCostMod().modifyMult(getName(), FLUX_MULT);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), tX(ROF_MULT), tX(FLUX_MULT));
  }
}

class BadBallistic extends Instruction {
  private static final float ROF_MULT = 0.7f;
  private static final float FLUX_MULT = 1.30f;

  public BadBallistic() {
    super("bad_ballistic", -1, 1, Group.NEGATIVE);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getBallisticRoFMult().modifyMult(getName(), ROF_MULT);
    stats.getBallisticWeaponFluxCostMod().modifyMult(getName(), FLUX_MULT);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), tX(ROF_MULT), tX(FLUX_MULT));
  }
}

class BadFlux extends Instruction {
  private static final float DISSIPATION_MULT = 0.8f;

  public BadFlux() {
    super("bad_flux", -2, 1, Group.NEGATIVE);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getFluxDissipation().modifyMult(getName(), DISSIPATION_MULT);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding, Misc.getHighlightColor(),
        tX(DISSIPATION_MULT));
  }
}

class ShipDrive extends Instruction {

  public static float MAX_SPEED_BONUS_MULT = 0.15f;
  public static float MAX_MULT = 0.45f;

  public ShipDrive() {
    super("ship_drive", 4, 2, Group.STANDARD);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getMaxSpeed().modifyMult(getName(), 1f + Math.min(MAX_MULT, MAX_SPEED_BONUS_MULT * levelMultiplier));
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding, Misc.getHighlightColor(),
        tX(1f + Math.min(MAX_MULT, MAX_SPEED_BONUS_MULT * levelMultiplier)),
        tX(1f + MAX_MULT));
  }
}

class HeavyArmorInst extends Instruction {
  private static final float ARMOR_BONUS = 0.1f;

  public HeavyArmorInst() {
    super("heavy_armor", 5, 1, Group.STANDARD);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getArmorBonus().modifyMult(getName(), 1f + ARMOR_BONUS * levelMultiplier);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding, Misc.getHighlightColor(), tX(1f + ARMOR_BONUS * levelMultiplier));
  }
}

class InsulatedEnginesInst extends Instruction {
  private static final float PROFILE_REDUCTION = 20f;

  public InsulatedEnginesInst() {
    super("insulated_engines", 2, 1, Group.STANDARD);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getSensorProfile().modifyFlat(getName(), -PROFILE_REDUCTION * levelMultiplier);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding, Misc.getHighlightColor(),
        t(PROFILE_REDUCTION * levelMultiplier));
  }
}

class HighMaintenanceInst extends Instruction {
  private static final float SUPPLY_USE_MULT = 2f;

  public HighMaintenanceInst() {
    super("high_maintenance", -2, 1, Group.NEGATIVE);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getSuppliesPerMonth().modifyMult(getName(), SUPPLY_USE_MULT);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding, Misc.getHighlightColor(),
        tX(SUPPLY_USE_MULT));
  }
}

class FluxShuntInst extends Instruction {
  private static final float HARD_FLUX_DISSIPATION = 0.2f;

  public FluxShuntInst() {
    super("flux_shunt", 7, 3, Group.STANDARD);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getHardFluxDissipationFraction().modifyFlat(getName(), HARD_FLUX_DISSIPATION * levelMultiplier);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding, Misc.getHighlightColor(),
        tP(HARD_FLUX_DISSIPATION * levelMultiplier));
  }
}

class ExtendedShieldInst extends Instruction {
  private static final float SHIELD_ARC_BONUS = 60f;

  public ExtendedShieldInst() {
    super("extended_shield", 4, 2, Group.STANDARD);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getShieldArcBonus().modifyFlat(getName(), SHIELD_ARC_BONUS * levelMultiplier);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding, Misc.getHighlightColor(),
        t(SHIELD_ARC_BONUS * levelMultiplier));
  }
}

class WeaponRangeInst extends Instruction {
  private static final float RANGE_BONUS = 0.10f;

  public WeaponRangeInst() {
    super("weapon_range", 4, 1, Group.STANDARD);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getBallisticWeaponRangeBonus().modifyPercent(getName(), RANGE_BONUS * 100f * levelMultiplier);
    stats.getEnergyWeaponRangeBonus().modifyPercent(getName(), RANGE_BONUS * 100f * levelMultiplier);
    stats.getMissileWeaponRangeBonus().modifyPercent(getName(), RANGE_BONUS * 100f * levelMultiplier);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding, Misc.getHighlightColor(),
        tP(RANGE_BONUS * levelMultiplier));
  }
}

class WeaponDamageInst extends Instruction {
  private static final float DAMAGE_BONUS = 0.08f;

  public WeaponDamageInst() {
    super("weapon_damage", 6, 2, Group.STANDARD);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getBallisticWeaponDamageMult().modifyPercent(getName(), DAMAGE_BONUS * 100f * levelMultiplier);
    stats.getEnergyWeaponDamageMult().modifyPercent(getName(), DAMAGE_BONUS * 100f * levelMultiplier);
    stats.getMissileWeaponDamageMult().modifyPercent(getName(), DAMAGE_BONUS * 100f * levelMultiplier);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding, Misc.getHighlightColor(),
        tP(DAMAGE_BONUS * levelMultiplier));
  }
}

class SalvageGantryInst extends Instruction {
  private static Map<HullSize, Float> bonus = new HashMap<>();
  static {
    bonus.put(HullSize.FRIGATE, 0.03f);
    bonus.put(HullSize.DESTROYER, 0.1f);
    bonus.put(HullSize.CRUISER, 0.15f);
    bonus.put(HullSize.CAPITAL_SHIP, 0.2f);
  }

  public SalvageGantryInst() {
    super("salvage_gantry", 7, 2, Group.STANDARD);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getDynamic().getMod(Stats.SALVAGE_VALUE_MULT_MOD).modifyFlat(getName(),
        bonus.get(hullSize) * levelMultiplier);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, HullSize hullSize, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(),
        tP(bonus.get(hullSize) * levelMultiplier));
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectSizesTemplate(),
        padding,
        Misc.getHighlightColor(),
        tP(bonus.get(HullSize.FRIGATE) * levelMultiplier),
        tP(bonus.get(HullSize.DESTROYER) * levelMultiplier),
        tP(bonus.get(HullSize.CRUISER) * levelMultiplier),
        tP(bonus.get(HullSize.CAPITAL_SHIP) * levelMultiplier));
  }
}

class GroundAttackInst extends Instruction {
  private static Map<HullSize, Float> bonus = new HashMap<>();
  static {
    bonus.put(HullSize.FRIGATE, 15f);
    bonus.put(HullSize.DESTROYER, 30f);
    bonus.put(HullSize.CRUISER, 45f);
    bonus.put(HullSize.CAPITAL_SHIP, 60f);
  }

  public GroundAttackInst() {
    super("ground_attack", 4, 2, Group.STANDARD);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getDynamic().getMod(Stats.FLEET_GROUND_SUPPORT).modifyFlat(getName(),
        bonus.get(hullSize) * levelMultiplier);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, HullSize hullSize, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(),
        t(bonus.get(hullSize) * levelMultiplier));
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectSizesTemplate(), padding,
        Misc.getHighlightColor(),
        t(bonus.get(HullSize.FRIGATE) * levelMultiplier),
        t(bonus.get(HullSize.DESTROYER) * levelMultiplier),
        t(bonus.get(HullSize.CRUISER) * levelMultiplier),
        t(bonus.get(HullSize.CAPITAL_SHIP) * levelMultiplier));
  }
}

class BadTurrets extends Instruction {
  private static final float TURN_RATE_MULT = 0.4f;

  public BadTurrets() {
    super("bad_turrets", -2, 1, Group.NEGATIVE);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getWeaponTurnRateBonus().modifyMult(getName(), TURN_RATE_MULT);
    stats.getBeamWeaponTurnRateBonus().modifyMult(getName(), TURN_RATE_MULT);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding, Misc.getHighlightColor(), tX(TURN_RATE_MULT));
  }
}

class ManeuverabilityInst extends Instruction {
  private static final float MANEUVER_BONUS = 20f;

  public ManeuverabilityInst() {
    super("maneuverability", 2, 1, Group.STANDARD);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getAcceleration().modifyPercent(getName(), MANEUVER_BONUS * 2f * levelMultiplier);
    stats.getDeceleration().modifyPercent(getName(), MANEUVER_BONUS * levelMultiplier);
    stats.getTurnAcceleration().modifyPercent(getName(), MANEUVER_BONUS * 2f * levelMultiplier);
    stats.getMaxTurnRate().modifyPercent(getName(), MANEUVER_BONUS * levelMultiplier);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding, Misc.getHighlightColor(),
        tP(MANEUVER_BONUS / 100f * levelMultiplier));
  }
}

class FighterDefense extends Instruction {
  private static final float DAMAGE_REDUCTION = 0.2f;

  public FighterDefense() {
    super("fighter_defense", 3, 1, Group.STANDARD);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
  }

  @Override
  public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id, float levelMultiplier) {
    MutableShipStatsAPI stats = fighter.getMutableStats();
    float mult = Math.min(3f, levelMultiplier);
    stats.getArmorDamageTakenMult().modifyPercent(getName(), -DAMAGE_REDUCTION * 100f * mult);
    stats.getShieldDamageTakenMult().modifyPercent(getName(), -DAMAGE_REDUCTION * 100f * mult);
    stats.getHullDamageTakenMult().modifyPercent(getName(), -DAMAGE_REDUCTION * 100f * mult);
  }

  @Override
  public boolean hasApplyEffectsToFighterSpawnedByShip() {
    return true;
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    float mult = Math.min(3f, levelMultiplier);
    a.addPara(effectTemplate(), padding, Misc.getHighlightColor(),
        tP(DAMAGE_REDUCTION * mult), tP(DAMAGE_REDUCTION * 3f));
  }
}

class RuggedConstructionInst extends Instruction {
  private static final float DMOD_EFFECT_MULT = 0.2f;

  public RuggedConstructionInst() {
    super("rugged_construction", 5, 5, Group.LOGISTIC);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getDynamic().getStat(Stats.DMOD_EFFECT_MULT).modifyPercent(getName(),
        -DMOD_EFFECT_MULT * 100f * levelMultiplier);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding, Misc.getHighlightColor(),
        tP(DMOD_EFFECT_MULT * levelMultiplier));
  }
}

class FighterRecoveryInst extends Instruction {
  private static final float CREW_LOSS_MULT = 0.5f;

  public FighterRecoveryInst() {
    super("fighter_recovery", 4, 5, Group.LOGISTIC);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getDynamic().getStat(Stats.FIGHTER_CREW_LOSS_MULT).modifyPercent(getName(),
        -CREW_LOSS_MULT * 100f * Math.min(2f, levelMultiplier));
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding, Misc.getHighlightColor(),
        tP(CREW_LOSS_MULT * Math.min(2f, levelMultiplier)));
  }
}

class BurnDriveInst extends Instruction {
  private static final float BURN_BONUS = 2f;

  public BurnDriveInst() {
    super("burn_drive", 7, 5, Group.LOGISTIC);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getMaxBurnLevel().modifyFlat(getName(), BURN_BONUS * levelMultiplier);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding, Misc.getHighlightColor(),
        t(BURN_BONUS * levelMultiplier));
  }
}

class EfficiencyInst extends Instruction {
  private static final float MAINTENANCE_MULT = 0.25f;

  public EfficiencyInst() {
    super("efficiency", 6, 10, Group.LOGISTIC);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getSuppliesPerMonth().modifyMult(getName(), 1f - MAINTENANCE_MULT * levelMultiplier);
    stats.getFuelUseMod().modifyMult(getName(), 1f - MAINTENANCE_MULT * levelMultiplier);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding, Misc.getHighlightColor(),
        tX(1f - MAINTENANCE_MULT * levelMultiplier));
  }
}

class CargoExpansionInst extends Instruction {
  private static final float CAPACITY_BONUS = 0.25f;

  public CargoExpansionInst() {
    super("cargo_expansion", 5, 7, Group.LOGISTIC);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getCargoMod().modifyPercent(getName(), CAPACITY_BONUS * 100f * levelMultiplier);
    stats.getFuelMod().modifyPercent(getName(), CAPACITY_BONUS * 100f * levelMultiplier);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding, Misc.getHighlightColor(),
        tP(CAPACITY_BONUS * levelMultiplier));
  }
}

class WeaponOPReductionInst extends Instruction {
  private static final float SMALL_REDUCTION = 1f;
  private static final float MEDIUM_REDUCTION = 1.5f;
  private static final float LARGE_REDUCTION = 2f;
  private static final float FIGHTER_REDUCTION = 2f;

  public WeaponOPReductionInst() {
    super("weapon_op_reduction", 15, 15, Group.SPECIAL);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getDynamic().getMod(Stats.SMALL_BALLISTIC_MOD).modifyFlat(getName(),
        -(int) (SMALL_REDUCTION * levelMultiplier));
    stats.getDynamic().getMod(Stats.SMALL_ENERGY_MOD).modifyFlat(getName(), -(int) (SMALL_REDUCTION * levelMultiplier));
    stats.getDynamic().getMod(Stats.SMALL_MISSILE_MOD).modifyFlat(getName(),
        -(int) (SMALL_REDUCTION * levelMultiplier));

    stats.getDynamic().getMod(Stats.MEDIUM_BALLISTIC_MOD).modifyFlat(getName(),
        -(int) (MEDIUM_REDUCTION * levelMultiplier));
    stats.getDynamic().getMod(Stats.MEDIUM_ENERGY_MOD).modifyFlat(getName(),
        -(int) (MEDIUM_REDUCTION * levelMultiplier));
    stats.getDynamic().getMod(Stats.MEDIUM_MISSILE_MOD).modifyFlat(getName(),
        -(int) (MEDIUM_REDUCTION * levelMultiplier));

    stats.getDynamic().getMod(Stats.LARGE_BALLISTIC_MOD).modifyFlat(getName(),
        -(int) (LARGE_REDUCTION * levelMultiplier));
    stats.getDynamic().getMod(Stats.LARGE_ENERGY_MOD).modifyFlat(getName(), -(int) (LARGE_REDUCTION * levelMultiplier));
    stats.getDynamic().getMod(Stats.LARGE_MISSILE_MOD).modifyFlat(getName(),
        -(int) (LARGE_REDUCTION * levelMultiplier));

    stats.getDynamic().getMod(Stats.BOMBER_COST_MOD).modifyFlat(getName(),
        -(int) (FIGHTER_REDUCTION * levelMultiplier));
    stats.getDynamic().getMod(Stats.FIGHTER_COST_MOD).modifyFlat(getName(),
        -(int) (FIGHTER_REDUCTION * levelMultiplier));
    stats.getDynamic().getMod(Stats.INTERCEPTOR_COST_MOD).modifyFlat(getName(),
        -(int) (FIGHTER_REDUCTION * levelMultiplier));
    stats.getDynamic().getMod(Stats.SUPPORT_COST_MOD).modifyFlat(getName(),
        -(int) (FIGHTER_REDUCTION * levelMultiplier));
  }

  @Override
  public boolean affectsOPCosts() {
    return true;
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(),
        padding, Misc.getHighlightColor(),
        t((int) (SMALL_REDUCTION * levelMultiplier)),
        t((int) (MEDIUM_REDUCTION * levelMultiplier)),
        t((int) (LARGE_REDUCTION * levelMultiplier)),
        t((int) (FIGHTER_REDUCTION * levelMultiplier)));
  }
}

class MissileReloadInst extends Instruction {
  private static final float RELOAD_INTERVAL = 100f;
  private static final float RELOAD_AMOUNT = 0.3f;

  private final Map<ShipAPI, IntervalUtil> intervals = new WeakHashMap<>();

  public MissileReloadInst() {
    super("missile_reload", 8, 10, Group.SPECIAL);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount, float levelMultiplier) {
    if (!ship.isAlive())
      return;

    IntervalUtil interval = intervals.get(ship);
    if (interval == null) {
      interval = new IntervalUtil(RELOAD_INTERVAL, RELOAD_INTERVAL);
      intervals.put(ship, interval);
    }

    interval.advance(amount * levelMultiplier);
    if (interval.intervalElapsed()) {
      for (WeaponAPI w : ship.getAllWeapons()) {
        if (!w.getType().equals(WeaponType.MISSILE))
          continue;
        if (w.usesAmmo() && w.getAmmo() < w.getMaxAmmo()) {
          int ammoToAdd = Math.max(1, (int) (w.getMaxAmmo() * RELOAD_AMOUNT));
          w.setAmmo(Math.min(w.getMaxAmmo(), w.getAmmo() + ammoToAdd));
        }
      }
    }
  }

  @Override
  public boolean hasAdvanceInCombat() {
    return true;
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding, Misc.getHighlightColor(),
        t(RELOAD_INTERVAL / levelMultiplier));
  }
}

class FighterBayInst extends Instruction {
  private static final Map<HullSize, Float> BAYS_BY_SIZE = new HashMap<HullSize, Float>() {
    {
      put(HullSize.FRIGATE, 0.6f);
      put(HullSize.DESTROYER, 0.9f);
      put(HullSize.CRUISER, 1.2f);
      put(HullSize.CAPITAL_SHIP, 1.5f);
    }
  };

  public FighterBayInst() {
    super("fighter_bay", 12, 12, Group.SPECIAL);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    int numBays = (int) Math.ceil(BAYS_BY_SIZE.get(hullSize) * levelMultiplier);
    stats.getNumFighterBays().modifyFlat(getName(), numBays);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, HullSize hullSize, float levelMultiplier) {
    int bays = (int) Math.ceil(BAYS_BY_SIZE.get(hullSize) * levelMultiplier);
    a.addPara(effectTemplate(), padding, Misc.getHighlightColor(),
        t(bays), bays == 1 ? "bay" : "bays");
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectSizesTemplate(), padding, Misc.getHighlightColor(),
        t((float) Math.ceil(BAYS_BY_SIZE.get(HullSize.FRIGATE) * levelMultiplier)),
        t((float) Math.ceil(BAYS_BY_SIZE.get(HullSize.DESTROYER) * levelMultiplier)),
        t((float) Math.ceil(BAYS_BY_SIZE.get(HullSize.CRUISER) * levelMultiplier)),
        t((float) Math.ceil(BAYS_BY_SIZE.get(HullSize.CAPITAL_SHIP) * levelMultiplier)));
  }
}

class TimeFlowInst extends Instruction {
  private static final float TIME_FLOW_BONUS = 0.10f;
  private static final float MAX_TIME_FLOW_BONUS = 0.40f;

  public TimeFlowInst() {
    super("time_flow", 12, 10, Group.SPECIAL);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount, float levelMultiplier) {
    float hpRatio = ship.getHitpoints() / ship.getMaxHitpoints();
    float bonusScale = (float) Math.pow(1 - hpRatio, 2);
    float bonus = TIME_FLOW_BONUS + (MAX_TIME_FLOW_BONUS - TIME_FLOW_BONUS) * bonusScale;
    bonus *= levelMultiplier;
    bonus += 1f;

    if (Global.getCombatEngine().isPaused()) {
      return;
    }

    if (!ship.isAlive() || ship.isPiece()) {
      return;
    }

    boolean player = ship.equals(Global.getCombatEngine().getPlayerShip());
    if (player) {
      ship.getMutableStats().getTimeMult().modifyMult(getName(), bonus);
      Global.getCombatEngine().getTimeMult().modifyMult(getName(), 1f / bonus);
    } else {
      ship.getMutableStats().getTimeMult().modifyMult(getName(), bonus);
      Global.getCombatEngine().getTimeMult().unmodify(getName());
    }
    if (hpRatio < 0.95f) {
      ship.setJitter(this, LGlobals.jitterColor, (1 - hpRatio), 3, 0, (1 - hpRatio) * (0 + bonus * 10f));
      ship.setJitterUnder(this, LGlobals.jitterUnderColor, (1 - hpRatio), 25, 0f,
          (1 - hpRatio) * (7f + bonus * 10f));
    }
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(),
        tX(1f + TIME_FLOW_BONUS * levelMultiplier), tX(1f + MAX_TIME_FLOW_BONUS * levelMultiplier));
  }

  @Override
  public boolean hasAdvanceInCombat() {
    return true;
  }
}

class RegenInst extends Instruction {

  private static Map<HullSize, Float> MAG_BY_SIZE = new HashMap<HullSize, Float>() {
    {
      put(HullSize.FRIGATE, 1f / 100f);
      put(HullSize.DESTROYER, 1f / 150f);
      put(HullSize.CRUISER, 1f / 200f);
      put(HullSize.CAPITAL_SHIP, 1f / 300f);
    }
  };

  private static final float MIN_REGEN_TIME = 1f / 80f;

  public RegenInst() {
    super("regen", 6, 10, Group.LOGISTIC);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount, float levelMultiplier) {
    if (!ship.isAlive() || ship.isHulk() || ship.isPhased())
      return;

    ArmorGridAPI armorGrid = ship.getArmorGrid();
    float[][] grid = armorGrid.getGrid();
    float maxArmorInCell = armorGrid.getMaxArmorInCell();

    if (MAG_BY_SIZE.get(ship.getHullSize()) == null) {
      return;
    }
    float regenArmorPercent = Math.min(MAG_BY_SIZE.get(ship.getHullSize()) * levelMultiplier, MIN_REGEN_TIME);
    float repairAmount = maxArmorInCell * regenArmorPercent * amount;

    for (int x = 0; x < grid.length; x++) {
      for (int y = 0; y < grid[0].length; y++) {
        if (grid[x][y] < maxArmorInCell) {
          float regen = Math.min(grid[x][y] + repairAmount, maxArmorInCell);
          armorGrid.setArmorValue(x, y, regen);
        }
      }
    }
    float regenHullPercent = Math.min(MAG_BY_SIZE.get(ship.getHullSize()) * levelMultiplier, MIN_REGEN_TIME);
    float regenHull = ship.getMaxHitpoints() * regenHullPercent * amount;
    ship.setHitpoints(Math.min(ship.getHitpoints() + regenHull, ship.getMaxHitpoints()));

    ship.syncWithArmorGridState();
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, HullSize hullSize, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(),
        t(1f / (Math.min(MAG_BY_SIZE.get(hullSize) * levelMultiplier, MIN_REGEN_TIME))));
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectSizesTemplate(),
        padding,
        Misc.getHighlightColor(),
        t(1f / (Math.min(MAG_BY_SIZE.get(HullSize.FRIGATE) * levelMultiplier, MIN_REGEN_TIME))),
        t(1f / (Math.min(MAG_BY_SIZE.get(HullSize.DESTROYER) * levelMultiplier, MIN_REGEN_TIME))),
        t(1f / (Math.min(MAG_BY_SIZE.get(HullSize.CRUISER) * levelMultiplier, MIN_REGEN_TIME))),
        t(1f / (Math.min(MAG_BY_SIZE.get(HullSize.CAPITAL_SHIP) * levelMultiplier, MIN_REGEN_TIME))),
        t(1f / MIN_REGEN_TIME));
  }

  @Override
  public boolean hasAdvanceInCombat() {
    return true;
  }
}

class DamageCapInst extends Instruction {

  private static Map<HullSize, Float> MAG_BY_SIZE = new HashMap<HullSize, Float>() {
    {
      put(HullSize.FRIGATE, 0.4f);
      put(HullSize.DESTROYER, 0.6f);
      put(HullSize.CRUISER, 0.8f);
      put(HullSize.CAPITAL_SHIP, 1.1f);
    }
  };

  public DamageCapInst() {
    super("damage_cap", 8, 3, Group.STANDARD);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
  }

  public float getDamageCap(HullSize hullSize, float levelMultiplier) {
    return 1f / (1f + MAG_BY_SIZE.get(hullSize) * levelMultiplier);
  }

  @Override
  public boolean hasApplyEffectsAfterShipCreation() {
    return true;
  }

  private static class DamageCapListener implements HullDamageAboutToBeTakenListener, AdvanceableListener {
    private ShipAPI ship;
    private float damageCap;
    private Deque<Pair<Float, Double>> damageQueue;
    private float totalDamage;
    private double currentTime;
    private boolean damageCapActive = false;

    private double fadeOutUntil = -1f;

    public DamageCapListener(ShipAPI ship, float damageCap) {
      this.ship = ship;
      this.damageCap = damageCap;
      this.damageQueue = new ArrayDeque<>();
      this.totalDamage = 0f;
      this.currentTime = 0f;
    }

    @Override
    public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
      while (!damageQueue.isEmpty() && currentTime - damageQueue.peekFirst().two >= 1.0) {
        totalDamage -= damageQueue.pollFirst().one;
      }

      boolean blocked = false;
      float maxDamage = ship.getMaxHitpoints() * damageCap;
      if (totalDamage + damageAmount > maxDamage) {
        damageAmount = Math.max(0f, maxDamage - totalDamage);
        if (damageAmount < ship.getHitpoints()) {
          ship.setHitpoints(ship.getHitpoints() - damageAmount);
          Global.getSoundPlayer().playSound("liminal_damage_cap", 1f, 1f,
              ship.getLocation(), ship.getVelocity());
          if (fadeOutUntil == -1f) {
            ship.setExtraAlphaMult2(0.3f);
            CombatEngineAPI engine = Global.getCombatEngine();
            engine.addNebulaSmokeParticle(point,
                ship.getVelocity(),
                100f,
                2.2f,
                0.7f,
                0.9f,
                2.2f,
                LGlobals.liminalityColor);
          }
          fadeOutUntil = currentTime + 0.5;
          blocked = true;
        }
      }

      damageQueue.addLast(new Pair<>(damageAmount, currentTime));
      totalDamage += damageAmount;
      return blocked;
    }

    @Override
    public void advance(float amount) {
      currentTime += amount;
      if (damageCapActive) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        stats.getHullDamageTakenMult().unmodify("damage_cap");
        damageCapActive = false;
      }
      if (fadeOutUntil != -1f && currentTime >= fadeOutUntil) {
        fadeOutUntil = -1f;
        ship.setExtraAlphaMult2(1f);
      }
    }
  }

  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id, float levelMultiplier) {
    float damageCap = getDamageCap(ship.getHullSize(), levelMultiplier);
    List<DamageCapListener> existing = ship.getListeners(DamageCapListener.class);
    if (existing.isEmpty()) {
      ship.addListener(new DamageCapListener(ship, damageCap));
    } else {
      DamageCapListener listener = existing.get(0);
      listener.damageCap = Math.min(listener.damageCap, damageCap);
    }
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, HullSize hullSize, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(),
        tP(getDamageCap(hullSize, levelMultiplier)));
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectSizesTemplate(), padding,
        Misc.getHighlightColor(),
        tP(getDamageCap(HullSize.FRIGATE, levelMultiplier)),
        tP(getDamageCap(HullSize.DESTROYER, levelMultiplier)),
        tP(getDamageCap(HullSize.CRUISER, levelMultiplier)),
        tP(getDamageCap(HullSize.CAPITAL_SHIP, levelMultiplier)));
  }
}

class GlitchedActuatorsInst extends Instruction {
  private static final float MALFUNCTION_CHANCE = 0.02f;

  public GlitchedActuatorsInst() {
    super("glitched_actuators", -2, 1, Group.NEGATIVE);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getWeaponMalfunctionChance().modifyFlat(getName(), MALFUNCTION_CHANCE);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), tP(MALFUNCTION_CHANCE));
  }
}

class LiquidArmorInst extends Instruction {
  private static final float EQUALIZE_RATE = 0.15f;
  private static final float EQUALIZE_INTERVAL = 5f;
  private static final float MAX_EQUALIZE_RATE = 0.45f;

  private final Map<ShipAPI, IntervalUtil> intervals = new WeakHashMap<>();

  public LiquidArmorInst() {
    super("liquid_armor", 5, 2, Group.STANDARD);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount, float levelMultiplier) {
    if (!ship.isAlive() || ship.isHulk()) {
      return;
    }
    IntervalUtil interval = intervals.get(ship);
    if (interval == null) {
      interval = new IntervalUtil(EQUALIZE_INTERVAL, EQUALIZE_INTERVAL);
      intervals.put(ship, interval);
    }
    interval.advance(amount);
    if (!interval.intervalElapsed()) {
      return;
    }

    float rate = Math.min(EQUALIZE_RATE * levelMultiplier, MAX_EQUALIZE_RATE);
    ArmorGridAPI armorGrid = ship.getArmorGrid();
    float[][] grid = armorGrid.getGrid();
    boolean changed = false;
    for (int x = 0; x < grid.length; x++) {
      for (int y = 0; y < grid[0].length; y++) {
        changed |= equalize(armorGrid, grid, x, y, x + 1, y, rate);
        changed |= equalize(armorGrid, grid, x, y, x, y + 1, rate);
      }
    }
    if (changed) {
      ship.syncWithArmorGridState();
    }
  }

  private boolean equalize(ArmorGridAPI armorGrid, float[][] grid, int x1, int y1, int x2, int y2, float rate) {
    if (x2 >= grid.length || y2 >= grid[0].length) {
      return false;
    }
    float a = armorGrid.getArmorValue(x1, y1);
    float b = armorGrid.getArmorValue(x2, y2);
    float transfer = (float) Math.floor(rate * Math.abs(a - b) * 16f) / 16f;
    if (transfer <= 0f) {
      return false;
    }
    if (a > b) {
      armorGrid.setArmorValue(x1, y1, a - transfer);
      armorGrid.setArmorValue(x2, y2, b + transfer);
    } else {
      armorGrid.setArmorValue(x1, y1, a + transfer);
      armorGrid.setArmorValue(x2, y2, b - transfer);
    }
    return true;
  }

  @Override
  public boolean hasAdvanceInCombat() {
    return true;
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), tP(Math.min(EQUALIZE_RATE * levelMultiplier, MAX_EQUALIZE_RATE)),
        t(EQUALIZE_INTERVAL));
  }
}

class BreachTargetingInst extends Instruction {
  private static final float DAMAGE_BONUS = 0.20f;
  private static final float MAX_DAMAGE_BONUS = 1.0f;

  public BreachTargetingInst() {
    super("breach_targeting", 3, 2, Group.STANDARD);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    float bonus = Math.min(DAMAGE_BONUS * levelMultiplier, MAX_DAMAGE_BONUS);
    stats.getDamageToTargetWeaponsMult().modifyPercent(getName(), bonus * 100f);
    stats.getDamageToTargetEnginesMult().modifyPercent(getName(), bonus * 100f);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), tP(Math.min(DAMAGE_BONUS * levelMultiplier, MAX_DAMAGE_BONUS)));
  }
}

class FireDisciplineInst extends Instruction {
  private static final float LEADING_BONUS = 0.15f;
  private static final float RECOIL_REDUCTION = 0.15f;
  private static final float MAX_RECOIL_REDUCTION = 0.9f;

  public FireDisciplineInst() {
    super("fire_discipline", 3, 1, Group.STANDARD);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getAutofireAimAccuracy().modifyFlat(getName(), LEADING_BONUS * levelMultiplier);
    float recoilMult = Math.max(1f - MAX_RECOIL_REDUCTION, 1f - RECOIL_REDUCTION * levelMultiplier);
    stats.getRecoilPerShotMult().modifyMult(getName(), recoilMult);
    stats.getMaxRecoilMult().modifyMult(getName(), recoilMult);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), tP(LEADING_BONUS * levelMultiplier),
        tP(Math.min(MAX_RECOIL_REDUCTION, RECOIL_REDUCTION * levelMultiplier)));
  }
}

class CloseAirSupportInst extends Instruction {
  private static final float DAMAGE_BONUS = 0.10f;
  private static final float RANGE = 1500f;
  private static final float MAX_DAMAGE_BONUS = 0.6f;

  public CloseAirSupportInst() {
    super("close_air_support", 4, 2, Group.STANDARD);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
  }

  @Override
  public void applyEffectsToFighterSpawnedByShip(final ShipAPI fighter, final ShipAPI ship, String id,
      final float levelMultiplier) {
    final float bonus = Math.min(DAMAGE_BONUS * levelMultiplier, MAX_DAMAGE_BONUS);
    fighter.addListener(new AdvanceableListener() {
      private final IntervalUtil check = new IntervalUtil(0.5f, 0.5f);

      @Override
      public void advance(float amount) {
        check.advance(amount);
        if (!check.intervalElapsed() || !fighter.isAlive()) {
          return;
        }
        MutableShipStatsAPI stats = fighter.getMutableStats();
        boolean inRange = ship != null && ship.isAlive()
            && Misc.getDistance(fighter.getLocation(), ship.getLocation()) <= RANGE;
        if (inRange) {
          stats.getBallisticWeaponDamageMult().modifyPercent(getName(), bonus * 100f);
          stats.getEnergyWeaponDamageMult().modifyPercent(getName(), bonus * 100f);
          stats.getMissileWeaponDamageMult().modifyPercent(getName(), bonus * 100f);
        } else {
          stats.getBallisticWeaponDamageMult().unmodify(getName());
          stats.getEnergyWeaponDamageMult().unmodify(getName());
          stats.getMissileWeaponDamageMult().unmodify(getName());
        }
      }
    });
  }

  @Override
  public boolean hasApplyEffectsToFighterSpawnedByShip() {
    return true;
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), tP(Math.min(DAMAGE_BONUS * levelMultiplier, MAX_DAMAGE_BONUS)), t(RANGE));
  }
}

class FlashRepairInst extends Instruction {
  private static final float REPAIR_FRACTION = 0.15f;

  public FlashRepairInst() {
    super("flash_repair", 3, 5, Group.LOGISTIC);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getDynamic().getMod(Stats.INSTA_REPAIR_FRACTION).modifyFlat(getName(),
        REPAIR_FRACTION * levelMultiplier);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), tP(REPAIR_FRACTION * levelMultiplier));
  }
}

class CapacitorBlankingPlatesInst extends Instruction {
  private static final float SMALL_DISSIPATION = 5f;
  private static final float MEDIUM_DISSIPATION = 10f;
  private static final float LARGE_DISSIPATION = 20f;

  public CapacitorBlankingPlatesInst() {
    super("capacitor_blanking_plates", 4, 5, Group.LOGISTIC);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    ShipVariantAPI variant = stats.getVariant();
    if (variant == null || variant.getHullSpec() == null) {
      return;
    }
    float bonus = 0f;
    for (WeaponSlotAPI slot : variant.getHullSpec().getAllWeaponSlotsCopy()) {
      if (slot.isDecorative() || slot.isBuiltIn() || slot.isSystemSlot()) {
        continue;
      }
      if (slot.getWeaponType() == WeaponType.LAUNCH_BAY) {
        continue;
      }
      String weaponId = variant.getWeaponId(slot.getId());
      if (weaponId != null && !weaponId.isEmpty()) {
        continue;
      }
      switch (slot.getSlotSize()) {
        case SMALL:
          bonus += SMALL_DISSIPATION * levelMultiplier;
          break;
        case MEDIUM:
          bonus += MEDIUM_DISSIPATION * levelMultiplier;
          break;
        case LARGE:
          bonus += LARGE_DISSIPATION * levelMultiplier;
          break;
      }
    }
    stats.getFluxDissipation().modifyFlat(getName(), bonus);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), t(SMALL_DISSIPATION * levelMultiplier),
        t(MEDIUM_DISSIPATION * levelMultiplier), t(LARGE_DISSIPATION * levelMultiplier));
  }
}

class OverrideBeaconInst extends Instruction {
  private static final float CAP_RATE_BONUS = 0.50f;
  private static final float CAP_RANGE_BONUS = 200f;
  private static final float MAX_RANGE_BONUS = 400f;

  public OverrideBeaconInst() {
    super("override_beacon", 2, 5, Group.LOGISTIC);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getDynamic().getStat(Stats.SHIP_OBJECTIVE_CAP_RATE_MULT).modifyPercent(getName(),
        CAP_RATE_BONUS * 100f * levelMultiplier);
    stats.getDynamic().getMod(Stats.SHIP_OBJECTIVE_CAP_RANGE_MOD).modifyFlat(getName(),
        Math.min(CAP_RANGE_BONUS * levelMultiplier, MAX_RANGE_BONUS));
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), tP(CAP_RATE_BONUS * levelMultiplier),
        t(Math.min(CAP_RANGE_BONUS * levelMultiplier, MAX_RANGE_BONUS)));
  }
}

class AnchoredFiringSolutionsInst extends Instruction {
  private static final float ROF_BONUS = 0.30f;
  private static final float SPEED_THRESHOLD = 20f;
  private static final float MAX_ROF_BONUS = 0.6f;

  private final Map<ShipAPI, IntervalUtil> checkIntervals = new WeakHashMap<>();

  public AnchoredFiringSolutionsInst() {
    super("anchored_firing_solutions", 10, 10, Group.SPECIAL);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount, float levelMultiplier) {
    if (!ship.isAlive()) {
      return;
    }
    IntervalUtil check = checkIntervals.get(ship);
    if (check == null) {
      check = new IntervalUtil(0.5f, 0.5f);
      checkIntervals.put(ship, check);
    }
    check.advance(amount);
    if (!check.intervalElapsed()) {
      return;
    }
    MutableShipStatsAPI stats = ship.getMutableStats();
    if (ship.getVelocity().length() < SPEED_THRESHOLD) {
      float mult = 1f + Math.min(ROF_BONUS * levelMultiplier, MAX_ROF_BONUS);
      stats.getBallisticRoFMult().modifyMult(getName(), mult);
      stats.getEnergyRoFMult().modifyMult(getName(), mult);
      stats.getMissileRoFMult().modifyMult(getName(), mult);
    } else {
      stats.getBallisticRoFMult().unmodify(getName());
      stats.getEnergyRoFMult().unmodify(getName());
      stats.getMissileRoFMult().unmodify(getName());
    }
  }

  @Override
  public boolean hasAdvanceInCombat() {
    return true;
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), tP(Math.min(ROF_BONUS * levelMultiplier, MAX_ROF_BONUS)), t(SPEED_THRESHOLD));
  }
}

public class Instructions {
  private static final Map<String, Instruction> INSTRUCTION_MAP = new LinkedHashMap<String, Instruction>() {
    {
      put(new BadMissile().getName(), new BadMissile());
      put(new BadEnergy().getName(), new BadEnergy());
      put(new BadBallistic().getName(), new BadBallistic());
      put(new BadFlux().getName(), new BadFlux());
      put(new ShipDrive().getName(), new ShipDrive());
      put(new HeavyArmorInst().getName(), new HeavyArmorInst());
      put(new InsulatedEnginesInst().getName(), new InsulatedEnginesInst());
      put(new HighMaintenanceInst().getName(), new HighMaintenanceInst());
      put(new FluxShuntInst().getName(), new FluxShuntInst());
      put(new ExtendedShieldInst().getName(), new ExtendedShieldInst());
      put(new WeaponRangeInst().getName(), new WeaponRangeInst());
      put(new WeaponDamageInst().getName(), new WeaponDamageInst());
      put(new SalvageGantryInst().getName(), new SalvageGantryInst());
      put(new GroundAttackInst().getName(), new GroundAttackInst());
      put(new BadTurrets().getName(), new BadTurrets());
      put(new ManeuverabilityInst().getName(), new ManeuverabilityInst());
      put(new FighterDefense().getName(), new FighterDefense());
      put(new FighterRecoveryInst().getName(), new FighterRecoveryInst());
      put(new BurnDriveInst().getName(), new BurnDriveInst());
      put(new EfficiencyInst().getName(), new EfficiencyInst());
      put(new CargoExpansionInst().getName(), new CargoExpansionInst());
      put(new WeaponOPReductionInst().getName(), new WeaponOPReductionInst());
      put(new MissileReloadInst().getName(), new MissileReloadInst());
      put(new FighterBayInst().getName(), new FighterBayInst());
      put(new TimeFlowInst().getName(), new TimeFlowInst());
      put(new RegenInst().getName(), new RegenInst());
      put(new DamageCapInst().getName(), new DamageCapInst());
      put(new GlitchedActuatorsInst().getName(), new GlitchedActuatorsInst());
      put(new LiquidArmorInst().getName(), new LiquidArmorInst());
      put(new BreachTargetingInst().getName(), new BreachTargetingInst());
      put(new FireDisciplineInst().getName(), new FireDisciplineInst());
      put(new CloseAirSupportInst().getName(), new CloseAirSupportInst());
      put(new FlashRepairInst().getName(), new FlashRepairInst());
      put(new CapacitorBlankingPlatesInst().getName(), new CapacitorBlankingPlatesInst());
      put(new OverrideBeaconInst().getName(), new OverrideBeaconInst());
      put(new AnchoredFiringSolutionsInst().getName(), new AnchoredFiringSolutionsInst());
    }
  };
  private static final List<Instruction> OWN_INSTRUCTIONS =
      Collections.unmodifiableList(new ArrayList<Instruction>(INSTRUCTION_MAP.values()));

  private static Map<String, Instruction> tableInstructions = new LinkedHashMap<String, Instruction>();
  private static List<Instruction> combined;
  private static SortedMap<Group, List<Instruction>> combinedByGroup;

  static {
    rebuildViews();
  }

  private static void rebuildViews() {
    List<Instruction> all = new ArrayList<Instruction>(INSTRUCTION_MAP.values());
    all.addAll(tableInstructions.values());
    combined = Collections.unmodifiableList(all);
    SortedMap<Group, List<Instruction>> byGroup = new TreeMap<Group, List<Instruction>>();
    for (Instruction instruction : all) {
      List<Instruction> group = byGroup.get(instruction.getGroup());
      if (group == null) {
        group = new ArrayList<Instruction>();
        byGroup.put(instruction.getGroup(), group);
      }
      group.add(instruction);
    }
    for (Map.Entry<Group, List<Instruction>> entry : byGroup.entrySet()) {
      entry.setValue(Collections.unmodifiableList(entry.getValue()));
    }
    combinedByGroup = byGroup;
  }

  public static void setTemplateInstructions(Collection<Instruction> instructions) {
    Map<String, Instruction> replacement = new LinkedHashMap<String, Instruction>();
    for (Instruction instruction : instructions) {
      if (INSTRUCTION_MAP.containsKey(instruction.getName())) {
        Global.getLogger(Instructions.class).warn("Liminal: another mod offers an Instruction under"
            + " the name '" + instruction.getName() + "', which this mod already uses, so the offered"
            + " one is ignored and templates holding that name keep this mod's version.");
        continue;
      }
      replacement.put(instruction.getName(), instruction);
    }
    tableInstructions = replacement;
    rebuildViews();
  }

  public static Instruction getInstruction(String name) {
    Instruction own = INSTRUCTION_MAP.get(name);
    return own != null ? own : tableInstructions.get(name);
  }

  public static List<Instruction> getInstructions() {
    return combined;
  }

  public static List<Instruction> getOwnInstructions() {
    return OWN_INSTRUCTIONS;
  }

  public static List<Group> getGroups() {
    return new ArrayList<>(combinedByGroup.keySet());
  }

  public static List<Instruction> getInstructions(Group group) {
    return combinedByGroup.get(group);
  }

  public static int getMaxInstructionsInGroup(Group group) {
    switch (group) {
      case STANDARD:
        return 5;
      case NEGATIVE:
        return 4;
      case LOGISTIC:
        return 2;
      case SPECIAL:
        return 2;
    }
    return 0;
  }

  public static Color getGroupColor(Group group) {
    switch (group) {
      case STANDARD:
        return Misc.getBasePlayerColor();
      case NEGATIVE:
        return Misc.getNegativeHighlightColor();
      case LOGISTIC:
        return LGlobals.liminalityColor;
      case SPECIAL:
        return LGlobals.liminalityHighlightColor;
    }
    return Misc.getBasePlayerColor();
  }

  public static String getGroupTitle(Group group) {
    switch (group) {
      case STANDARD:
        return "Standard";
      case NEGATIVE:
        return "Negative";
      case LOGISTIC:
        return "Advanced Logistic";
      case SPECIAL:
        return "Unique";
    }
    return "Standard";
  }

  public static final Map<String, Instruction> GACHA_INSTRUCTION_MAP = new LinkedHashMap<String, Instruction>() {
    {
      putGacha(new BadMissile());
      putGacha(new BadEnergy());
      putGacha(new BadBallistic());
      putGacha(new BadFlux());
      putGacha(new ShipDrive());
      putGacha(new HeavyArmorInst());
      putGacha(new InsulatedEnginesInst());
      putGacha(new HighMaintenanceInst());
      putGacha(new FluxShuntInst());
      putGacha(new ExtendedShieldInst());
      putGacha(new WeaponRangeInst());
      putGacha(new WeaponDamageInst());
      putGacha(new SalvageGantryInst());
      putGacha(new GroundAttackInst());
      putGacha(new BadTurrets());
      putGacha(new ManeuverabilityInst());
      putGacha(new FighterDefense());
      putGacha(new FighterRecoveryInst());
      putGacha(new BurnDriveInst());
      putGacha(new EfficiencyInst());
      putGacha(new CargoExpansionInst());
      putGacha(new WeaponOPReductionInst());
      putGacha(new MissileReloadInst());
      putGacha(new FighterBayInst());
      putGacha(new TimeFlowInst());
      putGacha(new RegenInst());
      putGacha(new DamageCapInst());
      putGacha(new GlitchedActuatorsInst());
      putGacha(new LiquidArmorInst());
      putGacha(new BreachTargetingInst());
      putGacha(new FireDisciplineInst());
      putGacha(new CloseAirSupportInst());
      putGacha(new FlashRepairInst());
      putGacha(new CapacitorBlankingPlatesInst());
      putGacha(new OverrideBeaconInst());
      putGacha(new AnchoredFiringSolutionsInst());

      for (Instruction instruction : GachaOnlyInstructions.all()) {
        put(instruction.getName(), instruction);
      }
    }

    private void putGacha(Instruction instruction) {
      instruction.setName(instruction.getName() + "_gacha");
      put(instruction.getName(), instruction);
    }
  };
}

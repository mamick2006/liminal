package liminal.instructions;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

import liminal.LGlobals;
import liminal.instructions.Instruction.Group;

public class GachaOnlyInstructions {
  public static List<Instruction> all() {
    return Arrays.asList(
        new NullFrameInst(),
        new EmergencyPurgeInst(),
        new MirrorSkinInst(),
        new LastRitesInst(),
        new SympatheticOverloadInst(),
        new PartingGiftInst(),
        new ResidualPlatingInst(),
        new DyingStarInst(),
        new ProwAegisInst(),
        new LoneHunterInst(),
        new PredationFeedbackInst(),
        new MineFabricatorInst(),
        new DuctTapeLiturgyInst(),
        new OpeningStatementInst(),
        new BunkerReflexInst(),
        new FaradayCageInst(),
        new HemovoreArrayInst(),
        new CoupDeGraceInst(),
        new LeechCouplingInst(),
        new HeadsOrTailsInst());
  }

  public static void resetStaticsForLoad() {
    for (Instruction instruction : Instructions.GACHA_INSTRUCTION_MAP.values()) {
      if (instruction instanceof PartingGiftInst) {
        ((PartingGiftInst) instruction).forgetBattle();
      }
    }
  }
}

abstract class GachaOnlyInstruction extends Instruction {
  public GachaOnlyInstruction(String name, int opCost, int rpMult, Group group) {
    super(name, opCost, rpMult, group);
  }

  @Override
  public final boolean isGachaOnly() {
    return true;
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
  }

  static float hitDamage(DamageAPI damage) {
    return damage.isDps() ? damage.getDamage() * damage.getDpsDuration() : damage.getDamage();
  }
}

class ShipDeathTracker {
  private static final float SCAN_INTERVAL = 0.5f;

  private static ShipDeathTracker current;

  static class DeathEvent {
    final int owner;
    final Vector2f location;
    final boolean fighter;

    DeathEvent(int owner, Vector2f location, boolean fighter) {
      this.owner = owner;
      this.location = location;
      this.fighter = fighter;
    }
  }

  private final java.lang.ref.WeakReference<CombatEngineAPI> engineRef;
  private final Map<String, DeathEvent> alive = new HashMap<>();
  private final List<DeathEvent> latestDeaths = new ArrayList<>();
  private float lastScanTime = -SCAN_INTERVAL;
  private int generation = 0;

  private ShipDeathTracker(CombatEngineAPI engine) {
    this.engineRef = new java.lang.ref.WeakReference<>(engine);
  }

  static ShipDeathTracker get(CombatEngineAPI engine) {
    if (current == null || current.engineRef.get() != engine) {
      current = new ShipDeathTracker(engine);
    }
    return current;
  }

  int getGeneration() {
    return generation;
  }

  List<DeathEvent> update(CombatEngineAPI engine) {
    float now = engine.getTotalElapsedTime(false);
    if (now - lastScanTime >= SCAN_INTERVAL) {
      lastScanTime = now;
      scan(engine);
      generation++;
    }
    return latestDeaths;
  }

  private void scan(CombatEngineAPI engine) {
    latestDeaths.clear();
    Map<String, DeathEvent> currentShips = new HashMap<>();
    for (ShipAPI ship : engine.getShips()) {
      if (!ship.isAlive() || ship.isHulk()) {
        continue;
      }
      currentShips.put(ship.getId(),
          new DeathEvent(ship.getOwner(), new Vector2f(ship.getLocation()), ship.isFighter()));
    }
    for (Map.Entry<String, DeathEvent> entry : alive.entrySet()) {
      if (!currentShips.containsKey(entry.getKey())) {
        latestDeaths.add(entry.getValue());
      }
    }
    alive.clear();
    alive.putAll(currentShips);
  }
}

class NullFrameInst extends GachaOnlyInstruction {
  private static final float DAMAGE_THRESHOLD = 500f;
  private static final float COOLDOWN = 20f;

  public NullFrameInst() {
    super("null_frame", 10, 10, Group.SPECIAL);
  }

  private static class NullFrameListener implements DamageTakenModifier, AdvanceableListener {
    private final ShipAPI ship;
    private float cooldown = 0f;

    NullFrameListener(ShipAPI ship) {
      this.ship = ship;
    }

    @Override
    public void advance(float amount) {
      if (cooldown > 0f) {
        cooldown -= amount;
      }
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point,
        boolean shieldHit) {
      if (cooldown > 0f || hitDamage(damage) <= DAMAGE_THRESHOLD) {
        return null;
      }
      cooldown = COOLDOWN;
      String key = "liminal_null_frame_" + ship.getId();
      damage.getModifier().modifyMult(key, 0f);
      Global.getCombatEngine().addNebulaSmokeParticle(point, ship.getVelocity(), 120f, 2f, 0.7f, 0.9f, 1.6f,
          LGlobals.liminalityColor);
      Global.getSoundPlayer().playSound("liminal_damage_cap", 1f, 1f, ship.getLocation(), ship.getVelocity());
      return key;
    }
  }

  @Override
  public boolean hasApplyEffectsAfterShipCreation() {
    return true;
  }

  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id, float levelMultiplier) {
    if (!ship.hasListenerOfClass(NullFrameListener.class)) {
      ship.addListener(new NullFrameListener(ship));
    }
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), t(DAMAGE_THRESHOLD), t(COOLDOWN));
  }
}

class EmergencyPurgeInst extends GachaOnlyInstruction {
  private static final float VENT_FRACTION = 0.20f;
  private static final float TRIGGER_LEVEL = 0.90f;
  private static final float COOLDOWN = 15f;

  private static class PurgeState {
    float cooldown = 0f;
  }

  private final Map<ShipAPI, PurgeState> states = new WeakHashMap<>();

  public EmergencyPurgeInst() {
    super("emergency_purge", 10, 10, Group.SPECIAL);
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount, float levelMultiplier) {
    if (!ship.isAlive()) {
      return;
    }
    PurgeState state = states.get(ship);
    if (state == null) {
      state = new PurgeState();
      states.put(ship, state);
    }
    if (state.cooldown > 0f) {
      state.cooldown -= amount;
      return;
    }
    FluxTrackerAPI flux = ship.getFluxTracker();
    if (flux.isOverloaded() || flux.getFluxLevel() < TRIGGER_LEVEL) {
      return;
    }
    float vent = VENT_FRACTION * flux.getMaxFlux();
    flux.setHardFlux(Math.max(0f, flux.getHardFlux() - vent));
    flux.setCurrFlux(Math.max(0f, flux.getCurrFlux() - vent));
    state.cooldown = COOLDOWN;
    Global.getCombatEngine().addNebulaSmokeParticle(ship.getLocation(), ship.getVelocity(), 100f, 2f, 0.7f, 0.9f,
        1.5f, LGlobals.liminalityColor);
  }

  @Override
  public boolean hasAdvanceInCombat() {
    return true;
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), tP(VENT_FRACTION), tP(TRIGGER_LEVEL), t(COOLDOWN));
  }
}

class MirrorSkinInst extends GachaOnlyInstruction {
  private static final float REFLECT_FRACTION = 0.05f;

  public MirrorSkinInst() {
    super("mirror_skin", 10, 10, Group.SPECIAL);
  }

  private static class MirrorSkinListener implements DamageTakenModifier {
    private final ShipAPI ship;

    MirrorSkinListener(ShipAPI ship) {
      this.ship = ship;
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point,
        boolean shieldHit) {
      if (!shieldHit) {
        return null;
      }
      ShipAPI source = null;
      if (param instanceof DamagingProjectileAPI) {
        source = ((DamagingProjectileAPI) param).getSource();
      } else if (param instanceof BeamAPI) {
        source = ((BeamAPI) param).getSource();
      }
      if (source == null || !source.isAlive() || source == ship) {
        return null;
      }
      float reflected = hitDamage(damage) * REFLECT_FRACTION;
      if (reflected <= 0f) {
        return null;
      }
      if (source.getHitpoints() - reflected <= 1f) {
        Global.getCombatEngine().applyDamage(source, source.getLocation(), reflected * 10f, DamageType.ENERGY, 0f,
            true, false, ship);
      } else {
        source.setHitpoints(source.getHitpoints() - reflected);
      }
      return null;
    }
  }

  @Override
  public boolean hasApplyEffectsAfterShipCreation() {
    return true;
  }

  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id, float levelMultiplier) {
    if (!ship.hasListenerOfClass(MirrorSkinListener.class)) {
      ship.addListener(new MirrorSkinListener(ship));
    }
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(),
        padding, Misc.getHighlightColor(), tP(REFLECT_FRACTION));
  }
}

class LastRitesInst extends GachaOnlyInstruction {
  private static final float VENT_FRACTION = 0.30f;
  private static final float RANGE = 1000f;

  private static class RitesState {
    int lastGeneration = -1;
  }

  private final Map<ShipAPI, RitesState> states = new WeakHashMap<>();

  public LastRitesInst() {
    super("last_rites", 10, 10, Group.SPECIAL);
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount, float levelMultiplier) {
    if (!ship.isAlive() || ship.isHulk()) {
      return;
    }
    CombatEngineAPI engine = Global.getCombatEngine();
    ShipDeathTracker tracker = ShipDeathTracker.get(engine);
    List<ShipDeathTracker.DeathEvent> deaths = tracker.update(engine);
    RitesState state = states.get(ship);
    if (state == null) {
      state = new RitesState();
      states.put(ship, state);
    }
    if (tracker.getGeneration() == state.lastGeneration) {
      return;
    }
    state.lastGeneration = tracker.getGeneration();
    for (ShipDeathTracker.DeathEvent death : deaths) {
      if (death.fighter || death.owner == ship.getOwner()) {
        continue;
      }
      if (Misc.getDistance(death.location, ship.getLocation()) > RANGE) {
        continue;
      }
      FluxTrackerAPI flux = ship.getFluxTracker();
      float vent = VENT_FRACTION * flux.getCurrFlux();
      if (vent <= 0f) {
        continue;
      }
      flux.setHardFlux(flux.getHardFlux() - Math.min(flux.getHardFlux(), vent));
      flux.setCurrFlux(Math.max(0f, flux.getCurrFlux() - vent));
      engine.addNebulaSmokeParticle(ship.getLocation(), ship.getVelocity(), 100f, 2f, 0.7f, 0.9f, 1.5f,
          LGlobals.liminalityColor);
    }
  }

  @Override
  public boolean hasAdvanceInCombat() {
    return true;
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(),
        padding, Misc.getHighlightColor(), t(RANGE), tP(VENT_FRACTION));
  }
}

class SympatheticOverloadInst extends GachaOnlyInstruction {
  private static final float HARD_FLUX_FRACTION = 0.30f;
  private static final float RANGE = 500f;

  private static class OverloadState {
    boolean wasOverloaded = false;
  }

  private final Map<ShipAPI, OverloadState> states = new WeakHashMap<>();

  public SympatheticOverloadInst() {
    super("sympathetic_overload", 10, 10, Group.SPECIAL);
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount, float levelMultiplier) {
    if (!ship.isAlive()) {
      return;
    }
    OverloadState state = states.get(ship);
    if (state == null) {
      state = new OverloadState();
      states.put(ship, state);
    }
    boolean overloaded = ship.getFluxTracker().isOverloaded();
    boolean justOverloaded = overloaded && !state.wasOverloaded;
    state.wasOverloaded = overloaded;
    if (!justOverloaded) {
      return;
    }
    CombatEngineAPI engine = Global.getCombatEngine();
    for (ShipAPI other : engine.getShips()) {
      if (other == ship || !other.isAlive() || other.isHulk() || other.getOwner() == 100) {
        continue;
      }
      if (Misc.getDistance(ship.getLocation(), other.getLocation()) > RANGE) {
        continue;
      }
      FluxTrackerAPI flux = other.getFluxTracker();
      flux.increaseFlux(HARD_FLUX_FRACTION * flux.getMaxFlux(), true);
      engine.spawnEmpArcVisual(ship.getLocation(), ship, other.getLocation(), other, 20f,
          LGlobals.liminalityColor, Color.WHITE);
    }
  }

  @Override
  public boolean hasAdvanceInCombat() {
    return true;
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), t(RANGE), tP(HARD_FLUX_FRACTION));
  }
}

class PartingGiftInst extends GachaOnlyInstruction {
  private static final float HULL_DAMAGE_FRACTION = 0.50f;
  private static final float RANGE = 600f;
  private static final float DEATH_CHECK_INTERVAL = 0.5f;

  private static class TrackedFighter {
    final ShipAPI fighter;
    final float maxHull;

    TrackedFighter(ShipAPI fighter) {
      this.fighter = fighter;
      this.maxHull = fighter.getMaxHitpoints();
    }
  }

  private static class GiftState {
    final IntervalUtil deathCheck = new IntervalUtil(DEATH_CHECK_INTERVAL, DEATH_CHECK_INTERVAL);
    final Map<String, TrackedFighter> tracked = new HashMap<>();
  }

  private final Map<ShipAPI, GiftState> states = new WeakHashMap<>();
  private java.lang.ref.WeakReference<CombatEngineAPI> statesEngine =
      new java.lang.ref.WeakReference<>(null);

  public PartingGiftInst() {
    super("parting_gift", 10, 10, Group.SPECIAL);
  }

  private GiftState stateFor(ShipAPI ship) {
    CombatEngineAPI engine = Global.getCombatEngine();
    if (statesEngine.get() != engine) {
      states.clear();
      statesEngine = new java.lang.ref.WeakReference<>(engine);
    }
    GiftState state = states.get(ship);
    if (state == null) {
      state = new GiftState();
      states.put(ship, state);
    }
    return state;
  }

  void forgetBattle() {
    states.clear();
  }

  @Override
  public boolean hasApplyEffectsToFighterSpawnedByShip() {
    return true;
  }

  @Override
  public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id, float levelMultiplier) {
    stateFor(ship).tracked.put(fighter.getId(), new TrackedFighter(fighter));
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount, float levelMultiplier) {
    GiftState state = stateFor(ship);
    if (state.tracked.isEmpty()) {
      return;
    }
    state.deathCheck.advance(amount);
    if (!state.deathCheck.intervalElapsed()) {
      return;
    }
    CombatEngineAPI engine = Global.getCombatEngine();
    List<String> gone = new ArrayList<>();
    for (Map.Entry<String, TrackedFighter> entry : state.tracked.entrySet()) {
      TrackedFighter tracked = entry.getValue();
      if (tracked.fighter.isAlive() && !tracked.fighter.isHulk()) {
        if (!engine.isEntityInPlay(tracked.fighter)) {
          gone.add(entry.getKey());
        }
        continue;
      }
      gone.add(entry.getKey());
      detonate(engine, ship, tracked);
    }
    for (String id : gone) {
      state.tracked.remove(id);
    }
  }

  private void detonate(CombatEngineAPI engine, ShipAPI ship, TrackedFighter tracked) {
    Vector2f location = new Vector2f(tracked.fighter.getLocation());
    List<ShipAPI> candidates = new ArrayList<>();
    for (ShipAPI other : engine.getShips()) {
      if (!other.isAlive() || other.isHulk() || other.getOwner() == ship.getOwner() || other.getOwner() == 100) {
        continue;
      }
      if (Misc.getDistance(location, other.getLocation()) > RANGE) {
        continue;
      }
      candidates.add(other);
    }
    if (candidates.isEmpty()) {
      return;
    }
    ShipAPI target = candidates.get(Misc.random.nextInt(candidates.size()));
    float damage = HULL_DAMAGE_FRACTION * tracked.maxHull;
    engine.spawnEmpArc(ship, location, null, target, DamageType.ENERGY, damage, damage, RANGE + 100f,
        "tachyon_lance_emp_impact", 20f, LGlobals.liminalityColor, Color.WHITE);
  }

  @Override
  public boolean hasAdvanceInCombat() {
    return true;
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(),
        padding, Misc.getHighlightColor(), tP(HULL_DAMAGE_FRACTION), t(RANGE));
  }
}

class ResidualPlatingInst extends GachaOnlyInstruction {
  private static final float MIN_ARMOR_BONUS = 0.10f;

  public ResidualPlatingInst() {
    super("residual_plating", 10, 10, Group.SPECIAL);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getMinArmorFraction().modifyFlat(getName(), MIN_ARMOR_BONUS);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    String raised = tP(0.05f + MIN_ARMOR_BONUS);
    LabelAPI para = a.addPara(effectTemplate() + raised
        + Global.getSettings().getString("liminal_instructions", "residual_plating_effect_suffix"),
        padding);
    para.setHighlight(raised);
    para.setHighlightColor(Misc.getHighlightColor());
  }
}

class DyingStarInst extends GachaOnlyInstruction {
  private static final float DAMAGE_MULT = 4f;
  private static final float RADIUS_MULT = 2f;

  public DyingStarInst() {
    super("dying_star", 10, 10, Group.SPECIAL);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getDynamic().getStat(Stats.EXPLOSION_DAMAGE_MULT).modifyMult(getName(), DAMAGE_MULT);
    stats.getDynamic().getStat(Stats.EXPLOSION_RADIUS_MULT).modifyMult(getName(), RADIUS_MULT);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), tP(DAMAGE_MULT - 1f), tP(RADIUS_MULT - 1f));
  }
}

class ProwAegisInst extends GachaOnlyInstruction {
  private static final float DAMAGE_REDUCTION = 0.40f;
  private static final float ARC = 60f;

  public ProwAegisInst() {
    super("prow_aegis", 10, 10, Group.SPECIAL);
  }

  private static class ProwAegisListener implements DamageTakenModifier {
    private final ShipAPI ship;

    ProwAegisListener(ShipAPI ship) {
      this.ship = ship;
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point,
        boolean shieldHit) {
      if (shieldHit) {
        return null;
      }
      float angleToHit = Misc.getAngleInDegrees(ship.getLocation(), point);
      if (Misc.getAngleDiff(angleToHit, ship.getFacing()) > ARC / 2f) {
        return null;
      }
      String key = "liminal_prow_aegis_" + ship.getId();
      damage.getModifier().modifyMult(key, 1f - DAMAGE_REDUCTION);
      return key;
    }
  }

  @Override
  public boolean hasApplyEffectsAfterShipCreation() {
    return true;
  }

  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id, float levelMultiplier) {
    if (!ship.hasListenerOfClass(ProwAegisListener.class)) {
      ship.addListener(new ProwAegisListener(ship));
    }
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), tP(DAMAGE_REDUCTION), t(ARC));
  }
}

class LoneHunterInst extends GachaOnlyInstruction {
  private static final float DAMAGE_BONUS = 0.40f;
  private static final float CHECK_INTERVAL = 1f;

  private static class HunterState {
    final IntervalUtil checkInterval = new IntervalUtil(CHECK_INTERVAL, CHECK_INTERVAL);
  }

  private final Map<ShipAPI, HunterState> states = new WeakHashMap<>();

  public LoneHunterInst() {
    super("lone_hunter", 10, 10, Group.SPECIAL);
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount, float levelMultiplier) {
    if (!ship.isAlive()) {
      return;
    }
    HunterState state = states.get(ship);
    if (state == null) {
      state = new HunterState();
      states.put(ship, state);
    }
    state.checkInterval.advance(amount);
    if (!state.checkInterval.intervalElapsed()) {
      return;
    }
    int allies = 0;
    for (ShipAPI other : Global.getCombatEngine().getShips()) {
      if (!other.isAlive() || other.isHulk() || other.isFighter()) {
        continue;
      }
      if (other.getOwner() == ship.getOwner()) {
        allies++;
      }
    }
    MutableShipStatsAPI stats = ship.getMutableStats();
    if (allies == 1) {
      stats.getBallisticWeaponDamageMult().modifyPercent(getName(), DAMAGE_BONUS * 100f);
      stats.getEnergyWeaponDamageMult().modifyPercent(getName(), DAMAGE_BONUS * 100f);
      stats.getMissileWeaponDamageMult().modifyPercent(getName(), DAMAGE_BONUS * 100f);
    } else {
      stats.getBallisticWeaponDamageMult().unmodify(getName());
      stats.getEnergyWeaponDamageMult().unmodify(getName());
      stats.getMissileWeaponDamageMult().unmodify(getName());
    }
  }

  @Override
  public boolean hasAdvanceInCombat() {
    return true;
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), tP(DAMAGE_BONUS));
  }
}

class PredationFeedbackInst extends GachaOnlyInstruction {
  private static final float RESTORE_FRACTION = 0.10f;
  private static final float RANGE = 1000f;

  private static class FeedbackState {
    int lastGeneration = -1;
  }

  private final Map<ShipAPI, FeedbackState> states = new WeakHashMap<>();

  public PredationFeedbackInst() {
    super("predation_feedback", 10, 10, Group.SPECIAL);
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount, float levelMultiplier) {
    if (!ship.isAlive() || ship.isHulk()) {
      return;
    }
    CombatEngineAPI engine = Global.getCombatEngine();
    ShipDeathTracker tracker = ShipDeathTracker.get(engine);
    List<ShipDeathTracker.DeathEvent> deaths = tracker.update(engine);
    FeedbackState state = states.get(ship);
    if (state == null) {
      state = new FeedbackState();
      states.put(ship, state);
    }
    if (tracker.getGeneration() == state.lastGeneration) {
      return;
    }
    state.lastGeneration = tracker.getGeneration();
    for (ShipDeathTracker.DeathEvent death : deaths) {
      if (death.fighter || death.owner == ship.getOwner()) {
        continue;
      }
      if (Misc.getDistance(death.location, ship.getLocation()) > RANGE) {
        continue;
      }
      ship.setHitpoints(Math.min(ship.getMaxHitpoints(),
          ship.getHitpoints() + RESTORE_FRACTION * ship.getMaxHitpoints()));
      float maxArmorInCell = ship.getArmorGrid().getMaxArmorInCell();
      float[][] grid = ship.getArmorGrid().getGrid();
      for (int x = 0; x < grid.length; x++) {
        for (int y = 0; y < grid[0].length; y++) {
          if (grid[x][y] < maxArmorInCell) {
            ship.getArmorGrid().setArmorValue(x, y,
                Math.min(maxArmorInCell, grid[x][y] + RESTORE_FRACTION * maxArmorInCell));
          }
        }
      }
      ship.syncWithArmorGridState();
      engine.addNebulaSmokeParticle(ship.getLocation(), ship.getVelocity(), 100f, 2f, 0.7f, 0.9f, 1.5f,
          LGlobals.liminalityColor);
    }
  }

  @Override
  public boolean hasAdvanceInCombat() {
    return true;
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), t(RANGE), tP(RESTORE_FRACTION));
  }
}

class MineFabricatorInst extends GachaOnlyInstruction {
  private static final float INTERVAL = 20f;
  private static final float SEARCH_RANGE = 2000f;
  private static final float LIVE_TIME = 5f;

  private static class FabricatorState {
    final IntervalUtil interval = new IntervalUtil(INTERVAL, INTERVAL);
  }

  private final Map<ShipAPI, FabricatorState> states = new WeakHashMap<>();

  public MineFabricatorInst() {
    super("mine_fabricator", 10, 10, Group.SPECIAL);
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount, float levelMultiplier) {
    if (!ship.isAlive()) {
      return;
    }
    FabricatorState state = states.get(ship);
    if (state == null) {
      state = new FabricatorState();
      states.put(ship, state);
    }
    state.interval.advance(amount);
    if (!state.interval.intervalElapsed()) {
      return;
    }
    CombatEngineAPI engine = Global.getCombatEngine();
    ShipAPI target = null;
    float closest = SEARCH_RANGE;
    for (ShipAPI other : engine.getShips()) {
      if (!other.isAlive() || other.isHulk() || other.isFighter()) {
        continue;
      }
      if (other.getOwner() == ship.getOwner() || other.getOwner() == 100) {
        continue;
      }
      float distance = Misc.getDistance(ship.getLocation(), other.getLocation());
      if (distance <= closest) {
        closest = distance;
        target = other;
      }
    }
    if (target == null) {
      return;
    }
    spawnMine(engine, ship, target);
  }

  private void spawnMine(CombatEngineAPI engine, ShipAPI ship, ShipAPI target) {
    Vector2f location = Misc.getPointAtRadius(target.getLocation(), 100f + Misc.random.nextFloat() * 60f);
    MissileAPI mine = (MissileAPI) engine.spawnProjectile(ship, null, "minelayer2", location,
        Misc.random.nextFloat() * 360f, null);
    engine.applyDamageModifiersToSpawnedProjectileWithNullWeapon(ship, WeaponType.MISSILE, false, mine.getDamage());
    mine.getVelocity().scale(0);
    mine.fadeOutThenIn(0.5f);
    mine.setFlightTime(mine.getMaxFlightTime() - LIVE_TIME);
    Global.getSoundPlayer().playSound("mine_teleport", 1f, 1f, mine.getLocation(), mine.getVelocity());
  }

  @Override
  public boolean hasAdvanceInCombat() {
    return true;
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), t(INTERVAL), t(SEARCH_RANGE));
  }
}

class DuctTapeLiturgyInst extends GachaOnlyInstruction {
  private static final float MALFUNCTION_REDUCTION = 0.75f;

  public DuctTapeLiturgyInst() {
    super("duct_tape_liturgy", 10, 10, Group.SPECIAL);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getWeaponMalfunctionChance().modifyMult(getName(), 1f - MALFUNCTION_REDUCTION);
    stats.getEngineMalfunctionChance().modifyMult(getName(), 1f - MALFUNCTION_REDUCTION);
    stats.getCriticalMalfunctionChance().modifyMult(getName(), 1f - MALFUNCTION_REDUCTION);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), tP(MALFUNCTION_REDUCTION));
  }
}

class OpeningStatementInst extends GachaOnlyInstruction {
  private static final float DAMAGE_BONUS = 0.50f;
  private static final float DURATION = 15f;

  private static class OpeningState {
    boolean fired = false;
    boolean expired = false;
    float elapsed = 0f;
  }

  private final Map<ShipAPI, OpeningState> states = new WeakHashMap<>();

  public OpeningStatementInst() {
    super("opening_statement", 10, 10, Group.SPECIAL);
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount, float levelMultiplier) {
    if (!ship.isAlive()) {
      return;
    }
    OpeningState state = states.get(ship);
    if (state == null) {
      state = new OpeningState();
      states.put(ship, state);
    }
    if (state.expired) {
      return;
    }
    MutableShipStatsAPI stats = ship.getMutableStats();
    if (!state.fired) {
      for (WeaponAPI weapon : ship.getAllWeapons()) {
        if (weapon.getType() == WeaponType.DECORATIVE) {
          continue;
        }
        if (weapon.isFiring()) {
          state.fired = true;
          break;
        }
      }
      if (state.fired) {
        stats.getBallisticWeaponDamageMult().modifyPercent(getName(), DAMAGE_BONUS * 100f);
        stats.getEnergyWeaponDamageMult().modifyPercent(getName(), DAMAGE_BONUS * 100f);
        stats.getMissileWeaponDamageMult().modifyPercent(getName(), DAMAGE_BONUS * 100f);
      }
      return;
    }
    state.elapsed += amount;
    if (state.elapsed >= DURATION) {
      state.expired = true;
      stats.getBallisticWeaponDamageMult().unmodify(getName());
      stats.getEnergyWeaponDamageMult().unmodify(getName());
      stats.getMissileWeaponDamageMult().unmodify(getName());
    }
  }

  @Override
  public boolean hasAdvanceInCombat() {
    return true;
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), tP(DAMAGE_BONUS), t(DURATION));
  }
}

class BunkerReflexInst extends GachaOnlyInstruction {
  private static final float DAMAGE_REDUCTION = 0.30f;

  public BunkerReflexInst() {
    super("bunker_reflex", 10, 10, Group.SPECIAL);
  }

  private static class BunkerReflexListener implements DamageTakenModifier {
    private final ShipAPI ship;

    BunkerReflexListener(ShipAPI ship) {
      this.ship = ship;
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point,
        boolean shieldHit) {
      if (!ship.getFluxTracker().isOverloadedOrVenting()) {
        return null;
      }
      String key = "liminal_bunker_reflex_" + ship.getId();
      damage.getModifier().modifyMult(key, 1f - DAMAGE_REDUCTION);
      return key;
    }
  }

  @Override
  public boolean hasApplyEffectsAfterShipCreation() {
    return true;
  }

  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id, float levelMultiplier) {
    if (!ship.hasListenerOfClass(BunkerReflexListener.class)) {
      ship.addListener(new BunkerReflexListener(ship));
    }
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), tP(DAMAGE_REDUCTION));
  }
}

class FaradayCageInst extends GachaOnlyInstruction {
  private static final float EMP_REDUCTION = 0.80f;

  public FaradayCageInst() {
    super("faraday_cage", 10, 10, Group.SPECIAL);
  }

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id,
      float levelMultiplier) {
    stats.getEmpDamageTakenMult().modifyMult(getName(), 1f - EMP_REDUCTION);
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding, Misc.getHighlightColor(), tP(EMP_REDUCTION));
  }
}

class HemovoreArrayInst extends GachaOnlyInstruction {
  private static final float HEAL_FRACTION = 0.30f;

  public HemovoreArrayInst() {
    super("hemovore_array", 10, 10, Group.SPECIAL);
  }

  private static class HemovoreListener implements com.fs.starfarer.api.combat.listeners.DamageListener {
    private final ShipAPI ship;

    HemovoreListener(ShipAPI ship) {
      this.ship = ship;
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target,
        com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI result) {
      if (!ship.isAlive() || result.getDamageToHull() <= 0f) {
        return;
      }
      ShipAPI dealer = resolveSource(source);
      if (dealer != ship) {
        return;
      }
      if (!(target instanceof ShipAPI)) {
        return;
      }
      ShipAPI victim = (ShipAPI) target;
      if (victim.isHulk() || victim.getOwner() == ship.getOwner() || victim.getOwner() == 100) {
        return;
      }
      float heal = HEAL_FRACTION * result.getDamageToHull();
      ship.setHitpoints(Math.min(ship.getMaxHitpoints(), ship.getHitpoints() + heal));
    }

    private ShipAPI resolveSource(Object source) {
      if (source instanceof ShipAPI) {
        return (ShipAPI) source;
      }
      if (source instanceof DamagingProjectileAPI) {
        return ((DamagingProjectileAPI) source).getSource();
      }
      if (source instanceof BeamAPI) {
        return ((BeamAPI) source).getSource();
      }
      return null;
    }
  }

  @Override
  public boolean hasApplyEffectsAfterShipCreation() {
    return true;
  }

  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id, float levelMultiplier) {
    CombatEngineAPI engine = Global.getCombatEngine();
    if (engine == null) {
      return;
    }
    for (HemovoreListener listener : engine.getListenerManager().getListeners(HemovoreListener.class)) {
      if (listener.ship == ship) {
        return;
      }
    }
    engine.getListenerManager().addListener(new HemovoreListener(ship));
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), tP(HEAL_FRACTION));
  }
}

class CoupDeGraceInst extends GachaOnlyInstruction {
  private static final float DAMAGE_BONUS = 0.50f;
  private static final float HULL_THRESHOLD = 0.30f;

  public CoupDeGraceInst() {
    super("coup_de_grace", 10, 10, Group.SPECIAL);
  }

  private static class CoupDeGraceListener implements com.fs.starfarer.api.combat.listeners.DamageDealtModifier {
    private final ShipAPI ship;

    CoupDeGraceListener(ShipAPI ship) {
      this.ship = ship;
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point,
        boolean shieldHit) {
      if (!(target instanceof ShipAPI)) {
        return null;
      }
      ShipAPI victim = (ShipAPI) target;
      if (!victim.isAlive() || victim.isHulk() || victim.getOwner() == ship.getOwner()) {
        return null;
      }
      if (victim.getHullLevel() >= HULL_THRESHOLD) {
        return null;
      }
      String key = "liminal_coup_de_grace_" + ship.getId();
      damage.getModifier().modifyMult(key, 1f + DAMAGE_BONUS);
      return key;
    }
  }

  @Override
  public boolean hasApplyEffectsAfterShipCreation() {
    return true;
  }

  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id, float levelMultiplier) {
    if (!ship.hasListenerOfClass(CoupDeGraceListener.class)) {
      ship.addListener(new CoupDeGraceListener(ship));
    }
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), tP(DAMAGE_BONUS), tP(HULL_THRESHOLD));
  }
}

class LeechCouplingInst extends GachaOnlyInstruction {
  private static final float TRANSFER_FRACTION_PER_SECOND = 0.10f;
  private static final float RANGE = 500f;
  private static final float TICK_INTERVAL = 0.5f;

  private static class LeechState {
    float sinceTransfer = 0f;
  }

  private final Map<ShipAPI, LeechState> states = new WeakHashMap<>();

  public LeechCouplingInst() {
    super("leech_coupling", 10, 10, Group.SPECIAL);
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount, float levelMultiplier) {
    if (!ship.isAlive()) {
      return;
    }
    LeechState state = states.get(ship);
    if (state == null) {
      state = new LeechState();
      states.put(ship, state);
    }
    state.sinceTransfer += amount;
    if (state.sinceTransfer < TICK_INTERVAL) {
      return;
    }
    float elapsed = state.sinceTransfer;
    state.sinceTransfer = 0f;
    FluxTrackerAPI flux = ship.getFluxTracker();
    if (flux.getHardFlux() <= 0f) {
      return;
    }
    ShipAPI ally = null;
    float closest = RANGE;
    for (ShipAPI other : Global.getCombatEngine().getShips()) {
      if (other == ship || !other.isAlive() || other.isHulk() || other.isFighter()) {
        continue;
      }
      if (other.getOwner() != ship.getOwner()) {
        continue;
      }
      if (other.getFluxTracker().isOverloaded()) {
        continue;
      }
      float distance = Misc.getDistance(ship.getLocation(), other.getLocation());
      if (distance <= closest) {
        closest = distance;
        ally = other;
      }
    }
    if (ally == null) {
      return;
    }
    float transfer = Math.min(flux.getHardFlux(), flux.getHardFlux() * TRANSFER_FRACTION_PER_SECOND * elapsed);
    if (transfer <= 0f) {
      return;
    }
    flux.setHardFlux(flux.getHardFlux() - transfer);
    flux.setCurrFlux(Math.max(0f, flux.getCurrFlux() - transfer));
    ally.getFluxTracker().increaseFlux(transfer, true);
  }

  @Override
  public boolean hasAdvanceInCombat() {
    return true;
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(),
        padding, Misc.getHighlightColor(), tP(TRANSFER_FRACTION_PER_SECOND), t(RANGE));
  }
}

class HeadsOrTailsInst extends GachaOnlyInstruction {
  private static final float INTERVAL = 10f;
  private static final float FLUX_AMOUNT = 500f;

  private static class CoinState {
    final IntervalUtil interval = new IntervalUtil(INTERVAL, INTERVAL);
  }

  private final Map<ShipAPI, CoinState> states = new WeakHashMap<>();

  public HeadsOrTailsInst() {
    super("heads_or_tails", 10, 10, Group.SPECIAL);
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount, float levelMultiplier) {
    if (!ship.isAlive()) {
      return;
    }
    CoinState state = states.get(ship);
    if (state == null) {
      state = new CoinState();
      states.put(ship, state);
    }
    state.interval.advance(amount);
    if (!state.interval.intervalElapsed()) {
      return;
    }
    FluxTrackerAPI flux = ship.getFluxTracker();
    if (Misc.random.nextBoolean()) {
      flux.setHardFlux(Math.max(0f, flux.getHardFlux() - FLUX_AMOUNT));
      flux.setCurrFlux(Math.max(0f, flux.getCurrFlux() - FLUX_AMOUNT));
    } else {
      flux.increaseFlux(FLUX_AMOUNT, true);
    }
  }

  @Override
  public boolean hasAdvanceInCombat() {
    return true;
  }

  @Override
  public void getEffectDescription(TooltipMakerAPI a, float padding, float levelMultiplier) {
    a.addPara(effectTemplate(), padding,
        Misc.getHighlightColor(), t(INTERVAL), t(FLUX_AMOUNT), t(FLUX_AMOUNT));
  }
}

package liminal.faction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.Pair;

public class Autofit {

  protected static Set<String> bannedBossHulls;

  protected static Set<String> getBannedBossHulls() {
    if (bannedBossHulls == null) {
      bannedBossHulls = new HashSet<>();
      try {
        JSONArray config = Global.getSettings().getMergedSpreadsheetDataForMod("id",
            "data/config/liminal/banned_hulls.csv", "liminal");
        for (int i = 0; i < config.length(); i++) {
          JSONObject row = config.getJSONObject(i);
          String id = row.getString("id");
          bannedBossHulls.add(id);
        }
      } catch (Exception e) {
        Global.getLogger(LUtils.class).error("Error getting banned boss hulls", e);
      }
    }
    return bannedBossHulls;
  }

  protected static Map<HullSize, List<ShipVariantAPI>> shipsOfSize;

  private static void loadShip() {
    shipsOfSize = new HashMap<>();
    for (HullSize hullSize : HullSize.values()) {
      shipsOfSize.put(hullSize, getShipsOfSizeFillCache(hullSize));
    }
  }

  private static List<ShipVariantAPI> getShipsOfSizeFillCache(HullSize hullSize) {
    List<ShipVariantAPI> ships = new ArrayList<>();
    for (String variantId : Global.getSettings().getAllVariantIds()) {
      ShipVariantAPI variant = Global.getSettings().getVariant(variantId);
      Set<ShipTypeHints> hints = variant.getHints();
      if (hints.contains(ShipTypeHints.UNBOARDABLE) || hints.contains(ShipTypeHints.STATION)
          || hints.contains(ShipTypeHints.HIDE_IN_CODEX) || hints.contains(ShipTypeHints.SHIP_WITH_MODULES)) {
        continue;
      }
      Collection<String> tags = variant.getTags();
      if (tags.contains(Tags.UNRECOVERABLE) || tags.contains(Tags.RESTRICTED) || tags.contains(Tags.NO_DROP)
          || tags.contains(Tags.NO_BP_DROP) || tags.contains(Tags.HULLMOD_NO_DROP_SALVAGE)) {
        continue;
      }
      if (variant.getNonBuiltInWeaponSlots().size() < 5) {
        continue;
      }
      if (getBannedBossHulls().contains(variant.getHullVariantId())) {
        continue;
      }
      if (variant.getHullSize().equals(hullSize)) {
        ships.add(variant);
      }
    }
    return ships;
  }

  private static List<ShipVariantAPI> getShipsOfSize(HullSize hullSize) {
    if (shipsOfSize == null) {
      loadShip();
    }
    return shipsOfSize.get(hullSize);
  }

  protected static Map<WeaponSize, Map<WeaponType, List<WeaponSpecAPI>>> allWeapons;
  protected static Map<WeaponSize, Map<WeaponType, List<WeaponSpecAPI>>> allWeaponsForBoss;

  private static void loadAllWeapons() {
    allWeapons = new HashMap<>();
    for (WeaponSize weaponSize : WeaponSize.values()) {
      allWeapons.put(weaponSize, new HashMap<WeaponType, List<WeaponSpecAPI>>());
      for (WeaponType weaponType : WeaponType.values()) {
        allWeapons.get(weaponSize).put(weaponType, getWeaponsOfSizeFillCache(weaponSize, weaponType, false));
      }
    }

    allWeaponsForBoss = new HashMap<>();
    for (WeaponSize weaponSize : WeaponSize.values()) {
      allWeaponsForBoss.put(weaponSize, new HashMap<WeaponType, List<WeaponSpecAPI>>());
      for (WeaponType weaponType : WeaponType.values()) {
        allWeaponsForBoss.get(weaponSize).put(weaponType, getWeaponsOfSizeFillCache(weaponSize, weaponType, true));
      }
    }
  }

  private static List<WeaponSpecAPI> getWeaponsOfSizeFillCache(WeaponSize weaponSize, WeaponType weaponType,
      boolean forBoss) {
    List<WeaponSpecAPI> weapons = new ArrayList<>();
    for (WeaponSpecAPI weapon : Global.getSettings().getAllWeaponSpecs()) {
      if (weapon.getSize().equals(weaponSize)) {
        Set<AIHints> hints = weapon.getAIHints();
        if (hints.contains(AIHints.SYSTEM) || hints.contains(AIHints.ANTI_FTR)
            || hints.contains(AIHints.USE_VS_FRIGATES) || hints.contains(AIHints.MISSILE_SPREAD)) {
          continue;
        }
        if (forBoss && (hints.contains(AIHints.ANTI_FTR)
            || hints.contains(AIHints.USE_VS_FRIGATES) || hints.contains(AIHints.MISSILE_SPREAD))) {
          continue;
        }
        Collection<String> tags = weapon.getTags();
        if (tags.contains(Tags.UNRECOVERABLE) || tags.contains(Tags.RESTRICTED) || tags.contains(Tags.NO_DROP)
            || tags.contains(Tags.NO_BP_DROP) || tags.contains(Tags.HULLMOD_NO_DROP_SALVAGE)) {
          continue;
        }
        WeaponType type = weapon.getType();
        if (type.equals(WeaponType.LAUNCH_BAY) || type.equals(WeaponType.BUILT_IN)
            || type.equals(WeaponType.DECORATIVE) || type.equals(WeaponType.SYSTEM)
            || type.equals(WeaponType.STATION_MODULE)) {
          continue;
        }
        if (weapon.getOrdnancePointCost(null) > 60) {
          continue;
        }
        if (forBoss) {
          Object o = weapon.getProjectileSpec();
          if (o instanceof ProjectileWeaponSpecAPI) {
            ProjectileWeaponSpecAPI projectile = (ProjectileWeaponSpecAPI) o;
            if (projectile.getBurstSize() > 2) {
              continue;
            }
          }
        }

        if (weaponType != null) {
          if (type.equals(WeaponType.HYBRID)) {
            if (!weaponType.equals(WeaponType.BALLISTIC) && !weaponType.equals(WeaponType.ENERGY)) {
              continue;
            }
          }
          else if (type.equals(WeaponType.COMPOSITE)) {
            if (!weaponType.equals(WeaponType.BALLISTIC) && !weaponType.equals(WeaponType.MISSILE)) {
              continue;
            }
          }
          else if (type.equals(WeaponType.SYNERGY)) {
            if (!weaponType.equals(WeaponType.ENERGY) && !weaponType.equals(WeaponType.MISSILE)) {
              continue;
            }
          }
          else if (type.equals(WeaponType.UNIVERSAL)) {
            if (!weaponType.equals(WeaponType.BALLISTIC) && !weaponType.equals(WeaponType.ENERGY)
                && !weaponType.equals(WeaponType.MISSILE)) {
              continue;
            }
          }
          else if (!type.equals(weaponType)) {
            continue;
          }
        }
        weapons.add(weapon);
      }
    }
    return weapons;
  }

  private static List<WeaponSpecAPI> getWeaponsOfSize(WeaponSize weaponSize, WeaponType weaponType, boolean forBoss) {
    if (allWeapons == null) {
      loadAllWeapons();
    }
    return forBoss ? allWeaponsForBoss.get(weaponSize).get(weaponType) : allWeapons.get(weaponSize).get(weaponType);
  }

  private static List<FighterWingSpecAPI> wings;

  private static void loadWings() {
    wings = new ArrayList<>();
    for (FighterWingSpecAPI wing : Global.getSettings().getAllFighterWingSpecs()) {
      Collection<String> tags = wing.getTags();
      if (tags.contains(Tags.HULLMOD_NO_DROP) || tags.contains(Tags.RESTRICTED) || tags.contains(Tags.NO_DROP)
          || tags.contains(Tags.NO_BP_DROP) || tags.contains(Tags.HULLMOD_NO_DROP_SALVAGE)
          || tags.contains(Tags.WING_NO_SELL)) {
        continue;
      }
      if (wing.getOpCost(null) > 60) {
        continue;
      }
      wings.add(wing);
    }
  }

  public static FighterWingSpecAPI getRandomWing() {
    if (wings == null) {
      loadWings();
    }
    return wings.get(random.nextInt(wings.size()));
  }

  private static WeaponSpecAPI getRandomWeapon(List<List<WeaponSpecAPI>> weaponLists, List<Float> probabilities,
      Random random) {
    float roll = random.nextFloat();
    float cumulative = 0f;

    for (int i = 0; i < weaponLists.size() && i < probabilities.size(); i++) {
      cumulative += probabilities.get(i);
      if (roll < cumulative) {
        List<WeaponSpecAPI> weapons = weaponLists.get(i);
        return weapons.isEmpty() ? null : weapons.get(random.nextInt(weapons.size()));
      }
    }

    return null;
  }

  private static WeaponSpecAPI getRandomWeaponOfSize(WeaponSize weaponSize, float skipProbability, boolean forBoss) {
    List<WeaponSpecAPI> ballisticWeapons = getWeaponsOfSize(weaponSize, WeaponType.BALLISTIC, forBoss);
    List<WeaponSpecAPI> energyWeapons = getWeaponsOfSize(weaponSize, WeaponType.ENERGY, forBoss);
    List<WeaponSpecAPI> missileWeapons = getWeaponsOfSize(weaponSize, WeaponType.MISSILE, forBoss);

    return getRandomWeapon(Arrays.asList(ballisticWeapons, energyWeapons, missileWeapons),
        Arrays.asList(0.4f * (1 - skipProbability), 0.4f * (1 - skipProbability), 0.2f * (1 - skipProbability)),
        random);
  }

  private static Map<String, Pair<ShipVariantAPI, ShipVariantAPI>> variantToTarget;

  private static Map<com.fs.starfarer.api.fleet.FleetMemberAPI, ShipVariantAPI> memberOriginals;

  public static void registerMemberOriginal(com.fs.starfarer.api.fleet.FleetMemberAPI member,
      ShipVariantAPI original) {
    if (memberOriginals == null) {
      memberOriginals = new HashMap<>();
    }
    if (!memberOriginals.containsKey(member)) {
      memberOriginals.put(member, original);
    }
  }

  public static void cleanup() {
    if (memberOriginals != null) {
      for (Map.Entry<com.fs.starfarer.api.fleet.FleetMemberAPI, ShipVariantAPI> entry : memberOriginals
          .entrySet()) {
        entry.getKey().setVariant(entry.getValue(), false, false);
      }
      memberOriginals = null;
    }
    if (variantToTarget == null) {
      return;
    }
    for (Map.Entry<String, Pair<ShipVariantAPI, ShipVariantAPI>> entry : variantToTarget.entrySet()) {
      cleanupVariant(entry.getValue().one, entry.getValue().two, entry.getKey());
    }
    variantToTarget = null;
    isFightingBoss = false;
    bossHasTransformed = false;
  }

  public static void resetStaticsForLoad() {
    memberOriginals = null;
    variantToTarget = null;
    isFightingBoss = false;
    bossHasTransformed = false;
  }

  private static void cleanupVariant(ShipVariantAPI source, ShipVariantAPI target, String originalId) {
    for (String weaponSlot : target.getNonBuiltInWeaponSlots()) {
      source.clearSlot(weaponSlot);
    }
    ShipVariantAPI variant = Global.getSettings().getVariant(originalId);
    if (variant == null) {
      return;
    }
    source.setHullSpecAPI(variant.getHullSpec());
    source.setHullVariantId(originalId);
    source.clear();
    source.getHullMods().clear();
    for (String weaponSlot : target.getHullSpec().getBuiltInWeapons().keySet()) {
      source.clearSlot(weaponSlot);
    }
    source.clearWings();
  }

  private static Random random = new Random();

  public static void outfitVariant(ShipVariantAPI source, HullSize forceHullSize, WeaponSize forceWeaponSize,
      boolean fillWings, String[] additionalHullmods, float weaponSkipProbability,
      boolean stripBuiltIn, boolean weaponsForBoss, boolean dontTransformShip) {

    ShipVariantAPI target;
    if (!dontTransformShip) {
      source.clear();
      List<ShipVariantAPI> ships = getShipsOfSize(forceHullSize != null ? forceHullSize : source.getHullSize());
      target = ships.get(random.nextInt(ships.size()));
      if (variantToTarget == null) {
        variantToTarget = new HashMap<>();
      }
      String originalId = source.getHullVariantId();
      if (variantToTarget.containsKey(originalId)) {
        cleanupVariant(source, variantToTarget.get(originalId).one, originalId);
        variantToTarget.remove(originalId);
      }
      variantToTarget.put(originalId, new Pair<>(source, target));

      source.setHullSpecAPI(target.getHullSpec());
      source.setHullVariantId(target.getHullVariantId());
    } else {
      target = source;
    }
    for (String weaponSlot : target.getNonBuiltInWeaponSlots()) {
      WeaponSize size = forceWeaponSize != null ? forceWeaponSize : target.getSlot(weaponSlot).getSlotSize();
      WeaponSpecAPI weapon = getRandomWeaponOfSize(size, weaponSkipProbability, weaponsForBoss);
      if (weapon != null) {
        source.addWeapon(weaponSlot, weapon.getWeaponId());
      }
    }
    for (String hullmod : additionalHullmods) {
      source.addMod(hullmod);
    }
    Collection<String> hullmods = source.getHullMods();
    for (String hullmod : target.getHullMods()) {
      if (!hullmods.contains(hullmod)) {
        source.addMod(hullmod);
      }
    }
    source.setNumFluxCapacitors(target.getNumFluxCapacitors());
    source.setNumFluxVents(target.getNumFluxVents());

    if (!fillWings) {
      source.clearWings();
    } else {
      int numBays = Global.getSettings().computeNumFighterBays(source);
      for (int i = 0; i < numBays; i++) {
        source.setWingId(i, getRandomWing().getId());
      }
    }

    if (!weaponsForBoss) {
      source.addTag(Tags.VARIANT_UNBOARDABLE);
      source.setSource(VariantSource.REFIT);
    }

  }

  private static boolean isFightingBoss = false;
  private static boolean bossHasTransformed = false;

  public static void setFightingBoss(boolean fightingBoss) {
    isFightingBoss = fightingBoss;
  }

  public static boolean isLiminalShipOrBoss(ShipVariantAPI variant) {
    String variantId = variant.getHullVariantId();
    return variantId.contains("liminal_ship_") || variantId.contains("liminal_boss")
        || variant.getHullSpec().getHullId().contains("liminal_abomination");
  }

  public static boolean shouldTransform(ShipVariantAPI variant) {
    String variantId = variant.getHullVariantId();

    if (isFightingBoss && !bossHasTransformed && variantId.contains("liminal_boss")) {
      bossHasTransformed = true;
      return true;
    }

    if (!isFightingBoss
        && (variantId.contains("liminal_ship_") || variant.getHullSpec().getHullId().contains("liminal_abomination"))) {
      return true;
    }

    return false;
  }
}

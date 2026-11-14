package liminal.faction;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;

import liminal.combat.LinkageWardPlugin;
import liminal.faction.LUtils.Difficulty;

public class AutofitPlugin extends BattleCreationPluginImpl {

  private void transformBoss(FleetMemberAPI member) {
    ShipVariantAPI variant = Global.getSettings().getVariant("liminal_boss").clone();
    variant.setSource(com.fs.starfarer.api.loading.VariantSource.REFIT);
    Autofit.registerMemberOriginal(member, member.getVariant());
    member.setVariant(variant, false, false);

    variant.addMod(LUtils.getHullmodForDifficulty(LUtils.getBossDifficulty(), 2));

    Map<String, String> allModules = variant.getStationModules();

    for (int i = 1; i <= 120; i++) {
      String moduleId = "WS " + String.format("%03d", i);
      if (Math.random() < LUtils.getBossModuleSkipProbability()) {
        allModules.remove(moduleId);
        continue;
      }
      ShipVariantAPI module = variant.getModuleVariant(moduleId);
      if (module == null) {
        continue;
      }
      module = module.clone();
      module.setSource(com.fs.starfarer.api.loading.VariantSource.REFIT);
      variant.setModuleVariant(moduleId, module);

      Difficulty difficulty = LUtils.getBossDifficulty();
      HullSize hullSize;
      if (difficulty.equals(Difficulty.EASY) || difficulty.equals(Difficulty.STANDARD)) {
        if (Math.random() < 0.5f) {
          hullSize = HullSize.CRUISER;
        } else {
          hullSize = HullSize.CAPITAL_SHIP;
        }
      } else {
        hullSize = HullSize.CAPITAL_SHIP;
      }

      WeaponSize weaponSize = difficulty.equals(Difficulty.EASY) ? null : WeaponSize.LARGE;

      Autofit.outfitVariant(module, hullSize, weaponSize, false,
          new String[] { "liminal_boss_module_hullmod",
              LUtils.getHullmodForDifficulty(LUtils.getBossDifficulty(), 1),
              "always_detaches", },
          LUtils.getBossModuleSkipProbability(), true, true, false);
    }
  }

  private static void transformShip(ShipVariantAPI variant, String hullmod) {
    Autofit.outfitVariant(variant, null, null, true,
        new String[] { hullmod }, 0, false, false, false);
  }

  public static void transformShipMember(FleetMemberAPI member) {
    ShipVariantAPI original = member.getVariant();
    String hullmod = LUtils.getCrisisHullmod();
    for (String h : original.getHullMods()) {
      if (h.contains("liminal_connection")) {
        hullmod = h;
        break;
      }
    }
    ShipVariantAPI clone = original.clone();
    clone.setSource(com.fs.starfarer.api.loading.VariantSource.REFIT);
    Autofit.registerMemberOriginal(member, original);
    member.setVariant(clone, false, false);
    transformShip(clone, hullmod);
  }

  @Override
  public void initBattle(BattleCreationContext context, MissionDefinitionAPI api) {
    super.initBattle(context, api);
    if (context.getOtherFleet().getMemoryWithoutUpdate().getBoolean(MemFlags.FLEET_FIGHT_TO_THE_LAST)) {
      context.aiRetreatAllowed = false;
      context.fightToTheLast = true;
      context.enemyDeployAll = true;
    }
    Autofit.cleanup();
    List<FleetMemberAPI> members = context.getOtherFleet().getFleetData().getMembersListCopy();
    boolean avatarPresent = false;
    for (FleetMemberAPI member : members) {
      if (member.getVariant().getHullVariantId().contains("liminal_boss")) {
        Autofit.setFightingBoss(true);
        avatarPresent = true;
      }
    }
    for (FleetMemberAPI member : members) {
      ShipVariantAPI variant = member.getVariant();
      if (Autofit.shouldTransform(variant)) {
        if (variant.getHullVariantId().contains("liminal_boss")) {
          transformBoss(member);
        } else {
          transformShipMember(member);
        }
      }
    }
    LinkageWardPlugin.addIfEarned(api, avatarPresent);
  }

}

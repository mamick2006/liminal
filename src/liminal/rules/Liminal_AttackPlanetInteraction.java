package liminal.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin.DataForEncounterSide;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.BaseFIDDelegate;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.FleetAdvanceScript;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;

import liminal.LGlobals;
import liminal.LiminalStrings;
import liminal.faction.Autofit;
import liminal.faction.LUtils;
import liminal.intel.LiminalCrisisIntel;

public class Liminal_AttackPlanetInteraction extends BaseCommandPlugin {

  public void addMercuryFS(CampaignFleetAPI fleet, int difficultyModifier) {
    FleetMemberAPI member = fleet.getFleetData().addFleetMember("mercury_FS");
    member.setShipName("E???R????R??????O??R???");
    String hullmod = LUtils.getCrisisHullmod(difficultyModifier);
    Autofit.outfitVariant(member.getVariant(), null, WeaponSize.LARGE, true,
        new String[] { hullmod }, 0, false, true, true);
    ShipVariantAPI variant = member.getVariant();
    variant.setNumFluxCapacitors(15);
    variant.setNumFluxVents(15);
    variant.addTag(Tags.VARIANT_UNBOARDABLE);
    variant.setSource(VariantSource.REFIT);
  }

  public void addMonitorEscort(CampaignFleetAPI fleet, int difficultyModifier) {
    FleetMemberAPI member = fleet.getFleetData().addFleetMember("monitor_Escort");
    member.setShipName("E???R????R??????O??R???");
    String hullmod = LUtils.getCrisisHullmod(difficultyModifier);
    Autofit.outfitVariant(member.getVariant(), null, WeaponSize.LARGE, true,
        new String[] { hullmod }, 0, false, true, true);
    ShipVariantAPI variant = member.getVariant();
    variant.setNumFluxCapacitors(15);
    variant.setNumFluxVents(15);
    variant.addTag(Tags.VARIANT_UNBOARDABLE);
    variant.setSource(VariantSource.REFIT);
  }

  public void addShadeAssault(CampaignFleetAPI fleet, int difficultyModifier) {
    FleetMemberAPI member = fleet.getFleetData().addFleetMember("shade_Assault");
    member.setShipName("E???R????R??????O??R???");
    String hullmod = LUtils.getCrisisHullmod(difficultyModifier);
    Autofit.outfitVariant(member.getVariant(), null, WeaponSize.LARGE, true,
        new String[] { hullmod }, 0, false, true, true);
    ShipVariantAPI variant = member.getVariant();
    variant.setNumFluxCapacitors(15);
    variant.setNumFluxVents(15);
    variant.addTag(Tags.VARIANT_UNBOARDABLE);
    variant.setSource(VariantSource.REFIT);
  }

  public void addZigguratStrike(CampaignFleetAPI fleet, int difficultyModifier, int index) {
    FleetMemberAPI member = fleet.getFleetData().addFleetMember("liminal_ziggurat_Experimental");
    member.setShipName("E???R????R??????O??R??? " + LGlobals.toRoman(index + 1));
    String hullmod = LUtils.getCrisisHullmod(difficultyModifier);
    Autofit.outfitVariant(member.getVariant(), null, WeaponSize.LARGE, true,
        new String[] { hullmod }, 0, false, true, true);
    ShipVariantAPI variant = member.getVariant();
    variant.setNumFluxCapacitors(50);
    variant.setNumFluxVents(50);
    variant.addTag(Tags.VARIANT_UNBOARDABLE);
    variant.setSource(VariantSource.REFIT);
  }

  private CampaignFleetAPI makeDefenders(MarketAPI market, StagingGroundDefense defense) {

    CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet(LGlobals.liminalFactionId,
        FleetTypes.PATROL_MEDIUM,
        market);
    fleet.setName("Staging Ground Defense Fleet");
    int difficultyModifier = 1;
    float shipCountMult = 1f;
    if (LUtils.getCrisisDifficulty().equals(LUtils.Difficulty.HARD)) {
      difficultyModifier = 3;
      shipCountMult = 1.2f;
    } else if (LUtils.getCrisisDifficulty().equals(LUtils.Difficulty.STANDARD)) {
      difficultyModifier = 2;
      shipCountMult = 1.1f;
    }
    if (defense == StagingGroundDefense.SHUTTLE_SWARM) {
      for (int i = 0; i < 30 * shipCountMult; i++) {
        addMercuryFS(fleet, difficultyModifier);
      }
    } else if (defense == StagingGroundDefense.WARSHIP_SWARM) {
      for (int i = 0; i < 14 * shipCountMult; i++) {
        addMonitorEscort(fleet, difficultyModifier);
      }
      for (int i = 0; i < 14 * shipCountMult; i++) {
        addShadeAssault(fleet, difficultyModifier);
      }
    } else if (defense == StagingGroundDefense.ZIGGURATS) {
      for (int i = 0; i < 3.9 * shipCountMult; i++) {
        addZigguratStrike(fleet, difficultyModifier + 1, i);
      }
    } else if (defense == StagingGroundDefense.COMBINED) {
      for (int i = 0; i < 16 * shipCountMult; i++) {
        addMercuryFS(fleet, difficultyModifier);
      }
      for (int i = 0; i < 9 * shipCountMult; i++) {
        addMonitorEscort(fleet, difficultyModifier);
      }
      for (int i = 0; i < 9 * shipCountMult; i++) {
        addShadeAssault(fleet, difficultyModifier);
      }
      for (int i = 0; i < 3 * shipCountMult; i++) {
        addZigguratStrike(fleet, difficultyModifier + 1, i);
      }
    }
    FleetParamsV3 params = new FleetParamsV3(
        market,
        Global.getSector().getPlayerFleet().getLocation(),
        LGlobals.liminalFactionId,
        null,
        FleetTypes.PATROL_MEDIUM,
        9999f,
        0f,
        0f,
        0f,
        0f,
        0f,
        0f
    );
    params.officerNumberBonus = 100;
    params.officerLevelLimit = 7;
    params.officerLevelBonus = 7;

    FleetFactoryV3.addCommanderAndOfficersV2(fleet, params, new Random(), false, true);
    for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
      m.getRepairTracker().setCR(1.0f);
    }

    fleet.getFleetData().sort();
    fleet.getFleetData().setSyncNeeded();
    fleet.getFleetData().syncIfNeeded();
    return fleet;
  }

  public static String engageOptionText(int remaining) {
    return StagingGroundDefense.isSwarm(remaining)
        ? LiminalStrings.dialog("planet_engage_swarm_option")
        : LiminalStrings.dialog("planet_engage_defenders_option");
  }

  public static String reengageOptionText(int remaining) {
    return StagingGroundDefense.isSwarm(remaining)
        ? LiminalStrings.dialog("planet_reengage_swarm_option")
        : LiminalStrings.dialog("planet_reengage_defenders_option");
  }

  public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params,
      final Map<String, MemoryAPI> memoryMap) {
    if (dialog == null)
      return false;

    final SectorEntityToken entity = dialog.getInteractionTarget();

    int remaining = LiminalCrisisIntel.getIntel().getRemainingInvasionPlanets();
    final CampaignFleetAPI defenders = makeDefenders(entity.getMarket(), StagingGroundDefense.forRemaining(remaining));
    if (defenders == null)
      return false;

    dialog.setInteractionTarget(defenders);

    final FIDConfig config = new FIDConfig();
    config.leaveAlwaysAvailable = true;
    config.showCommLinkOption = false;
    config.showEngageText = false;
    config.showFleetAttitude = false;
    config.showTransponderStatus = false;
    config.showWarningDialogWhenNotHostile = false;
    config.alwaysAttackVsAttack = true;
    config.impactsAllyReputation = true;
    config.impactsEnemyReputation = false;
    config.pullInAllies = true;
    config.pullInEnemies = true;
    config.pullInStations = false;
    config.lootCredits = false;

    config.firstTimeEngageOptionText = engageOptionText(remaining);
    config.afterFirstTimeEngageOptionText = reengageOptionText(remaining);
    config.noSalvageLeaveOptionText = "Continue";

    config.dismissOnLeave = false;
    config.printXPToDialog = true;

    long seed = (long) (Math.random() * 1000000000000000000L);
    config.salvageRandom = Misc.getRandom(seed, 75);

    final FleetInteractionDialogPluginImpl plugin = new FleetInteractionDialogPluginImpl(config);

    final InteractionDialogPlugin originalPlugin = dialog.getPlugin();
    config.delegate = new BaseFIDDelegate() {
      @Override
      public void notifyLeave(InteractionDialogAPI dialog) {
        defenders.getMemoryWithoutUpdate().clear();
        defenders.clearAssignments();
        defenders.deflate();
        defenders.despawn(FleetDespawnReason.OTHER, null);

        dialog.setPlugin(originalPlugin);
        dialog.setInteractionTarget(entity);

        if (plugin.getContext() instanceof FleetEncounterContext) {
          FleetEncounterContext context = (FleetEncounterContext) plugin.getContext();
          if (context.didPlayerWinEncounterOutright()) {
            entity.removeScriptsOfClass(FleetAdvanceScript.class);
            dialog.getTextPanel().addPara(
                LiminalStrings.dialog("planet_win_1"));
            dialog.getTextPanel().highlightInLastPara(LGlobals.liminalityColor, "the Entity's");
            dialog.getTextPanel().addPara(
                LiminalStrings.dialog("planet_win_2"));
            dialog.getTextPanel().highlightInLastPara(LGlobals.liminalityColor, "The wound seals shut");
            dialog.getTextPanel().addPara(
                LiminalStrings.dialog("planet_win_3"));
            MarketAPI market = entity.getMarket();
            LUtils.destroyLiminalMarket(market);
            FireBest.fire(null, dialog, memoryMap, "LiminalLeaveOption");
            LiminalCrisisIntel.getIntel().getRemainingInvasionPlanets();
          } else {
            dialog.getTextPanel().addPara(LiminalStrings.dialog("planet_loss"), LGlobals.liminalityColor);
            FireBest.fire(null, dialog, memoryMap, "LiminalLeaveOption");
          }
        } else {
          FireBest.fire(null, dialog, memoryMap, "LiminalLeaveOption");
        }
      }

      @Override
      public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
        bcc.aiRetreatAllowed = false;
        bcc.enemyDeployAll = true;
      }

      @Override
      public void postPlayerSalvageGeneration(InteractionDialogAPI dialog, FleetEncounterContext context,
          CargoAPI salvage) {
        DataForEncounterSide winner = context.getWinnerData();
        DataForEncounterSide loser = context.getLoserData();

        if (winner == null || loser == null)
          return;

        List<DropData> dropRandom = new ArrayList<DropData>();
        List<DropData> dropValue = new ArrayList<DropData>();

        float valueMultFleet = Global.getSector().getPlayerFleet().getStats().getDynamic()
            .getValue(Stats.BATTLE_SALVAGE_MULT_FLEET);
        float valueModShips = context.getSalvageValueModPlayerShips();

        float fuelMult = Global.getSector().getPlayerFleet().getStats().getDynamic()
            .getValue(Stats.FUEL_SALVAGE_VALUE_MULT_FLEET);

        CargoAPI extra = SalvageEntity.generateSalvage(config.salvageRandom, valueMultFleet + valueModShips, 1f, 1f,
            fuelMult, dropValue, dropRandom);
        for (CargoStackAPI stack : extra.getStacksCopy()) {
          if (stack.isFuelStack()) {
            stack.setSize((int) (stack.getSize() * fuelMult));
          }
          salvage.addFromStack(stack);
        }

      }

    };

    dialog.setPlugin(plugin);
    plugin.init(dialog);

    return true;
  }

}

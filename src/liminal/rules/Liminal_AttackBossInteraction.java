package liminal.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.BaseFIDDelegate;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.FleetAdvanceScript;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;

import liminal.LGlobals;
import liminal.LiminalStrings;
import liminal.faction.Autofit;
import liminal.faction.LUtils;
import liminal.instructions.InstructionsManager;
import liminal.intel.LiminalCrisisIntel;
import liminal.intel.LiminalCrisisTimeListener;
import liminal.start.StartPlace;

public class Liminal_AttackBossInteraction extends BaseCommandPlugin {

  private CampaignFleetAPI makeDefenders(MarketAPI market) {

    CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet(LGlobals.liminalFactionId,
        FleetTypes.BATTLESTATION,
        market);
    fleet.setName("??B?E??E???R????R??HJ??????O??NMR???");
    FleetMemberAPI member = fleet.getFleetData().addFleetMember("liminal_boss");
    member.setShipName("S???EA???R???R???R??Y????O???R???");
    com.fs.starfarer.api.combat.ShipVariantAPI bossVariant = member.getVariant().clone();
    bossVariant.setSource(com.fs.starfarer.api.loading.VariantSource.REFIT);
    member.setVariant(bossVariant, false, false);
    FleetParamsV3 params = new FleetParamsV3(
        market,
        Global.getSector().getPlayerFleet().getLocation(),
        LGlobals.liminalFactionId,
        null,
        FleetTypes.BATTLESTATION,
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

  private void clearLocation(LocationAPI location) {
    List<CampaignFleetAPI> campaignFleetAPIS = new ArrayList<>(location.getFleets());

    for (CampaignFleetAPI fleet : campaignFleetAPIS) {
      if (fleet.getFaction() == null || !fleet.getFaction().getId().equals(LGlobals.liminalFactionId)) {
        continue;
      }
      if (fleet.isPlayerFleet()
          || fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.ENTITY_MISSION_IMPORTANT))
        continue;

      Global.getSector().reportFleetDespawned(fleet, CampaignEventListener.FleetDespawnReason.OTHER, null);
      location.removeEntity(fleet);
    }
  }

  public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params,
      final Map<String, MemoryAPI> memoryMap) {
    if (dialog == null)
      return false;

    final SectorEntityToken entity = dialog.getInteractionTarget();

    final CampaignFleetAPI defenders = makeDefenders(entity.getMarket());

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
    config.pullInAllies = false;
    config.pullInEnemies = false;
    config.pullInStations = false;
    config.lootCredits = false;

    config.firstTimeEngageOptionText = LiminalStrings.dialog("boss_engage_option");
    config.afterFirstTimeEngageOptionText = LiminalStrings.dialog("boss_reengage_option");
    config.noSalvageLeaveOptionText = "Continue";

    config.dismissOnLeave = false;
    config.printXPToDialog = true;

    long seed = (long) (Math.random() * 1000000000000000000L);
    config.salvageRandom = Misc.getRandom(seed, 75);

    final FleetInteractionDialogPluginImpl plugin = new FleetInteractionDialogPluginImpl(config) {
      @Override
      public void backFromEngagement(EngagementResultAPI result) {
        Autofit.cleanup();
        super.backFromEngagement(result);
      }
    };

    final InteractionDialogPlugin originalPlugin = dialog.getPlugin();
    config.delegate = new BaseFIDDelegate() {
      @Override
      public void notifyLeave(InteractionDialogAPI dialog) {
        Autofit.cleanup();
        defenders.getMemoryWithoutUpdate().clear();
        defenders.clearAssignments();
        defenders.deflate();
        defenders.despawn(FleetDespawnReason.OTHER, null);

        dialog.setPlugin(originalPlugin);
        dialog.setInteractionTarget(entity);

        if (plugin.getContext() instanceof FleetEncounterContext) {
          FleetEncounterContext context = (FleetEncounterContext) plugin.getContext();
          if (context.didPlayerWinEncounterOutright()) {
            dialog.getTextPanel().addPara(
                LiminalStrings.dialog("boss_win_1"));
            dialog.getTextPanel().highlightInLastPara(LGlobals.liminalityColor, "The Avatar of the Entity",
                "the Entity's");

            dialog.getTextPanel().addPara(
                LiminalStrings.dialog("boss_win_2"));
            dialog.getTextPanel().highlightInLastPara(LGlobals.liminalityColor, "the Entity's",
                "Servants of the Entity");

            dialog.getTextPanel().addPara(
                String.format(LiminalStrings.dialog("boss_win_3"), StartPlace.name()));
            dialog.getTextPanel().highlightInLastPara(LGlobals.liminalityColor, "Liminality");

            dialog.getTextPanel().addPara(
                LiminalStrings.dialog("boss_win_4"));
            dialog.getTextPanel().highlightInLastPara(LGlobals.liminalityColor, "Liminality", "the Entity");

            dialog.getTextPanel().addPara(
                LiminalStrings.dialog("boss_win_5"));

            entity.removeScriptsOfClass(FleetAdvanceScript.class);

            InstructionsManager im = InstructionsManager.getInstance();
            im.addResearchPoints(300);
            TextPanelAPI text = dialog.getTextPanel();
            text.setFontSmallInsignia();
            text.addParagraph(LiminalStrings.dialog("boss_win_rp_gained"), Misc.getPositiveHighlightColor());
            text.highlightInLastPara(LGlobals.liminalityColor, "300");
            text.addParagraph(LiminalStrings.dialog("boss_win_reward"),
                Misc.getPositiveHighlightColor());
            text.highlightInLastPara("Liminal Templates", "30% more OP", "Instructions");
            text.setHighlightColorsInLastPara(LGlobals.liminalityColor, Misc.getHighlightColor(),
                LGlobals.instructionColor);
            text.setFontInsignia();

            LiminalCrisisIntel lci = LiminalCrisisIntel.getIntel();
            if (lci != null) {
              lci.setCrisisEnded(true);
            }

            LiminalCrisisTimeListener listener = LiminalCrisisTimeListener.getInstance();
            if (listener != null) {
              listener.clearInvasionFleet(true);
            }
            Global.getSector().getListenerManager().removeListenerOfClass(LiminalCrisisTimeListener.class);

            for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
              if (market.getFactionId().equals(LGlobals.liminalFactionId)) {
                LUtils.destroyLiminalMarket(market);
              }
            }
            clearLocation(Global.getSector().getHyperspace());
            for (LocationAPI loc : Global.getSector().getAllLocations()) {
              clearLocation(loc);
            }
            FireBest.fire(null, dialog, memoryMap, "LiminalLeaveOption");
          } else {
            dialog.getTextPanel().addPara(LiminalStrings.dialog("boss_loss"), LGlobals.liminalityColor);
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
      }

    };

    dialog.setPlugin(plugin);
    plugin.init(dialog);

    return true;
  }

}

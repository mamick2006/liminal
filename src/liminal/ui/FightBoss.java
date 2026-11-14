package liminal.ui;

import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;

import liminal.LGlobals;
import liminal.LiminalStrings;
import liminal.faction.Autofit;
import liminal.faction.LUtils;

public class FightBoss implements InteractionDialogPlugin {
  InteractionDialogAPI dialog;
  int[] bossStats;

  public FightBoss() {
  }

  enum OptionId {
    INIT, LEAVE, AH_BACK_AGAIN, ARENT_YOU_GETTING, BACK_AGAIN, BUT_THIS_FIGHT, HELLO_ARE_YOU, LIKE_A_GAME, NO_NOT_YOU, NOT_YET_OUR_TIME, SO_DO_YOU_WANT, WELL_I_SUPPOSE, YOUVE_WON, BATTLE
  }

  @Override
  public void init(InteractionDialogAPI dialog) {
    this.dialog = dialog;
    dialog.getTextPanel().addPara("<ERROR> <ERROR> <ERROR> <ERROR> <ERROR> <ERROR>", LGlobals.liminalityColor);
    dialog.getTextPanel().addPara("<ERROR> <ERROR> <ERROR> <ERROR> <ERROR> <ERROR>", LGlobals.liminalityColor);
    dialog.getTextPanel().addPara("<ERROR> <ERROR> <ERROR> <ERROR> <ERROR> <ERROR>", LGlobals.liminalityColor);
    OptionPanelAPI options = dialog.getOptionPanel();
    options.addOption(".....", OptionId.INIT);
    bossStats = LUtils.getBossStats();
  }

  public String getDialog(String name) {
    return Global.getSettings().getSpriteName("dialog", name);
  }

  public void addImageToDialog(String name) {
    dialog.getTextPanel().addPara("");
    dialog.getTextPanel().addImage(getDialog(name));
  }

  @Override
  public void optionSelected(String optionText, Object optionData) {
    if (optionData.equals(OptionId.INIT)) {
      if (bossStats[0] == 0) {
        addImageToDialog("liminal_hello_are_you");
        dialog.getOptionPanel().clearOptions();
        dialog.getOptionPanel().addOption("\"Hello?\"", OptionId.NO_NOT_YOU);
      } else if (bossStats[1] > 0) {
        addImageToDialog("liminal_back_again");
        dialog.getOptionPanel().clearOptions();
        dialog.getOptionPanel().addOption("...", OptionId.WELL_I_SUPPOSE);
      } else if (bossStats[0] < 3) {
        addImageToDialog("liminal_ah_back_again");
        dialog.getOptionPanel().clearOptions();
        dialog.getOptionPanel().addOption(LiminalStrings.console("fightboss_remember_option"), OptionId.SO_DO_YOU_WANT);
      } else {
        addImageToDialog("liminal_arent_you_getting");
        dialog.getOptionPanel().clearOptions();
        dialog.getOptionPanel().addOption("\"Fuck you.\"", OptionId.WELL_I_SUPPOSE);
      }
    } else if (optionData.equals(OptionId.NO_NOT_YOU)) {
      addImageToDialog("liminal_no_not_you");
      dialog.getOptionPanel().clearOptions();
      dialog.getOptionPanel().addOption("\"What?\"", OptionId.NOT_YET_OUR_TIME);
    } else if (optionData.equals(OptionId.NOT_YET_OUR_TIME)) {
      addImageToDialog("liminal_not_yet_our_time");
      dialog.getOptionPanel().clearOptions();
      dialog.getOptionPanel().addOption("\"Abominations?\"", OptionId.LIKE_A_GAME);
    } else if (optionData.equals(OptionId.LIKE_A_GAME)) {
      addImageToDialog("liminal_like_a_game");
      dialog.getOptionPanel().clearOptions();
      dialog.getOptionPanel().addOption("...", OptionId.SO_DO_YOU_WANT);
    } else if (optionData.equals(OptionId.SO_DO_YOU_WANT)) {
      addImageToDialog("liminal_so_do_you_want");
      dialog.getOptionPanel().clearOptions();
      dialog.getOptionPanel().addOption("\"Yes.\"", OptionId.BATTLE);
      dialog.getOptionPanel().addOption("\"No.\"", OptionId.LEAVE);
    } else if (optionData.equals(OptionId.WELL_I_SUPPOSE)) {
      addImageToDialog("liminal_well_i_suppose");
      dialog.getOptionPanel().clearOptions();
      dialog.getOptionPanel().addOption("\"Yes.\"", OptionId.BATTLE);
      dialog.getOptionPanel().addOption("\"No.\"", OptionId.LEAVE);
    } else if (optionData.equals(OptionId.SO_DO_YOU_WANT)) {
      addImageToDialog("liminal_so_do_you_want");
      dialog.getOptionPanel().clearOptions();
      dialog.getOptionPanel().addOption("\"Yes.\"", OptionId.BATTLE);
      dialog.getOptionPanel().addOption("\"No.\"", OptionId.LEAVE);
    } else if (optionData.equals(OptionId.BATTLE)) {
      fightBoss();
    } else if (optionData.equals(OptionId.LEAVE)) {
      dialog.dismiss();
      return;
    }
  }

  public void fightBoss() {
    CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet(LGlobals.liminalFactionId,
        FleetTypes.TASK_FORCE,
        null);
    fleet.setName("Abomination");
    fleet.setNoFactionInName(true);

    FleetMemberAPI member = fleet.getFleetData().addFleetMember("liminal_boss");
    member.setShipName("????????");
    com.fs.starfarer.api.combat.ShipVariantAPI bossVariant = member.getVariant().clone();
    bossVariant.setSource(com.fs.starfarer.api.loading.VariantSource.REFIT);
    member.setVariant(bossVariant, false, false);

    LUtils.incrementBossAttempt();

    BattleCreationContext bcc = new BattleCreationContext(Global.getSector().getPlayerFleet(), FleetGoal.ATTACK,
        fleet, FleetGoal.ATTACK);
    bcc.setPlayerCommandPoints(
        (int) Global.getSector().getPlayerFleet().getCommanderStats().getCommandPoints().getModifiedValue());

    dialog.getVisualPanel().fadeVisualOut();
    Global.getSector().getCampaignUI().startBattle(bcc);
  }

  @Override
  public void optionMousedOver(String optionText, Object optionData) {
  }

  @Override
  public void advance(float amount) {
  }

  @Override
  public void backFromEngagement(EngagementResultAPI battleResult) {
    Autofit.cleanup();
    if (battleResult.didPlayerWin()) {
      LUtils.incrementBossWins();
      addImageToDialog("liminal_youve_won");
      dialog.getOptionPanel().clearOptions();
      dialog.getOptionPanel().addOption("<ERROR>", OptionId.LEAVE);
    } else {
      dialog.getTextPanel().addPara("LOSER", LGlobals.liminalityColor);
      dialog.getOptionPanel().clearOptions();
      dialog.getOptionPanel().addOption("<ERROR>", OptionId.LEAVE);
    }
  }

  @Override
  public Object getContext() {
    return null;
  }

  @Override
  public Map<String, MemoryAPI> getMemoryMap() {
    return null;
  }
}

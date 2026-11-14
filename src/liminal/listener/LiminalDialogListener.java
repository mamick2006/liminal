package liminal.listener;

import java.util.Arrays;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;

import liminal.intel.LiminalCrisisIntel;

public class LiminalDialogListener implements EconomyTickListener {

  private static final List<String> DIALOG_STRINGS = Arrays.asList("liminal_warning_one", "liminal_warning_two",
      "liminal_crisis_begin", "liminal_crisis_progress", "liminal_crisis_end");

  @Override
  public void reportEconomyTick(int iterIndex) {
    LiminalCrisisIntel intel = LiminalCrisisIntel.getIntel();
    if (intel == null) {
      return;
    }
    float percentProgress = (float) intel.getProgress() / (float) intel.getMaxProgress();
    boolean crisisBegun = intel.crisisBegun();

    if ((crisisBegun || percentProgress >= 0.5f)
        && !Global.getSector().getMemoryWithoutUpdate().getBoolean("$" + DIALOG_STRINGS.get(0))) {
      Global.getSector().getCampaignUI().showInteractionDialog(
          new RuleBasedInteractionDialogPluginImpl(DIALOG_STRINGS.get(0)), Global.getSector().getPlayerFleet());
    } else if ((crisisBegun || percentProgress >= 0.75f)
        && !Global.getSector().getMemoryWithoutUpdate().getBoolean("$" + DIALOG_STRINGS.get(1))) {
      Global.getSector().getCampaignUI().showInteractionDialog(
          new RuleBasedInteractionDialogPluginImpl(DIALOG_STRINGS.get(1)), Global.getSector().getPlayerFleet());
    } else if (crisisBegun && !Global.getSector().getMemoryWithoutUpdate().getBoolean("$" + DIALOG_STRINGS.get(2))) {
      Global.getSector().getCampaignUI().showInteractionDialog(
          new RuleBasedInteractionDialogPluginImpl(DIALOG_STRINGS.get(2)), Global.getSector().getPlayerFleet());
    } else if (crisisBegun && percentProgress >= 0.5f
        && !Global.getSector().getMemoryWithoutUpdate().getBoolean("$" + DIALOG_STRINGS.get(3))) {
      Global.getSector().getCampaignUI().showInteractionDialog(
          new RuleBasedInteractionDialogPluginImpl(DIALOG_STRINGS.get(3)), Global.getSector().getPlayerFleet());
    } else if (crisisBegun && percentProgress >= 1.0f
        && !Global.getSector().getMemoryWithoutUpdate().getBoolean("$" + DIALOG_STRINGS.get(4))) {
      Global.getSector().getCampaignUI().showInteractionDialog(
          new RuleBasedInteractionDialogPluginImpl(DIALOG_STRINGS.get(4)), Global.getSector().getPlayerFleet());
    }
  }

  @Override
  public void reportEconomyMonthEnd() {
  }
}

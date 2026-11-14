package liminal.instructions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.comm.CommMessageAPI;
import com.fs.starfarer.api.campaign.econ.MonthlyReport;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.MutableValue;

import liminal.LGlobals;
import liminal.LiminalStrings;
import liminal.crossmod.StandingLinkage;

public class InstructionsMonthlyCost implements EconomyTickListener {
  public static final String LIMINALITY_NODE = "node_id_liminality";

  @Override
  public void reportEconomyTick(int iterIndex) {
    final InstructionsManager im = InstructionsManager.getInstance();
    int investment = im.getResearchInvestmentPerMonth();
    if (investment <= 0) {
      return;
    }
    if (investment < im.getMinimumResearchInvestmentPerMonth()) {
      im.setResearchInvestmentPerMonth(investment);
      investment = im.getResearchInvestmentPerMonth();
    }

    MonthlyReport report = SharedData.getData().getCurrentReport();
    MonthlyReport.FDNode baseNode = report.getNode(LIMINALITY_NODE);
    baseNode.name = "Liminality";
    baseNode.custom = LIMINALITY_NODE;
    baseNode.tooltipCreator = new LGlobals.InfoTooltip() {
      @Override
      public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        tooltip.addTitle("Research Investment", LGlobals.liminalityColor);
        tooltip.setParaFontColor(Misc.getTextColor());
        tooltip.addPara(LiminalStrings.intel("report_upkeep_tooltip_note"), 5f, Misc.getTextColor());
        im.addMonthlyCostTooltip(tooltip, 5f);
      }
    };

    int numIter = (int) Global.getSettings().getFloat("economyIterPerMonth");
    baseNode.upkeep += investment / numIter;
    im.addTotalEverResearchInvestment(1f / numIter);
    baseNode.icon = Global.getSettings().getSpriteName("icons", "liminal_intel_research");

    final MutableValue credits = Global.getSector().getPlayerFleet().getCargo().getCredits();
    final int amount = (int) credits.get();
    if (amount < baseNode.upkeep) {
      im.setResearchInvestmentPerMonth(0);
      MessageIntel intel = new MessageIntel("Liminality Research Stopped", Misc.getNegativeHighlightColor());
      intel.addLine(LiminalStrings.intel("msg_upkeep_credits_low"), Misc.getTextColor(),
          new String[] { "Liminality", LiminalStrings.intel("msg_upkeep_disabled_consequence") },
          LGlobals.liminalityColor, Misc.getNegativeHighlightColor());
      intel.setIcon(Global.getSettings().getSpriteName("icons", "liminal_intel_research"));
      intel.setSound("liminal_monthly_income_negative");
      Global.getSector().getCampaignUI().addMessage(intel, CommMessageAPI.MessageClickAction.INTEL_TAB, null);
    }
  }

  @Override
  public void reportEconomyMonthEnd() {
    StandingLinkage.grantPeriodicResearch();
  }
}

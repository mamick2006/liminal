package liminal;

import java.awt.Color;
import java.util.TreeMap;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class LGlobals {
  public static Color liminalityColor = new Color(255, 165, 0, 255);
  public static Color liminalityHighlightColor = new Color(255, 200, 100, 255);
  public static Color liminalityDarkColor = new Color(150, 90, 0, 255);
  public static Color liminalityDarkGreenColor = new Color(0, 90, 0, 255);
  public static Color liminalityTextColor = new Color(
      Math.min(255, Misc.getTextColor().getRed() + 50),
      Math.min(255, Misc.getTextColor().getGreen() + 30),
      Math.min(255, Misc.getTextColor().getBlue()),
      255);
  public static Color instructionColor = new Color(155, 255, 0, 255);

  public static Color jitterColor = new Color(255, 165, 0, 45);
  public static Color jitterUnderColor = new Color(255, 165, 0, 35);

  public static String researchTeamDialog = "LiminalityResearchTeamDialog";
  public static String liminalFactionId = "liminal";
  public static String liminalInvasionTag = "liminal_invasion";

  public static boolean ignoreRestrictions = false;

  private final static TreeMap<Integer, String> map = new TreeMap<Integer, String>();

  static {
    map.put(1000, "M");
    map.put(900, "CM");
    map.put(500, "D");
    map.put(400, "CD");
    map.put(100, "C");
    map.put(90, "XC");
    map.put(50, "L");
    map.put(40, "XL");
    map.put(10, "X");
    map.put(9, "IX");
    map.put(5, "V");
    map.put(4, "IV");
    map.put(1, "I");
  }

  public final static String toRoman(int number) {
    int l = map.floorKey(number);
    if (number == l) {
      return map.get(number);
    }
    return map.get(l) + toRoman(number - l);
  }

  public static boolean playerIsDocked() {
    CampaignUIAPI ui = Global.getSector().getCampaignUI();
    if (ui == null || ui.getCurrentInteractionDialog() == null) {
      return false;
    }

    SectorEntityToken interactionTarget = ui.getCurrentInteractionDialog().getInteractionTarget();
    if (interactionTarget == null) {
      return false;
    }

    MarketAPI market = interactionTarget.getMarket();
    if (market == null) {
      return false;
    }

    Object tradeMode = interactionTarget.getMemory().get("$tradeMode");
    if (tradeMode == null || tradeMode.equals(CoreUITradeMode.NONE) || tradeMode.equals("NONE")) {
      return false;
    }

    for (Industry ind : market.getIndustries()) {
      if (ind.getSpec().hasTag(Industries.TAG_STATION) || ind.getSpec().hasTag(Industries.TAG_SPACEPORT)) {
        return true;
      }
    }
    return false;
  }

  public static abstract class InfoTooltip implements TooltipMakerAPI.TooltipCreator {

    @Override
    public boolean isTooltipExpandable(Object var1) {
      return false;
    }

    @Override
    public float getTooltipWidth(Object var1) {
      return 300;
    }

    public abstract void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam);
  }

  public static void setEnabledSayingWhy(ButtonAPI button, boolean enabled) {
    button.setEnabled(enabled);
    button.setShowTooltipWhileInactive(true);
  }
}

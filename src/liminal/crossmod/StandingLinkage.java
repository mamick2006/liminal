package liminal.crossmod;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;

import liminal.LiminalSettings;
import liminal.LiminalStrings;
import liminal.instructions.InstructionsManager;

public final class StandingLinkage {

  public static final String ITEM_ID = "liminal_standing_linkage";

  private static final String RP_KEY = "liminal_cq_linkage_rp_per_month";
  private static final String PERIOD_KEY = "liminal_cq_linkage_period_months";
  private static final String WARD_KEY = "liminal_cq_linkage_boss_damage_reduction";

  private static final int RP_FALLBACK = 1;
  private static final int PERIOD_FALLBACK = 3;
  private static final float WARD_FALLBACK = 15f;

  private StandingLinkage() {
  }

  public static boolean isOwned() {
    return CqSeam.isOwned(ITEM_ID);
  }

  public static int researchPointsPerMonth() {
    return LinkageMath.clampRpPerMonth(LiminalSettings.getInt(RP_KEY, RP_FALLBACK));
  }

  public static int periodMonths() {
    return Math.min(12, Math.max(1, LiminalSettings.getInt(PERIOD_KEY, PERIOD_FALLBACK)));
  }

  public static float wardPercent() {
    return LinkageMath.clampWardPercent(LiminalSettings.getFloat(WARD_KEY, WARD_FALLBACK));
  }

  public static float wardDamageTakenMult() {
    return LinkageMath.wardDamageTakenMult(LiminalSettings.getFloat(WARD_KEY, WARD_FALLBACK));
  }

  public static String researchLine() {
    return String.format(LiminalStrings.mgmt("linkage_effect_research"), researchToken(),
        periodMonths());
  }

  public static String wardLine() {
    return String.format(LiminalStrings.mgmt("linkage_effect_ward"), wardToken());
  }

  public static String researchToken() {
    return "+" + researchPointsPerMonth();
  }

  public static String wardToken() {
    return "-" + (int) wardPercent() + "%";
  }

  public static void grantPeriodicResearch() {
    CampaignClockAPI clock = Global.getSector().getClock();
    int points = researchPointsPerMonth();
    if (points <= 0 || !isOwned()
        || (clock.getCycle() * 12 + clock.getMonth()) % periodMonths() != 0) {
      return;
    }
    InstructionsManager.getInstance().addResearchPoints(points);
  }
}

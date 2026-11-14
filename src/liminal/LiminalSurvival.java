package liminal;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;

import liminal.instructions.SurvivalPriceMath;

public final class LiminalSurvival {

  public static final String ADJUSTMENTS_KEY = "liminal_survival_adjustments";
  public static final String CREDIT_MULT_KEY = "liminal_survival_credit_mult";
  private static final float CREDIT_MULT_FALLBACK = 0.5f;

  private LiminalSurvival() {
  }

  public static float creditMult() {
    SectorAPI sector = Global.getSector();
    return SurvivalPriceMath.mult(sector != null && sector.isExtMode(),
        LiminalSettings.getBoolean(ADJUSTMENTS_KEY, true),
        LiminalSettings.getFloat(CREDIT_MULT_KEY, CREDIT_MULT_FALLBACK));
  }

  public static int price(int base) {
    return SurvivalPriceMath.price(base, creditMult());
  }
}

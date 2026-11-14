package liminal.crossmod;

public final class LinkageMath {

  public static final int MAX_RP_PER_MONTH = 3;

  public static final float MAX_WARD_PERCENT = 60f;

  private LinkageMath() {
  }

  public static int clampRpPerMonth(int configured) {
    if (configured <= 0) {
      return 0;
    }
    return configured > MAX_RP_PER_MONTH ? MAX_RP_PER_MONTH : configured;
  }

  public static float clampWardPercent(float configured) {
    if (Float.isNaN(configured) || configured <= 0f) {
      return 0f;
    }
    return configured > MAX_WARD_PERCENT ? MAX_WARD_PERCENT : configured;
  }

  public static float wardDamageTakenMult(float configuredPercent) {
    return 1f - clampWardPercent(configuredPercent) / 100f;
  }

  public static float effectiveDurabilityMult(float damageTakenMult) {
    if (damageTakenMult <= 0f) {
      return Float.POSITIVE_INFINITY;
    }
    return 1f / damageTakenMult;
  }
}

package liminal.instructions;

public final class SurvivalPriceMath {

  private SurvivalPriceMath() {
  }

  public static float mult(boolean survivalSave, boolean adjustmentsOn, float setting) {
    if (!survivalSave || !adjustmentsOn || !(setting > 0f) || setting > 1f) {
      return 1f;
    }
    return setting;
  }

  public static int price(int base, float mult) {
    return Math.round(base * mult);
  }
}

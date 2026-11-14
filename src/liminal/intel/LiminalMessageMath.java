package liminal.intel;

public final class LiminalMessageMath {

  public static final String TAB_LABEL = "Liminality";
  public static final String INTEL_SCREEN = "Intel screen";

  private LiminalMessageMath() {
  }

  public static String detailsPlace(boolean tabAlive) {
    return tabAlive ? TAB_LABEL + " tab" : INTEL_SCREEN;
  }

  public static String timeUntil(int months) {
    if (months <= 0) {
      return "a few days";
    }
    if (months == 1) {
      return "1 month";
    }
    return months + " months";
  }

  public static String timeUntil(float months) {
    return timeUntil(Math.round(months));
  }

  public static boolean usesOPOverride(int templateOPOverride) {
    return templateOPOverride > 0;
  }

  public static boolean minimumDoubles(int base, int max) {
    return 2L * base <= max;
  }
}

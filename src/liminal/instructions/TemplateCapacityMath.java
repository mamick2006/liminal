package liminal.instructions;

public final class TemplateCapacityMath {

  public static final int POINTS_PER_STEP = 5;

  private TemplateCapacityMath() {
  }

  public static long pointPrice(int n, int start, double first, double growth, double factor) {
    if (n < 1 || n <= start || !(first > 0) || !(factor > 0)) {
      return 0L;
    }
    double g = Double.isNaN(growth) || growth < 1 ? 1 : growth;
    int step = (n - 1) / POINTS_PER_STEP;
    int freeInStep = Math.max(0, Math.min(POINTS_PER_STEP, start - step * POINTS_PER_STEP));
    double raw = first * Math.pow(g, step) / (POINTS_PER_STEP - freeInStep) * factor;
    if (!(raw < 9.0e18)) {
      return Long.MAX_VALUE;
    }
    return Math.round(raw / 1000.0) * 1000L;
  }
}

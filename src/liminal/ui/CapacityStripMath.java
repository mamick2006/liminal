package liminal.ui;

public final class CapacityStripMath {

  public static final int PIP_WIDTH = 9;
  public static final int PIP_HEIGHT = 16;
  public static final int PIP_GAP = 2;
  public static final int GROUP_SIZE = 5;
  public static final int GROUP_GAP = 5;
  public static final int MIN_PIP_WIDTH = 3;

  public static final int SUB_NONE = 0;
  public static final int SUB_LEFT = 1;
  public static final int SUB_LEFT_FITS = 2;
  public static final int SUB_OVER = 3;

  private CapacityStripMath() {
  }

  public static int pipCount(int used, int capacity) {
    return Math.max(0, Math.max(used, capacity));
  }

  public static int gapsWidth(int count) {
    if (count <= 1) {
      return 0;
    }
    return (count - 1) * PIP_GAP + ((count - 1) / GROUP_SIZE) * GROUP_GAP;
  }

  public static int pipWidth(int count, int maxWidth) {
    if (count <= 0) {
      return 0;
    }
    int each = (maxWidth - gapsWidth(count)) / count;
    if (each >= PIP_WIDTH) {
      return PIP_WIDTH;
    }
    return each >= MIN_PIP_WIDTH ? each : 0;
  }

  public static int pipX(int index, int pipWidth) {
    return index * (pipWidth + PIP_GAP) + (index / GROUP_SIZE) * GROUP_GAP;
  }

  public static int rowWidth(int count, int maxWidth) {
    if (count <= 0) {
      return 0;
    }
    int each = pipWidth(count, maxWidth);
    return each == 0 ? maxWidth : count * each + gapsWidth(count);
  }

  public static int barFill(int part, int whole, int width) {
    if (whole <= 0 || part <= 0) {
      return 0;
    }
    if (part >= whole) {
      return width;
    }
    return (int) Math.round((double) part * width / whole);
  }

  public static int largestFit(int left, int[] weights) {
    int fit = -1;
    for (int i = 0; i < weights.length; i++) {
      if (weights[i] <= left) {
        fit = i;
      }
    }
    return fit;
  }

  public static int subLine(int left, int[] weights) {
    if (left < 0) {
      return SUB_OVER;
    }
    if (left == 0) {
      return SUB_NONE;
    }
    return largestFit(left, weights) >= 0 ? SUB_LEFT_FITS : SUB_LEFT;
  }
}

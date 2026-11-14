package liminal.ui;

import java.util.HashMap;
import java.util.Map;

public final class ListScrollMemory {
  public static final String RESEARCH = "research";

  public static final int INSTRUCTIONS_LIST = 0;
  public static final int TEMPLATE_LIST = 1;

  private static final Map<String, Float> offsets = new HashMap<>();

  private ListScrollMemory() {
  }

  public static String tabKey(String hullModId) {
    return hullModId == null ? RESEARCH : hullModId;
  }

  public static void remember(String tab, int list, float offset) {
    offsets.put(key(tab, list), Math.max(0f, offset));
  }

  public static float restore(String tab, int list, float contentHeight, float viewHeight) {
    Float left = offsets.get(key(tab, list));
    if (left == null) {
      return 0f;
    }
    float maxOffset = Math.max(0f, contentHeight - viewHeight);
    return Math.min(Math.max(0f, left), maxOffset);
  }

  public static void clear() {
    offsets.clear();
  }

  private static String key(String tab, int list) {
    return tab + "#" + list;
  }
}

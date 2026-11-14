package liminal.topbar;

public final class LiminalMgmtKey {

  public static final int DEFAULT_KEY = 21;
  public static final int NO_KEY = 0;
  private static final int MAX_KEY = 255;
  public static final int LABEL_NAME_MAX = 5;

  public static final String[][] SETTINGS = {
      {"nomadic_fleet", "nomad_mgmt_key"},
      {"liminal", "liminal_mgmt_key"},
      {"captains_quarters", "cq_mgmt_key"},
      {"arondight", "arondight_mgmt_key"}};

  private static final int[] RENAMED_CODES = {
      41, 181, 55, 78, 74, 28, 42, 54, 29, 157, 219, 220, 56, 184, 183, 57, 1, 15};
  private static final String[] RENAMED_NAMES = {
      "Tilde", "/", "*", "+", "-", "Enter", "Shift", "Shift", "Ctrl", "Ctrl", "Ctrl", "Ctrl",
      "Alt", "Alt", "Print Screen", "Space", "Esc", "Tab"};

  private LiminalMgmtKey() {
  }

  public static int resolve(Integer[] values) {
    if (values != null) {
      for (Integer v : values) {
        if (v != null && v.intValue() >= NO_KEY && v.intValue() <= MAX_KEY
            && v.intValue() != DEFAULT_KEY) {
          return v.intValue();
        }
      }
    }
    return DEFAULT_KEY;
  }

  public static String gameKeyName(int code, String lwjglName) {
    if (code <= NO_KEY) {
      return null;
    }
    for (int i = 0; i < RENAMED_CODES.length; i++) {
      if (RENAMED_CODES[i] == code) {
        return RENAMED_NAMES[i];
      }
    }
    return lwjglName == null || lwjglName.isEmpty() ? null : lwjglName;
  }

  public static String label(String keyName) {
    return keyName == null || keyName.length() > LABEL_NAME_MAX
        ? "Manage" : "Manage (" + keyName + ")";
  }

  public static String labelMarkup(String keyName) {
    if (label(keyName).equals("Manage")) {
      return "Manage<highlight></highlight>";
    }
    int last = keyName.length() - 1;
    return "Manage (<highlight>" + keyName.substring(0, last) + "</highlight>"
        + keyName.substring(last) + ")";
  }

  public static boolean menuSeen(boolean seen, boolean menu, boolean dialog) {
    return menu || (seen && dialog);
  }

  public static boolean recheckDue(boolean seen, boolean menu, boolean dialog) {
    return seen && !menu && !dialog;
  }
}

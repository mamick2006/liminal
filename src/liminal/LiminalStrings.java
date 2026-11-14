package liminal;

import com.fs.starfarer.api.Global;

public final class LiminalStrings {

  private LiminalStrings() {
  }

  public static String mgmt(String key) {
    return Global.getSettings().getString("liminal_mgmt_ui", key);
  }

  public static String intel(String key) {
    return Global.getSettings().getString("liminal_intel", key);
  }

  public static String dialog(String key) {
    return Global.getSettings().getString("liminal_dialog", key);
  }

  public static String console(String key) {
    return Global.getSettings().getString("liminal_console", key);
  }
}

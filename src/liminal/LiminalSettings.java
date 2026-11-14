package liminal;

import com.fs.starfarer.api.Global;

import lunalib.lunaSettings.LunaSettings;

public final class LiminalSettings {

  private static final String MOD_ID = "liminal";

  private LiminalSettings() {
  }

  public static float getFloat(String key, float fallback) {
    return Global.getSettings() == null ? fallback : Global.getSettings().optFloat(key, fallback);
  }

  public static int getInt(String key, int fallback) {
    return (int) getFloat(key, fallback);
  }

  public static String getString(String key, String fallback) {
    if (Global.getSettings() == null) {
      return fallback;
    }
    try {
      String value = Global.getSettings().getString(key);
      return value != null ? value : fallback;
    } catch (Exception e) {
      return fallback;
    }
  }

  public static boolean getBoolean(String key, boolean fallback) {
    if (lunaLib()) {
      Boolean value = LunaSettings.getBoolean(MOD_ID, key);
      if (value != null) {
        return value;
      }
    }
    return Global.getSettings() == null ? fallback : Global.getSettings().optBoolean(key, fallback);
  }

  public static int lunaInt(String key, int fallback) {
    if (lunaLib()) {
      Integer value = LunaSettings.getInt(MOD_ID, key);
      if (value != null) {
        return value;
      }
    }
    return getInt(key, fallback);
  }

  public static double lunaDouble(String key, double fallback) {
    if (lunaLib()) {
      Double value = LunaSettings.getDouble(MOD_ID, key);
      if (value != null) {
        return value;
      }
    }
    return getFloat(key, (float) fallback);
  }

  public static String lunaString(String key, String fallback) {
    if (lunaLib()) {
      String value = LunaSettings.getString(MOD_ID, key);
      if (value != null) {
        return value;
      }
    }
    return getString(key, fallback);
  }

  private static boolean lunaLib() {
    return Global.getSettings() != null
        && Global.getSettings().getModManager().isModEnabled("lunalib");
  }
}

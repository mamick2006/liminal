package liminal.faction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Planets;
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD;
import com.fs.starfarer.api.loading.HullModSpecAPI;

import liminal.LGlobals;
import liminal.intel.LiminalCrisisIntel;
import liminal.LiminalSettings;

public class LUtils {

  public static int[] getBossStats() {
    if (Global.getSettings().fileExistsInCommon("liminal")) {
      try {
        String data = Global.getSettings().readTextFileFromCommon("liminal");
        String[] split = data.split(",");
        return new int[] { Integer.parseInt(split[0]), Integer.parseInt(split[1]) };
      } catch (IOException e) {
        Global.getLogger(LUtils.class).error("Error getting boss stats", e);
      }
    }
    return new int[] { 0, 0 };
  }

  public static void incrementBossAttempt() {
    int[] stats = getBossStats();
    stats[0]++;
    try {
      Global.getSettings().writeTextFileToCommon("liminal", stats[0] + "," + stats[1]);
    } catch (IOException e) {
      Global.getLogger(LUtils.class).error("Error incrementing boss attempts", e);
    }
  }

  public static void incrementBossWins() {
    int[] stats = getBossStats();
    stats[1]++;
    try {
      Global.getSettings().writeTextFileToCommon("liminal", stats[0] + "," + stats[1]);
    } catch (IOException e) {
      Global.getLogger(LUtils.class).error("Error incrementing boss wins", e);
    }
  }

  public enum Difficulty {
    EASY(0), STANDARD(3), HARD(6);

    private final int value;

    Difficulty(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  public static Difficulty getCrisisDifficulty() {
    String difficulty = LiminalSettings.lunaString("liminal_crisis_difficulty", "Standard");
    if (difficulty.equalsIgnoreCase("Easy")) {
      return Difficulty.EASY;
    } else if (difficulty.equalsIgnoreCase("Hard")) {
      return Difficulty.HARD;
    } else if (difficulty.equalsIgnoreCase("Standard")) {
      return Difficulty.STANDARD;
    } else {
      throw new RuntimeException("Invalid crisis difficulty: " + difficulty);
    }
  }

  public static Difficulty getBossDifficulty() {
    String difficulty = LiminalSettings.lunaString("liminal_boss_difficulty", "Standard");
    if (difficulty.equalsIgnoreCase("Easy")) {
      return Difficulty.EASY;
    } else if (difficulty.equalsIgnoreCase("Hard")) {
      return Difficulty.HARD;
    } else if (difficulty.equalsIgnoreCase("Standard")) {
      return Difficulty.STANDARD;
    } else {
      throw new RuntimeException("Invalid boss difficulty: " + difficulty);
    }
  }

  private static List<String> difficultyToHullmod;

  public static String getHullmodForDifficulty(Difficulty difficulty, int difficultyModifier) {
    int difficultyValue = difficulty.getValue() + difficultyModifier;
    if (difficultyToHullmod == null) {
      difficultyToHullmod = new ArrayList<>();
      difficultyToHullmod.add("liminal_connection_25");
      difficultyToHullmod.add("liminal_connection_50");
      difficultyToHullmod.add("liminal_connection_75");
      difficultyToHullmod.add("liminal_connection_100");
      difficultyToHullmod.add("liminal_connection_125");
      difficultyToHullmod.add("liminal_connection_150");
      difficultyToHullmod.add("liminal_connection_175");
      difficultyToHullmod.add("liminal_connection_200");
      difficultyToHullmod.add("liminal_connection_225");
      difficultyToHullmod.add("liminal_connection_250");
      difficultyToHullmod.add("liminal_connection_275");
      difficultyToHullmod.add("liminal_connection_300");

      for (String hullmod : difficultyToHullmod) {
        HullModSpecAPI spec = Global.getSettings().getHullModSpec(hullmod);
        if (spec == null) {
          throw new RuntimeException("Hullmod not found: " + hullmod);
        }
      }
    }
    difficultyValue = Math.max(0, Math.min(difficultyValue, difficultyToHullmod.size() - 1));
    return difficultyToHullmod.get(difficultyValue);
  }

  private static final float bossComputeDefault = 0.4f;

  public static float getBossModuleSkipProbability() {
    return (float) (1f - Math.sqrt(getBossCompute()));
  }

  public static float getBossCompute() {
    return (float) LiminalSettings.lunaDouble("liminal_boss_compute", bossComputeDefault);
  }

  private static final int npcInfluencePercentDefault = 10;

  public static float getNpcInfluenceMaxChance() {
    return LiminalSettings.lunaInt("liminal_npc_influence_chance", npcInfluencePercentDefault) / 100f;
  }

  public static float getAllShipHullmodChance() {
    LiminalCrisisIntel intel = LiminalCrisisIntel.getIntel();
    if (intel == null) {
      return 0f;
    }
    float max = getNpcInfluenceMaxChance();
    if (!intel.crisisBegun()) {
      float percentProgress = (float) intel.getProgress() / (float) intel.getMaxProgress();
      if (percentProgress < 0.5f) {
        return 0f;
      } else if (percentProgress < 0.75f) {
        return max * 0.25f;
      } else {
        return max * 0.5f;
      }
    }
    return max;
  }

  public static void destroyLiminalMarket(MarketAPI market) {
    DecivTracker.decivilize(market, true);
    MarketCMD.addBombardVisual(market.getPrimaryEntity());
    market.getConditions().clear();
    market.removeTag(LGlobals.liminalInvasionTag);
    PlanetAPI planet = market.getPlanetEntity();
    if (planet != null) {
      planet.setCustomDescriptionId("liminal_barren_deep_space");
      planet.changeType(Planets.BARREN_BOMBARDED, new Random());
      market.addCondition(Conditions.HOT);
      market.addCondition(Conditions.EXTREME_WEATHER);
    }
  }

  public static String getAllShipHullmod() {
    return getHullmodForDifficulty(getCrisisDifficulty(), 0);
  }

  public static int getInvasionFrequencyMonths() {
    LiminalCrisisIntel intel = LiminalCrisisIntel.getIntel();
    if (intel == null) {
      return 0;
    }
    if (!intel.crisisBegun()) {
      return 0;
    }
    return getInvasionFrequencyMonthsAt(intel.getProgress());
  }

  public static int getInvasionFrequencyMonthsAt(int progress) {
    return 1 + (progress / 100) * 2;
  }

  public static String getCrisisHullmod(int additionalMod) {
    LiminalCrisisIntel intel = LiminalCrisisIntel.getIntel();
    int mod = 0;
    if (intel != null) {
      mod = getCrisisStrengthStepAt(intel.getProgress());
    }
    return getHullmodForDifficulty(getCrisisDifficulty(), mod + additionalMod);
  }

  public static int getCrisisStrengthStepAt(int progress) {
    if (progress >= 400) {
      return 2;
    }
    if (progress >= 200) {
      return 1;
    }
    return 0;
  }

  public static String getCrisisHullmodAt(int progress) {
    return getHullmodForDifficulty(getCrisisDifficulty(), getCrisisStrengthStepAt(progress));
  }

  public static String getCrisisHullmod() {
    return getCrisisHullmod(0);
  }

  public static String toPercent(String hullmod) {
    int index = hullmod.lastIndexOf('_');
    if (index == -1) {
      return "0";
    }
    return hullmod.substring(index + 1);
  }

}

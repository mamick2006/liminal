package liminal.crossmod;

import java.lang.invoke.MethodHandles;
import java.util.List;

public final class SotfSeam {

  public static final String TRACKER = "data.scripts.campaign.plugins.fel.SotfGuiltTracker";

  private SotfSeam() {
  }

  public static boolean exemptFromGuilt(String factionId) {
    if (factionId == null) {
      return false;
    }
    try {
      Class<?> tracker = Class.forName(TRACKER);
      Object list = MethodHandles.publicLookup()
          .findStaticGetter(tracker, "GUILTY_FACTIONS", List.class).invoke();
      if (!(list instanceof List)) {
        return false;
      }
      @SuppressWarnings("unchecked")
      List<Object> factions = (List<Object>) list;
      if (!factions.contains(factionId)) {
        factions.add(factionId);
      }
      return true;
    } catch (Throwable t) {
      return false;
    }
  }
}

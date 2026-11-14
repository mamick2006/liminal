package liminal.instructions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ResearchVisit {

  private Map<String, Integer> openingLevels;
  private int spent;
  private final List<String> templatesUnlocked = new ArrayList<String>();
  private boolean pressPending;

  public boolean canUndo() {
    return openingLevels != null;
  }

  public void beforePurchase(Map<String, Integer> levels, int cost) {
    if (openingLevels == null) {
      openingLevels = new HashMap<String, Integer>(levels);
    }
    spent += cost;
  }

  public void templateUnlocked(String hullModId) {
    templatesUnlocked.add(hullModId);
  }

  public List<String> getTemplatesUnlocked() {
    return new ArrayList<String>(templatesUnlocked);
  }

  public int undo(Map<String, Integer> levels) {
    if (openingLevels == null) {
      return 0;
    }
    levels.clear();
    levels.putAll(openingLevels);
    int refund = spent;
    end();
    return refund;
  }

  public void end() {
    openingLevels = null;
    spent = 0;
    templatesUnlocked.clear();
    pressPending = false;
  }

  public void notePress() {
    pressPending = true;
  }

  public void onResearchTabBuilt() {
    if (!pressPending) {
      end();
    }
    pressPending = false;
  }
}

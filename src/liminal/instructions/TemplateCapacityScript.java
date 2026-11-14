package liminal.instructions;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.RefitScreenListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.IntervalUtil;

public class TemplateCapacityScript implements EveryFrameScript, RefitScreenListener {
  private static final String KEY = "liminal_instructions";
  private final IntervalUtil interval = new IntervalUtil(1f, 1f);

  @Override
  public boolean isDone() {
    return false;
  }

  @Override
  public boolean runWhilePaused() {
    return true;
  }

  @Override
  public void advance(float amount) {
    interval.advance(amount);
    if (!interval.intervalElapsed()) {
      return;
    }
    recount();
  }

  @Override
  public void reportFleetMemberVariantSaved(FleetMemberAPI member, MarketAPI dockedAt) {
    recount();
  }

  private static void recount() {
    if (Global.getSector().getPersistentData().containsKey(KEY)) {
      InstructionsManager.getInstance().refreshCapacity(true);
    }
  }
}

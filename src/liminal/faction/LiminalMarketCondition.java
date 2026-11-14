package liminal.faction;

import com.fs.starfarer.api.impl.campaign.econ.BaseHazardCondition;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class LiminalMarketCondition extends BaseHazardCondition {

  public static float STAB_BONUS = 5f;
  public static float FLEET_SIZE_MULT = 4f;
  public static float FLEET_SPAWN_RATE_MULT = 1.2f;
  public static float GROUND_DEFENSES_MOD = 2.0f;
  public static float ACCESS_MOD = 0.0f;
  public static float HAZARD_MULT = 0.0f;

  public void apply(String id) {
    super.apply(id);
    market.getStability().modifyFlat(id, STAB_BONUS, "Liminal Gateway");
    market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult(id, FLEET_SIZE_MULT,
        "Liminal Gateway");
    market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SPAWN_RATE_MULT).modifyMult(id, FLEET_SPAWN_RATE_MULT,
        "Liminal Gateway");
    market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(id, GROUND_DEFENSES_MOD,
        "Liminal Gateway");
    market.getAccessibilityMod().modifyMult(id, ACCESS_MOD, "Liminal Gateway");
    market.getHazard().modifyMult(id, HAZARD_MULT, "Liminal Gateway");
  }

  public void unapply(String id) {
    super.unapply(id);
    market.getStability().unmodify(id);
    market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodify(id);
    market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SPAWN_RATE_MULT).unmodify(id);
    market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodify(id);
    market.getAccessibilityMod().unmodify(id);
    market.getHazard().unmodify(id);
  }

  protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
    super.createTooltipAfterDescription(tooltip, expanded);

    tooltip.addPara("%s stability",
        10f, Misc.getHighlightColor(),
        "+" + (int) STAB_BONUS);
    tooltip.addPara("%s fleet size",
        10f, Misc.getHighlightColor(),
        "+" + FLEET_SIZE_MULT + "x");
    tooltip.addPara("%s fleet spawn rate",
        10f, Misc.getHighlightColor(),
        "+" + FLEET_SPAWN_RATE_MULT + "x");
    tooltip.addPara("%s ground defenses",
        10f, Misc.getHighlightColor(),
        "+" + GROUND_DEFENSES_MOD + "x");
    tooltip.addPara("%s accessibility",
        10f, Misc.getHighlightColor(),
        "+" + ACCESS_MOD + "x");
    tooltip.addPara("%s hazard",
        10f, Misc.getHighlightColor(),
        "+" + HAZARD_MULT + "x");
  }
}

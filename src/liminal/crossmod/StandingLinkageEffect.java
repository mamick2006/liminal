package liminal.crossmod;

import java.util.ArrayList;
import java.util.List;

import cq.api.BaseCqFurnishingEffect;
import cq.items.CqCatalog.Furnishing;

public class StandingLinkageEffect extends BaseCqFurnishingEffect {

  @Override
  public List<String> effectLines(Furnishing furnishing) {
    List<String> lines = new ArrayList<String>(2);
    lines.add(StandingLinkage.researchLine());
    lines.add(StandingLinkage.wardLine());
    return lines;
  }

  @Override
  public String[] effectHighlights(Furnishing furnishing) {
    return new String[] { StandingLinkage.researchToken(), StandingLinkage.wardToken() };
  }
}

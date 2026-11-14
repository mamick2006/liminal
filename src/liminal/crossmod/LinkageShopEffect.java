package liminal.crossmod;

import liminal.LiminalSettings;
import nomad.api.BaseNomadWorkEffect;

public class LinkageShopEffect extends BaseNomadWorkEffect {

  private static final String CAPACITY_KEY = "liminal_nomad_linkage_shop_capacity";
  private static final int CAPACITY_FALLBACK = 2;

  private static final String WORK_ID = "liminal_linkage_shop";

  public static int liveCapacity() {
    if (!nomad.api.NomadWorks.isEffectOn(WORK_ID)) {
      return 0;
    }
    return Math.max(0, LiminalSettings.getInt(CAPACITY_KEY, CAPACITY_FALLBACK));
  }
}

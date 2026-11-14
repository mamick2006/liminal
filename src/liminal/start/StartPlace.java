package liminal.start;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.CommMessageAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.util.Misc;

import liminal.LGlobals;
import liminal.LiminalStrings;

public final class StartPlace {

  public static final String ACADEMY_ID = "station_galatia_academy";

  public static final String MARKET_KEY = "$liminal_start_market";

  private StartPlace() {
  }

  public static void decide() {
    SectorAPI sector = Global.getSector();
    MemoryAPI memory = sector.getMemoryWithoutUpdate();
    String id = memory.getString(MARKET_KEY);
    if (id != null && sector.getEconomy().getMarket(id) != null) {
      return;
    }
    if (sector.getEntityById(ACADEMY_ID) != null) {
      memory.unset(MARKET_KEY);
      return;
    }
    pick();
  }

  public static MarketAPI market() {
    SectorAPI sector = Global.getSector();
    String id = sector.getMemoryWithoutUpdate().getString(MARKET_KEY);
    if (id == null) {
      return null;
    }
    MarketAPI market = sector.getEconomy().getMarket(id);
    return market != null ? market : pick();
  }

  public static boolean isStandIn(MarketAPI market) {
    if (market == null) {
      return false;
    }
    String id = Global.getSector().getMemoryWithoutUpdate().getString(MARKET_KEY);
    return id != null && id.equals(market.getId());
  }

  public static boolean isHere(SectorEntityToken entity, MemoryAPI local) {
    if (entity == null) {
      return false;
    }
    if (ACADEMY_ID.equals(entity.getId())) {
      return true;
    }
    if (local == null || !isStandIn(entity.getMarket())) {
      return false;
    }
    return "main".equals(local.getString("$menuState")) && !"NONE".equals(local.getString("$tradeMode"));
  }

  public static String name() {
    MarketAPI market = market();
    return market == null ? LiminalStrings.dialog("place_academy") : market.getName();
  }

  public static String shortName() {
    MarketAPI market = market();
    return market == null ? LiminalStrings.dialog("place_academy_short") : market.getName();
  }

  public static void tell(String format, String icon, Color highlight) {
    MarketAPI market = market();
    if (market == null) {
      return;
    }
    String system = market.getContainingLocation() == null ? "" : market.getContainingLocation().getName();
    MessageIntel message = new MessageIntel(format, Misc.getTextColor(),
        new String[] { market.getName(), system }, highlight, highlight);
    message.setIcon(icon);
    Global.getSector().addTransientScript(new Teller(message));
  }

  private static final class Teller implements EveryFrameScript {
    private final MessageIntel message;
    private boolean done;

    Teller(MessageIntel message) {
      this.message = message;
    }

    @Override
    public boolean isDone() {
      return done;
    }

    @Override
    public boolean runWhilePaused() {
      return true;
    }

    @Override
    public void advance(float amount) {
      if (done) {
        return;
      }
      done = true;
      Global.getSector().getCampaignUI().addMessage(message, CommMessageAPI.MessageClickAction.NOTHING, null);
    }
  }

  private static MarketAPI pick() {
    SectorAPI sector = Global.getSector();
    MemoryAPI memory = sector.getMemoryWithoutUpdate();
    List<MarketAPI> markets = new ArrayList<MarketAPI>();
    for (MarketAPI market : sector.getEconomy().getMarketsCopy()) {
      if (market.getPrimaryEntity() == null || LGlobals.liminalFactionId.equals(market.getFactionId())) {
        continue;
      }
      markets.add(market);
    }
    String[] ids = new String[markets.size()];
    int[] sizes = new int[markets.size()];
    boolean[] open = new boolean[markets.size()];
    for (int i = 0; i < ids.length; i++) {
      MarketAPI market = markets.get(i);
      ids[i] = market.getId();
      sizes[i] = market.getSize();
      open[i] = !market.isHidden() && !(market.getPrimaryEntity() instanceof CampaignFleetAPI)
          && !market.getFaction().isHostileTo(Factions.PLAYER);
    }
    int picked = StartPlaceMath.pick(ids, sizes, open);
    if (picked < 0) {
      memory.unset(MARKET_KEY);
      return null;
    }
    MarketAPI market = markets.get(picked);
    memory.set(MARKET_KEY, market.getId());
    Global.getLogger(StartPlace.class).info("Liminal: this sector has no Galatia Academy, so Liminal"
        + " and Gacha Hullmods start at " + market.getName() + " (" + market.getId() + ").");
    return market;
  }
}

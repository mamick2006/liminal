package liminal.intel;

import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.CommMessageAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import liminal.LGlobals;
import liminal.LiminalStrings;
import liminal.faction.LUtils;
import liminal.start.StartPlace;

public class LiminalCrisisTimeListener implements EconomyTickListener {
  float monthsSinceLastInvastion = 0;
  float daysTillCapture = -999;
  private String invasionTargetId = null;
  private transient CampaignFleetAPI invasionFleet = null;
  private String invasionFleetId = null;
  private static final String KEY = "liminal_crisis_time_listener";

  public static LiminalCrisisTimeListener getInstance() {
    if (!Global.getSector().getPersistentData().containsKey(KEY)) {
      Global.getLogger(LiminalCrisisTimeListener.class).info("Needed to make new persistent data for Liminal.");
      Global.getSector().getPersistentData().put(KEY, new LiminalCrisisTimeListener());
    }

    return (LiminalCrisisTimeListener) Global.getSector().getPersistentData().get(KEY);
  }

  private LiminalCrisisTimeListener() {
  }

  private MarketAPI invasionTarget() {
    return invasionTargetId == null ? null : Global.getSector().getEconomy().getMarket(invasionTargetId);
  }

  @Override
  public void reportEconomyTick(int iterIndex) {
    LiminalCrisisIntel lci = LiminalCrisisIntel.getIntel();
    if (lci != null && lci.crisisEnded()) {
      return;
    }
    if (!lci.crisisBegun()) {
      return;
    }

    int numIter = (int) Global.getSettings().getFloat("economyIterPerMonth");
    CampaignFleetAPI fleet = getInvasionFleet();
    if (fleet == null) {
      monthsSinceLastInvastion += 1f / numIter;
      getInvasionTargetTrySetting();
      if (monthsSinceLastInvastion >= LUtils.getInvasionFrequencyMonths()) {
        monthsSinceLastInvastion = 0;
        createInvasionFleet();
      }
    } else {
      try {
        MarketAPI target = invasionTarget();
        float distance = Misc.getDistance(fleet.getLocation(), target.getPrimaryEntity().getLocation());
        if (fleet.getFleetData().getMembersListCopy().size() < 5) {
          fleet.getMemoryWithoutUpdate().clear();
          fleet.clearAssignments();
          fleet.deflate();
          fleet.despawn(FleetDespawnReason.DESTROYED_BY_BATTLE, null);
          clearInvasionFleet(true);
        } else {
          if (distance < 2000f && daysTillCapture == -999) {
            daysTillCapture = 10;
            MessageIntel intel = new MessageIntel("Liminal Invasion", LGlobals.liminalityColor);
            intel.addLine(LiminalStrings.intel("msg_invasion_capture_warning"), Misc.getTextColor(),
                new String[] { "Liminal", target.getName(), (int) daysTillCapture + " days" },
                LGlobals.liminalityColor,
                LGlobals.liminalityColor, LGlobals.liminalityColor);
            intel.setIcon(Global.getSettings().getSpriteName("icons", "liminal_intel_crisis"));
            Global.getSector().getCampaignUI().addMessage(intel, CommMessageAPI.MessageClickAction.NOTHING, null);
          } else if (daysTillCapture > 0) {
            daysTillCapture -= 30f / numIter;
          } else if (daysTillCapture <= 0 && daysTillCapture != -999) {
            performTakeover();
            fleet.clearAssignments();
            fleet.addAssignment(FleetAssignment.DEFEND_LOCATION, target.getPrimaryEntity(), 999999f);
            MessageIntel intel = new MessageIntel("Liminal Invasion", LGlobals.liminalityColor);
            intel.addLine(LiminalStrings.intel("msg_invasion_captured"), Misc.getTextColor(),
                new String[] { "Liminal", target.getName() },
                LGlobals.liminalityColor);
            intel.setIcon(Global.getSettings().getSpriteName("icons", "liminal_intel_crisis"));
            intel.setSound(LiminalCrisisIntel.getSoundColonyThreat());
            Global.getSector().getCampaignUI().addMessage(intel, CommMessageAPI.MessageClickAction.NOTHING, null);
            clearInvasionFleet(false);
          }

        }
      } catch (Exception e) {
        Global.getLogger(LiminalCrisisTimeListener.class).error("Error checking invasion fleet size", e);
        clearInvasionFleet(true);
      }
    }
  }

  private void performTakeover() {
    MarketAPI target = invasionTarget();
    target.setFactionId(LGlobals.liminalFactionId);
    target.setPlayerOwned(false);
    target.setAdmin(null);
    target.setFreePort(false);
    target.setUseStockpilesForShortages(false);

    for (SectorEntityToken curr : target.getConnectedEntities()) {
      curr.setFaction(LGlobals.liminalFactionId);
    }
  }

  @Override
  public void reportEconomyMonthEnd() {
    LiminalCrisisIntel lci = LiminalCrisisIntel.getIntel();
    if (lci != null && lci.crisisEnded()) {
      return;
    }
    if (!lci.crisisBegun()) {
      return;
    }
    CampaignFleetAPI fleet = getInvasionFleet();
    MarketAPI target = invasionTarget();
    if (fleet != null && target != null) {
      fleet.clearAssignments();
      fleet.addAssignment(FleetAssignment.ATTACK_LOCATION, target.getPrimaryEntity(), 999999f);
    }
  }

  public void createInvasionFleet() {
    MarketAPI target = invasionTarget();
    if (target == null) {
      return;
    }
    FleetParamsV3 params = new FleetParamsV3(
        null,
        target.getLocation(),
        LGlobals.liminalFactionId,
        null,
        FleetTypes.TASK_FORCE,
        1600f,
        0f,
        0f,
        0f,
        0f,
        0f,
        0f
    );
    params.ignoreMarketFleetSizeMult = true;
    CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
    float angle = (float) (Math.random() * Math.PI * 2);
    float distance = 15000f;
    float spawnX = target.getPrimaryEntity().getLocation().x + (float) Math.cos(angle) * distance;
    float spawnY = target.getPrimaryEntity().getLocation().y + (float) Math.sin(angle) * distance;
    fleet.setLocation(spawnX, spawnY);
    target.getContainingLocation().addEntity(fleet);
    fleet.addAssignment(FleetAssignment.ATTACK_LOCATION, target.getPrimaryEntity(), 999999f);
    List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
    for (FleetMemberAPI member : members) {
      member.getRepairTracker().setCR(0.7f);
    }

    fleet.getFleetData().sort();
    fleet.getFleetData().setSyncNeeded();
    fleet.getFleetData().syncIfNeeded();
    fleet.setTransponderOn(true);
    invasionFleet = fleet;
    invasionFleetId = fleet.getId();
    MessageIntel intel = new MessageIntel("Liminal Invasion", LGlobals.liminalityColor);
    intel.addLine(LiminalStrings.intel("msg_invasion_appeared"), Misc.getTextColor(),
        new String[] { "Liminal", target.getContainingLocation().getName(), target.getName() },
        LGlobals.liminalityColor,
        LGlobals.liminalityColor, LGlobals.liminalityColor);
    intel.setIcon(Global.getSettings().getSpriteName("icons", "liminal_intel_crisis"));
    intel.setSound(LiminalCrisisIntel.getSoundColonyThreat());
    Global.getSector().getCampaignUI().addMessage(intel, CommMessageAPI.MessageClickAction.NOTHING, null);
  }

  public CampaignFleetAPI getInvasionFleet() {
    if (invasionFleet == null && invasionFleetId != null) {
      for (LocationAPI loc : Global.getSector().getAllLocations()) {
        for (CampaignFleetAPI f : loc.getFleets()) {
          if (invasionFleetId.equals(f.getId())) {
            invasionFleet = f;
            return f;
          }
        }
      }
      invasionFleetId = null;
    }
    return invasionFleet;
  }

  public void clearInvasionFleet(boolean fleetDestroyed) {
    if (fleetDestroyed) {
      try {
        CampaignFleetAPI fleet = getInvasionFleet();
        if (fleet != null) {
          fleet.getMemoryWithoutUpdate().clear();
          fleet.clearAssignments();
          fleet.deflate();
          fleet.despawn(FleetDespawnReason.DESTROYED_BY_BATTLE, null);
        }
      } catch (Exception e) {
        Global.getLogger(LiminalCrisisTimeListener.class).error("Error clearing invasion fleet", e);
      }
      MessageIntel intel = new MessageIntel("Liminal Invasion", LGlobals.liminalityColor);
      intel.addLine(LiminalStrings.intel("msg_invasion_destroyed"), Misc.getTextColor(),
          new String[] { "Liminal" },
          LGlobals.liminalityColor);
      intel.setIcon(Global.getSettings().getSpriteName("icons", "liminal_intel_crisis"));
      intel.setSound(LiminalCrisisIntel.getSoundStandardUpdate());
      Global.getSector().getCampaignUI().addMessage(intel, CommMessageAPI.MessageClickAction.INTEL_TAB, null);
    }
    invasionFleet = null;
    invasionFleetId = null;
    monthsSinceLastInvastion = 0;
    invasionTargetId = null;
    daysTillCapture = -999;
  }

  private boolean validTarget(MarketAPI market) {
    try {
      if (market == null) {
        return false;
      }
      FactionAPI marketFaction = market.getFaction();
      if (marketFaction == null || marketFaction.getId().equals(LGlobals.liminalFactionId)
          || marketFaction.isNeutralFaction()) {
        return false;
      }
      if (market.hasCondition(Conditions.ABANDONED_STATION) || market.isHidden() || !market.isInEconomy()
          || market.getContainingLocation().isHyperspace()) {
        return false;
      }
      if (market.getPrimaryEntity() instanceof CampaignFleetAPI) {
        return false;
      }
      if (StartPlace.isStandIn(market)) {
        return false;
      }
      return true;
    } catch (Exception e) {
      Global.getLogger(LiminalCrisisTimeListener.class).error("Error checking valid target", e);
      return false;
    }
  }

  public MarketAPI getInvasionTargetTrySetting() {
    if (invasionTargetId == null) {
      setInvasionTarget();
    }
    if (!validTarget(invasionTarget())) {
      invasionTargetId = null;
      setInvasionTarget();
    }
    return invasionTarget();
  }

  public float getMonthsSinceLastInvasion() {
    return monthsSinceLastInvastion;
  }

  public void setInvasionTarget() {
    setInvasionTarget(0);
  }

  public void setInvasionTarget(int forceMonthsSinceLastInvasion) {
    WeightedRandomPicker<MarketAPI> picker = new WeightedRandomPicker<>();
    for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
      if (validTarget(market)) {
        picker.add(market, market.getSize());
      }
    }
    monthsSinceLastInvastion = forceMonthsSinceLastInvasion;

    if (!picker.isEmpty()) {
      MarketAPI target = picker.pick();
      invasionTargetId = target.getId();
      MessageIntel intel = new MessageIntel("Liminal Invasion", LGlobals.liminalityColor);
      intel.addLine(LiminalStrings.intel("msg_invasion_incoming"), Misc.getTextColor(),
          new String[] { "Liminal",
              target.getName() + " in the " + target.getContainingLocation().getName(),
              LiminalMessageMath.timeUntil(
                  LUtils.getInvasionFrequencyMonths() - forceMonthsSinceLastInvasion) },
          LGlobals.liminalityColor,
          LGlobals.liminalityColor,
          LGlobals.liminalityColor);
      intel.setIcon(Global.getSettings().getSpriteName("icons", "liminal_intel_crisis"));
      intel.setSound(LiminalCrisisIntel.getSoundColonyThreat());
      Global.getSector().getCampaignUI().addMessage(intel, CommMessageAPI.MessageClickAction.INTEL_TAB, null);

    }
  }
}

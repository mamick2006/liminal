package liminal.intel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.CommMessageAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.misc.MapMarkerIntel;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import liminal.LGlobals;
import liminal.LiminalStrings;
import liminal.faction.LUtils;
import liminal.faction.LiminalFaction;
import liminal.instructions.InstructionsManager;
import liminal.start.StartPlace;
import liminal.topbar.LiminalMgmtInjector;
import liminal.ui.CapacityStrip;
import liminal.ui.InstructionTopPanel;
import liminal.ui.LiminalConsolePanel;
import liminal.ui.LiminalIllustration;
import liminal.ui.TabbedPanelsInterface;
import liminal.LiminalSettings;

class CheckRPProgress extends BaseEventFactor {

  private LiminalCrisisIntel intel;

  public CheckRPProgress(LiminalCrisisIntel intel) {
    this.intel = intel;
  }

  @Override
  public int getProgress(BaseEventIntel intel) {
    this.intel.updateProgress();
    return 0;
  }

  public boolean shouldShow(BaseEventIntel intel) {
    return false;
  }

  public String getDesc(BaseEventIntel intel) {
    return "";
  }

}

public class LiminalCrisisIntel extends BaseEventIntel {

  CheckRPProgress checkRPProgress;
  private int liminalCrisisBeginsRP = 300;
  private boolean crisisBegun = false;

  private final int MAX_SHIP_DESTROYED_PROGRESS = 100;
  private int shipDestroyedProgress = 0;
  private boolean crisisEnded = false;

  private static final int STAGING_GROUNDS = 4;
  private static final int STAGING_GROUND_PROGRESS = 150;

  private static final String ENTRY_NAME = "Liminal Management and Crisis";

  public boolean crisisBegun() {
    return crisisBegun;
  }

  private enum Stage {
    BEGINNING, CRISIS_BEGINS, CRISIS_1, CRISIS_2, CRISIS_3, CRISIS_4, CRISIS_5, CRISIS_6,
  }

  private static final String KEY = "liminal_crisis_intel";

  public static LiminalCrisisIntel getIntel() {
    if (!Global.getSector().getPersistentData().containsKey(KEY)) {
      return null;
    }

    return (LiminalCrisisIntel) Global.getSector().getPersistentData().get(KEY);
  }

  public LiminalCrisisIntel() {
    super();

    initStages();
    Global.getSector().getPersistentData().put(KEY, this);
  }

  public boolean crisisEnded() {
    return crisisEnded;
  }

  public void setCrisisEnded(boolean crisisEnded) {
    this.crisisEnded = crisisEnded;
  }

  public void postInit() {
    checkRPProgress = new CheckRPProgress(this);
    addFactor(checkRPProgress);

    updateProgress();
  }

  public void initStages() {
    if (!crisisBegun) {
      this.stages = new ArrayList<>();
      setMaxProgress(liminalCrisisBeginsRP);
      addStage(Stage.BEGINNING, 0);
      addStage(Stage.CRISIS_BEGINS, liminalCrisisBeginsRP);
    } else {
      this.stages = new ArrayList<>();
      setMaxProgress(600);
      addStage(Stage.BEGINNING, 0, true);
      addStage(Stage.CRISIS_1, 100, true);
      addStage(Stage.CRISIS_2, 200, true);
      addStage(Stage.CRISIS_3, 300, true);
      addStage(Stage.CRISIS_4, 400, true);
      addStage(Stage.CRISIS_5, 500, true);
      addStage(Stage.CRISIS_6, 600, true);
    }
  }

  public void updateDestroyedProgress(float multDestroyed, int shipsDestroyed) {
    int originalProgress = shipDestroyedProgress;
    shipDestroyedProgress += (int) multDestroyed;
    if (shipDestroyedProgress > MAX_SHIP_DESTROYED_PROGRESS) {
      shipDestroyedProgress = MAX_SHIP_DESTROYED_PROGRESS;
    }
    if (originalProgress != shipDestroyedProgress) {
      MessageIntel intel = new MessageIntel("Liminal Invasion", LGlobals.liminalityColor);
      intel.addLine(LiminalStrings.intel("msg_ships_destroyed"), Misc.getTextColor(),
          new String[] { String.valueOf(shipsDestroyed),
              "+" + String.valueOf(shipDestroyedProgress - originalProgress) },
          LGlobals.liminalityColor,
          LGlobals.liminalityColor);
      intel.setIcon(Global.getSettings().getSpriteName("icons", "liminal_intel_crisis"));
      Global.getSector().getCampaignUI().addMessage(intel, CommMessageAPI.MessageClickAction.INTEL_TAB, null);

      updateProgress();
    }
  }

  public int getRemainingInvasionPlanets() {
    int remainingInvasionPlanets = 0;
    for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
      if (isStagingGround(market)) {
        remainingInvasionPlanets++;
      }
    }
    Global.getSector().getMemoryWithoutUpdate().set("$liminalRemainingPlanets", remainingInvasionPlanets);
    return remainingInvasionPlanets;
  }

  private static boolean isStagingGround(MarketAPI market) {
    return market.hasTag(LGlobals.liminalInvasionTag);
  }

  private static List<String> stagingGroundsLeft() {
    List<String> left = new ArrayList<>();
    for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
      if (isStagingGround(market)) {
        left.add(market.getName());
      }
    }
    return left;
  }

  public void updateProgress() {
    if (crisisEnded) {
      return;
    }
    if (crisisBegun) {
      int totalProgress = shipDestroyedProgress;

      totalProgress += (STAGING_GROUNDS - getRemainingInvasionPlanets()) * STAGING_GROUND_PROGRESS;
      totalProgress = Math.min(totalProgress, getMaxProgress());
      setProgress(totalProgress);
      return;
    }
    int updatedRP = LiminalSettings.lunaInt("liminal_crisis_begins", 300);
    if (!LiminalSettings.getBoolean("liminal_crisis_enabled", true)) {
      updatedRP = Math.max(updatedRP, 1 + InstructionsManager.getInstance().getTotalRPGained());
    }
    if (updatedRP != liminalCrisisBeginsRP) {
      liminalCrisisBeginsRP = updatedRP;
      initStages();
    }
    setProgress(0);
    setProgress(InstructionsManager.getInstance().getTotalRPGained());
  }

  @Override
  public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
    info.addPara(ENTRY_NAME, getTitleColor(mode), 0f);
    if (mode.equals(ListInfoMode.MESSAGES)) {
      EventStageData esd = (EventStageData) getListInfoParam();
      if (esd.id.equals(Stage.CRISIS_BEGINS)) {
        info.addPara(LiminalStrings.intel("stage_msg_begun"), 5f, getStageColor(null),
            "invasion");
      } else if (esd.id.equals(Stage.CRISIS_1)) {
        info.addPara(LiminalStrings.intel("stage_msg_continues"), 5f, getStageColor(null),
            "The Servants of the Entity");
      } else if (esd.id.equals(Stage.CRISIS_2)) {
        info.addPara(LiminalStrings.intel("stage_msg_continues"), 5f, getStageColor(null),
            "The Servants of the Entity");
      } else if (esd.id.equals(Stage.CRISIS_3)) {
        info.addPara(LiminalStrings.intel("stage_msg_continues"), 5f, getStageColor(null),
            "The Servants of the Entity");
      } else if (esd.id.equals(Stage.CRISIS_4)) {
        info.addPara(LiminalStrings.intel("stage_msg_continues"), 5f, getStageColor(null),
            "The Servants of the Entity");
      } else if (esd.id.equals(Stage.CRISIS_5)) {
        info.addPara(LiminalStrings.intel("stage_msg_continues"), 5f, getStageColor(null),
            "The Servants of the Entity");
      } else if (esd.id.equals(Stage.CRISIS_6)) {
        info.addPara(LiminalStrings.intel("stage_msg_return"), 5f, getStageColor(null),
            StartPlace.name());
      }
    }
    addBulletPoints(info, mode);
  }

  @Override
  public Color getTitleColor(ListInfoMode mode) {
    return LGlobals.liminalityColor;
  }

  @Override
  public Color getStageColor(Object stageId) {
    return LGlobals.liminalityHighlightColor;
  }

  public void beginCrisis() {
    Global.getSoundPlayer().playUISound(LiminalCrisisIntel.getSoundColonyThreat(), 1f, 1f);
    crisisBegun = true;
    shipDestroyedProgress = 0;
    LiminalFaction.initializeSystems();
    initStages();
    setProgress(0);
    if (!Global.getSector().getListenerManager().hasListenerOfClass(LiminalCrisisTimeListener.class)) {
      Global.getSector().getListenerManager().addListener(LiminalCrisisTimeListener.getInstance());
    }
    LiminalCrisisTimeListener listener = LiminalCrisisTimeListener.getInstance();
    listener.setInvasionTarget(999);
    getRemainingInvasionPlanets();
  }

  @Override
  public void notifyStageReached(EventStageData stage) {
    if (stage.id.equals(Stage.CRISIS_BEGINS)) {
      Global.getLogger(this.getClass()).info("Beginning crisis.");
      beginCrisis();
    } else if (stage.id.equals(Stage.CRISIS_1) || stage.id.equals(Stage.CRISIS_2)
        || stage.id.equals(Stage.CRISIS_3) || stage.id.equals(Stage.CRISIS_4)
        || stage.id.equals(Stage.CRISIS_5) || stage.id.equals(Stage.CRISIS_6)) {
    } else {
      throw new RuntimeException("Unknown stage: " + stage.id);
    }
  }

  @Override
  public void addStageDescriptionText(TooltipMakerAPI info, float width, Object stageId) {
    if (crisisEnded) {
      if (stageId.equals(Stage.CRISIS_6)) {
        info.addPara(LiminalStrings.intel("crisis_over_desc"), 5f,
            LGlobals.liminalityColor);
      }
      return;
    }
    if (stageId.equals(Stage.CRISIS_BEGINS)) {
      boolean crisisEnabled = LiminalSettings.getBoolean("liminal_crisis_enabled", true);
      if (crisisEnabled) {
        info.addPara(LiminalStrings.intel("crisis_begins_desc"),
            5f, LGlobals.liminalityColor, "Liminality", "" + liminalCrisisBeginsRP);
      } else {
        info.addPara(LiminalStrings.intel("crisis_begins_desc_disabled"),
            5f, LGlobals.liminalityColor, "Liminality", "[Crisis disabled in settings]");
      }
      info.addPara(LiminalStrings.intel("crisis_influence_desc"), 5f,
          LGlobals.liminalityColor,
          "Liminality");
      info.addPara(LiminalStrings.intel("crisis_forces_desc"), 5f,
          LGlobals.liminalityColor,
          "extremely dangerous");
    }
    if (canFightBoss()) {
      if (stageId.equals(Stage.CRISIS_6)) {
        info.addPara(LiminalStrings.intel("crisis_stage6_desc"), 5f,
            LGlobals.liminalityColor, "Servants of the Entity", StartPlace.name());
      }
    } else {
      if (stageId.equals(Stage.CRISIS_1)) {
        info.addPara(LiminalStrings.intel("crisis_stage1_desc"), 5f,
            getStageColor(stageId), "Servants of the Entity");
        info.addPara(
            LiminalStrings.intel("crisis_stage1_progress_desc"),
            5f,
            getStageColor(stageId), "Servants'");
      } else if (stageId.equals(Stage.CRISIS_2)) {
        info.addPara(
            LiminalStrings.intel("crisis_stage2_desc"),
            5f,
            getStageColor(stageId), "Servants");
      }
    }
  }

  public boolean canFightBoss() {
    if (crisisEnded) {
      return false;
    }
    return crisisBegun && getProgress() == maxProgress;
  }

  private static final float TILE_WIDTH = 180f;
  private static final float TILE_GAP = 10f;
  private static final float TILE_HEIGHT = 60f;
  private static final float TILE_VALUE_Y = 17f;
  private static final float TILE_SUB_Y = 44f;
  private static final float TILE_BOTTOM = 1f;
  private static final float TILES_PAD = 20f;
  private static final float HOVER_WIDTH = 400f;
  private static final float PAD = 3f;
  private static final float OPAD = 10f;
  private static final boolean HOVER_ON_TILE = true;

  private static final class Tile {
    final String caption;
    final String value;
    final String sub;
    final String subHighlight;
    final TooltipMakerAPI.TooltipCreator hover;

    Tile(String caption, String value, String sub, String subHighlight,
        TooltipMakerAPI.TooltipCreator hover) {
      this.caption = caption;
      this.value = value;
      this.sub = sub;
      this.subHighlight = subHighlight;
      this.hover = hover;
    }
  }

  private abstract static class CrisisHover extends LGlobals.InfoTooltip {
    @Override
    public float getTooltipWidth(Object tooltipParam) {
      return HOVER_WIDTH;
    }
  }

  private List<Tile> tiles() {
    List<Tile> tiles = new ArrayList<>();
    boolean during = crisisBegun && !crisisEnded;
    if (during) {
      tiles.add(invasionTile());
      List<String> left = stagingGroundsLeft();
      int destroyed = STAGING_GROUNDS - left.size();
      String leftLine = null;
      if (!left.isEmpty() && left.size() < STAGING_GROUNDS) {
        leftLine = String.format(LiminalStrings.intel("tile_staging_left"), Misc.getAndJoined(left));
      }
      TooltipMakerAPI.TooltipCreator progress = progressHover();
      tiles.add(new Tile("Staging grounds destroyed", destroyed + " / " + STAGING_GROUNDS, leftLine,
          null, progress));
      tiles.add(new Tile("Progress from ships",
          shipDestroyedProgress + " / " + MAX_SHIP_DESTROYED_PROGRESS, null, null, progress));
    }
    if (LUtils.getAllShipHullmodChance() > 0 || crisisBegun) {
      if (crisisEnded) {
        tiles.add(templateOpTile());
      }
      tiles.add(influenceTile(during));
    }
    return tiles;
  }

  private static String targetLine(MarketAPI target) {
    return String.format(LiminalStrings.intel("tile_invasion_target"), target.getName(),
        target.getContainingLocation().getNameWithTypeIfNebula());
  }

  private Tile invasionTile() {
    LiminalCrisisTimeListener listener = LiminalCrisisTimeListener.getInstance();
    int everyMonths = LUtils.getInvasionFrequencyMonths();
    final String frequency = everyMonths == 1 ? "month" : everyMonths + " months";
    String value = "None";
    String sub = null;
    String fleetAt = null;
    String fleetTarget = null;
    if (listener.getInvasionFleet() != null) {
      try {
        MarketAPI invasionTarget = listener.getInvasionTargetTrySetting();
        LocationAPI at = listener.getInvasionFleet().getContainingLocation();
        String atName = at.getName();
        String targetName = invasionTarget.getName();
        sub = targetLine(invasionTarget);
        fleetAt = atName;
        fleetTarget = targetName;
        value = "Under way";
      } catch (Exception e) {
        Global.getLogger(LiminalCrisisIntel.class).error("Error getting invasion fleet location", e);
        listener.clearInvasionFleet(true);
      }
    } else {
      MarketAPI invasionTarget = listener.getInvasionTargetTrySetting();
      if (invasionTarget != null) {
        value = LiminalMessageMath.timeUntil(
            LUtils.getInvasionFrequencyMonths() - listener.getMonthsSinceLastInvasion());
        sub = targetLine(invasionTarget);
      }
    }
    final String at = fleetAt;
    final String target = fleetTarget;
    return new Tile("Next invasion", value, sub, null,
        new CrisisHover() {
          @Override
          public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
            tooltip.addTitle("Next invasion");
            tooltip.addPara(LiminalStrings.intel("status_invasion_frequency"), OPAD,
                LGlobals.liminalityColor, frequency);
            if (at != null) {
              tooltip.addPara(LiminalStrings.intel("status_fleet_current"), OPAD,
                  LGlobals.liminalityColor, at, target);
            }
          }
        });
  }

  private TooltipMakerAPI.TooltipCreator progressHover() {
    return new CrisisHover() {
      @Override
      public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        List<String> left = stagingGroundsLeft();
        tooltip.addTitle("Progress");
        tooltip.addPara(LiminalStrings.intel("crisis_progress_total"), OPAD, Misc.getHighlightColor(),
            "" + getProgress());
        tooltip.beginGridFlipped(HOVER_WIDTH, 1, 64f, 10f);
        tooltip.addToGrid(0, 0,
            String.format(LiminalStrings.intel("crisis_staging_row"), "" + STAGING_GROUND_PROGRESS),
            "+" + (STAGING_GROUNDS - left.size()) * STAGING_GROUND_PROGRESS);
        if (!left.isEmpty()) {
          tooltip.addToGrid(0, 1,
              String.format(LiminalStrings.intel("crisis_ships_row"), "" + MAX_SHIP_DESTROYED_PROGRESS),
              "+" + shipDestroyedProgress);
        }
        tooltip.addGrid(PAD);
        if (!left.isEmpty()) {
          LabelAPI line = tooltip.addPara(
              String.format(LiminalStrings.intel("crisis_staging_left"), Misc.getAndJoined(left)), OPAD);
          line.setHighlightColor(Misc.getHighlightColor());
          line.setHighlight(left.toArray(new String[0]));
        }
      }
    };
  }

  private Tile influenceTile(final boolean during) {
    float chanceNow = LUtils.getAllShipHullmodChance();
    final String chance = String.format("%.1f%%", chanceNow * 100f);
    final String strength = chanceNow == 0 ? "0%" : LUtils.toPercent(LUtils.getAllShipHullmod()) + "%";
    return new Tile("Ships influenced", chance,
        String.format(LiminalStrings.intel("tile_influenced_sub"), strength), strength,
        new CrisisHover() {
          @Override
          public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
            tooltip.addTitle("Ships influenced");
            tooltip.addPara(LiminalStrings.intel("status_influence_effect"), OPAD,
                LGlobals.liminalityColor, "Liminality", "all", strength);
            tooltip.addPara(LiminalStrings.intel("status_influence_chance_note"), OPAD);
            tooltip.addPara(LiminalStrings.intel("status_influence_rate"), OPAD,
                LGlobals.liminalityColor, chance, strength, "25%");
            if (during) {
              tooltip.addPara(LiminalStrings.intel("status_faction_influence"), OPAD,
                  LGlobals.liminalityColor, LUtils.toPercent(LUtils.getCrisisHullmod()) + "%");
            }
          }
        });
  }

  private Tile templateOpTile() {
    final String bonus = Math.round((InstructionsManager.HullmodLoadout.CRISIS_OP_MULT - 1f) * 100f)
        + "%";
    return new Tile("Template OP", "+" + bonus, null, null,
        new CrisisHover() {
          @Override
          public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
            tooltip.addTitle("Template OP");
            tooltip.addPara(LiminalStrings.intel("status_crisis_over_reward"), OPAD,
                LGlobals.liminalityColor, "Liminal Templates", bonus + " more OP");
          }
        });
  }

  @Override
  public void afterStageDescriptions(TooltipMakerAPI main) {
    List<Tile> tiles = tiles();
    if (tiles.isEmpty()) {
      return;
    }
    float rowWidth = getBarWidth();
    float pitch = TILE_WIDTH + TILE_GAP;
    LabelAPI[] subs = new LabelAPI[tiles.size()];
    float rowHeight = TILE_HEIGHT;
    for (int i = 0; i < tiles.size(); i++) {
      Tile tile = tiles.get(i);
      if (tile.sub == null) {
        continue;
      }
      float x = i * pitch;
      float room = rowWidth - x;
      for (int j = i + 1; j < tiles.size(); j++) {
        if (tiles.get(j).sub != null) {
          room = j * pitch - x - TILE_GAP;
          break;
        }
      }
      LabelAPI sub = tileLabel(tile.sub, Fonts.DEFAULT_SMALL, Misc.getTextColor());
      if (sub.computeTextWidth(tile.sub) > room) {
        sub.autoSizeToWidth(room);
      }
      if (tile.subHighlight != null) {
        sub.setHighlightColor(LGlobals.liminalityColor);
        sub.setHighlight(tile.subHighlight);
      }
      subs[i] = sub;
      rowHeight = Math.max(rowHeight, TILE_SUB_Y + sub.getPosition().getHeight() + TILE_BOTTOM);
    }
    CustomPanelAPI row = Global.getSettings().createCustom(rowWidth, rowHeight, null);
    for (int i = 0; i < tiles.size(); i++) {
      Tile tile = tiles.get(i);
      CustomPanelAPI slot = row.createCustomPanel(TILE_WIDTH, rowHeight, null);
      List<LabelAPI> labels = new ArrayList<>();
      labels.add(tileLabel(tile.caption, Fonts.ORBITRON_12, Misc.getBasePlayerColor()));
      labels.add(tileLabel(tile.value, Fonts.INSIGNIA_VERY_LARGE, Misc.getHighlightColor()));
      slot.addComponent(labels.get(0)).inTL(0f, 0f);
      slot.addComponent(labels.get(1)).inTL(0f, TILE_VALUE_Y);
      if (subs[i] != null) {
        slot.addComponent(subs[i]).inTL(0f, TILE_SUB_Y);
        labels.add(subs[i]);
      }
      row.addComponent(slot).inTL(i * pitch, 0f);
      if (HOVER_ON_TILE) {
        main.addTooltipTo(tile.hover, slot, TooltipMakerAPI.TooltipLocation.BELOW, false);
      } else {
        for (LabelAPI label : labels) {
          main.addTooltipTo(tile.hover, label, TooltipMakerAPI.TooltipLocation.BELOW, false);
        }
      }
    }
    main.addCustom(row, TILES_PAD);
  }

  private static LabelAPI tileLabel(String text, String font, Color color) {
    LabelAPI label = Global.getSettings().createLabel(text, font);
    label.setColor(color);
    return label;
  }

  @Override
  public TooltipMakerAPI.TooltipCreator getBarTooltip() {
    if (crisisBegun) {
      return progressHover();
    }
    return new CrisisHover() {
      @Override
      public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        String gained = "" + InstructionsManager.getInstance().getTotalRPGained();
        if (LiminalSettings.getBoolean("liminal_crisis_enabled", true)) {
          tooltip.addPara(LiminalStrings.intel("crisis_bar_before"), 0f, Misc.getHighlightColor(),
              gained, "" + liminalCrisisBeginsRP);
        } else {
          tooltip.addPara("Total RP: %s", 0f, Misc.getHighlightColor(), gained);
        }
      }
    };
  }

  @Override
  public TooltipMakerAPI.TooltipCreator getStageTooltipImpl(Object stageId) {
    EventStageData stage = getDataFor(stageId);
    if (!crisisBegun || stage == null || stage.progress <= 0 || stage.progress >= getMaxProgress()) {
      return null;
    }
    final int at = stage.progress;
    return new CrisisHover() {
      @Override
      public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        tooltip.addTitle(String.format(LiminalStrings.intel("stage_at"), "" + at));
        tooltip.addPara(LiminalStrings.intel("status_invasion_frequency"), OPAD,
            LGlobals.liminalityColor, LUtils.getInvasionFrequencyMonthsAt(at) + " months");
        if (LUtils.getCrisisStrengthStepAt(at) != LUtils.getCrisisStrengthStepAt(at - 1)) {
          tooltip.addPara(LiminalStrings.intel("status_faction_influence"), OPAD,
              LGlobals.liminalityColor, LUtils.toPercent(LUtils.getCrisisHullmodAt(at)) + "%");
        }
      }
    };
  }

  public String getIllustrationKey() {
    return crisisBegun ? "liminal_console_crisis" : "liminal_console_crisis_pre";
  }

  public void createCrisisBody(CustomPanelAPI panel, float width, float height) {
    updateProgress();
    CustomPanelAPI mainPanel = panel.createCustomPanel(width, height, null);
    super.createLargeDescription(mainPanel, width, height);
    LiminalIllustration.fillRightOfEventUI(mainPanel, getIllustrationKey(), width, height);
    panel.addComponent(mainPanel).inTL(0, 0);
  }

  @Override
  public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
    LiminalityIntel menus = LiminalMgmtInjector.isAlive() ? null : LiminalityIntel.getIntel();
    if (menus == null) {
      createCrisisBody(panel, width, height);
      return;
    }
    LiminalConsolePanel.ConsoleTabs tabs = new LiminalConsolePanel.ConsoleTabs(menus) {
      @Override
      public String activeTab() {
        return mergedActiveTab(getTabNames());
      }
    };
    TabbedPanelsInterface.TabbedPanelsUtils.createTopLevelPanels(panel, width, height, tabs);
  }

  private static String mergedActiveTab(List<String> names) {
    String remembered = LiminalConsolePanel.restoreSubtab(LiminalConsolePanel.CRISIS_TAB);
    if (names.contains(remembered) && !TabbedPanelsInterface.LOCKED_TAB.equals(remembered)) {
      return remembered;
    }
    if (names.contains(LiminalConsolePanel.CRISIS_TAB)) {
      return LiminalConsolePanel.CRISIS_TAB;
    }
    return LiminalityIntel.FUNDING_TAB;
  }

  @Override
  public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
    if (TabbedPanelsInterface.TabbedPanelsUtils.isTabButton(buttonId)) {
      LiminalConsolePanel.rememberSubtab(
          TabbedPanelsInterface.TabbedPanelsUtils.getTabName(buttonId));
      refreshMergedView(ui);
      return;
    }
    LiminalityIntel menus = LiminalMgmtInjector.isAlive() ? null : LiminalityIntel.getIntel();
    if (menus != null) {
      menus.handleMenuButton(buttonId, new MergedHostUI(ui));
      return;
    }
    ui.updateUIForItem(this);
  }

  @Override
  public boolean doesButtonHaveConfirmDialog(Object buttonId) {
    return InstructionTopPanel.isBuyCapacityButton(buttonId);
  }

  @Override
  public void createConfirmationPrompt(Object buttonId, TooltipMakerAPI prompt) {
    CapacityStrip.addConfirmPrompt(buttonId, prompt);
  }

  @Override
  public String getConfirmText(Object buttonId) {
    return CapacityStrip.acceptText();
  }

  private void refreshMergedView(IntelUIAPI ui) {
    for (TabbedPanelsInterface.Panel p : TabbedPanelsInterface.Panel.getCurrentPanels()) {
      p.saveScrollState();
    }
    ui.updateUIForItem(this);
  }

  private final class MergedHostUI implements IntelUIAPI {
    private final IntelUIAPI ui;

    MergedHostUI(IntelUIAPI ui) {
      this.ui = ui;
    }

    @Override
    public void updateUIForItem(IntelInfoPlugin plugin) {
      ui.updateUIForItem(LiminalCrisisIntel.this);
    }

    @Override
    public void recreateIntelUI() {
      ui.recreateIntelUI();
    }

    @Override
    public void showDialog(SectorEntityToken target, String trigger) {
      ui.showDialog(target, trigger);
    }

    @Override
    public void showDialog(SectorEntityToken target, InteractionDialogPlugin plugin) {
      ui.showDialog(target, plugin);
    }

    @Override
    public void showEditIntelMarkerDialog(MapMarkerIntel intel) {
      ui.showEditIntelMarkerDialog(intel);
    }

    @Override
    public void updateIntelList() {
      ui.updateIntelList();
    }

    @Override
    public void updateIntelList(boolean retainCurrentSelection) {
      ui.updateIntelList(retainCurrentSelection);
    }

    @Override
    public void updateIntelList(boolean retainCurrentSelection, List<IntelInfoPlugin> show) {
      ui.updateIntelList(retainCurrentSelection, show);
    }

    @Override
    public void selectItem(IntelInfoPlugin plugin) {
      ui.selectItem(plugin);
    }

    @Override
    public boolean isShowingCustomIntelSubset() {
      return ui.isShowingCustomIntelSubset();
    }

    @Override
    public void showOnMap(SectorEntityToken token) {
      ui.showOnMap(token);
    }

    @Override
    public void saveSmallDescScrollState() {
      ui.saveSmallDescScrollState();
    }

    @Override
    public void restoreSmallDescScrollState() {
      ui.restoreSmallDescScrollState();
    }
  }

  @Override
  protected String getStageIconImpl(Object stageId) {
    if (stageId.equals(Stage.CRISIS_BEGINS)) {
      return Global.getSettings().getSpriteName("icons", "liminal_intel_crisis");
    } else if (stageId.equals(Stage.CRISIS_1)) {
      return Global.getSettings().getSpriteName("icons", "liminal_intel_crisis1");
    } else if (stageId.equals(Stage.CRISIS_2)) {
      return Global.getSettings().getSpriteName("icons", "liminal_intel_crisis2");
    } else if (stageId.equals(Stage.CRISIS_3)) {
      return Global.getSettings().getSpriteName("icons", "liminal_intel_crisis3");
    } else if (stageId.equals(Stage.CRISIS_4)) {
      return Global.getSettings().getSpriteName("icons", "liminal_intel_crisis4");
    } else if (stageId.equals(Stage.CRISIS_5)) {
      return Global.getSettings().getSpriteName("icons", "liminal_intel_crisis5");
    } else if (stageId.equals(Stage.CRISIS_6)) {
      return Global.getSettings().getSpriteName("icons", "liminal_intel_crisis6");
    }
    return Global.getSettings().getSpriteName("icons", "liminal_intel_crisis");
  }

  @Override
  public String getName() {
    return crisisBegun ? "Liminal Invasion" : "Crisis";
  }

  @Override
  public String getSmallDescriptionTitle() {
    return ENTRY_NAME;
  }

  @Override
  public String getIcon() {
    return Global.getSettings().getSpriteName("icons", "liminal_intel_crisis");
  }

  @Override
  public Set<String> getIntelTags(SectorMapAPI map) {
    Set<String> tags = super.getIntelTags(map);
    tags.add(Tags.INTEL_MAJOR_EVENT);
    return tags;
  }

  @Override
  public String getSortString() {
    return "Research";
  }

  @Override
  public boolean shouldRemoveIntel() {
    return false;
  }

  @Override
  public boolean isHidden() {
    return LiminalMgmtInjector.isAlive();
  }

  @Override
  public boolean withMonthlyFactors() {
    return false;
  }

  @Override
  public boolean withOneTimeFactors() {
    return false;
  }
}

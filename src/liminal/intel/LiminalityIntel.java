package liminal.intel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import liminal.LGlobals;
import liminal.LiminalStrings;
import liminal.instructions.InstructionsManager;
import liminal.instructions.InstructionsManager.HullmodLoadout;
import liminal.ui.InstructionTopPanel;
import liminal.ui.InstructionsManagement;
import liminal.ui.LiminalIllustration;
import liminal.ui.LiminalityUpkeep;
import liminal.ui.TabbedPanelsInterface;
import liminal.LiminalSettings;

class OngoingResearch extends BaseEventFactor {

  public OngoingResearch() {
  }

  @Override
  public int getProgress(BaseEventIntel intel) {
    return (int) InstructionsManager.getInstance().getResearchProgressPerMonth();
  }

  public boolean shouldShow(BaseEventIntel intel) {
    return true;
  }

  public String getDesc(BaseEventIntel intel) {
    return String.format(LiminalStrings.intel("factor_research_investment"),
        Misc.getDGSCredits(InstructionsManager.getInstance().getResearchInvestmentPerMonth()));
  }

}

public class LiminalityIntel extends BaseEventIntel implements TabbedPanelsInterface {

  public static final String MEMORY_KEY = "$liminality_research";
  public static final String FUNDING_TAB = "Funding";
  OngoingResearch ongoingResearch;
  private String activeTab;

  private static int RP_PER_EVENT = 4;

  private enum Stage {
    BEGINNING, RP1, RP2, RP3, RP4, RP5,
  }

  public static LiminalityIntel getIntel() {
    for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(LiminalityIntel.class)) {
      return (LiminalityIntel) intel;
    }
    return null;
  }

  public LiminalityIntel() {
    super();

    RP_PER_EVENT = LiminalSettings.lunaInt("liminal_rpmult", 4);
    Global.getSector().getMemoryWithoutUpdate().set(MEMORY_KEY, Boolean.TRUE);
    setMaxProgress(500);
    ongoingResearch = new OngoingResearch();
    addFactor(ongoingResearch);
    addStage(Stage.BEGINNING, 0);
    addStage(Stage.RP1, 100);
    addStage(Stage.RP2, 200);
    addStage(Stage.RP3, 300);
    addStage(Stage.RP4, 400);
    addStage(Stage.RP5, 500);
  }

  @Override
  public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
    info.addPara(getName(), getTitleColor(mode), 0f);
    if (mode.equals(ListInfoMode.MESSAGES)) {
      if (getProgress() == getMaxProgress()) {
        info.addPara(LiminalStrings.intel("msg_rp_gained"), 5f, getStageColor(null),
            "" + RP_PER_EVENT);
        if (LiminalMessageMath.minimumDoubles(
            InstructionsManager.getInstance().getBaseMinimumResearchInvestmentPerMonth(),
            InstructionsManager.MAX_MINIMUM_RESEARCH_INVESTMENT)) {
          info.addPara(LiminalStrings.intel("msg_min_investment_doubled"), 5f, getStageColor(null));
        }
      } else if (getProgress() == 0) {
        String where = LiminalMessageMath.detailsPlace(liminal.topbar.LiminalMgmtInjector.isAlive());
        info.addPara(LiminalStrings.intel("msg_open_tab_hint"), 5f, getStageColor(null), where);
      } else {
        info.addPara(LiminalStrings.intel("msg_rp_gained"), 5f, getStageColor(null),
            "" + RP_PER_EVENT);
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

  @Override
  public void notifyStageReached(EventStageData stage) {
    RP_PER_EVENT = LiminalSettings.lunaInt("liminal_rpmult", 4);
    InstructionsManager im = InstructionsManager.getInstance();
    if (stage.id.equals(Stage.RP1) || stage.id.equals(Stage.RP2) || stage.id.equals(Stage.RP3)
        || stage.id.equals(Stage.RP4) || stage.id.equals(Stage.RP5)) {
      im.addResearchPoints(RP_PER_EVENT);
    }
    if (stage.id.equals(Stage.RP5)) {
      setProgress(0);
      im.setMinimumResearchInvestmentPerMonth(
          im.getBaseMinimumResearchInvestmentPerMonth() * 2);
      if (im.getResearchInvestmentPerMonth() < im.getMinimumResearchInvestmentPerMonth()) {
        im.setResearchInvestmentPerMonth(
            im.getMinimumResearchInvestmentPerMonth());
      }
    }
  }

  @Override
  public void addStageDescriptionText(TooltipMakerAPI info, float width, Object stageId) {
    RP_PER_EVENT = LiminalSettings.lunaInt("liminal_rpmult", 4);
    if (stageId.equals(Stage.RP5)) {
      info.addPara(
          LiminalStrings.intel("research_stage_desc"),
          5f, getStageColor(stageId), "" + RP_PER_EVENT, "100",
          Misc.getDGSCredits(liminal.LiminalSurvival.price(
              InstructionsManager.MAX_MINIMUM_RESEARCH_INVESTMENT)));
      info.addPara(LiminalStrings.intel("research_invest_hint"), 5f, LGlobals.instructionColor,
          "Instructions", "templates");
    }
  }

  @Override
  public List<String> getTabNames() {
    ArrayList<String> tabs = new ArrayList<>();
    tabs.add(FUNDING_TAB);
    tabs.add("Research");
    InstructionsManager im = InstructionsManager.getInstance();
    for (String loadoutId : im.getLoadoutIds()) {
      if (Global.getSector().getCharacterData().knowsHullMod(loadoutId)) {
        tabs.add(im.getHullmodName(loadoutId));
      } else {
        tabs.add(TabbedPanelsInterface.LOCKED_TAB);
      }
    }
    return tabs;
  }

  @Override
  public Map<String, List<Panel>> getPanelsForTabs() {
    Map<String, List<Panel>> panels = new HashMap<>();
    panels.put(FUNDING_TAB, Arrays.<Panel>asList(new Panel(0.67f) {
      @Override
      public void createPanel(CustomPanelAPI panel, float width, float height) {
        createUpkeepPanel(panel, width, height);
      }
    }, new LiminalityUpkeep()));
    List<Panel> researchPanels = new ArrayList<>();
    InstructionsManager instructionsManager = InstructionsManager.getInstance();
    Panel p = new InstructionsManagement(null, instructionsManager);
    researchPanels.add(p);
    panels.put("Research",
        Arrays.<Panel>asList(new InstructionTopPanel(null, instructionsManager),
            p));
    for (String loadoutId : instructionsManager.getLoadoutIds()) {
      p = new InstructionsManagement(loadoutId, instructionsManager);
      researchPanels.add(p);
      panels.put(instructionsManager.getHullmodName(loadoutId),
          Arrays.<Panel>asList(new InstructionTopPanel(loadoutId, instructionsManager),
              p));
    }
    TabbedPanelsInterface.Panel.setCurrentPanels(researchPanels.toArray(new TabbedPanelsInterface.Panel[0]));
    return panels;
  }

  public void createUpkeepPanel(CustomPanelAPI panel, float width, float height) {
    super.createLargeDescription(panel, width, height);
    LiminalIllustration.fillRightOfEventUI(panel, "liminal_console_upkeep", width, height);
  }

  @Override
  public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
    TabbedPanelsInterface.TabbedPanelsUtils.createTopLevelPanels(panel, width,
        height, this);
  }

  @Override
  public String activeTab() {
    if (activeTab == null) {
      activeTab = FUNDING_TAB;
    }
    return activeTab;
  }

  @Override
  public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
    if (TabbedPanelsInterface.TabbedPanelsUtils.isTabButton(buttonId)) {
      activeTab = TabbedPanelsInterface.TabbedPanelsUtils.getTabName(buttonId);
      updateUI(ui);
    } else {
      handleMenuButton(buttonId, ui);
    }
  }

  public void handleMenuButton(Object buttonId, IntelUIAPI ui) {
    InstructionsManager im = InstructionsManager.getInstance();
    if (InstructionsManagement.isUnlockButton(buttonId)) {
      String instructionName = InstructionsManagement.getUnlockName(buttonId);
      if (im.isInstructionUnlocked(instructionName)) {
        return;
      }
      int cost = im.getResearchCost(instructionName);
      if (im.getResearchPoints() < cost) {
        return;
      }
      im.spendOnResearch(cost);
      im.setInstructionLevel(instructionName, 1);
    } else if (InstructionsManagement.isLevelButton(buttonId)) {
      String instructionName = InstructionsManagement.getLevelName(buttonId);
      if (!im.isInstructionUnlocked(instructionName)) {
        return;
      }
      int cost = im.getResearchCost(instructionName);
      if (im.getResearchPoints() < cost) {
        return;
      }
      im.spendOnResearch(cost);
      im.setInstructionLevel(instructionName,
          im.getInstructionLevel(instructionName) + 1);
    } else if (InstructionTopPanel.UNDO_RESEARCH_BUTTON_ID.equals(buttonId)) {
      if (!im.undoResearchVisit()) {
        return;
      }
    } else if (InstructionsManagement.isEnableButton(buttonId)) {
      String[] info = InstructionsManagement.getEnableDisableInfo(buttonId);
      String hullModId = info[0];
      String instructionName = info[1];
      if (!im.isInstructionUnlocked(instructionName)) {
        return;
      }
      HullmodLoadout loadout = im.getLoadout(hullModId);
      if (loadout.instructions.contains(instructionName)) {
        return;
      }
      loadout.instructions.add(instructionName);
    } else if (InstructionsManagement.isDisableButton(buttonId)) {
      String[] info = InstructionsManagement.getEnableDisableInfo(buttonId);
      String hullModId = info[0];
      String instructionName = info[1];
      if (!im.isInstructionUnlocked(instructionName)) {
        return;
      }
      im.getLoadout(hullModId).instructions.remove(instructionName);
    } else if (InstructionsManagement.isSaveSlotButton(buttonId)) {
      String[] info = InstructionsManagement.getSlotInfo(buttonId);
      im.saveLoadoutToSlot(info[0], Integer.parseInt(info[1]));
    } else if (InstructionsManagement.isLoadSlotButton(buttonId)) {
      String[] info = InstructionsManagement.getSlotInfo(buttonId);
      im.loadLoadoutFromSlot(info[0], Integer.parseInt(info[1]));
    } else if (InstructionTopPanel.isUnlockTemplateButton(buttonId)) {
      String hullModId = InstructionTopPanel.getUnlockTemplateName(buttonId);
      if (Global.getSector().getCharacterData().knowsHullMod(hullModId)) {
        return;
      }
      int cost = im.getCostForLoadout(hullModId);
      if (im.getResearchPoints() < cost) {
        return;
      }
      im.spendOnResearch(cost);
      Global.getSector().getCharacterData().addHullMod(hullModId);
      im.getResearchVisit().templateUnlocked(hullModId);
    } else if (LiminalityUpkeep.SAVE_BUTTON_ID.equals(buttonId)) {
      String upkeepString = LiminalityUpkeep.tf.getText();
      upkeepString = upkeepString.replace(",", "");
      try {
        int upkeep = Integer.parseInt(upkeepString);
        im.setResearchInvestmentPerMonth(upkeep);
      } catch (NumberFormatException e) {
        Global.getSoundPlayer().playUISound("ui_button_disabled_pressed", 1f, 1f);
      }
    } else if (LiminalityUpkeep.RESEARCH_INVESTMENT_BUTTON_ID.equals(buttonId)) {
      im.setResearchInvestmentPerMonth(
          im.getResearchInvestmentPerMonth() > 0 ? 0 : im.getMinimumResearchInvestmentPerMonth());
    } else if (InstructionTopPanel.isBuyCapacityButton(buttonId)) {
      if (!im.buyCapacity(InstructionTopPanel.getBuyCapacityTemplate(buttonId))) {
        return;
      }
    } else if (LiminalityUpkeep.TALK_TO_RESEARCH_TEAM_BUTTON_ID.equals(buttonId)) {
      Global.getSector().getMemoryWithoutUpdate().set("$liminal_increases",
          im.getLiminalIncreases());
      MemoryAPI playerMemory = Global.getSector().getCharacterData().getMemoryWithoutUpdate();
      CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
      playerMemory.set("$supplies", (int) cargo.getSupplies(), 0);
      playerMemory.set("$metals", (int) cargo.getCommodityQuantity(Commodities.METALS), 0);
      playerMemory.set("$alpha_core", (int) cargo.getCommodityQuantity(Commodities.ALPHA_CORE), 0);
      playerMemory.set("$gamma_core", (int) cargo.getCommodityQuantity(Commodities.GAMMA_CORE), 0);
      ui.showDialog(null, LGlobals.researchTeamDialog);
    }
    if (isResearchTabButton(buttonId)) {
      im.getResearchVisit().notePress();
    }
    updateUI(ui);
  }

  private static boolean isResearchTabButton(Object buttonId) {
    return InstructionsManagement.isUnlockButton(buttonId) || InstructionsManagement.isLevelButton(buttonId)
        || InstructionTopPanel.isUnlockTemplateButton(buttonId)
        || InstructionTopPanel.UNDO_RESEARCH_BUTTON_ID.equals(buttonId);
  }

  private void updateUI(IntelUIAPI ui) {
    for (TabbedPanelsInterface.Panel p : TabbedPanelsInterface.Panel.getCurrentPanels()) {
      p.saveScrollState();
    }
    ui.updateUIForItem(this);
  }

  @Override
  protected String getStageIconImpl(Object stageId) {
    if (stageId.equals(Stage.RP1)) {
      return Global.getSettings().getSpriteName("icons", "liminal_intel_research1");
    } else if (stageId.equals(Stage.RP2)) {
      return Global.getSettings().getSpriteName("icons", "liminal_intel_research2");
    } else if (stageId.equals(Stage.RP3)) {
      return Global.getSettings().getSpriteName("icons", "liminal_intel_research3");
    } else if (stageId.equals(Stage.RP4)) {
      return Global.getSettings().getSpriteName("icons", "liminal_intel_research4");
    }
    return Global.getSettings().getSpriteName("icons", "liminal_intel_research");
  }

  @Override
  public String getName() {
    return "Liminality";
  }

  @Override
  public String getSmallDescriptionTitle() {
    return getName();
  }

  @Override
  public String getIcon() {
    return Global.getSettings().getSpriteName("icons", "liminal_intel_research");
  }

  @Override
  public Set<String> getIntelTags(SectorMapAPI map) {
    Set<String> tags = super.getIntelTags(map);
    if (isHidden()) {
      tags.remove(Tags.INTEL_MAJOR_EVENT);
    }
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
    return true;
  }

  @Override
  public String getCommMessageSound() {
    return getSoundMajorPosting();
  }

  @Override
  public boolean withOneTimeFactors() {
    return false;
  }
}

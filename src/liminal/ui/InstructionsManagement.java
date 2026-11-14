package liminal.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.ScrollPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import liminal.LGlobals;
import liminal.LiminalStrings;
import liminal.instructions.Instruction;
import liminal.instructions.Instruction.Group;
import liminal.instructions.Instructions;
import liminal.instructions.InstructionsManager;
import liminal.instructions.InstructionsManager.HullmodLoadout;
import liminal.ui.TabbedPanelsInterface.Panel;

public class InstructionsManagement extends Panel {
  public static String UNLOCK_PREFIX = "unlock_";
  public static String LEVEL_PREFIX = "level_";
  public static String ENABLE_PREFIX = "enable_";
  public static String DISABLE_PREFIX = "disable_";

  public static boolean isUnlockButton(Object buttonId) {
    return buttonId instanceof String && ((String) buttonId).startsWith(UNLOCK_PREFIX);
  }

  public static String getUnlockName(Object buttonId) {
    return ((String) buttonId).substring(UNLOCK_PREFIX.length());
  }

  public static boolean isLevelButton(Object buttonId) {
    return buttonId instanceof String && ((String) buttonId).startsWith(LEVEL_PREFIX);
  }

  public static String getLevelName(Object buttonId) {
    return ((String) buttonId).substring(LEVEL_PREFIX.length());
  }

  public static boolean isEnableButton(Object buttonId) {
    return buttonId instanceof String && ((String) buttonId).startsWith(ENABLE_PREFIX);
  }

  public static boolean isDisableButton(Object buttonId) {
    return buttonId instanceof String && ((String) buttonId).startsWith(DISABLE_PREFIX);
  }

  public static String[] getEnableDisableInfo(Object buttonId) {
    String prefix = isEnableButton(buttonId) ? ENABLE_PREFIX : DISABLE_PREFIX;
    String withoutPrefix = ((String) buttonId).substring(prefix.length());
    return withoutPrefix.split("\\*", 2);
  }

  public static String SAVE_SLOT_PREFIX = "saveslot_";
  public static String LOAD_SLOT_PREFIX = "loadslot_";

  public static boolean isSaveSlotButton(Object buttonId) {
    return buttonId instanceof String && ((String) buttonId).startsWith(SAVE_SLOT_PREFIX);
  }

  public static boolean isLoadSlotButton(Object buttonId) {
    return buttonId instanceof String && ((String) buttonId).startsWith(LOAD_SLOT_PREFIX);
  }

  public static String[] getSlotInfo(Object buttonId) {
    String prefix = isSaveSlotButton(buttonId) ? SAVE_SLOT_PREFIX : LOAD_SLOT_PREFIX;
    String withoutPrefix = ((String) buttonId).substring(prefix.length());
    return withoutPrefix.split("\\*", 2);
  }

  private static final float ELEMENT_INSET = 5f;
  private static final float SCROLLBAR_GUTTER = 12f;

  private static final float PICTURE_WIDTH = 160f;
  private static final float PICTURE_HEIGHT = 100f;
  private static final float ROW_MIN_HEIGHT = PICTURE_HEIGHT;
  private static final float ROW_GAP = 6f;
  private static final float TEXT_GAP = 10f;
  private static final float NAME_Y = 3f;
  private static final float LINE_PAD = 4f;
  private static final float FLAVOUR_PAD = 6f;
  private static final float TEXT_MARGIN = 6f;
  private static final float MIN_TEXT_WIDTH = 100f;
  private static final float TEXT_END_GAP = 10f;
  private static final float RESEARCH_COST_WIDTH = 170f;
  private static final float TEMPLATE_COST_WIDTH = 110f;
  private static final float COST_LABEL_Y = 28f;
  private static final float COST_LINE_HEIGHT = 15f;
  private static final float COST_BUTTON_PAD = 4f;
  private static final float BUTTON_HEIGHT = 25f;
  private static final float HOVER_WIDTH = 400f;
  private static final float EFFECT_PAD = 3f;
  private static final float REASON_PAD = 5f;

  private static final float HEADING_HEIGHT = 18f;
  private static final float FIRST_HEADING_Y = 2f;
  private static final float HEADING_PAD = 10f;
  private static final float HEADING_ROW_PAD = 8f;
  private static final float NOT_RESEARCHED_PAD = 20f;

  private static final float LEFT_PANE_SHARE = 0.44f;
  private static final float LEFT_PANE_MIN = 546f;
  private static final float LEFT_PANE_MAX = 700f;
  private static final float PANE_GAP = 16f;
  private static final float SLOT_WIDTH = 110f;
  private static final float SLOT_HEIGHT = 22f;
  private static final float SLOT_GAP = 8f;
  private static final float SLOT_PAD = 8f;
  private static final float TEMPLATE_FIRST_ROW_PAD = 10f;

  private HullmodLoadout loadout;
  private InstructionsManager instructionsManager;
  private String hullModId;

  private TooltipMakerAPI instructionsHolder;
  private TooltipMakerAPI templateHolder;

  private int templateOP;
  private int templateMaxOP;

  public InstructionsManagement(String hullModId, InstructionsManager instructionsManager) {
    super(0.8f);
    this.hullModId = hullModId;
    if (hullModId != null) {
      this.loadout = instructionsManager.getLoadout(hullModId);
    }
    this.instructionsManager = instructionsManager;
  }

  @Override
  public void saveScrollState() {
    if (instructionsHolder == null || instructionsHolder.getExternalScroller() == null) {
      return;
    }
    String tab = ListScrollMemory.tabKey(hullModId);
    ListScrollMemory.remember(tab, ListScrollMemory.INSTRUCTIONS_LIST,
        instructionsHolder.getExternalScroller().getYOffset());
    if (templateHolder != null && templateHolder.getExternalScroller() != null) {
      ListScrollMemory.remember(tab, ListScrollMemory.TEMPLATE_LIST,
          templateHolder.getExternalScroller().getYOffset());
    }
  }

  @Override
  public void createPanel(CustomPanelAPI panel, float width, float height) {
    final boolean isDocked = LGlobals.playerIsDocked();
    templateHolder = null;

    if (loadout == null) {
      float rowWidth = width - SCROLLBAR_GUTTER;
      instructionsHolder = createList(panel, rowWidth, height);
      addGroups(panel, instructionsHolder, rowWidth, isDocked);
      placeList(panel, instructionsHolder, 0f, height, ListScrollMemory.INSTRUCTIONS_LIST);
      return;
    }

    templateOP = instructionsManager.getCurrentOPForLoadout(hullModId);
    templateMaxOP = instructionsManager.getMaxOPForLoadout(hullModId);

    float leftPane = (float) Math.floor(
        Math.min(LEFT_PANE_MAX, Math.max(LEFT_PANE_MIN, width * LEFT_PANE_SHARE)));
    float leftRowWidth = leftPane - SCROLLBAR_GUTTER;
    templateHolder = createList(panel, leftRowWidth, height);
    addTemplateList(panel, templateHolder, leftRowWidth, isDocked);
    placeList(panel, templateHolder, 0f, height, ListScrollMemory.TEMPLATE_LIST);

    float rightX = leftPane + PANE_GAP;
    float rowWidth = width - rightX - SCROLLBAR_GUTTER;
    instructionsHolder = createList(panel, rowWidth, height);
    int notResearched = addGroups(panel, instructionsHolder, rowWidth, isDocked);
    addNotResearchedLine(instructionsHolder, notResearched);
    placeList(panel, instructionsHolder, rightX, height, ListScrollMemory.INSTRUCTIONS_LIST);
  }

  private TooltipMakerAPI createList(CustomPanelAPI panel, float rowWidth, float height) {
    return panel.createUIElement(rowWidth + ELEMENT_INSET + SCROLLBAR_GUTTER, height, true);
  }

  private void placeList(CustomPanelAPI panel, TooltipMakerAPI list, float x, float height, int whichList) {
    panel.addUIElement(list).inTL(x - ELEMENT_INSET, 0f);
    ScrollPanelAPI scroller = list.getExternalScroller();
    if (scroller != null) {
      scroller.setYOffset(ListScrollMemory.restore(ListScrollMemory.tabKey(hullModId), whichList,
          list.getHeightSoFar(), height));
    }
  }

  private int addGroups(CustomPanelAPI panel, TooltipMakerAPI list, float rowWidth, boolean isDocked) {
    int notResearched = 0;
    boolean first = true;
    for (Group group : Instructions.getGroups()) {
      List<Instruction> rows = new ArrayList<>();
      for (Instruction instruction : Instructions.getInstructions(group)) {
        if (loadout != null) {
          if (!instructionsManager.isInstructionUnlocked(instruction.getName())) {
            notResearched++;
            continue;
          }
          if (loadout.instructions.contains(instruction.getName())) {
            continue;
          }
        }
        rows.add(instruction);
      }
      if (rows.isEmpty()) {
        continue;
      }
      list.addCustom(createGroupHeading(panel, rowWidth, group), first ? FIRST_HEADING_Y : HEADING_PAD);
      first = false;
      boolean firstRow = true;
      for (Instruction instruction : rows) {
        list.addCustom(createInstructionRow(panel, rowWidth, instruction, isDocked, false),
            firstRow ? HEADING_ROW_PAD : ROW_GAP);
        firstRow = false;
      }
    }
    return notResearched;
  }

  private void addNotResearchedLine(TooltipMakerAPI list, int notResearched) {
    if (notResearched <= 0) {
      return;
    }
    String count = "" + notResearched;
    list.setParaFontColor(Misc.getGrayColor());
    LabelAPI notResearchedLine = list.addPara(String.format(LiminalStrings.mgmt(
        notResearched == 1 ? "instruction_not_researched_one" : "instructions_not_researched"), count),
        list.getHeightSoFar() > 0f ? NOT_RESEARCHED_PAD : FIRST_HEADING_Y);
    notResearchedLine.setHighlight(count);
    notResearchedLine.setHighlightColor(Misc.getHighlightColor());
    list.setParaFontColor(Misc.getTextColor());
  }

  private void addTemplateList(CustomPanelAPI panel, TooltipMakerAPI list, float rowWidth, boolean isDocked) {
    list.addCustom(createHeading(panel, rowWidth, LiminalStrings.mgmt("template_list_title"),
        Misc.getBasePlayerColor(), null), FIRST_HEADING_Y);
    list.addCustom(createSlotButtons(panel, rowWidth, isDocked), SLOT_PAD);
    boolean empty = true;
    for (Group group : Instructions.getGroups()) {
      for (Instruction instruction : Instructions.getInstructions(group)) {
        if (!loadout.instructions.contains(instruction.getName())) {
          continue;
        }
        list.addCustom(createInstructionRow(panel, rowWidth, instruction, isDocked, true),
            empty ? TEMPLATE_FIRST_ROW_PAD : ROW_GAP);
        empty = false;
      }
    }
    if (empty) {
      list.addPara(LiminalStrings.mgmt("template_empty"), Misc.getGrayColor(),
          TEMPLATE_FIRST_ROW_PAD + NAME_Y);
    }
  }

  private CustomPanelAPI createGroupHeading(CustomPanelAPI panel, float barWidth, Group group) {
    final int max = Instructions.getMaxInstructionsInGroup(group);
    String title = Instructions.getGroupTitle(group) + " Instructions";
    if (loadout != null) {
      title = String.format(LiminalStrings.mgmt("group_heading_template"), Instructions.getGroupTitle(group),
          "" + loadout.getNumberOfInstructionsInGroupSelected(group), "" + max);
    }
    TooltipMakerAPI.TooltipCreator hover = new RowTooltip() {
      @Override
      public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        tooltip.setParaFontColor(Misc.getBasePlayerColor());
        tooltip.addPara(LiminalStrings.mgmt("group_capacity"), 0f,
            new Color[] { Misc.getHighlightColor(), LGlobals.instructionColor }, "" + max, "Instructions");
        tooltip.setParaFontColor(Misc.getTextColor());
      }
    };
    return createHeading(panel, barWidth, title, Instructions.getGroupColor(group), hover);
  }

  private CustomPanelAPI createHeading(CustomPanelAPI panel, float barWidth, String text, Color color,
      TooltipMakerAPI.TooltipCreator hover) {
    CustomPanelAPI bar = panel.createCustomPanel(barWidth, HEADING_HEIGHT, null);
    TooltipMakerAPI holder = bar.createUIElement(barWidth, HEADING_HEIGHT, false);
    holder.addSectionHeading(text, color, Misc.getDarkPlayerColor(), Alignment.MID, 0f);
    if (hover != null) {
      holder.addTooltipToPrevious(hover, TooltipMakerAPI.TooltipLocation.BELOW, false);
    }
    bar.addUIElement(holder).inTL(0f, 0f);
    return bar;
  }

  private String rowName(Instruction instruction) {
    if (!instructionsManager.isInstructionUnlocked(instruction.getName())) {
      return instruction.getDisplayName();
    }
    return instruction.getDisplayName() + " Mk. "
        + LGlobals.toRoman(instructionsManager.getInstructionLevel(instruction.getName()));
  }

  private boolean isFlavourHidden(Instruction instruction) {
    return instruction.getGroup().equals(Group.SPECIAL)
        && !instructionsManager.isInstructionUnlocked(instruction.getName());
  }

  private TooltipMakerAPI.TooltipCreator createRowHover(final Instruction instruction) {
    return new RowTooltip() {
      @Override
      public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        tooltip.addPara(LiminalStrings.mgmt("instruction_stats_tooltip"),
            0f,
            Misc.getHighlightColor(),
            "" + instruction.getMaxLevel(), "" + instruction.getMaxLevelMultiplier() + "x",
            "" + instruction.getOpCost());
      }
    };
  }

  private void addPicture(CustomPanelAPI row, Instruction instruction, float width, float height,
      TooltipMakerAPI.TooltipCreator hover) {
    String illustration = instruction.illustrationName();
    if (illustration == null) {
      return;
    }
    TooltipMakerAPI holder = row.createUIElement(width, height, false);
    holder.addImage(illustration, width, height, 0f);
    if (hover != null) {
      holder.addTooltipToPrevious(hover, TooltipMakerAPI.TooltipLocation.BELOW, false);
    }
    row.addUIElement(holder).inTL(-ELEMENT_INSET, 0f);
  }

  private void addCostLabel(TooltipMakerAPI holder, String unit, int cost, boolean canPay, float y) {
    String number = "" + cost;
    LabelAPI label = holder.addPara(unit + ": " + number, Misc.getBasePlayerColor(), y);
    label.highlightLast(number);
    label.setHighlightColor(canPay ? Misc.getHighlightColor() : Misc.getNegativeHighlightColor());
  }

  private CustomPanelAPI createInstructionRow(CustomPanelAPI panel, float rowWidth,
      final Instruction instruction, boolean isDocked, boolean held) {
    final String name = instruction.getName();
    final boolean unlocked = instructionsManager.isInstructionUnlocked(name);
    float costWidth = loadout == null ? RESEARCH_COST_WIDTH : TEMPLATE_COST_WIDTH;
    float textWidth = Math.max(MIN_TEXT_WIDTH,
        rowWidth - PICTURE_WIDTH - TEXT_GAP - TEXT_END_GAP - costWidth);
    CustomPanelAPI row = panel.createCustomPanel(rowWidth, ROW_MIN_HEIGHT, getPlugin());
    TooltipMakerAPI.TooltipCreator hover = createRowHover(instruction);

    TooltipMakerAPI text = row.createUIElement(textWidth + 2f * ELEMENT_INSET, 0f, false);
    text.addPara(rowName(instruction), Instructions.getGroupColor(instruction.getGroup()), NAME_Y);
    text.addTooltipToPrevious(hover, TooltipMakerAPI.TooltipLocation.BELOW, false);
    if (unlocked) {
      text.setParaFontColor(LGlobals.liminalityTextColor);
      instruction.getEffectDescription(text, LINE_PAD, instructionsManager.getInstructionLevel(name));
      text.addTooltipToPrevious(new LGlobals.InfoTooltip() {
        @Override
        public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
          tooltip.addPara(
              LiminalStrings.mgmt("effect_notation_tooltip"),
              0,
              Misc.getHighlightColor(),
              "x",
              "%");
        }
      }, TooltipMakerAPI.TooltipLocation.BELOW, false);
      text.setParaFontColor(Misc.getTextColor());
    }
    if (isFlavourHidden(instruction)) {
      text.addPara("More research required.", Misc.getGrayColor(), LINE_PAD);
    } else {
      text.addPara(instruction.getDescription(), Misc.getGrayColor(), unlocked ? FLAVOUR_PAD : LINE_PAD);
    }

    float rowHeight = Math.max(ROW_MIN_HEIGHT, text.getHeightSoFar() - NAME_Y + TEXT_MARGIN);
    row.getPosition().setSize(rowWidth, rowHeight);
    addPicture(row, instruction, PICTURE_WIDTH, PICTURE_HEIGHT, hover);
    row.addUIElement(text).inTL(PICTURE_WIDTH + TEXT_GAP - ELEMENT_INSET, 0f);

    TooltipMakerAPI costHolder = row.createUIElement(costWidth, rowHeight, false);
    if (loadout == null) {
      addResearchButtons(costHolder, instruction, isDocked);
    } else if (held) {
      addRemoveButton(costHolder, instruction, isDocked);
    } else {
      addAddButton(costHolder, instruction, isDocked);
    }
    costHolder.setHeightSoFar(rowHeight);
    row.addUIElement(costHolder).inTL(rowWidth - costWidth - ELEMENT_INSET, 0f);
    return row;
  }

  private void addResearchButtons(TooltipMakerAPI buttonHolder, final Instruction instruction,
      final boolean isDocked) {
    final String name = instruction.getName();
    final boolean unlocked = instructionsManager.isInstructionUnlocked(name);
    final int level = instructionsManager.getInstructionLevel(name);
    final boolean isMaxLevel = level >= instruction.getMaxLevel();
    int cost = instructionsManager.getResearchCost(name);
    final boolean enoughRP = instructionsManager.getResearchPoints() >= cost;
    if (unlocked && isMaxLevel) {
      buttonHolder.addSpacer(COST_LABEL_Y + COST_LINE_HEIGHT);
    } else {
      addCostLabel(buttonHolder, "RP", cost, enoughRP, COST_LABEL_Y);
    }
    ButtonAPI actionButton;
    if (!unlocked) {
      actionButton = buttonHolder.addButton(
          "Unlock",
          UNLOCK_PREFIX + name,
          Misc.getTextColor(),
          LGlobals.liminalityDarkColor,
          Alignment.MID,
          CutStyle.NONE,
          RESEARCH_COST_WIDTH,
          BUTTON_HEIGHT,
          COST_BUTTON_PAD);
    } else {
      actionButton = buttonHolder.addButton(
          isMaxLevel ? "Maxed" : "Upgrade",
          LEVEL_PREFIX + name,
          Misc.getTextColor(),
          LGlobals.liminalityDarkGreenColor,
          Alignment.MID,
          CutStyle.NONE,
          RESEARCH_COST_WIDTH,
          BUTTON_HEIGHT,
          COST_BUTTON_PAD);
    }
    boolean enabled = (enoughRP && !isMaxLevel && isDocked);
    LGlobals.setEnabledSayingWhy(actionButton, enabled);
    final boolean showNextLevel = unlocked && !isMaxLevel;
    if (!enabled || showNextLevel) {
      buttonHolder.addTooltipToPrevious(new LGlobals.InfoTooltip() {
        @Override
        public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
          float pad = 0f;
          if (showNextLevel) {
            tooltip.addPara(LiminalStrings.mgmt("upgrade_to"), 0f, Misc.getHighlightColor(),
                LGlobals.toRoman(level + 1));
            tooltip.setParaFontColor(LGlobals.liminalityTextColor);
            instruction.getEffectDescription(tooltip, EFFECT_PAD, level + 1);
            tooltip.setParaFontColor(Misc.getTextColor());
            pad = REASON_PAD;
          }
          if (!enoughRP) {
            tooltip.addPara("Not enough RP.", Misc.getNegativeHighlightColor(), pad);
            pad = 0f;
          }
          if (isMaxLevel) {
            tooltip.addPara("Already at max level.", Misc.getNegativeHighlightColor(), pad);
            pad = 0f;
          }
          if (!isDocked) {
            tooltip.addPara(LiminalStrings.mgmt("err_docked_upgrade"), Misc.getNegativeHighlightColor(), pad);
          }
        }
      }, TooltipMakerAPI.TooltipLocation.BELOW, false);
    }
  }

  private void addAddButton(TooltipMakerAPI buttonHolder, final Instruction instruction, final boolean isDocked) {
    final boolean enoughOP = templateOP + instruction.getOpCost() <= templateMaxOP;
    addCostLabel(buttonHolder, "OP", instruction.getOpCost(), enoughOP, COST_LABEL_Y);
    final boolean unlocked = instructionsManager.isInstructionUnlocked(instruction.getName());
    final boolean hasTooManyInGroup = loadout.getNumberOfInstructionsInGroupSelected(instruction.getGroup())
        >= Instructions.getMaxInstructionsInGroup(instruction.getGroup());
    final boolean isIncompatible = instructionsManager.isIncompatible(loadout, instruction);
    String text;
    if (isIncompatible) {
      text = "Incompatible";
    } else if (unlocked) {
      text = "Add";
    } else {
      text = "Locked";
    }
    ButtonAPI actionButton = buttonHolder.addButton(
        text,
        ENABLE_PREFIX + hullModId + "*" + instruction.getName(),
        Misc.getTextColor(),
        LGlobals.liminalityDarkColor,
        Alignment.MID,
        CutStyle.NONE,
        TEMPLATE_COST_WIDTH,
        BUTTON_HEIGHT,
        COST_BUTTON_PAD);
    boolean enabled = (enoughOP && unlocked && !hasTooManyInGroup && isDocked && !isIncompatible);
    LGlobals.setEnabledSayingWhy(actionButton, enabled);
    if (!enabled) {
      buttonHolder.addTooltipToPrevious(new LGlobals.InfoTooltip() {
        @Override
        public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
          if (!enoughOP) {
            tooltip.addPara("Not enough OP.", Misc.getNegativeHighlightColor(), 0f);
          }
          if (!unlocked) {
            tooltip.addPara("Instruction is locked.", Misc.getNegativeHighlightColor(), 0f);
          }
          if (hasTooManyInGroup) {
            tooltip.addPara(LiminalStrings.mgmt("err_group_full"), Misc.getNegativeHighlightColor(), 0f);
          }
          if (isIncompatible) {
            tooltip.addPara("Incompatible with other Instructions.", Misc.getNegativeHighlightColor(), 0f);
          }
          if (!isDocked) {
            tooltip.addPara(LiminalStrings.mgmt("err_docked_add"), Misc.getNegativeHighlightColor(), 0f);
          }
        }
      }, TooltipMakerAPI.TooltipLocation.BELOW, false);
    }
  }

  private void addRemoveButton(TooltipMakerAPI buttonHolder, Instruction instruction, final boolean isDocked) {
    int opCostAfterRemoval = templateOP - instruction.getOpCost();
    final boolean wouldExceedMaxOP = opCostAfterRemoval > templateMaxOP && instruction.getOpCost() < 0;
    addCostLabel(buttonHolder, "OP", instruction.getOpCost(), !wouldExceedMaxOP, COST_LABEL_Y);
    ButtonAPI removeButton = buttonHolder.addButton(
        "Remove",
        DISABLE_PREFIX + hullModId + "*" + instruction.getName(),
        Misc.getTextColor(),
        LGlobals.liminalityDarkGreenColor,
        Alignment.MID,
        CutStyle.NONE,
        TEMPLATE_COST_WIDTH,
        BUTTON_HEIGHT,
        COST_BUTTON_PAD);
    boolean canRemove = isDocked && !wouldExceedMaxOP;
    LGlobals.setEnabledSayingWhy(removeButton, canRemove);
    if (!canRemove) {
      buttonHolder.addTooltipToPrevious(new LGlobals.InfoTooltip() {
        @Override
        public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
          if (!isDocked) {
            tooltip.addPara(LiminalStrings.mgmt("err_docked_remove"), Misc.getNegativeHighlightColor(), 0f);
          }
          if (wouldExceedMaxOP) {
            tooltip.addPara(LiminalStrings.mgmt("err_remove_exceeds_op"), Misc.getNegativeHighlightColor(), 0f);
          }
        }
      }, TooltipMakerAPI.TooltipLocation.BELOW, false);
    }
  }

  private CustomPanelAPI createSlotButtons(CustomPanelAPI panel, float rowWidth, final boolean isDocked) {
    CustomPanelAPI slotRow = panel.createCustomPanel(rowWidth, SLOT_HEIGHT, getPlugin());
    TooltipMakerAPI slotHolder = slotRow.createUIElement(rowWidth, SLOT_HEIGHT, false);
    final String[] slotNames = { "A", "B" };
    ButtonAPI previous = null;
    for (int i = 0; i < InstructionsManager.LOADOUT_SLOTS * 2; i++) {
      final int slot = i / 2;
      final boolean isSave = (i % 2 == 0);
      String label = (isSave ? "Save " : "Load ") + slotNames[slot];
      String buttonId = (isSave ? SAVE_SLOT_PREFIX : LOAD_SLOT_PREFIX) + hullModId + "*" + slot;
      ButtonAPI button = slotHolder.addButton(label, buttonId, Misc.getTextColor(),
          isSave ? LGlobals.liminalityDarkColor : LGlobals.liminalityDarkGreenColor, Alignment.MID,
          CutStyle.NONE, SLOT_WIDTH, SLOT_HEIGHT, 0f);
      final boolean slotEmpty = instructionsManager.isSavedLoadoutSlotEmpty(hullModId, slot);
      LGlobals.setEnabledSayingWhy(button, isDocked && (isSave || !slotEmpty));
      if (previous != null) {
        button.getPosition().rightOfTop(previous, SLOT_GAP);
      }
      previous = button;
      slotHolder.addTooltipToPrevious(new LGlobals.InfoTooltip() {
        @Override
        public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
          if (isSave) {
            tooltip.addPara(LiminalStrings.mgmt("save_slot_tooltip"), 0f,
                Misc.getHighlightColor(), slotNames[slot]);
          } else {
            tooltip.addPara(
                LiminalStrings.mgmt("load_slot_tooltip"),
                0f, Misc.getHighlightColor(), slotNames[slot]);
            if (slotEmpty) {
              tooltip.addPara("Empty slot.", Misc.getGrayColor(), 5f);
            } else {
              for (String instructionName : instructionsManager.getSavedLoadoutSlot(hullModId, slot)) {
                Instruction saved = Instructions.getInstruction(instructionName);
                tooltip.addPara("- " + (saved != null ? saved.getDisplayName() : instructionName), 3f);
              }
            }
          }
          if (!isDocked) {
            tooltip.addPara(LiminalStrings.mgmt("err_docked_use"), Misc.getNegativeHighlightColor(), 5f);
          }
        }
      }, TooltipMakerAPI.TooltipLocation.BELOW, false);
    }
    slotHolder.setHeightSoFar(SLOT_HEIGHT);
    slotRow.addUIElement(slotHolder).inTL(-ELEMENT_INSET, 0f);
    return slotRow;
  }

  private abstract static class RowTooltip extends LGlobals.InfoTooltip {
    private final float width;

    RowTooltip() {
      this(HOVER_WIDTH);
    }

    RowTooltip(float width) {
      this.width = width;
    }

    @Override
    public float getTooltipWidth(Object tooltipParam) {
      return width;
    }
  }
}

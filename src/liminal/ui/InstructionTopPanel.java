package liminal.ui;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import liminal.LGlobals;
import liminal.LiminalStrings;
import liminal.hullmods.InstructionsHullmod;
import liminal.instructions.InstructionsManager;
import liminal.instructions.InstructionsManager.HullmodLoadout;
import liminal.ui.TabbedPanelsInterface.Panel;

public class InstructionTopPanel extends Panel {
  public static final float HEIGHT = 126f;

  private static final float INSET = 5f;
  private static final float LABEL_SLACK = 10f;
  private static final float GAP = 10f;

  private static final float TEXT_WIDTH = 600f;
  private static final float TEXT_HEIGHT = 74f;
  private static final float TOP_ROW_Y = 5f;
  private static final float ERRORS_HEIGHT = 45f;
  private static final float ICON_SIZE = 50f;
  private static final float ICON_FROM_RIGHT = 58f;

  private static final float CAPTION_Y = 80f;
  private static final float CAPTION_HEIGHT = 15f;
  private static final String CAPTION_FONT = Fonts.ORBITRON_12;
  private static final float VALUE_Y = 97f;
  private static final float VALUE_HEIGHT = 28f;
  private static final String VALUE_FONT = Fonts.INSIGNIA_VERY_LARGE;

  private static final float STRIP_X = 5f;
  private static final float VALUE_WIDTH = 96f;
  private static final float PIPS_GAP = 8f;
  private static final float PIPS_Y = 81f;
  private static final float PIPS_MAX_WIDTH = 295f;
  private static final float PIPS_MIN_WIDTH = 60f;
  private static final float SUB_LINE_Y = 103f;
  private static final float BUTTON_GAP = 14f;
  private static final float BUTTON_Y = 78f;
  private static final float PRICE_GAP = 10f;
  private static final float PRICE_Y = 82f;

  private static final float TILE_FROM_RIGHT = 180f;
  private static final float SECOND_TILE_FROM_RIGHT = 300f;

  private static final float UNLOCK_BUTTON_PAD = 4f;
  private static final float UNLOCK_BUTTON_HEIGHT = 25f;
  private static final float UNLOCK_HOLDER_HEIGHT = 50f;
  private static final int TABS_BEFORE_TEMPLATES = 2;

  public static final String UNDO_RESEARCH_BUTTON_ID = "liminal_research_undo";
  private static final float UNDO_WIDTH = 120f;
  private static final float UNDO_HEIGHT = 25f;
  private static final float UNDO_HOVER_WIDTH = 400f;

  public static String UNLOCK_TEMPLATE_PREFIX = "template_unlock_";
  private HullmodLoadout loadout;
  private InstructionsManager instructionsManager;
  private String hullModId;

  public static boolean isUnlockTemplateButton(Object buttonId) {
    return buttonId instanceof String && ((String) buttonId).startsWith(UNLOCK_TEMPLATE_PREFIX);
  }

  public static String getUnlockTemplateName(Object buttonId) {
    return ((String) buttonId).substring(UNLOCK_TEMPLATE_PREFIX.length());
  }

  public static final String BUY_CAPACITY_PREFIX = "template_capacity_";

  public static boolean isBuyCapacityButton(Object buttonId) {
    return buttonId instanceof String && ((String) buttonId).startsWith(BUY_CAPACITY_PREFIX);
  }

  public static String getBuyCapacityTemplate(Object buttonId) {
    return ((String) buttonId).substring(BUY_CAPACITY_PREFIX.length());
  }

  public InstructionTopPanel(String hullModId, InstructionsManager instructionsManager) {
    super(0.2f);
    setFixedHeight(HEIGHT);
    this.hullModId = hullModId;
    if (hullModId != null) {
      this.loadout = instructionsManager.getLoadout(hullModId);
    }
    this.instructionsManager = instructionsManager;
  }

  @Override
  public void createPanel(CustomPanelAPI panel, float width, float height) {
    boolean docked = LGlobals.playerIsDocked();
    if (loadout == null) {
      createResearchHeader(panel, width, docked);
    } else {
      createTemplateHeader(panel, width, docked);
    }
  }

  private void createTemplateHeader(CustomPanelAPI panel, float width, boolean docked) {
    CapacityStrip strip = CapacityStrip.of(hullModId, instructionsManager);
    float errorsX = width / 2f;
    float iconX = width - ICON_FROM_RIGHT;
    float tileX = width - TILE_FROM_RIGHT;

    TooltipMakerAPI text = panel.createUIElement(textWidth(errorsX), TEXT_HEIGHT, false);
    text.setTitleOrbitronVeryLarge();
    text.addTitle(instructionsManager.getHullmodName(hullModId), Misc.getBasePlayerColor());
    text.addPara(LiminalStrings.mgmt("template_intro"), 5f, LGlobals.instructionColor);
    panel.addUIElement(text).inTL(0f, 0f);

    TooltipMakerAPI errors = panel.createUIElement(iconX - INSET - GAP - errorsX, ERRORS_HEIGHT, false);
    if (!docked) {
      errors.addPara(LiminalStrings.mgmt("err_not_docked"), Misc.getNegativeHighlightColor(), TOP_ROW_Y);
    }
    if (strip.isOver()) {
      instructionsManager.addOverCapacityLine(errors, hullModId, strip.getUsed(), TOP_ROW_Y);
    }
    panel.addUIElement(errors).inTL(errorsX - INSET, 0f);

    addIcon(panel, iconX);
    addStrip(panel, strip, tileX);
    addTile(panel, tileX, TILE_FROM_RIGHT - GAP, CapacityStrip.CAPTION_OP, CapacityStrip.opHover(hullModId),
        instructionsManager.getCurrentOPForLoadout(hullModId) + " / " + loadout.getMaxOP());
  }

  private void createResearchHeader(CustomPanelAPI panel, float width, boolean docked) {
    instructionsManager.getResearchVisit().onResearchTabBuilt();
    int researchPoints = instructionsManager.getResearchPoints();
    float errorsX = width / 2f;
    float topRowX = docked ? addUnlockHolders(panel, width, researchPoints) : errorsX;

    TooltipMakerAPI text = panel.createUIElement(textWidth(topRowX), TEXT_HEIGHT, false);
    text.setTitleOrbitronVeryLarge();
    text.addTitle("Research", Misc.getBasePlayerColor());
    text.addPara(LiminalStrings.mgmt("research_intro"), 5f, LGlobals.instructionColor, "Instructions");
    text.addPara(LiminalStrings.mgmt("research_diminishing_returns"), 5f, LGlobals.instructionColor,
        "Instructions");
    panel.addUIElement(text).inTL(0f, 0f);

    if (!docked) {
      TooltipMakerAPI errors = panel.createUIElement(width - GAP - errorsX, ERRORS_HEIGHT, false);
      errors.addPara(LiminalStrings.mgmt("err_not_docked"), Misc.getNegativeHighlightColor(), TOP_ROW_Y);
      panel.addUIElement(errors).inTL(errorsX - INSET, 0f);
    }

    float totalX = width - SECOND_TILE_FROM_RIGHT;
    float currentX = width - TILE_FROM_RIGHT;
    addTile(panel, totalX, currentX - INSET - GAP - totalX, LiminalStrings.mgmt("caption_total_rp"), null,
        "" + instructionsManager.getTotalRPGained());
    addTile(panel, currentX, TILE_FROM_RIGHT - GAP, LiminalStrings.mgmt("caption_current_rp"),
        new LGlobals.InfoTooltip() {
          @Override
          public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
            tooltip.addPara(LiminalStrings.mgmt("rp_sources_tooltip"), 0f,
                Misc.getHighlightColor(), "event progress");
          }
        }, "" + researchPoints);
    addUndoButton(panel);
  }

  private void addUndoButton(CustomPanelAPI panel) {
    TooltipMakerAPI holder = panel.createUIElement(UNDO_WIDTH + INSET, UNDO_HEIGHT, false);
    ButtonAPI undo = holder.addButton(LiminalStrings.mgmt("undo_research_button"), UNDO_RESEARCH_BUTTON_ID,
        Misc.getTextColor(), LGlobals.liminalityDarkColor, Alignment.MID, CutStyle.NONE, UNDO_WIDTH,
        UNDO_HEIGHT, 0f);
    LGlobals.setEnabledSayingWhy(undo, instructionsManager.getResearchVisit().canUndo());
    holder.addTooltipToPrevious(new LGlobals.InfoTooltip() {
      @Override
      public float getTooltipWidth(Object tooltipParam) {
        return UNDO_HOVER_WIDTH;
      }

      @Override
      public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        tooltip.addPara(LiminalStrings.mgmt("undo_research_tooltip"), 0f);
      }
    }, TooltipMakerAPI.TooltipLocation.BELOW, false);
    panel.addUIElement(holder).inTL(STRIP_X - INSET, BUTTON_Y);
  }

  private static float textWidth(float nextX) {
    return Math.min(TEXT_WIDTH, nextX - GAP - 2f * INSET);
  }

  private void addStrip(CustomPanelAPI panel, CapacityStrip strip, float rightX) {
    addCaption(panel, STRIP_X, VALUE_WIDTH, CapacityStrip.CAPTION_CAPACITY, strip.capacityHover());
    float valueWidth = Math.max(VALUE_WIDTH - LABEL_SLACK,
        stringWidth(strip.getUsed() + " / " + strip.getCapacity(), VALUE_FONT));
    strip.addValue(panel, STRIP_X, VALUE_Y, valueWidth, VALUE_FONT, Alignment.TL);

    float pipsX = STRIP_X + valueWidth + LABEL_SLACK + PIPS_GAP;
    float priceWidth = strip.priceWidth();
    float rowEnd = rightX - INSET - GAP - LABEL_SLACK;
    float pipsRoom = rowEnd - priceWidth - PRICE_GAP - CapacityStrip.BUTTON_WIDTH - BUTTON_GAP - pipsX;
    float pipsWidth = strip.addPips(panel, pipsX, PIPS_Y,
        Math.max(PIPS_MIN_WIDTH, Math.min(PIPS_MAX_WIDTH, pipsRoom)));
    strip.addSubLine(panel, pipsX, SUB_LINE_Y, pipsWidth);

    float buttonX = pipsX + pipsWidth + BUTTON_GAP;
    strip.addButton(panel, buttonX, BUTTON_Y);
    strip.addPrice(panel, buttonX + CapacityStrip.BUTTON_WIDTH + PRICE_GAP, PRICE_Y, priceWidth);
  }

  private static void addTile(CustomPanelAPI panel, float x, float maxWidth, String caption,
      TooltipMakerAPI.TooltipCreator hover, String value) {
    addCaption(panel, x, maxWidth, caption, hover);
    TooltipMakerAPI el = panel.createUIElement(
        Math.min(maxWidth, stringWidth(value, VALUE_FONT) + LABEL_SLACK), VALUE_HEIGHT, false);
    el.setParaFont(VALUE_FONT);
    el.addPara(value, Misc.getHighlightColor(), 0f);
    if (hover != null) {
      CapacityStrip.attachHover(el, hover);
    }
    panel.addUIElement(el).inTL(x - INSET, VALUE_Y);
  }

  private static void addCaption(CustomPanelAPI panel, float x, float maxWidth, String caption,
      TooltipMakerAPI.TooltipCreator hover) {
    TooltipMakerAPI el = panel.createUIElement(
        Math.min(maxWidth, stringWidth(caption, CAPTION_FONT) + LABEL_SLACK), CAPTION_HEIGHT, false);
    el.setParaFontOrbitron();
    el.addPara(caption, Misc.getBasePlayerColor(), 0f);
    if (hover != null) {
      CapacityStrip.attachHover(el, hover);
    }
    panel.addUIElement(el).inTL(x - INSET, CAPTION_Y);
  }

  private static float stringWidth(String text, String font) {
    return Global.getSettings().computeStringWidth(text, font);
  }

  private int tabCount() {
    int tabs = getTabCount();
    return tabs > 0 ? tabs : TABS_BEFORE_TEMPLATES + instructionsManager.getLoadoutIds().size();
  }

  private float addUnlockHolders(CustomPanelAPI panel, float width, int researchPoints) {
    List<String> loadoutIds = new ArrayList<>(instructionsManager.getLoadoutIds());
    float tabWidth = width / tabCount();
    float buttonWidth = tabWidth - TabbedPanelsInterface.TOTAL_PADDING;
    float leftmost = width;
    for (int i = 1; i < loadoutIds.size(); i++) {
      String lockedId = loadoutIds.get(i);
      if (Global.getSector().getCharacterData().knowsHullMod(lockedId)) {
        continue;
      }
      int cost = instructionsManager.getCostForLoadout(lockedId);
      boolean enoughRP = researchPoints >= cost;
      float x = TabbedPanelsInterface.TOTAL_PADDING / 2f + (TABS_BEFORE_TEMPLATES + i) * tabWidth;
      TooltipMakerAPI holder = panel.createUIElement(buttonWidth + INSET, UNLOCK_HOLDER_HEIGHT, false);
      holder.setParaFontColor(Misc.getBasePlayerColor());
      holder.addPara("RP: %s", TOP_ROW_Y,
          enoughRP ? Misc.getHighlightColor() : Misc.getNegativeHighlightColor(), "" + cost);
      ButtonAPI button = holder.addButton("Unlock Template", UNLOCK_TEMPLATE_PREFIX + lockedId,
          Misc.getTextColor(), LGlobals.liminalityDarkColor, Alignment.MID, CutStyle.ALL,
          buttonWidth, UNLOCK_BUTTON_HEIGHT, UNLOCK_BUTTON_PAD);
      button.setEnabled(enoughRP);
      panel.addUIElement(holder).inTL(x - INSET, 0f);
      leftmost = Math.min(leftmost, x);
    }
    return leftmost;
  }

  private void addIcon(CustomPanelAPI panel, float x) {
    final HullModSpecAPI spec = Global.getSettings().getHullModSpec(hullModId);
    final HullModEffect effect = spec.getEffect();
    TooltipMakerAPI icon = panel.createUIElement(ICON_SIZE + INSET, TOP_ROW_Y + ICON_SIZE, false);
    icon.addImage(spec.getSpriteName(), ICON_SIZE, ICON_SIZE, TOP_ROW_Y);
    CapacityStrip.attachHover(icon, new LGlobals.InfoTooltip() {
      @Override
      public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        tooltip.addTitle(spec.getDisplayName());
        tooltip.setParaFontColor(Misc.getGrayColor());
        tooltip.addPara("Design type: %s", 10f, LGlobals.liminalityColor,
            spec.getManufacturer());
        tooltip.setParaFontColor(Misc.getTextColor());
        tooltip.addPara(spec.getDescriptionFormat(), 10f,
            Misc.getHighlightColor());
        ((InstructionsHullmod) effect).addPostDescriptionSection(tooltip, null, null,
            getTooltipWidth(tooltipParam), true);
      }
    });
    panel.addUIElement(icon).inTL(x - INSET, 0f);
  }
}

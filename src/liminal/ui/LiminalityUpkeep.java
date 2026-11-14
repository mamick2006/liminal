package liminal.ui;

import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import liminal.LGlobals;
import liminal.LiminalStrings;
import liminal.instructions.InstructionsManager;
import liminal.intel.LiminalCrisisIntel;
import liminal.ui.TabbedPanelsInterface.Panel;

public class LiminalityUpkeep extends Panel {
  public LiminalityUpkeep() {
    super(0.33f);
  }

  public static TextFieldAPI tf;
  public static final String SAVE_BUTTON_ID = "save_upkeep";
  public static final String RESEARCH_INVESTMENT_BUTTON_ID = "research_investment_button";
  public static final String TALK_TO_RESEARCH_TEAM_BUTTON_ID = "talk_to_research_team_button";

  private static final float EL_INSET = 5f;
  private static final float EL_SLACK = 10f;
  private static final float LINE_HEIGHT = 15f;

  private static final float BAR_Y = 2f;
  private static final float BAR_HEIGHT = 18f;
  private static final float BLOCK_GAP = 20f;
  private static final float TEXT_X = 5f;

  private static final float MINIMUM_Y = 27f;
  private static final float CONTROLS_Y = 49f;
  private static final float CONTROL_HEIGHT = 18f;
  private static final float FIELD_WIDTH = 104f;
  private static final float SAVE_X = 114f;
  private static final float SAVE_WIDTH = 62f;
  private static final float TOGGLE_WIDTH = 220f;
  private static final float TALK_Y = 77f;
  private static final float TALK_WIDTH = 310f;
  private static final float TALK_HEIGHT = 36f;
  private static final float TOTAL_Y = 123f;

  private static final float ROWS_Y = 26f;
  private static final float ROW_PITCH = 38f;
  private static final float ROW_HEIGHT = 36f;
  private static final float ICON_X = 5f;
  private static final float ICON_DY = 2f;
  private static final float ICON_SIZE = 32f;
  private static final float NAME_X = 45f;
  private static final float NAME_DY = 1f;
  private static final float SUB_LINE_DY = 18f;
  private static final float NAME_WIDTH = 215f;
  private static final float VALUE_RIGHT = 326f;
  private static final float VALUE_DY = 7f;
  private static final float VALUE_WIDTH = 64f;
  private static final float PIPS_X = 338f;
  private static final float PIPS_DY = 10f;
  private static final float PIPS_MAX_WIDTH = 155f;
  private static final float BUTTON_X = 512f;
  private static final float BUTTON_DY = 7f;
  private static final float PRICE_X = 672f;
  private static final float PRICE_DY = 11f;
  private static final float PRICE_WIDTH = 135f;

  private static final float INVESTMENT_HEIGHT = TOTAL_Y + LINE_HEIGHT;
  private static final float ROW_WIDTH = PRICE_X + PRICE_WIDTH;

  private static final float SIDE_BY_SIDE_MIN_WIDTH = 1220f;
  private static final float STACKED_LEFT_WIDTH = 413f;
  private static final float STACK_GAP = 10f;

  @Override
  public void createPanel(CustomPanelAPI panel, float width, float height) {
    InstructionsManager im = InstructionsManager.getInstance();
    List<CapacityStrip> strips = CapacityStrip.ofUnlocked(im);
    float rowsBottom = strips.isEmpty() ? 0f
        : ROWS_Y + strips.size() * ROW_PITCH - (ROW_PITCH - ROW_HEIGHT);
    if (width >= SIDE_BY_SIDE_MIN_WIDTH && rowsBottom <= height) {
      float leftWidth = (float) Math.floor(width / 3f);
      float rightX = leftWidth + BLOCK_GAP;
      addInvestmentBlock(panel, im, leftWidth);
      addCapacityBlock(panel, im, strips, rightX, 0f, width - rightX);
      return;
    }
    float stackHeight = INVESTMENT_HEIGHT + (strips.isEmpty() ? 0f : STACK_GAP + rowsBottom);
    CustomPanelAPI stack = panel.createCustomPanel(ROW_WIDTH, stackHeight, getPlugin());
    addInvestmentBlock(stack, im, STACKED_LEFT_WIDTH);
    addCapacityBlock(stack, im, strips, 0f, INVESTMENT_HEIGHT + STACK_GAP, ROW_WIDTH);
    TooltipMakerAPI scroller = panel.createUIElement(width, height, true);
    scroller.addCustom(stack, 0f);
    panel.addUIElement(scroller).inTL(-EL_INSET, 0f);
  }

  private void addInvestmentBlock(CustomPanelAPI panel, InstructionsManager im, float blockWidth) {
    boolean docked = LGlobals.playerIsDocked();

    TooltipMakerAPI bar = panel.createUIElement(blockWidth, BAR_HEIGHT, false);
    bar.addSectionHeading("Research Investment Per Month", Alignment.MID, 0f);
    addLineHover(bar, LiminalStrings.mgmt("investment_diminishing_returns"), false);
    panel.addUIElement(bar).inTL(0f, BAR_Y);

    float textWidth = blockWidth - 2f * TEXT_X;
    TooltipMakerAPI minimum = box(panel, textWidth, LINE_HEIGHT);
    minimum.addPara("Minimum investment: %s", 0f, Misc.getHighlightColor(),
        "" + Misc.getDGSCredits(im.getMinimumResearchInvestmentPerMonth()));
    addLineHover(minimum, LiminalStrings.mgmt("investment_diminishing_returns"), false);
    place(panel, minimum, TEXT_X, MINIMUM_Y);

    TooltipMakerAPI field = box(panel, FIELD_WIDTH, CONTROL_HEIGHT);
    tf = field.addTextField(FIELD_WIDTH, CONTROL_HEIGHT, Fonts.DEFAULT_SMALL, 0f);
    tf.setText("" + im.getResearchInvestmentPerMonth());
    tf.setLimitByStringWidth(true);
    tf.setColor(Misc.getTextColor());
    place(panel, field, TEXT_X, CONTROLS_Y);

    TooltipMakerAPI save = box(panel, SAVE_WIDTH, CONTROL_HEIGHT);
    ButtonAPI saveButton = save.addButton("Save", SAVE_BUTTON_ID, Misc.getTextColor(),
        LGlobals.liminalityDarkGreenColor, Alignment.MID, CutStyle.NONE, SAVE_WIDTH, CONTROL_HEIGHT, 0f);
    LGlobals.setEnabledSayingWhy(saveButton, docked);
    if (!docked) {
      addLineHover(save, LiminalStrings.mgmt("err_docked_save"), true);
    }
    place(panel, save, SAVE_X, CONTROLS_Y);

    boolean investing = im.getResearchInvestmentPerMonth() > 0;
    TooltipMakerAPI toggle = box(panel, TOGGLE_WIDTH, CONTROL_HEIGHT);
    ButtonAPI toggleButton = toggle.addButton((investing ? "Disable" : "Enable") + " Research Investment",
        RESEARCH_INVESTMENT_BUTTON_ID, Misc.getTextColor(),
        investing ? LGlobals.liminalityDarkGreenColor : LGlobals.liminalityDarkColor, Alignment.MID,
        CutStyle.NONE, TOGGLE_WIDTH, CONTROL_HEIGHT, 0f);
    LGlobals.setEnabledSayingWhy(toggleButton, docked || investing);
    if (!docked) {
      if (investing) {
        addLineHover(toggle, LiminalStrings.mgmt("investment_disable_until_dock"), false);
      } else {
        addLineHover(toggle, LiminalStrings.mgmt("err_docked_research_investment"), true);
      }
    }
    place(panel, toggle, blockWidth - TOGGLE_WIDTH, CONTROLS_Y);

    TooltipMakerAPI talk = box(panel, TALK_WIDTH, TALK_HEIGHT);
    ButtonAPI talkButton = talk.addButton("Talk to research team", TALK_TO_RESEARCH_TEAM_BUTTON_ID,
        Misc.getTextColor(), LGlobals.liminalityDarkGreenColor, Alignment.MID, CutStyle.NONE, TALK_WIDTH,
        TALK_HEIGHT, 0f);
    if (LiminalCrisisIntel.getIntel() != null && LiminalCrisisIntel.getIntel().crisisBegun()) {
      LGlobals.setEnabledSayingWhy(talkButton, false);
      addLineHover(talk, LiminalStrings.mgmt("err_crisis_begun"), true);
    } else if (im.hasNewDialogue()) {
      talkButton.flash(false, 0.5f, 0.5f);
      talkButton.highlight();
    }
    place(panel, talk, TEXT_X, TALK_Y);

    TooltipMakerAPI total = box(panel, textWidth, LINE_HEIGHT);
    total.addPara(LiminalStrings.mgmt("label_total_research_invested"), 0f, Misc.getHighlightColor(),
        "" + Misc.getDGSCredits(im.getTotalEverResearchInvestment()));
    place(panel, total, TEXT_X, TOTAL_Y);
  }

  private void addCapacityBlock(CustomPanelAPI panel, InstructionsManager im, List<CapacityStrip> strips,
      float x, float y, float blockWidth) {
    if (strips.isEmpty()) {
      return;
    }
    TooltipMakerAPI bar = panel.createUIElement(blockWidth, BAR_HEIGHT, false);
    bar.addSectionHeading(CapacityStrip.CAPTION_TEMPLATE_CAPACITY, Alignment.MID, 0f);
    panel.addUIElement(bar).inTL(x, y + BAR_Y);
    for (int i = 0; i < strips.size(); i++) {
      addRow(panel, im, strips.get(i), x, y + ROWS_Y + i * ROW_PITCH);
    }
  }

  private void addRow(CustomPanelAPI panel, InstructionsManager im, CapacityStrip strip, float x, float y) {
    HullModSpecAPI spec = Global.getSettings().getHullModSpec(strip.getModId());
    if (spec != null) {
      TooltipMakerAPI icon = box(panel, ICON_SIZE, ICON_SIZE);
      icon.addImage(spec.getSpriteName(), ICON_SIZE, ICON_SIZE, 0f);
      place(panel, icon, x + ICON_X, y + ICON_DY);
    }
    TooltipMakerAPI name = box(panel, NAME_WIDTH, LINE_HEIGHT);
    name.addPara(im.getHullmodName(strip.getModId()), LGlobals.liminalityColor, 0f);
    place(panel, name, x + NAME_X, y + NAME_DY);
    strip.addSubLine(panel, x + NAME_X, y + SUB_LINE_DY, NAME_WIDTH);
    strip.addValue(panel, x + VALUE_RIGHT - VALUE_WIDTH, y + VALUE_DY, VALUE_WIDTH, Fonts.INSIGNIA_LARGE,
        Alignment.RMID);
    strip.addPips(panel, x + PIPS_X, y + PIPS_DY, PIPS_MAX_WIDTH);
    strip.addButton(panel, x + BUTTON_X, y + BUTTON_DY);
    strip.addPrice(panel, x + PRICE_X, y + PRICE_DY, PRICE_WIDTH);
  }

  private static TooltipMakerAPI box(CustomPanelAPI panel, float width, float height) {
    return panel.createUIElement(width + EL_SLACK, height, false);
  }

  private static void place(CustomPanelAPI panel, TooltipMakerAPI el, float x, float y) {
    panel.addUIElement(el).inTL(x - EL_INSET, y);
  }

  private static void addLineHover(TooltipMakerAPI el, final String text, final boolean negative) {
    el.addTooltipToPrevious(new LGlobals.InfoTooltip() {
      @Override
      public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        if (negative) {
          tooltip.addPara(text, Misc.getNegativeHighlightColor(), 0f);
        } else {
          tooltip.addPara(text, 0f);
        }
      }
    }, TooltipMakerAPI.TooltipLocation.BELOW, false);
  }
}

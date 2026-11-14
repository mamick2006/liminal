package liminal.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import liminal.LGlobals;
import liminal.LiminalStrings;
import liminal.instructions.Instruction;
import liminal.instructions.InstructionsManager;
import liminal.instructions.InstructionsManager.CapacityUse;
import liminal.instructions.InstructionsManager.HullmodLoadout;

public final class CapacityStrip {

  public static final float BUTTON_WIDTH = 150f;
  public static final float BUTTON_HEIGHT = 22f;
  public static final float PIP_HEIGHT = CapacityStripMath.PIP_HEIGHT;
  public static final float HOVER_WIDTH = 400f;
  public static final float CONFIRM_LINE_PAD = 7f;

  public static final String CAPTION_CAPACITY = "Capacity";
  public static final String CAPTION_OP = "Current OP";
  public static final String CAPTION_TEMPLATE_CAPACITY = "Template Capacity";

  public static final int CONFIRM_QUESTION = 0;
  public static final int CONFIRM_CREDITS_LEFT = 1;
  public static final int CONFIRM_NOT_REFUNDED = 2;

  private static final float INSET = 5f;
  private static final float OPAD = 10f;
  private static final float PAD = 3f;
  private static final float GRID_VALUE_WIDTH = 64f;
  private static final float GRID_VALUE_PAD = 10f;
  private static final boolean HOVER_ON_PIPS = true;

  private final String modId;
  private final InstructionsManager im;
  private final int used;
  private final int capacity;
  private final long price;
  private final float credits;
  private List<CapacityUse> uses;

  private CapacityStrip(String modId, InstructionsManager im, int used) {
    this.modId = modId;
    this.im = im;
    this.used = used;
    this.capacity = im.getCapacity(modId);
    this.price = im.getNextCapacityPrice(modId);
    this.credits = playerCredits();
  }

  public static CapacityStrip of(String modId, InstructionsManager im) {
    return new CapacityStrip(modId, im, im.countUsedCapacity(modId));
  }

  public static List<CapacityStrip> ofUnlocked(InstructionsManager im) {
    Map<String, Integer> usedByTemplate = im.countUsedCapacityByTemplate();
    List<CapacityStrip> strips = new ArrayList<>();
    for (String modId : im.getUnlockedLoadoutIds()) {
      Integer used = usedByTemplate.get(modId);
      strips.add(new CapacityStrip(modId, im, used == null ? 0 : used));
    }
    return strips;
  }

  public String getModId() {
    return modId;
  }

  public int getUsed() {
    return used;
  }

  public int getCapacity() {
    return capacity;
  }

  public int getLeft() {
    return capacity - used;
  }

  public boolean isOver() {
    return used > capacity;
  }

  public long getPrice() {
    return price;
  }

  public boolean canAfford() {
    return credits >= price;
  }

  public float addValue(CustomPanelAPI panel, float x, float y, float width, String font,
      Alignment align) {
    String text = used + " / " + capacity;
    float textWidth = Global.getSettings().computeStringWidth(text, font);
    float boxWidth = Math.max(width, textWidth);
    boolean right = align == Alignment.RMID || align == Alignment.TR || align == Alignment.BR;
    if (right) {
      x -= boxWidth - width;
    }
    TooltipMakerAPI el = box(panel, boxWidth);
    el.setParaFont(font);
    LabelAPI label = el.addPara("%s / %s", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
        "" + used, "" + capacity);
    label.setHighlightColors(isOver() ? Misc.getNegativeHighlightColor() : Misc.getHighlightColor(),
        Misc.getHighlightColor());
    label.setAlignment(align);
    attachHover(el, capacityHover());
    place(panel, el, x, y);
    return textWidth;
  }

  public float pipsWidth(float maxWidth) {
    return CapacityStripMath.rowWidth(CapacityStripMath.pipCount(used, capacity), (int) maxWidth);
  }

  public float addPips(CustomPanelAPI panel, float x, float y, float maxWidth) {
    float rowWidth = pipsWidth(maxWidth);
    if (rowWidth <= 0f) {
      return 0f;
    }
    TooltipMakerAPI el = box(panel, rowWidth);
    PipsPlugin plugin = new PipsPlugin(used, capacity, (int) maxWidth);
    CustomPanelAPI pips = panel.createCustomPanel(rowWidth, PIP_HEIGHT, plugin);
    plugin.positionChanged(pips.getPosition());
    el.addCustom(pips, 0f);
    if (HOVER_ON_PIPS) {
      attachHover(el, capacityHover());
    }
    place(panel, el, x, y);
    return rowWidth;
  }

  public void addSubLine(CustomPanelAPI panel, float x, float y, float width) {
    int kind = CapacityStripMath.subLine(getLeft(), hullWeights());
    if (kind == CapacityStripMath.SUB_NONE) {
      return;
    }
    boolean over = kind == CapacityStripMath.SUB_OVER;
    String number = "" + Math.abs(getLeft());
    String text;
    if (over) {
      text = String.format(LiminalStrings.mgmt("capacity_over_by"), number);
    } else if (kind == CapacityStripMath.SUB_LEFT) {
      text = String.format(LiminalStrings.mgmt("capacity_left"), number);
    } else {
      text = String.format(LiminalStrings.mgmt("capacity_left_fits"), number,
          largestHullThatFits(getLeft()));
    }
    float textWidth = Global.getSettings().computeStringWidth(text, Fonts.DEFAULT_SMALL);
    TooltipMakerAPI el = box(panel, Math.max(width, textWidth));
    LabelAPI label = el.addPara(text, over ? Misc.getNegativeHighlightColor() : Misc.getTextColor(), 0f);
    if (!over) {
      label.setHighlight(number);
      label.setHighlightColor(Misc.getHighlightColor());
    }
    place(panel, el, x, y);
  }

  public ButtonAPI addButton(CustomPanelAPI panel, float x, float y) {
    TooltipMakerAPI el = box(panel, BUTTON_WIDTH);
    ButtonAPI button = el.addButton(acceptText(), InstructionTopPanel.BUY_CAPACITY_PREFIX + modId,
        Misc.getTextColor(), LGlobals.liminalityDarkGreenColor, Alignment.MID, CutStyle.NONE,
        BUTTON_WIDTH, BUTTON_HEIGHT, 0f);
    LGlobals.setEnabledSayingWhy(button, canAfford());
    attachHover(el, addHover());
    place(panel, el, x, y);
    return button;
  }

  public float priceWidth() {
    return Global.getSettings().computeStringWidth(
        String.format(LiminalStrings.mgmt("add_capacity_price"), priceText(price)),
        Fonts.DEFAULT_SMALL);
  }

  public float addPrice(CustomPanelAPI panel, float x, float y, float width) {
    float textWidth = priceWidth();
    TooltipMakerAPI el = box(panel, Math.max(width, textWidth));
    el.addPara(LiminalStrings.mgmt("add_capacity_price"), 0f, Misc.getTextColor(),
        canAfford() ? Misc.getHighlightColor() : Misc.getNegativeHighlightColor(), priceText(price));
    place(panel, el, x, y);
    return textWidth;
  }

  private static TooltipMakerAPI box(CustomPanelAPI panel, float width) {
    return panel.createUIElement(width + 2f * INSET, 0f, false);
  }

  private static void place(CustomPanelAPI panel, TooltipMakerAPI el, float x, float y) {
    panel.addUIElement(el).inTL(x - INSET, y);
  }

  public static void attachHover(TooltipMakerAPI el, TooltipMakerAPI.TooltipCreator hover) {
    el.addTooltipToPrevious(hover, TooltipMakerAPI.TooltipLocation.BELOW, false);
  }

  public TooltipMakerAPI.TooltipCreator capacityHover() {
    return new Hover() {
      @Override
      public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        tooltip.addTitle(CAPTION_CAPACITY);
        tooltip.addPara(LiminalStrings.mgmt("template_capacity_rule"), OPAD, Misc.getHighlightColor(),
            "" + InstructionsManager.sizeWeight(HullSize.FRIGATE),
            "" + InstructionsManager.sizeWeight(HullSize.DESTROYER),
            "" + InstructionsManager.sizeWeight(HullSize.CRUISER),
            "" + InstructionsManager.sizeWeight(HullSize.CAPITAL_SHIP));

        tooltip.addPara(LiminalStrings.mgmt("hover_used"), OPAD,
            isOver() ? Misc.getNegativeHighlightColor() : Misc.getHighlightColor(), "" + used);
        if (uses == null) {
          uses = im.getCapacityUses(modId);
        }
        if (!uses.isEmpty()) {
          beginGrid(tooltip);
          int row = 0;
          for (CapacityUse use : uses) {
            gridRow(tooltip, row++, "" + use.weight, use.shipName + ", " + use.hullClass);
          }
          tooltip.addGrid(PAD);
        }

        tooltip.addPara(LiminalStrings.mgmt("capacity_hover_total"), OPAD, Misc.getHighlightColor(),
            "" + capacity);
        beginGrid(tooltip);
        gridRow(tooltip, 0, "" + InstructionsManager.getCapacityStart(), "Starting capacity");
        gridRow(tooltip, 1, "" + im.getCapacityBought(modId), "Bought");
        int shop = capacity - InstructionsManager.getCapacityStart() - im.getCapacityBought(modId);
        if (shop != 0) {
          gridRow(tooltip, 2, "" + shop, "Linkage Shop");
        }
        tooltip.addGrid(PAD);

        if (isOver()) {
          im.addOverCapacityLine(tooltip, modId, used, OPAD);
        }
      }
    };
  }

  public TooltipMakerAPI.TooltipCreator addHover() {
    return new Hover() {
      @Override
      public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        float pad = 0f;
        String hull = largestHullThatFits(getLeft() + 1);
        if (hull != null) {
          addNumberAndHull(tooltip, LiminalStrings.mgmt("add_capacity_after"), getLeft() + 1, hull, pad);
          pad = OPAD;
        }
        float factor = InstructionsManager.getCapacityPriceFactor();
        if (factor != 1f) {
          tooltip.addPara(LiminalStrings.mgmt("price_line"), pad, Misc.getHighlightColor(),
              priceText(price));
          String normal = priceText(im.getNextCapacityNormalPrice(modId));
          float valueWidth = Math.max(GRID_VALUE_WIDTH, (float) Math.ceil(
              Global.getSettings().computeStringWidth(normal, Fonts.DEFAULT_SMALL)));
          beginGrid(tooltip, valueWidth);
          gridRow(tooltip, 0, normal, "Normal price", valueWidth);
          gridRow(tooltip, 1, factorText(factor), "Survival Mode", valueWidth);
          tooltip.addGrid(PAD);
          pad = OPAD;
        }
        tooltip.addPara(LiminalStrings.mgmt("add_capacity_next_price"), pad, Misc.getHighlightColor(),
            priceText(im.getCapacityPriceAfterNext(modId)));
        if (!canAfford()) {
          String have = priceText((long) credits);
          LabelAPI label = tooltip.addPara(LiminalStrings.mgmt("err_not_enough_credits") + " "
              + String.format(LiminalStrings.mgmt("credits_you_have"), have),
              Misc.getNegativeHighlightColor(), OPAD);
          label.setHighlight(have);
          label.setHighlightColor(Misc.getHighlightColor());
        }
      }
    };
  }

  public static TooltipMakerAPI.TooltipCreator opHover(final String modId) {
    return new Hover() {
      @Override
      public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        InstructionsManager im = InstructionsManager.getInstance();
        HullmodLoadout loadout = im.getLoadout(modId);
        tooltip.addTitle(CAPTION_OP);

        int usedOP = im.getCurrentOPForLoadout(modId);
        tooltip.addPara(LiminalStrings.mgmt("hover_used"), OPAD, Misc.getHighlightColor(), "" + usedOP);
        List<Instruction> held = LGlobals.ignoreRestrictions ? new ArrayList<Instruction>()
            : im.getInstructionsForLoadout(modId);
        if (!held.isEmpty()) {
          beginGrid(tooltip);
          int row = 0;
          for (Instruction instruction : held) {
            gridRow(tooltip, row++, "" + instruction.getOpCost(), instruction.getDisplayName());
          }
          tooltip.addGrid(PAD);
        }

        tooltip.addPara(LiminalStrings.mgmt("op_hover_total"), OPAD, Misc.getHighlightColor(),
            "" + loadout.getMaxOP());
        beginGrid(tooltip);
        int row = 0;
        gridRow(tooltip, row++, "" + loadout.getBaseOP(), im.getHullmodName(modId));
        if (loadout.isCrisisOPBonusActive()) {
          gridRow(tooltip, row++, factorText(HullmodLoadout.CRISIS_OP_MULT), "After the crisis");
        }
        if (loadout.getSettingOPMult() != 1.0) {
          gridRow(tooltip, row++, factorText(loadout.getSettingOPMult()), "Template OP Multiplier");
        }
        tooltip.addGrid(PAD);

        HullModSpecAPI spec = Global.getSettings().getHullModSpec(modId);
        if (spec != null) {
          String frigate = "" + spec.getCostFor(HullSize.FRIGATE);
          String destroyer = "" + spec.getCostFor(HullSize.DESTROYER);
          String cruiser = "" + spec.getCostFor(HullSize.CRUISER);
          String capital = "" + spec.getCostFor(HullSize.CAPITAL_SHIP);
          if (liminal.intel.LiminalMessageMath.usesOPOverride(InstructionsManager.templateOPOverride())) {
            tooltip.addPara(LiminalStrings.mgmt("op_cost_by_size_tooltip_override"), OPAD,
                Misc.getHighlightColor(), frigate, destroyer, cruiser, capital);
          } else {
            tooltip.addPara(LiminalStrings.mgmt("op_cost_by_size_tooltip"), OPAD,
                Misc.getHighlightColor(), frigate, destroyer, cruiser, capital);
          }
        }
      }
    };
  }

  private abstract static class Hover extends LGlobals.InfoTooltip {
    @Override
    public float getTooltipWidth(Object tooltipParam) {
      return HOVER_WIDTH;
    }
  }

  private static void beginGrid(TooltipMakerAPI tooltip) {
    beginGrid(tooltip, GRID_VALUE_WIDTH);
  }

  private static void beginGrid(TooltipMakerAPI tooltip, float valueWidth) {
    tooltip.beginGridFlipped(HOVER_WIDTH, 1, valueWidth, GRID_VALUE_PAD);
  }

  private static void gridRow(TooltipMakerAPI tooltip, int row, String value, String label) {
    gridRow(tooltip, row, value, label, GRID_VALUE_WIDTH);
  }

  private static void gridRow(TooltipMakerAPI tooltip, int row, String value, String label,
      float valueWidth) {
    String shown = tooltip.shortenString(label, HOVER_WIDTH - valueWidth - GRID_VALUE_PAD);
    tooltip.addToGrid(0, row, shown, value, Misc.getHighlightColor());
  }

  private static String factorText(double factor) {
    String number = String.format(Locale.US, "%.2f", factor);
    number = number.replaceAll("0+$", "").replaceAll("\\.$", "");
    return Strings.X + number;
  }

  public static String acceptText() {
    return LiminalStrings.mgmt("add_capacity_button");
  }

  public static void addConfirmPrompt(Object buttonId, TooltipMakerAPI prompt) {
    String modId = InstructionTopPanel.getBuyCapacityTemplate(buttonId);
    for (int line = CONFIRM_QUESTION; line <= CONFIRM_NOT_REFUNDED; line++) {
      addConfirmLine(modId, line, prompt, line == CONFIRM_QUESTION ? 0f : CONFIRM_LINE_PAD);
    }
  }

  public static LabelAPI addConfirmLine(String modId, int line, TooltipMakerAPI prompt, float pad) {
    InstructionsManager im = InstructionsManager.getInstance();
    long price = im.getNextCapacityPrice(modId);
    if (line == CONFIRM_QUESTION) {
      return prompt.addPara(LiminalStrings.mgmt("confirm_add_capacity"), pad, Misc.getHighlightColor(),
          im.getHullmodName(modId), Misc.getFormat().format(price));
    }
    if (line == CONFIRM_CREDITS_LEFT) {
      return prompt.addPara(LiminalStrings.mgmt("confirm_credits_left"), pad, Misc.getHighlightColor(),
          priceText((long) playerCredits() - price));
    }
    return prompt.addPara(LiminalStrings.mgmt("capacity_not_refunded"), Misc.getGrayColor(), pad);
  }

  public static String priceText(long credits) {
    return Misc.getFormat().format(credits) + Strings.C;
  }

  public static float playerCredits() {
    CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
    return fleet == null ? 0f : fleet.getCargo().getCredits().get();
  }

  private static final HullSize[] HULLS = { HullSize.FRIGATE, HullSize.DESTROYER, HullSize.CRUISER,
      HullSize.CAPITAL_SHIP };
  private static final String[] HULL_NAMES = { "frigate", "destroyer", "cruiser", "capital ship" };

  private static int[] hullWeights() {
    int[] weights = new int[HULLS.length];
    for (int i = 0; i < HULLS.length; i++) {
      weights[i] = InstructionsManager.sizeWeight(HULLS[i]);
    }
    return weights;
  }

  public static String largestHullThatFits(int left) {
    int fit = CapacityStripMath.largestFit(left, hullWeights());
    return fit < 0 ? null : HULL_NAMES[fit];
  }

  private static void addNumberAndHull(TooltipMakerAPI to, String format, int number, String hull,
      float pad) {
    LabelAPI label = to.addPara(String.format(format, "" + number, hull), Misc.getTextColor(), pad);
    label.setHighlight("" + number);
    label.setHighlightColor(Misc.getHighlightColor());
  }

  private static final class PipsPlugin extends BaseCustomUIPanelPlugin {
    private final float[] xs;
    private final float[] widths;
    private final Color[] colors;
    private PositionAPI position;

    PipsPlugin(int used, int capacity, int maxWidth) {
      Color usedColor = Misc.getHighlightColor();
      Color freeColor = Misc.getDarkPlayerColor();
      Color overColor = Misc.getNegativeHighlightColor();
      int count = CapacityStripMath.pipCount(used, capacity);
      int pipWidth = CapacityStripMath.pipWidth(count, maxWidth);
      if (pipWidth > 0) {
        xs = new float[count];
        widths = new float[count];
        colors = new Color[count];
        for (int i = 0; i < count; i++) {
          xs[i] = CapacityStripMath.pipX(i, pipWidth);
          widths[i] = pipWidth;
          colors[i] = i >= capacity ? overColor : i < used ? usedColor : freeColor;
        }
      } else {
        int usedEnd = CapacityStripMath.barFill(Math.min(used, capacity), count, maxWidth);
        int capacityEnd = CapacityStripMath.barFill(capacity, count, maxWidth);
        xs = new float[] { 0f, usedEnd, capacityEnd };
        widths = new float[] { usedEnd, capacityEnd - usedEnd, maxWidth - capacityEnd };
        colors = new Color[] { usedColor, freeColor, overColor };
      }
    }

    @Override
    public void positionChanged(PositionAPI position) {
      this.position = position;
    }

    @Override
    public void render(float alphaMult) {
      if (position == null) {
        return;
      }
      float left = position.getX();
      float bottom = position.getY();
      GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glBegin(GL11.GL_QUADS);
      for (int i = 0; i < xs.length; i++) {
        if (widths[i] <= 0f) {
          continue;
        }
        Color color = colors[i];
        GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(),
            (byte) (color.getAlpha() * alphaMult));
        float x = left + xs[i];
        GL11.glVertex2f(x, bottom);
        GL11.glVertex2f(x, bottom + PIP_HEIGHT);
        GL11.glVertex2f(x + widths[i], bottom + PIP_HEIGHT);
        GL11.glVertex2f(x + widths[i], bottom);
      }
      GL11.glEnd();
      GL11.glPopAttrib();
    }
  }
}

package liminal.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.intel.misc.MapMarkerIntel;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import liminal.LGlobals;
import liminal.intel.LiminalCrisisIntel;
import liminal.intel.LiminalityIntel;
import liminal.ui.TabbedPanelsInterface.Panel;

public final class LiminalConsolePanel implements CustomUIPanelPlugin {

  private static final float CONTENT_PAD = 10f;

  private static LiminalConsolePanel live;

  public static final String CRISIS_TAB = "Crisis";

  public static final String CONFIRM_OK = "liminal_console_confirm_ok";
  public static final String CONFIRM_CANCEL = "liminal_console_confirm_cancel";

  private Object pendingConfirm;

  private static volatile boolean dialogOpenFailWarned;

  private final CustomPanelAPI root;
  private final float width;
  private final float height;
  private CustomPanelAPI content;
  private PositionAPI pos;
  private static final String SUBTAB_KEY = "$liminality_mgmt_subtab";

  private String activeTab = restoreSubtab(LiminalityIntel.FUNDING_TAB);

  public LiminalConsolePanel(float width, float height) {
    this.width = width;
    this.height = height;
    this.root = Global.getSettings().createCustom(width, height, this);
    live = this;
    rebuild();
  }

  public static void routeButton(Object buttonId) {
    if (live != null) {
      live.handleButton(buttonId);
    }
  }

  public static void dropLive(LiminalConsolePanel console) {
    if (console != null && live == console) {
      live = null;
      LiminalityUpkeep.tf = null;
      TabbedPanelsInterface.Panel.setCurrentPanels(new TabbedPanelsInterface.Panel[0]);
    }
  }

  public static void resetStaticsForLoad() {
    live = null;
    LiminalityUpkeep.tf = null;
    TabbedPanelsInterface.Panel.setCurrentPanels(new TabbedPanelsInterface.Panel[0]);
    ListScrollMemory.clear();
  }

  public CustomPanelAPI getPanel() {
    return root;
  }

  private void rebuild() {
    if (content != null) {
      try {
        root.removeComponent(content);
      } catch (Throwable ignore) {
      }
    }
    float cw = width - 2f * CONTENT_PAD;
    float ch = height - 2f * CONTENT_PAD;
    content = root.createCustomPanel(cw, ch, LiminalConsoleRoute.INSTANCE);
    root.addComponent(content).inTL(CONTENT_PAD, CONTENT_PAD);
    LiminalityIntel menus = LiminalityIntel.getIntel();
    if (menus == null) {
      return;
    }
    if (pendingConfirm != null) {
      buildConfirm(cw, ch);
      return;
    }
    ConsoleTabs tabs = new ConsoleTabs(menus) {
      @Override
      public String activeTab() {
        return activeTab;
      }
    };
    List<String> names = tabs.getTabNames();
    if (!names.contains(activeTab)) {
      activeTab = names.isEmpty() ? LiminalityIntel.FUNDING_TAB : names.get(0);
    }
    try {
      TabbedPanelsInterface.TabbedPanelsUtils.createTopLevelPanels(content, cw, ch, tabs,
          LiminalConsoleRoute.INSTANCE);
    } catch (Throwable ignore) {
    }
  }

  @Override
  public void buttonPressed(Object buttonId) {
    handleButton(buttonId);
  }

  private void handleButton(Object buttonId) {
    LiminalityIntel menus = LiminalityIntel.getIntel();
    if (menus == null) {
      return;
    }
    if (pendingConfirm != null) {
      if (!CONFIRM_OK.equals(buttonId) && !CONFIRM_CANCEL.equals(buttonId)) {
        return;
      }
      Object parked = pendingConfirm;
      pendingConfirm = null;
      if (CONFIRM_CANCEL.equals(buttonId)) {
        rebuild();
        return;
      }
      buttonId = parked;
    } else if (CONFIRM_OK.equals(buttonId) || CONFIRM_CANCEL.equals(buttonId)) {
      return;
    } else if (InstructionTopPanel.isBuyCapacityButton(buttonId)) {
      pendingConfirm = buttonId;
      saveScrollState();
      rebuild();
      return;
    }
    if (TabbedPanelsInterface.TabbedPanelsUtils.isTabButton(buttonId)) {
      activeTab = TabbedPanelsInterface.TabbedPanelsUtils.getTabName(buttonId);
      rememberSubtab(activeTab);
      saveScrollState();
      rebuild();
      return;
    }
    menus.handleMenuButton(buttonId, new RefreshShim());
  }

  private static final float CONFIRM_WIDTH = 500f;
  private static final float CONFIRM_HEIGHT = 140f;
  private static final float CONFIRM_ICON = 48f;
  private static final float CONFIRM_ICON_X = 16f;
  private static final float CONFIRM_TOP = 16f;
  private static final float CONFIRM_TEXT_X = 80f;
  private static final float CONFIRM_LINE_GAP = 10f;
  private static final float CONFIRM_TEXT_HEIGHT = 65f;
  private static final float CONFIRM_BUTTON_WIDTH = 200f;
  private static final float CONFIRM_BUTTON_HEIGHT = 24f;
  private static final float CONFIRM_BUTTON_Y = 98f;
  private static final float CONFIRM_ACCEPT_X = 40f;
  private static final float CONFIRM_CANCEL_X = 260f;
  private static final float ELEMENT_INSET = 5f;

  private void buildConfirm(float cw, float ch) {
    String modId = InstructionTopPanel.getBuyCapacityTemplate(pendingConfirm);

    float textWidth = CONFIRM_WIDTH - CONFIRM_TEXT_X - CONFIRM_ICON_X;
    TooltipMakerAPI text = content.createUIElement(textWidth + 2f * ELEMENT_INSET, CONFIRM_TEXT_HEIGHT,
        false);
    for (int line = CapacityStrip.CONFIRM_QUESTION; line <= CapacityStrip.CONFIRM_NOT_REFUNDED; line++) {
      CapacityStrip.addConfirmLine(modId, line, text,
          line == CapacityStrip.CONFIRM_QUESTION ? 0f : CONFIRM_LINE_GAP);
    }
    float grown = Math.max(0f, text.getHeightSoFar() - CONFIRM_TEXT_HEIGHT);
    float boxHeight = CONFIRM_HEIGHT + grown;
    float bx = Math.round((cw - CONFIRM_WIDTH) / 2f);
    float by = Math.round((ch - boxHeight) / 2f);

    CustomPanelAPI box = content.createCustomPanel(CONFIRM_WIDTH, boxHeight, new ConfirmBox());
    content.addComponent(box).inTL(bx, by);

    HullModSpecAPI spec = Global.getSettings().getHullModSpec(modId);
    if (spec != null) {
      TooltipMakerAPI icon = content.createUIElement(CONFIRM_ICON + 2f * ELEMENT_INSET, CONFIRM_ICON,
          false);
      icon.addImage(spec.getSpriteName(), CONFIRM_ICON, CONFIRM_ICON, 0f);
      content.addUIElement(icon).inTL(bx + CONFIRM_ICON_X - ELEMENT_INSET, by + CONFIRM_TOP);
    }
    content.addUIElement(text).inTL(bx + CONFIRM_TEXT_X - ELEMENT_INSET, by + CONFIRM_TOP);

    float buttonY = by + CONFIRM_BUTTON_Y + grown;
    addConfirmButton(CapacityStrip.acceptText(), CONFIRM_OK, LGlobals.liminalityDarkGreenColor,
        bx + CONFIRM_ACCEPT_X, buttonY);
    addConfirmButton("Cancel", CONFIRM_CANCEL, LGlobals.liminalityDarkColor, bx + CONFIRM_CANCEL_X,
        buttonY);
  }

  private void addConfirmButton(String face, String id, Color background, float x, float y) {
    TooltipMakerAPI el = content.createUIElement(CONFIRM_BUTTON_WIDTH + 2f * ELEMENT_INSET,
        CONFIRM_BUTTON_HEIGHT, false);
    el.addButton(face, id, Misc.getTextColor(), background, Alignment.MID, CutStyle.NONE,
        CONFIRM_BUTTON_WIDTH, CONFIRM_BUTTON_HEIGHT, 0f);
    content.addUIElement(el).inTL(x - ELEMENT_INSET, y);
  }

  private static final class ConfirmBox extends BaseCustomUIPanelPlugin {
    private PositionAPI pos;

    @Override
    public void positionChanged(PositionAPI position) {
      this.pos = position;
    }

    @Override
    public void renderBelow(float alphaMult) {
      if (pos == null) {
        return;
      }
      float x = pos.getX();
      float y = pos.getY();
      float w = pos.getWidth();
      float h = pos.getHeight();
      Color border = Misc.getDarkPlayerColor();
      GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glBegin(GL11.GL_QUADS);
      GL11.glColor4ub((byte) 0, (byte) 0, (byte) 0, (byte) (255f * alphaMult));
      quad(x, y, w, h);
      GL11.glColor4ub((byte) border.getRed(), (byte) border.getGreen(), (byte) border.getBlue(),
          (byte) (border.getAlpha() * alphaMult));
      quad(x, y, w, 1f);
      quad(x, y + h - 1f, w, 1f);
      quad(x, y + 1f, 1f, h - 2f);
      quad(x + w - 1f, y + 1f, 1f, h - 2f);
      GL11.glEnd();
      GL11.glPopAttrib();
    }

    private static void quad(float x, float y, float w, float h) {
      GL11.glVertex2f(x, y);
      GL11.glVertex2f(x, y + h);
      GL11.glVertex2f(x + w, y + h);
      GL11.glVertex2f(x + w, y);
    }
  }

  public static String restoreSubtab(String fallback) {
    try {
      Object v = Global.getSector() == null ? null
          : Global.getSector().getMemoryWithoutUpdate().get(SUBTAB_KEY);
      return v instanceof String ? (String) v : fallback;
    } catch (Throwable ignore) {
      return fallback;
    }
  }

  public static void rememberSubtab(String tab) {
    try {
      if (Global.getSector() != null) {
        Global.getSector().getMemoryWithoutUpdate().set(SUBTAB_KEY, tab);
      }
    } catch (Throwable ignore) {
    }
  }

  private void saveScrollState() {
    for (Panel p : Panel.getCurrentPanels()) {
      p.saveScrollState();
    }
  }

  @Override
  public void positionChanged(PositionAPI position) {
    this.pos = position;
  }

  @Override
  public void renderBelow(float alphaMult) {
    if (pos == null) {
      return;
    }
    float x = pos.getX();
    float y = pos.getY();
    float w = pos.getWidth();
    float h = pos.getHeight();
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4ub((byte) 4, (byte) 6, (byte) 9, (byte) (255f * alphaMult));
    GL11.glBegin(GL11.GL_QUADS);
    GL11.glVertex2f(x, y);
    GL11.glVertex2f(x, y + h);
    GL11.glVertex2f(x + w, y + h);
    GL11.glVertex2f(x + w, y);
    GL11.glEnd();
    GL11.glPopAttrib();
  }

  @Override
  public void render(float alphaMult) {
  }

  @Override
  public void advance(float amount) {
  }

  @Override
  public void processInput(List<InputEventAPI> events) {
  }

  public static abstract class ConsoleTabs implements TabbedPanelsInterface {
    private final LiminalityIntel menus;

    protected ConsoleTabs(LiminalityIntel menus) {
      this.menus = menus;
    }

    @Override
    public List<String> getTabNames() {
      List<String> names = new ArrayList<String>(menus.getTabNames());
      if (crisisExists()) {
        names.add(CRISIS_TAB);
      }
      return names;
    }

    @Override
    public Map<String, List<Panel>> getPanelsForTabs() {
      Map<String, List<Panel>> panels = new HashMap<String, List<Panel>>(menus.getPanelsForTabs());
      if (crisisExists()) {
        panels.put(CRISIS_TAB, Arrays.<Panel>asList(new CrisisPanel()));
      }
      return panels;
    }

    private boolean crisisExists() {
      return LiminalCrisisIntel.getIntel() != null;
    }
  }

  private static final class CrisisPanel extends Panel {
    CrisisPanel() {
      super(1.0f);
    }

    @Override
    public void createPanel(CustomPanelAPI panel, float width, float height) {
      LiminalCrisisIntel crisis = LiminalCrisisIntel.getIntel();
      if (crisis != null) {
        crisis.createCrisisBody(panel, width, height);
      }
    }
  }

  private final class RefreshShim implements IntelUIAPI {
    @Override
    public void updateUIForItem(IntelInfoPlugin plugin) {
      rebuild();
    }

    @Override
    public void recreateIntelUI() {
      rebuild();
    }

    @Override
    public void showDialog(SectorEntityToken target, String trigger) {
      if (target == null) {
        target = Global.getSector().getPlayerFleet();
      }
      openDialogOverCore(new RuleBasedInteractionDialogPluginImpl(trigger), target);
    }

    @Override
    public void showDialog(SectorEntityToken target, InteractionDialogPlugin plugin) {
      openDialogOverCore(plugin, target);
    }

    private void openDialogOverCore(InteractionDialogPlugin plugin, SectorEntityToken target) {
      boolean shown = Global.getSector().getCampaignUI().showInteractionDialogFromCargo(plugin, target,
          new CampaignUIAPI.DismissDialogDelegate() {
            @Override
            public void dialogDismissed() {
              rebuild();
            }
          });
      if (!shown && !dialogOpenFailWarned) {
        dialogOpenFailWarned = true;
        Global.getLogger(LiminalConsolePanel.class).warn("Liminal: the research-team dialog did not open"
            + " (the engine refused a dialog mid state transition). Reopen the Liminal tab and press the"
            + " button again.");
      }
    }

    @Override
    public void showEditIntelMarkerDialog(MapMarkerIntel intel) {
    }

    @Override
    public void updateIntelList() {
    }

    @Override
    public void updateIntelList(boolean retainCurrentSelection) {
    }

    @Override
    public void updateIntelList(boolean retainCurrentSelection, List<IntelInfoPlugin> show) {
    }

    @Override
    public void selectItem(IntelInfoPlugin plugin) {
    }

    @Override
    public boolean isShowingCustomIntelSubset() {
      return false;
    }

    @Override
    public void showOnMap(SectorEntityToken token) {
    }

    @Override
    public void saveSmallDescScrollState() {
    }

    @Override
    public void restoreSmallDescScrollState() {
    }
  }
}

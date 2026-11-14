package liminal.ui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import com.fs.starfarer.api.util.Misc;

import liminal.topbar.LiminalMgmtInjector;

public final class LiminalMgmtScreen {

  private static final float TAB_W = 130f;
  private static final float TAB_H = 18f;
  private static final float ROW_PAD = 6f;
  private static final float FLASH_RETRIGGER = 0.6f;
  private static final String TAB_DATA_PREFIX = "mgmt_tab_";
  private static final String LAST_TAB_KEY = "$mgmt_last_tab";

  private static LiminalMgmtScreen live;
  private static LiminalMgmtScreen staged;

  public static boolean isOpen() {
    return live != null;
  }

  public static String selectedTenant() {
    LiminalMgmtScreen s = live;
    return s == null ? null : s.selectedModId;
  }

  public static CustomPanelAPI contentPanel() {
    LiminalMgmtScreen s = live;
    return s == null ? null : s.content;
  }

  public static Object liveShell() {
    LiminalMgmtScreen s = live;
    return s == null ? null : s.shell;
  }

  public static boolean close() {
    LiminalMgmtScreen s = live;
    if (s == null) {
      return false;
    }
    return LiminalMgmtInjector.dismissCore(s.hostL);
  }

  public static void resetStaticsForLoad() {
    live = null;
    staged = null;
  }

  public static CustomPanelAPI stageForShell(Object shell, float width, float height) {
    LiminalMgmtScreen s = new LiminalMgmtScreen(shell, width, height);
    s.buildRoot();
    staged = s;
    return s.root;
  }

  public static void promoteStaged(Object shell, Object hostL) {
    LiminalMgmtScreen s = staged;
    if (s == null || s.shell != shell) {
      return;
    }
    staged = null;
    s.hostL = hostL;
    live = s;
    LiminalMgmtInjector.noteScreenOpen(true);
    s.applyInitialSelect();
  }

  public static void discardStaged(Object shell) {
    LiminalMgmtScreen s = staged;
    if (s != null && s.shell == shell) {
      staged = null;
    }
  }

  public static void onTabClosed(Object shell) {
    LiminalMgmtScreen s = live;
    if (s == null || s.shell != shell) {
      LiminalMgmtScreen st = staged;
      if (st != null && st.shell == shell) {
        staged = null;
      }
      return;
    }
    live = null;
    LiminalMgmtInjector.noteScreenOpen(false);
    try {
      LiminalMgmtInjector.releaseTenantConsole(s.selectedModId);
    } catch (Throwable ignore) {
    }
    s.content = null;
    s.tabButtons.clear();
  }

  private final List<LiminalMgmtInjector.Tenant> tenants;
  private final Map<String, ButtonAPI> tabButtons = new LinkedHashMap<String, ButtonAPI>();
  private final Object shell;
  private final float width;
  private final float height;
  private final String firstArrivalModId;
  private Object hostL;
  private CustomPanelAPI root;
  private CustomPanelAPI content;
  private String selectedModId;
  private float flashTimer;

  private LiminalMgmtScreen(Object shell, float width, float height) {
    this.shell = shell;
    this.width = width;
    this.height = height;
    tenants = LiminalMgmtInjector.tenants();
    firstArrivalModId = tenants.isEmpty() ? null : tenants.get(0).modId;
    java.util.Collections.sort(tenants, new java.util.Comparator<LiminalMgmtInjector.Tenant>() {
      @Override
      public int compare(LiminalMgmtInjector.Tenant a, LiminalMgmtInjector.Tenant b) {
        int r = Integer.compare(tenantRank(a.modId), tenantRank(b.modId));
        return r != 0 ? r : a.modId.compareTo(b.modId);
      }
    });
  }

  private static int tenantRank(String modId) {
    if ("nomadic_fleet".equals(modId)) {
      return 0;
    }
    if ("liminal".equals(modId)) {
      return 1;
    }
    if ("captains_quarters".equals(modId)) {
      return 2;
    }
    return 3;
  }

  private final class Plugin extends BaseCustomUIPanelPlugin {

    @Override
    public void buttonPressed(Object data) {
      if (data instanceof String && ((String) data).startsWith(TAB_DATA_PREFIX)) {
        applySelect(((String) data).substring(TAB_DATA_PREFIX.length()));
      }
    }

    @Override
    public void advance(float amount) {
      if (live != LiminalMgmtScreen.this) {
        return;
      }
      String late = LiminalMgmtInjector.consumeQueuedSelectTab();
      if (late != null) {
        applySelect(late);
      }
      flashTimer += amount;
      if (flashTimer < FLASH_RETRIGGER) {
        return;
      }
      flashTimer = 0f;
      for (Map.Entry<String, ButtonAPI> e : tabButtons.entrySet()) {
        String modId = e.getKey();
        if (!modId.equals(selectedModId) && LiminalMgmtInjector.attentionActive(modId)) {
          e.getValue().flash(false, 0.25f, 0.25f);
        }
      }
    }
  }

  private void applyInitialSelect() {
    String initial = LiminalMgmtInjector.consumeQueuedSelectTab();
    if (initial == null || !tabButtons.containsKey(initial)) {
      initial = lastSelectedTab();
    }
    if (initial == null || !tabButtons.containsKey(initial)) {
      initial = firstArrivalModId;
    }
    if (initial != null) {
      applySelect(initial);
    }
  }

  private static String lastSelectedTab() {
    try {
      if (Global.getSector() == null) {
        return null;
      }
      Object v = Global.getSector().getMemoryWithoutUpdate().get(LAST_TAB_KEY);
      return v instanceof String ? (String) v : null;
    } catch (Throwable ignore) {
      return null;
    }
  }

  private static void rememberSelectedTab(String modId) {
    try {
      if (Global.getSector() != null) {
        Global.getSector().getMemoryWithoutUpdate().set(LAST_TAB_KEY, modId);
      }
    } catch (Throwable ignore) {
    }
  }

  private void buildRoot() {
    root = Global.getSettings().createCustom(width, height, new Plugin());
    TooltipMakerAPI row = root.createUIElement(width, TAB_H + 4f, false);
    int i = 0;
    for (LiminalMgmtInjector.Tenant t : tenants) {
      boolean hasShortcut = i <= 8;
      String label = hasShortcut ? "[" + (i + 1) + "] " + t.label : t.label;
      ButtonAPI b = row.addButton(label, TAB_DATA_PREFIX + t.modId,
          t.color != null ? t.color : Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
          Alignment.MID, CutStyle.TOP, TAB_W, TAB_H, 0f);
      if (hasShortcut) {
        b.setShortcut(Keyboard.KEY_1 + i, false);
      }
      b.getPosition().inTL(i * (TAB_W + ROW_PAD), 2f);
      b.getPosition().setSize(TAB_W, TAB_H);
      tabButtons.put(t.modId, b);
      i++;
    }
    root.addUIElement(row).inTL(0f, 0f);
  }

  private void applySelect(String modId) {
    if (modId == null || modId.equals(selectedModId) || !tabButtons.containsKey(modId)) {
      return;
    }
    try {
      if (content != null) {
        root.removeComponent(content);
        LiminalMgmtInjector.releaseTenantConsole(selectedModId);
        content = null;
      }
      float ch = height - TAB_H - 10f;
      content = LiminalMgmtInjector.buildTenantConsole(modId, width, ch);
      root.addComponent(content).inTL(0f, TAB_H + 8f);
      selectedModId = modId;
      for (Map.Entry<String, ButtonAPI> e : tabButtons.entrySet()) {
        boolean sel = e.getKey().equals(modId);
        e.getValue().setChecked(sel);
        if (sel) {
          e.getValue().highlight();
        } else {
          e.getValue().unhighlight();
        }
      }
      LiminalMgmtInjector.noteTabSelected(modId);
      rememberSelectedTab(modId);
    } catch (Throwable t) {
      Global.getLogger(LiminalMgmtScreen.class).warn("Liminal: tenant console " + modId
          + " failed to build (" + t + "); its tab stays and its fallback intel entry covers it.");
    }
  }
}

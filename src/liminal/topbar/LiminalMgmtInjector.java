package liminal.topbar;

import java.awt.Color;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

import liminal.intel.LiminalityIntel;
import liminal.ui.LiminalConsolePanel;
import liminal.ui.LiminalMgmtScreen;

public class LiminalMgmtInjector implements EveryFrameScript {

  static final String HANDSHAKE_KEY = "$mgmt_bar";
  public static final String OPEN_KEY = "$mgmt_open";
  public static final String NOTICE_KEY = "$mgmt_notice";
  static final int CONTRACT_VERSION = 2;
  private static final String DK_HOST = "host";
  private static final String DK_VERSION = "version";
  private static final String DK_SEAM = "seam";

  private static final String MOD_ID = "liminal";
  private static final String SEAM_CLASS = "liminal.topbar.LiminalMgmtInjector";
  private static final String SHELL_NAME = "LiminalMgmtTabShell";
  private static MethodHandle shellCtorCache;
  private static final String KEYMAP_PAIR_CLASS = "com.fs.util.container.Pair";
  private static boolean keymapResolved;
  private static Class<?> keymapControl;
  private static Object keymapPairOf;
  private static MethodHandle keymapSlotOne;
  private static MethodHandle keymapSlotTwo;
  private static Class<?> keySlotClass;
  private static final String RULE_LISTENER = "button listener";
  private static final String RULE_BAR = "bar";
  private static final String RULE_CONTROL = "control enum";
  private static final String RULE_KEYMAP = "keymap class";
  private static final String RULE_KEYS = "control keys method";
  private static final String RULE_SLOT = "key slot";
  private static final Map<String, String> LOOKUPS = new LinkedHashMap<String, String>();
  private static boolean lookupsLogged;
  private static final Object BUTTON_TAB_ID = "mgmt_tab";
  private static final String ATTENTION_KEY = "$liminal_mgmt_attention";
  private static final String OPENED_KEY = "$liminal_mgmt_opened";
  private static final String SELECT_TAB_KEY = "$liminality_select_tab";

  private static final float BAR_BTN_W_MIN = 110f;
  private static final float BAR_BTN_W_MAX = 134f;
  private static final float BAR_BTN_H_MIN = 17f;
  private static final float BAR_BTN_H_MAX = 25f;
  private static final int MIN_BAR_BUTTONS = 5;
  private static final int SEARCH_DEPTH = 8;
  private static final int MAX_LOCATE_FAILS = 60;
  private static final float BUTTON_PAD = 6f;
  private static final float PLATE_PERIOD_W = 128f;
  private static final float PLATE_EDGE_W = 11f;
  private static final float PLATE_W = PLATE_PERIOD_W + PLATE_EDGE_W;
  private static final float PLATE_H = 35f;
  private static final float PLATE_SRC_W = 914f;
  private static final float PLATE_SRC_H = 35f;
  private static final float PLATE_SRC_PATTERN_X = PLATE_SRC_W - PLATE_W;
  private static final float PLATE_SRC_EDGE_X = PLATE_SRC_W - PLATE_EDGE_W;

  private static final List<String> MGMT_SCOPE_BINDINGS = Arrays.asList(
      "GENERAL_PAUSE", "GENERAL_SCREENSHOT", "GENERAL_ZOOM_IN", "GENERAL_ZOOM_OUT",
      "GENERAL_TOGGLE_UI", "GENERAL_EXPAND_TOOLTIP", "GENERAL_CODEX", "QUICK_SAVE", "QUICK_LOAD",
      "FAST_FORWARD", "GO_SLOW", "EXIT",
      "CORE_ABILITY_1", "CORE_ABILITY_2", "CORE_ABILITY_3", "CORE_ABILITY_4", "CORE_ABILITY_5",
      "CORE_ABILITY_6", "CORE_ABILITY_7", "CORE_ABILITY_8", "CORE_ABILITY_9", "CORE_ABILITY_10",
      "CORE_ABILITY_BAR_1", "CORE_ABILITY_BAR_2", "CORE_ABILITY_BAR_3", "CORE_ABILITY_BAR_4",
      "CORE_ABILITY_BAR_5",
      "CORE_CHARACTER", "CORE_FLEET", "CORE_REFIT", "CORE_CARGO", "CORE_INTEL", "CORE_OUTPOSTS",
      "CORE_OFFICERS", "CORE_MAP",
      "CORE_REPAIR", "CORE_CONFIRM", "CORE_RESET", "CORE_TAKEALL", "CORE_BUY_ACTION",
      "CORE_SELL_ACTION", "CORE_MOTHBALL_ACTION", "CORE_SCUTTLE_ACTION", "CORE_SUSPEND_ACTION",
      "CORE_LOGISTICS_ACTION", "CORE_MORE_INFO_ACTION", "CORE_REFIT_ACTION",
      "REFIT_ADD_MOD", "REFIT_MANAGE_VARIANTS", "REFIT_NEXT_UNSPENT", "REFIT_RESTORE",
      "REFIT_RUNSIM", "REFIT_SAVE", "REFIT_STRIP", "REFIT_UNDO", "REFIT_WEAPON_GROUPS",
      "SUBTAB_1", "SUBTAB_2", "SUBTAB_3", "SUBTAB_4",
      "TAB_0", "TAB_1", "TAB_2", "TAB_3", "TAB_4", "TAB_5", "TAB_6", "TAB_7", "TAB_8", "TAB_9",
      "TRADE_TAB_MARKET_INFO", "MV_SAVE", "MISSION_REFIT");

  public static final class Tenant {
    public final String modId;
    public final String label;
    public final String factoryClass;
    public final String attentionKey;
    public final Color color;

    Tenant(String modId, String label, String factoryClass, String attentionKey, Color color) {
      this.modId = modId;
      this.label = label;
      this.factoryClass = factoryClass;
      this.attentionKey = attentionKey;
      this.color = color;
    }
  }

  private static final Map<String, Tenant> TENANTS = new LinkedHashMap<String, Tenant>();

  public static synchronized void registerTenant(String modId, String label, String factoryClass,
      String attentionKey, int r, int g, int b) {
    if (modId == null || label == null || factoryClass == null) {
      return;
    }
    if (!TENANTS.containsKey(modId)) {
      TENANTS.put(modId, new Tenant(modId, label, factoryClass, attentionKey, new Color(r, g, b, 255)));
    }
  }

  public static synchronized List<Tenant> tenants() {
    return new ArrayList<Tenant>(TENANTS.values());
  }

  public static synchronized List<String> presentTenantIds() {
    return new ArrayList<String>(TENANTS.keySet());
  }

  private static LiminalConsolePanel liminalConsole;

  public static CustomPanelAPI buildMgmtConsole(float width, float height) {
    releaseMgmtConsole();
    liminalConsole = new LiminalConsolePanel(width, height);
    return liminalConsole.getPanel();
  }

  public static void releaseMgmtConsole() {
    if (liminalConsole != null) {
      LiminalConsolePanel.dropLive(liminalConsole);
      liminalConsole = null;
    }
  }

  public static CustomPanelAPI buildTenantConsole(String modId, float width, float height)
      throws Exception {
    if (MOD_ID.equals(modId)) {
      return buildMgmtConsole(width, height);
    }
    Tenant t;
    synchronized (LiminalMgmtInjector.class) {
      t = TENANTS.get(modId);
    }
    if (t == null) {
      throw new IllegalStateException("liminal: no registered tenant " + modId);
    }
    Class<?> cls = Class.forName(t.factoryClass);
    Object m = LiminalCoreReflect.methodNamed(cls, "buildMgmtConsole", 2);
    if (m == null) {
      throw new NoSuchMethodException("liminal: no buildMgmtConsole(w, h) on " + t.factoryClass);
    }
    Object panel = LiminalCoreReflect.invokeMethod(m, null,
        new Object[] { Float.valueOf(width), Float.valueOf(height) });
    return (CustomPanelAPI) panel;
  }

  public static void releaseTenantConsole(String modId) {
    try {
      if (MOD_ID.equals(modId)) {
        releaseMgmtConsole();
        return;
      }
      Tenant t;
      synchronized (LiminalMgmtInjector.class) {
        t = TENANTS.get(modId);
      }
      if (t == null) {
        return;
      }
      Object m = LiminalCoreReflect.methodNamed(Class.forName(t.factoryClass), "releaseMgmtConsole", 0);
      if (m != null) {
        LiminalCoreReflect.invokeMethod(m, null, new Object[0]);
      }
    } catch (Throwable ignore) {
    }
  }

  public static boolean attentionActive(String modId) {
    try {
      Tenant t;
      synchronized (LiminalMgmtInjector.class) {
        t = TENANTS.get(modId);
      }
      if (t == null || t.attentionKey == null || Global.getSector() == null) {
        return false;
      }
      return Boolean.TRUE.equals(Global.getSector().getMemoryWithoutUpdate().get(t.attentionKey));
    } catch (Throwable ignore) {
      return false;
    }
  }

  public static void noteTabSelected(String modId) {
    try {
      Tenant t;
      synchronized (LiminalMgmtInjector.class) {
        t = TENANTS.get(modId);
      }
      MemoryAPI mem = Global.getSector() == null ? null : Global.getSector().getMemoryWithoutUpdate();
      if (mem == null) {
        return;
      }
      if (t != null && t.attentionKey != null) {
        mem.set(t.attentionKey, Boolean.FALSE);
      }
      if (MOD_ID.equals(modId)) {
        mem.set(OPENED_KEY, Boolean.TRUE);
      }
    } catch (Throwable ignore) {
    }
  }

  private static LiminalMgmtInjector live;
  private static volatile boolean forceFailForTest;
  private static volatile boolean forceNoMatchForTest;
  private static volatile Throwable lastErrorForTest;
  private static volatile String queuedSelectModId;
  public static volatile int mgmtDispatchCount;
  public static volatile String shortcutNotice;
  public static volatile String attachNotice;

  public static boolean isAlive() {
    LiminalMgmtInjector l = live;
    if (l == null || l.dead) {
      return false;
    }
    return l.hosting || l.hostSeamAlive;
  }

  public static boolean isInjected() {
    LiminalMgmtInjector l = live;
    if (l == null || l.dead || !l.hosting) {
      return false;
    }
    BarSlot s = l.activeSlotQuiet();
    return s != null && s.bar != null && s.button != null;
  }

  public static boolean isSelected() {
    return LiminalMgmtScreen.isOpen() && MOD_ID.equals(LiminalMgmtScreen.selectedTenant());
  }

  public static boolean isHost() {
    LiminalMgmtInjector l = live;
    return l != null && !l.dead && l.hosting;
  }

  public static boolean isAttached() {
    LiminalMgmtInjector l = live;
    return l != null && !l.dead && (l.hosting || l.registeredWithHost);
  }

  public static void requestSelect() {
    requestTabSelect(MOD_ID);
  }

  public static void requestTabSelect(String modId) {
    LiminalMgmtInjector l = live;
    if (l != null && !l.hosting && handshakePresentQuiet()) {
      try {
        Object desc = Global.getSector().getMemoryWithoutUpdate().get(HANDSHAKE_KEY);
        Object seam = ((Map<?, ?>) desc).get(DK_SEAM);
        Object m = LiminalCoreReflect.methodNamed(Class.forName((String) seam), "requestTabSelect", 1);
        if (m != null) {
          LiminalCoreReflect.invokeMethod(m, null, new Object[] { modId });
          return;
        }
      } catch (Throwable ignore) {
      }
    }
    queuedSelectModId = modId;
  }

  public static String consumeQueuedSelectTab() {
    String id = queuedSelectModId;
    queuedSelectModId = null;
    return id;
  }

  public static void injectReflectionFailureForTest() {
    forceFailForTest = true;
  }

  public static void injectNoMatchForTest(boolean on) {
    forceNoMatchForTest = on;
  }

  public static String lastErrorForTest() {
    Throwable t = lastErrorForTest;
    if (t == null) {
      return "none";
    }
    StackTraceElement[] st = t.getStackTrace();
    return t + (st.length > 0 ? " @ " + st[0] : "");
  }

  public static float[] plateExtRect() {
    LiminalMgmtInjector l = live;
    BarSlot s = l == null ? null : l.activeSlotQuiet();
    if (s == null || s.plate == null || s.plate.getPosition() == null) {
      return null;
    }
    PositionAPI p = s.plate.getPosition();
    return new float[] { p.getX(), p.getY(), p.getWidth(), p.getHeight() };
  }

  public static float[] buttonRect() {
    LiminalMgmtInjector l = live;
    BarSlot s = l == null ? null : l.activeSlotQuiet();
    if (s == null || s.button == null || s.button.getPosition() == null) {
      return null;
    }
    PositionAPI p = s.button.getPosition();
    return new float[] { p.getX(), p.getY(), p.getWidth(), p.getHeight() };
  }

  public static boolean isButtonFlashing() {
    LiminalMgmtInjector l = live;
    BarSlot s = l == null ? null : l.activeSlotQuiet();
    return s != null && s.flashOn;
  }

  public static boolean openManagementScreen() {
    mgmtDispatchCount++;
    LiminalMgmtInjector l = live;
    if (l != null && !l.hosting && handshakePresentQuiet()) {
      try {
        Object desc = Global.getSector().getMemoryWithoutUpdate().get(HANDSHAKE_KEY);
        Object seam = ((Map<?, ?>) desc).get(DK_SEAM);
        Object m = LiminalCoreReflect.methodNamed(Class.forName((String) seam), "openManagementScreen", 0);
        if (m != null) {
          return Boolean.TRUE.equals(LiminalCoreReflect.invokeMethod(m, null, new Object[0]));
        }
      } catch (Throwable ignore) {
        return false;
      }
      return false;
    }
    return openOwnScreen();
  }

  private static boolean openOwnScreen() {
    LiminalMgmtInjector l = live;
    if (l == null || l.dead) {
      return false;
    }
    try {
      CampaignUIAPI ui = Global.getSector() == null ? null : Global.getSector().getCampaignUI();
      if (ui == null) {
        return false;
      }
      boolean docked = ui.getCurrentInteractionDialog() != null;
      UIPanelAPI activeL = activeCore(ui);
      if (activeL == null) {
        return false;
      }
      BarSlot slot = docked ? l.dockedSlot : l.campaignSlot;
      if (slot.button == null || slot.hostL != activeL) {
        return false;
      }
      Object shell = LiminalMgmtScreen.liveShell();
      if (shell != null && LiminalCoreReflect.getCurrentTab(activeL) == shell) {
        return true;
      }
      if (ui.isShowingMenu()) {
        return false;
      }
      if (!docked && ui.isShowingDialog() && ui.getCurrentCoreTab() == null
          && LiminalMgmtScreen.liveShell() == null) {
        return false;
      }
      if (tenants().isEmpty()) {
        return false;
      }
      float w = ((Number) LiminalCoreReflect.invokeOneArg(
          "computePanelWidth", activeL, Boolean.TRUE)).floatValue();
      float h = ((Number) LiminalCoreReflect.invokeOneArg(
          "computePanelHeight", activeL, Boolean.TRUE)).floatValue();
      float capW = 1600f;
      float capH = 980f;
      try {
        capW = (float) MethodHandles.lookup()
            .findStaticGetter(activeL.getClass(), "MAX_INTEL_WIDTH", float.class).invoke();
        capH = (float) MethodHandles.lookup()
            .findStaticGetter(activeL.getClass(), "MAX_INTEL_HEIGHT", float.class).invoke();
      } catch (Throwable ignore) {
        capW = 1600f;
        capH = 980f;
      }
      w = Math.min(w, capW);
      h = Math.min(h, capH);
      Object newShell = shellCtor(activeL.getClass()).invoke(w, h);
      ((UIPanelAPI) newShell).addComponent(LiminalMgmtScreen.stageForShell(newShell, w, h))
          .inTL(0f, 0f);
      Object m = LiminalCoreReflect.methodNamed(activeL.getClass(), "showPanelAsDialog", 4);
      if (m == null) {
        throw new NoSuchMethodException("liminal: no showPanelAsDialog(button, panel, bl, bl) on"
            + " the active core UI");
      }
      LiminalCoreReflect.invokeMethod(m, activeL,
          new Object[] { slot.button, newShell, Boolean.TRUE, Boolean.FALSE });
      if (LiminalCoreReflect.getCurrentTab(activeL) == newShell) {
        LiminalMgmtScreen.promoteStaged(newShell, activeL);
        return true;
      }
      LiminalMgmtScreen.discardStaged(newShell);
      return false;
    } catch (Throwable t) {
      l.fail(t);
      return false;
    }
  }

  private static MethodHandle shellCtor(Class<?> coreClass) throws Exception {
    MethodHandle h = shellCtorCache;
    if (h == null) {
      Object show = LiminalCoreReflect.onlyMethodNamed(LiminalMgmtShell.RULE_PANEL, coreClass,
          "showPanelAsDialog", 4);
      Class<?> panel = LiminalCoreReflect.paramTypes(show)[1];
      if (!UIPanelAPI.class.isAssignableFrom(panel)) {
        throw new ClassCastException(LiminalMgmtShell.ruleFailed(LiminalMgmtShell.RULE_PANEL,
            panel.getName() + " is not a UIPanelAPI"));
      }
      h = LiminalMgmtShell.define(MethodHandles.lookup(), SHELL_NAME, coreClass, panel,
          LiminalMgmtScreen.class, "onTabClosed");
      shellCtorCache = h;
      noteLookup(LiminalMgmtShell.RULE_PANEL, panel.getName());
      noteLookup(LiminalMgmtShell.RULE_TAB,
          h.type().returnType().getInterfaces()[0].getName());
    }
    return h;
  }

  private static synchronized void noteLookup(String rule, String found) {
    LOOKUPS.put(rule, found);
  }

  public static synchronized List<String> lookupReport() {
    List<String> lines = new ArrayList<String>();
    for (Map.Entry<String, String> e : LOOKUPS.entrySet()) {
      lines.add(e.getKey() + ": " + e.getValue());
    }
    return lines;
  }

  public static List<String> lookupSelfCheck(Class<?> coreClass) throws Throwable {
    MethodHandle ctor = shellCtor(coreClass);
    Object shell = ctor.invoke(64f, 64f);
    Class<?> tab = ctor.type().returnType().getInterfaces()[0];
    MethodHandles.publicLookup().findVirtual(tab, LiminalMgmtShell.TAB_METHOD,
        MethodType.methodType(void.class)).invoke(shell);
    Object buttons = LiminalCoreReflect.onlyMethodNamed(RULE_BAR, coreClass, "getButtons", 0);
    Class<?> bar = LiminalCoreReflect.returnType(buttons);
    Object add = LiminalCoreReflect.onlyMethodNamed(RULE_LISTENER, bar, "addButton", 4);
    Class<?> listener = LiminalCoreReflect.paramTypes(add)[3];
    try {
      barListener(listener);
    } catch (Throwable t) {
      throw new IllegalStateException(LiminalMgmtShell.ruleFailed(RULE_LISTENER,
          listener.getName() + " cannot be implemented as the button's listener (" + t + ")"));
    }
    noteLookup(RULE_LISTENER, listener.getName());
    resolveKeymapOn(bar);
    keymapResolved = true;
    int read = 0;
    for (String name : MGMT_SCOPE_BINDINGS) {
      if (readKeySlots(name) != null) {
        read++;
      }
    }
    if (keySlotClass == null) {
      throw new IllegalStateException(LiminalMgmtShell.ruleFailed(RULE_SLOT, "none of the "
          + MGMT_SCOPE_BINDINGS.size() + " scoped controls holds a key slot to read"));
    }
    noteLookup("controls read", read + " of " + MGMT_SCOPE_BINDINGS.size());
    return lookupReport();
  }

  private static Object barListener(Class<?> listenerType) throws Throwable {
    return LiminalCoreReflect.samProxy(listenerType, MethodHandles.lookup().findStatic(
        LiminalMgmtInjector.class, "onBarButtonClick",
        MethodType.methodType(void.class, Object.class, Object.class)));
  }

  private static void logLookupsOnce() {
    if (lookupsLogged) {
      return;
    }
    lookupsLogged = true;
    StringBuilder found = new StringBuilder();
    for (String line : lookupReport()) {
      found.append(found.length() == 0 ? "" : "; ").append(line);
    }
    Global.getLogger(LiminalMgmtInjector.class).info("Liminal: the Manage button is on the"
        + " bar. Found on this game build: " + found + ".");
  }

  public static boolean dismissCore(Object coreL) {
    if (coreL == null) {
      return false;
    }
    try {
      Object m = LiminalCoreReflect.methodNamed(coreL.getClass(), "dismiss", 1);
      if (m == null) {
        return false;
      }
      LiminalCoreReflect.invokeMethod(m, coreL, new Object[] { Integer.valueOf(0) });
      return true;
    } catch (Throwable ignore) {
      return false;
    }
  }

  public static void noteScreenOpen(boolean open) {
    try {
      if (Global.getSector() == null) {
        return;
      }
      MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
      if (open) {
        mem.set(OPEN_KEY, Boolean.TRUE);
      } else {
        mem.unset(OPEN_KEY);
      }
    } catch (Throwable ignore) {
    }
  }

  public static void onBarButtonClick(Object button, Object event) {
    try {
      LiminalMgmtInjector l = live;
      if (l != null) {
        if (l.campaignSlot.button != null) {
          l.campaignSlot.button.setChecked(false);
        }
        if (l.dockedSlot.button != null) {
          l.dockedSlot.button.setChecked(false);
        }
        if (!l.dead && l.hosting) {
          UIPanelAPI activeL = l.resolveActiveLQuiet();
          Object shell = LiminalMgmtScreen.liveShell();
          if (activeL != null && shell != null
              && LiminalCoreReflect.getCurrentTab(activeL) == shell) {
            mgmtDispatchCount++;
            dismissCore(activeL);
            return;
          }
        }
      }
      openManagementScreen();
    } catch (Throwable ignore) {
    }
  }

  public static void resetStaticsForLoad() {
    LiminalMgmtInjector l = live;
    if (l != null) {
      hideSlot(l.campaignSlot);
      hideSlot(l.dockedSlot);
    }
    synchronized (LiminalMgmtInjector.class) {
      TENANTS.clear();
    }
    releaseMgmtConsole();
    queuedSelectModId = null;
    mgmtDispatchCount = 0;
    shortcutNotice = null;
    attachNotice = null;
    live = null;
    noteScreenOpen(false);
    try {
      if (Global.getSector() != null) {
        Global.getSector().getMemoryWithoutUpdate().unset(NOTICE_KEY);
      }
    } catch (Throwable ignore) {
    }
  }

  private static final class BarSlot {
    UIPanelAPI hostL;
    UIPanelAPI bar;
    ButtonAPI button;
    CustomPanelAPI plate;
    boolean proxyFailed;
    boolean flashOn;
    boolean shortcutOn;
    float faceX;
  }

  private final IntervalUtil interval = new IntervalUtil(0.15f, 0.25f);
  private boolean dead;

  private final BarSlot campaignSlot = new BarSlot();
  private final BarSlot dockedSlot = new BarSlot();
  private boolean hosting;
  private boolean registeredWithHost;
  private boolean hostSeamAlive;
  private int locateFails;
  private int shortcutKey = -1;
  private boolean menuWasShowing;

  public LiminalMgmtInjector() {
    live = this;
    queuedSelectModId = null;
    if (Global.getSector() != null) {
      Global.getSector().getMemoryWithoutUpdate().unset(SELECT_TAB_KEY);
      Global.getSector().getMemoryWithoutUpdate().unset(HANDSHAKE_KEY);
      Global.getSector().getMemoryWithoutUpdate().unset(OPEN_KEY);
      Global.getSector().getMemoryWithoutUpdate().unset(NOTICE_KEY);
    }
  }

  @Override
  public boolean isDone() {
    return false;
  }

  @Override
  public boolean runWhilePaused() {
    return true;
  }

  @Override
  public void advance(float amount) {
    if (dead) {
      return;
    }
    pollDegradedButton(campaignSlot);
    pollDegradedButton(dockedSlot);
    if (dead) {
      return;
    }
    interval.advance(amount);
    if (!interval.intervalElapsed()) {
      return;
    }
    try {
      tick();
    } catch (Throwable t) {
      fail(t);
    }
  }

  private void pollDegradedButton(BarSlot slot) {
    if (!slot.proxyFailed || slot.button == null) {
      return;
    }
    try {
      if (slot.button.isChecked()) {
        slot.button.setChecked(false);
        openManagementScreen();
      }
    } catch (Throwable t) {
      fail(t);
    }
  }

  private void tick() throws Exception {
    if (forceFailForTest) {
      forceFailForTest = false;
      throw new RuntimeException("liminal: injected management-bar reflection failure (test hook)");
    }
    if (LiminalityIntel.getIntel() == null) {
      locateFails = 0;
      return;
    }
    maintainOwnAttentionKey();

    MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
    Object desc = mem.get(HANDSHAKE_KEY);
    if (desc instanceof Map && !MOD_ID.equals(((Map<?, ?>) desc).get(DK_HOST))) {
      arriveTick((Map<?, ?>) desc);
      return;
    }
    hostTick(mem);
  }

  private void hostTick(MemoryAPI mem) throws Exception {
    CampaignUIAPI ui = Global.getSector().getCampaignUI();
    if (ui != null) {
      boolean menu = ui.isShowingMenu();
      boolean dialog = ui.isShowingDialog();
      boolean due = LiminalMgmtKey.recheckDue(menuWasShowing, menu, dialog);
      menuWasShowing = LiminalMgmtKey.menuSeen(menuWasShowing, menu, dialog);
      if (due) {
        recheckShortcut();
      }
    }
    boolean docked = ui != null && ui.getCurrentInteractionDialog() != null;
    UIPanelAPI activeL = ui == null ? null : activeCore(ui);
    if (activeL == null) {
      locateFails = 0;
      return;
    }
    registerTenant(MOD_ID, liminal.intel.LiminalMessageMath.TAB_LABEL, SEAM_CLASS, ATTENTION_KEY,
        Misc.getBasePlayerColor().getRed(), Misc.getBasePlayerColor().getGreen(),
        Misc.getBasePlayerColor().getBlue());

    BarSlot slot = docked ? dockedSlot : campaignSlot;
    if (slot.hostL != null && slot.hostL != activeL) {
      clearSlot(slot);
    }
    boolean present = slot.bar != null && slot.button != null && slot.hostL == activeL
        && slot.button.getParent() == slot.bar;
    if (!present) {
      clearSlot(slot);
      UIPanelAPI found = locateBar(activeL);
      if (found == null) {
        giveUpIfStuck();
        return;
      }
      inject(slot, activeL, found);
      if (slot.button == null) {
        giveUpIfStuck();
        return;
      }
      hosting = true;
      publishHandshake(mem);
    }
    locateFails = 0;

    if (mem.get(SELECT_TAB_KEY) != null) {
      mem.unset(SELECT_TAB_KEY);
      requestSelect();
    }
    updateButtonFlash(slot);
  }

  private void arriveTick(Map<?, ?> desc) {
    hosting = false;
    Object v = desc.get(DK_VERSION);
    int version = v instanceof Integer ? ((Integer) v).intValue() : -1;
    if (version > CONTRACT_VERSION) {
      if (!declinedLogged) {
        declinedLogged = true;
        Global.getLogger(LiminalMgmtInjector.class).warn("Liminal: the management-bar host"
            + " publishes contract version " + version + " (this mod knows " + CONTRACT_VERSION
            + "); not attaching. Liminal management is available from the crisis intel entry"
            + " on the Intel screen.");
      }
      hostSeamAlive = false;
      return;
    }
    Object seam = desc.get(DK_SEAM);
    if (!(seam instanceof String)) {
      hostSeamAlive = false;
      giveUpIfStuck();
      return;
    }
    try {
      Class<?> seamCls = Class.forName((String) seam);
      Object aliveM = LiminalCoreReflect.methodNamed(seamCls, "isAlive", 0);
      hostSeamAlive = aliveM != null
          && Boolean.TRUE.equals(LiminalCoreReflect.invokeMethod(aliveM, null, new Object[0]));
      if (!registeredWithHost) {
        Object regM = LiminalCoreReflect.methodNamed(seamCls, "registerTenant", 7);
        if (regM == null) {
          throw new NoSuchMethodException("liminal: no registerTenant(..) on " + seam);
        }
        LiminalCoreReflect.invokeMethod(regM, null, new Object[] { MOD_ID,
            liminal.intel.LiminalMessageMath.TAB_LABEL, SEAM_CLASS,
            ATTENTION_KEY, Integer.valueOf(Misc.getBasePlayerColor().getRed()),
            Integer.valueOf(Misc.getBasePlayerColor().getGreen()),
            Integer.valueOf(Misc.getBasePlayerColor().getBlue()) });
        registeredWithHost = true;
      }
      MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
      if (mem.get(SELECT_TAB_KEY) != null) {
        mem.unset(SELECT_TAB_KEY);
        requestSelect();
      }
      locateFails = 0;
    } catch (Throwable t) {
      hostSeamAlive = false;
      giveUpIfStuck();
    }
  }

  private transient boolean declinedLogged;

  private static UIPanelAPI activeCore(CampaignUIAPI ui) throws Exception {
    UIPanelAPI core = (UIPanelAPI) ui.getCore2();
    if (core == null || ui.getCurrentInteractionDialog() == null) {
      return core;
    }
    return core == LiminalCoreReflect.getCore() ? null : core;
  }

  private UIPanelAPI resolveActiveLQuiet() {
    try {
      CampaignUIAPI ui = Global.getSector() == null ? null : Global.getSector().getCampaignUI();
      return ui == null ? null : activeCore(ui);
    } catch (Throwable t) {
      return null;
    }
  }

  private boolean activeContextLive() {
    return resolveActiveLQuiet() != null;
  }

  private BarSlot activeSlotQuiet() {
    try {
      CampaignUIAPI ui = Global.getSector() == null ? null : Global.getSector().getCampaignUI();
      if (ui == null) {
        return null;
      }
      return ui.getCurrentInteractionDialog() != null ? dockedSlot : campaignSlot;
    } catch (Throwable t) {
      return null;
    }
  }

  private UIPanelAPI locateBar(UIPanelAPI root) throws Exception {
    if (forceNoMatchForTest) {
      return null;
    }
    if (root == null) {
      return null;
    }
    return searchBar(root, SEARCH_DEPTH);
  }

  private UIPanelAPI searchBar(UIPanelAPI panel, int depth) throws Exception {
    if (panel == null || depth < 0) {
      return null;
    }
    List<UIComponentAPI> children = panel.getChildrenCopy2();
    if (looksLikeBar(children)
        && LiminalCoreReflect.methodNamed(panel.getClass(), "addButton", 4) != null) {
      return panel;
    }
    for (UIComponentAPI child : children) {
      if (child instanceof UIPanelAPI) {
        UIPanelAPI found = searchBar((UIPanelAPI) child, depth - 1);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static boolean looksLikeBar(List<UIComponentAPI> children) {
    int n = 0;
    for (UIComponentAPI child : children) {
      if (child instanceof ButtonAPI && isRecipeButton(child.getPosition())) {
        n++;
      }
    }
    return n >= MIN_BAR_BUTTONS;
  }

  private static boolean isRecipeButton(PositionAPI pos) {
    if (pos == null) {
      return false;
    }
    float w = pos.getWidth();
    float h = pos.getHeight();
    return w >= BAR_BTN_W_MIN && w <= BAR_BTN_W_MAX && h >= BAR_BTN_H_MIN && h <= BAR_BTN_H_MAX;
  }

  private static float rightmostButtonRightLocal(UIPanelAPI bar) {
    PositionAPI barPos = bar.getPosition();
    float best = -1f;
    for (UIComponentAPI child : bar.getChildrenCopy2()) {
      PositionAPI pos = child.getPosition();
      if (child instanceof ButtonAPI && isRecipeButton(pos)) {
        best = Math.max(best, pos.getX() + pos.getWidth());
      }
    }
    if (best < 0f || barPos == null) {
      throw new IllegalStateException("liminal: the bar lost its buttons between match and inject");
    }
    return best - barPos.getX();
  }

  public static volatile int injectCount;

  private void inject(BarSlot slot, UIPanelAPI activeL, UIPanelAPI foundBar) throws Exception {
    injectCount++;
    shellCtor(activeL.getClass());
    float prevRight = rightmostButtonRightLocal(foundBar);

    slot.plate = Global.getSettings().createCustom(PLATE_W, PLATE_H, new PlatePlugin());
    foundBar.addComponent(slot.plate).inBL(prevRight, 0f);

    Object m = LiminalCoreReflect.onlyMethodNamed(RULE_LISTENER, foundBar.getClass(),
        "addButton", 4);
    if (shortcutKey < 0) {
      shortcutKey = readShortcutKey();
    }
    String keyName = shortcutKeyName();
    String keyOwner = keyName == null ? null : mgmtScopeBindingOn(keyName);
    boolean withShortcut = keyName != null && keyOwner == null;
    String label = LiminalMgmtKey.label(withShortcut ? keyName : null);
    Class<?>[] pts = LiminalCoreReflect.paramTypes(m);
    Object listener;
    try {
      listener = barListener(pts[3]);
      slot.proxyFailed = false;
      noteLookup(RULE_LISTENER, pts[3].getName());
    } catch (Throwable t) {
      listener = null;
      slot.proxyFailed = true;
    }
    LiminalCoreReflect.invokeMethod(m, foundBar,
        new Object[] { label, BUTTON_TAB_ID, null, listener });
    Object btn = LiminalCoreReflect.invokeOneArg("getButton", foundBar, BUTTON_TAB_ID);
    if (!(btn instanceof ButtonAPI)) {
      throw new IllegalStateException("liminal: the bar's addButton did not register the Manage"
          + " button under its tab id");
    }
    slot.button = (ButtonAPI) btn;
    slot.bar = foundBar;
    slot.hostL = activeL;
    slot.faceX = prevRight + BUTTON_PAD;
    slot.shortcutOn = withShortcut;

    if (keyOwner != null) {
      logKeyCollision(keyName, keyOwner);
    } else if (withShortcut) {
      slot.button.setShortcut(shortcutKey, false);
      slot.button.setText(LiminalMgmtKey.labelMarkup(keyName));
    }

    if (slot.button.getPosition() != null) {
      slot.button.getPosition().inBL(slot.faceX, 4f);
    }
    logLookupsOnce();
  }

  private void recheckShortcut() {
    String keyName = shortcutKey < 0 ? null : shortcutKeyName();
    if (keyName == null) {
      return;
    }
    String keyOwner = mgmtScopeBindingOn(keyName);
    boolean on = keyOwner == null;
    boolean changed = applyShortcut(campaignSlot, keyName, on);
    changed |= applyShortcut(dockedSlot, keyName, on);
    if (changed && keyOwner != null) {
      logKeyCollision(keyName, keyOwner);
    }
  }

  private boolean applyShortcut(BarSlot slot, String keyName, boolean on) {
    if (slot.button == null || slot.shortcutOn == on) {
      return false;
    }
    if (on) {
      String label = LiminalMgmtKey.label(keyName);
      slot.button.setText(label);
      slot.button.setShortcut(shortcutKey, false);
      slot.button.setText(LiminalMgmtKey.labelMarkup(keyName));
    } else {
      slot.button.setShortcut(-1, false);
      slot.button.setText(LiminalMgmtKey.labelMarkup(null));
    }
    slot.shortcutOn = on;
    if (slot.button.getPosition() != null) {
      slot.button.getPosition().inBL(slot.faceX, 4f);
    }
    return true;
  }

  private static int readShortcutKey() {
    String[][] settings = LiminalMgmtKey.SETTINGS;
    Integer[] values = new Integer[settings.length];
    for (int i = 0; i < settings.length; i++) {
      values[i] = readKeySetting(settings[i][0], settings[i][1]);
    }
    return LiminalMgmtKey.resolve(values);
  }

  private static Integer readKeySetting(String modId, String fieldId) {
    try {
      if (!Global.getSettings().getModManager().isModEnabled(modId)) {
        return null;
      }
      if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
        Integer v = lunalib.lunaSettings.LunaSettings.getInt(modId, fieldId);
        if (v != null) {
          return v;
        }
      }
      return Integer.valueOf(Global.getSettings().getInt(fieldId));
    } catch (Throwable t) {
      return null;
    }
  }

  private String shortcutKeyName() {
    return keyName(shortcutKey);
  }

  private static String keyName(int code) {
    String lwjglName;
    try {
      lwjglName = Keyboard.getKeyName(code);
    } catch (Throwable t) {
      lwjglName = null;
    }
    return LiminalMgmtKey.gameKeyName(code, lwjglName);
  }

  private static void logKeyCollision(String keyName, String keyOwner) {
    String why = " the player keymap binds " + keyName + " to " + keyOwner + ", a campaign-map or"
        + " core-screen control, so the Manage button has no keyboard shortcut.";
    Global.getLogger(LiminalMgmtInjector.class).info("Liminal:" + why);
    postShortcutNotice(keyName);
  }

  private static void postShortcutNotice(String keyName) {
    if (shortcutNotice != null) {
      return;
    }
    try {
      String text = String.format(
          Global.getSettings().getString("liminal_mgmt_bar", "shortcut_key_in_use"), keyName);
      Global.getSector().getCampaignUI().addMessage(text);
      shortcutNotice = text;
    } catch (Throwable ignore) {
    }
  }

  private static void postAttachNotice() {
    try {
      MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
      if (handshakePresent() || mem.get(NOTICE_KEY) != null) {
        return;
      }
      String text = Global.getSettings().getString("liminal_mgmt_bar", "button_not_added");
      Global.getSector().getCampaignUI().addMessage(text);
      mem.set(NOTICE_KEY, Boolean.TRUE);
      attachNotice = text;
    } catch (Throwable ignore) {
    }
  }

  public static String mgmtScopeBindingOn(String keyName) {
    if (keyName == null) {
      return null;
    }
    for (String name : MGMT_SCOPE_BINDINGS) {
      String[] slots = keySlots(name);
      if (slots != null && (keyName.equals(slots[0]) || keyName.equals(slots[1]))) {
        return name;
      }
    }
    return null;
  }

  private static String[] keySlots(String binding) {
    resolveKeymap();
    if (keymapPairOf != null) {
      try {
        return readKeySlots(binding);
      } catch (Throwable t) {
        keymapPairOf = null;
        logKeymapFallback(t);
      }
    }
    try {
      return new String[] { Global.getSettings().getControlStringForEnumName(binding), null };
    } catch (Throwable t) {
      return null;
    }
  }

  private static String[] readKeySlots(String binding) throws Throwable {
    Object control;
    try {
      control = enumConstant(keymapControl, binding);
    } catch (IllegalArgumentException absent) {
      return null;
    }
    Object pair = LiminalCoreReflect.invokeMethod(keymapPairOf, null, new Object[] { control });
    if (pair == null) {
      return new String[] { null, null };
    }
    return new String[] { slotName(keymapSlotOne.invoke(pair)),
        slotName(keymapSlotTwo.invoke(pair)) };
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static Object enumConstant(Class<?> enumClass, String name) {
    return Enum.valueOf((Class) enumClass, name);
  }

  private static String slotName(Object slot) throws Exception {
    if (slot == null) {
      return null;
    }
    List<Object> codes = LiminalCoreReflect.fieldValuesOfType(slot, int.class);
    if (codes.size() != 1) {
      throw new IllegalStateException(LiminalMgmtShell.ruleFailed(RULE_SLOT,
          slot.getClass().getName() + " has " + codes.size() + " int fields, and exactly one,"
              + " the key code, is needed"));
    }
    if (slot.getClass() != keySlotClass) {
      keySlotClass = slot.getClass();
      noteLookup(RULE_SLOT, keySlotClass.getName());
    }
    int code = ((Integer) codes.get(0)).intValue();
    String name = code < 1 ? null : keyName(code);
    if (name == null) {
      return null;
    }
    for (Object held : LiminalCoreReflect.fieldValuesOfType(slot, boolean.class)) {
      if (Boolean.TRUE.equals(held)) {
        return "Mod-" + name;
      }
    }
    return name;
  }

  private static void resolveKeymap() {
    if (keymapResolved) {
      return;
    }
    Object core;
    try {
      core = Global.getSector().getCampaignUI().getCore2();
    } catch (Throwable t) {
      core = null;
    }
    if (core == null) {
      return;
    }
    keymapResolved = true;
    try {
      Object bar = LiminalCoreReflect.invokeNoArg("getButtons", core);
      if (bar == null) {
        throw new IllegalStateException(LiminalMgmtShell.ruleFailed(RULE_BAR,
            "the core UI's getButtons() returned no bar"));
      }
      resolveKeymapOn(bar.getClass());
    } catch (Throwable t) {
      keymapPairOf = null;
      logKeymapFallback(t);
    }
  }

  private static void resolveKeymapOn(Class<?> barClass) throws Exception {
    Object add = LiminalCoreReflect.onlyMethodNamed(RULE_CONTROL, barClass, "addButton", 4);
    Class<?> control = LiminalCoreReflect.paramTypes(add)[2];
    if (!control.isEnum()) {
      throw new IllegalStateException(LiminalMgmtShell.ruleFailed(RULE_CONTROL,
          "the bar's shortcut parameter " + control.getName() + " is not an enum"));
    }
    Class<?> controls = control.getEnclosingClass();
    if (controls == null) {
      throw new IllegalStateException(LiminalMgmtShell.ruleFailed(RULE_KEYMAP,
          "the control enum " + control.getName() + " is not nested in a class"));
    }
    Class<?> pair = Class.forName(KEYMAP_PAIR_CLASS);
    Object pairOf = LiminalCoreReflect.methodBySignature(controls, pair, control);
    if (pairOf == null) {
      throw new NoSuchMethodException(LiminalMgmtShell.ruleFailed(RULE_KEYS, controls.getName()
          + " has no method, or several, that takes a control and returns a "
          + pair.getSimpleName() + ", and exactly one is needed"));
    }
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    keymapSlotOne = lookup.findGetter(pair, "one", Object.class);
    keymapSlotTwo = lookup.findGetter(pair, "two", Object.class);
    keymapControl = control;
    keymapPairOf = pairOf;
    noteLookup(RULE_CONTROL, control.getName());
    noteLookup(RULE_KEYMAP, controls.getName());
    noteLookup(RULE_KEYS, LiminalCoreReflect.methodName(pairOf));
  }

  private static void logKeymapFallback(Throwable t) {
    String why = " could not read the keymap's second keys (" + t + "), so the Manage shortcut"
        + " check reads one key per control.";
    Global.getLogger(LiminalMgmtInjector.class).info("Liminal:" + why);
  }

  private static final class PlatePlugin extends BaseCustomUIPanelPlugin {
    private PositionAPI pos;
    private SpriteAPI sprite;
    private boolean spriteUnusable;

    @Override
    public void positionChanged(PositionAPI position) {
      pos = position;
    }

    @Override
    public void render(float alphaMult) {
      if (pos == null || spriteUnusable) {
        return;
      }
      if (sprite == null) {
        try {
          sprite = Global.getSettings().getSprite("ui", "tripad_button_holder");
        } catch (Throwable t) {
          spriteUnusable = true;
          return;
        }
        if (sprite == null) {
          spriteUnusable = true;
          return;
        }
      }
      if (Math.round(sprite.getWidth()) != (int) PLATE_SRC_W
          || Math.round(sprite.getHeight()) != (int) PLATE_SRC_H) {
        return;
      }
      sprite.setAlphaMult(alphaMult);
      float x = pos.getX();
      float y = pos.getY();
      sprite.renderRegion(x - PLATE_SRC_PATTERN_X, y,
          PLATE_SRC_PATTERN_X / PLATE_SRC_W, 0f, PLATE_PERIOD_W / PLATE_SRC_W, 1f);
      sprite.renderRegion(x + PLATE_PERIOD_W - PLATE_SRC_EDGE_X, y,
          PLATE_SRC_EDGE_X / PLATE_SRC_W, 0f, PLATE_EDGE_W / PLATE_SRC_W, 1f);
    }
  }

  private void maintainOwnAttentionKey() {
    try {
      MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
      boolean opened = Boolean.TRUE.equals(mem.get(OPENED_KEY));
      Object key = mem.get(ATTENTION_KEY);
      if (key == null) {
        mem.set(ATTENTION_KEY, Boolean.valueOf(!opened));
      } else if (Boolean.FALSE.equals(key) && !opened) {
        mem.set(OPENED_KEY, Boolean.TRUE);
      } else if (Boolean.TRUE.equals(key) && opened) {
        mem.set(ATTENTION_KEY, Boolean.FALSE);
      }
    } catch (Throwable ignore) {
    }
  }

  private void updateButtonFlash(BarSlot slot) {
    if (slot.button == null) {
      return;
    }
    boolean want = false;
    for (Tenant t : tenants()) {
      if (t.attentionKey != null && attentionActive(t.modId)) {
        want = true;
        break;
      }
    }
    try {
      if (want && !slot.flashOn) {
        slot.button.flash(false, 0.25f, 0.25f);
        Object fader = LiminalCoreReflect.invokeNoArg("getFlashFader", slot.button);
        if (fader != null) {
          LiminalCoreReflect.invokeOneArg("setBounceUp", fader, Boolean.TRUE);
        }
        slot.flashOn = true;
      } else if (!want && slot.flashOn) {
        Object fader = LiminalCoreReflect.invokeNoArg("getFlashFader", slot.button);
        if (fader != null) {
          LiminalCoreReflect.invokeOneArg("setBounceUp", fader, Boolean.FALSE);
        }
        slot.flashOn = false;
      }
    } catch (Throwable ignore) {
      slot.flashOn = want;
    }
  }

  private void clearSlot(BarSlot slot) {
    slot.hostL = null;
    slot.bar = null;
    slot.button = null;
    slot.plate = null;
    slot.proxyFailed = false;
    slot.flashOn = false;
    slot.shortcutOn = false;
    slot.faceX = 0f;
  }

  private void giveUpIfStuck() {
    try {
      if (hostingRoleActive() ? !activeContextLive() : !handshakePresent()) {
        return;
      }
    } catch (Throwable t) {
      return;
    }
    if (++locateFails > MAX_LOCATE_FAILS) {
      String context;
      try {
        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        context = "tab " + ui.getCurrentCoreTab() + ", docked "
            + (ui.getCurrentInteractionDialog() != null) + ", role "
            + (hostingRoleActive() ? "host" : "arriver");
      } catch (Throwable t) {
        context = "context unavailable";
      }
      Global.getLogger(LiminalMgmtInjector.class).warn("Liminal: could not attach the shared"
          + " Manage button after " + MAX_LOCATE_FAILS + " attempts (" + context + "); giving"
          + " up for this session. Likely cause: another mod restructured the campaign bottom bar,"
          + " or a game update changed it. Liminal management is available from the crisis intel"
          + " entry on the Intel screen.");
      lastErrorForTest = new RuntimeException("liminal: management bar never attached after "
          + MAX_LOCATE_FAILS + " in-context intervals; self-disabling so the fallback shows");
      disableForSession();
    }
  }

  private boolean hostingRoleActive() {
    if (hosting) {
      return true;
    }
    return !handshakePresentQuiet();
  }

  private static boolean handshakePresent() {
    MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
    Object desc = mem.get(HANDSHAKE_KEY);
    return desc instanceof Map && !MOD_ID.equals(((Map<?, ?>) desc).get(DK_HOST));
  }

  private static boolean handshakePresentQuiet() {
    try {
      return handshakePresent();
    } catch (Throwable t) {
      return false;
    }
  }

  private void publishHandshake(MemoryAPI mem) {
    HashMap<String, Object> desc = new HashMap<String, Object>();
    desc.put(DK_HOST, MOD_ID);
    desc.put(DK_VERSION, Integer.valueOf(CONTRACT_VERSION));
    desc.put(DK_SEAM, SEAM_CLASS);
    mem.set(HANDSHAKE_KEY, desc);
  }

  private void fail(Throwable t) {
    lastErrorForTest = t;
    Global.getLogger(LiminalMgmtInjector.class).warn("Liminal: the management-bar injector"
        + " hit a reflection error (" + t + ") and disabled itself for the session. Likely cause:"
        + " a game version change. Liminal management is available from the crisis intel entry"
        + " on the Intel screen.");
    disableForSession();
  }

  private void disableForSession() {
    dead = true;
    postAttachNotice();
    hideSlot(campaignSlot);
    hideSlot(dockedSlot);
    clearSlot(campaignSlot);
    clearSlot(dockedSlot);
  }

  private static void hideSlot(BarSlot slot) {
    try {
      if (slot.button != null) {
        slot.button.setOpacity(0f);
        if (slot.button.getPosition() != null) {
          slot.button.getPosition().setSize(0f, slot.button.getPosition().getHeight());
        }
      }
    } catch (Throwable ignore) {
    }
    try {
      if (slot.plate != null) {
        slot.plate.setOpacity(0f);
      }
    } catch (Throwable ignore) {
    }
  }
}

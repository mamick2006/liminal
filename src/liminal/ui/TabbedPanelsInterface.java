package liminal.ui;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public interface TabbedPanelsInterface {
  public static String TAB_PREFIX = "tab_";
  public static String LOCKED_TAB = "<Locked>";
  public static float TOTAL_PADDING = 4f;

  abstract class Panel {
    protected final float heightFraction;

    protected CustomUIPanelPlugin plugin;

    public Panel(float heightFraction) {
      this.heightFraction = heightFraction;
    }

    public float getHeightFraction() {
      return heightFraction;
    }

    private float fixedHeight;

    public void setFixedHeight(float px) {
      this.fixedHeight = Math.max(0f, px);
    }

    public float getFixedHeight() {
      return fixedHeight;
    }

    private int tabCount;

    public void setTabCount(int count) {
      this.tabCount = Math.max(0, count);
    }

    public int getTabCount() {
      return tabCount;
    }

    public void setPlugin(CustomUIPanelPlugin plugin) {
      this.plugin = plugin;
    }

    public CustomUIPanelPlugin getPlugin() {
      return plugin;
    }

    public abstract void createPanel(CustomPanelAPI panel, float width, float height);

    public void saveScrollState() {
    }

    protected static Panel[] currentPanels = new Panel[0];

    public static void setCurrentPanels(Panel[] panels) {
      currentPanels = panels;
    }

    public static Panel[] getCurrentPanels() {
      return currentPanels;
    }
  }

  static class TabbedPanelsUtils {
    public static void createTopLevelPanels(CustomPanelAPI panel, float width, float height,
        TabbedPanelsInterface inter) {
      createTopLevelPanels(panel, width, height, inter, null);
    }

    public static void createTopLevelPanels(CustomPanelAPI panel, float width, float height,
        TabbedPanelsInterface inter, CustomUIPanelPlugin plugin) {
      CustomPanelAPI mainPanel = panel.createCustomPanel(width, height, plugin);
      float buttonHeight = height * 0.03f;

      TooltipMakerAPI buttonTooltip = mainPanel.createUIElement(width, buttonHeight, false);
      float buttonWidth = width / inter.getTabNames().size();
      float totalPadding = TOTAL_PADDING;
      for (int i = 0; i < inter.getTabNames().size(); i++) {
        String tabName = inter.getTabNames().get(i);
        ButtonAPI button = buttonTooltip.addButton(tabName, TAB_PREFIX + tabName, Misc.getBasePlayerColor(),
            Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.TOP,
            buttonWidth - totalPadding, buttonHeight - totalPadding, 0f);
        button.setEnabled(!tabName.equals(inter.activeTab()) && !tabName.equals(LOCKED_TAB));
        float xOffset = buttonWidth;
        if (i == 0) {
          xOffset = totalPadding / 2;
        }
        button.getPosition().setXAlignOffset(xOffset);
        float yOffset = buttonHeight - totalPadding;
        if (i == 0) {
          yOffset = -totalPadding / 2;
        }
        button.getPosition().setYAlignOffset(yOffset);
      }
      mainPanel.addUIElement(buttonTooltip).inTL(0, 0);

      float remainingHeight = height - buttonHeight;
      float yOffset = buttonHeight;

      List<Panel> panels = inter.getPanelsForTabs().get(inter.activeTab());
      float fixedSum = 0f;
      float fractionSum = 0f;
      for (Panel p : panels) {
        if (p.getFixedHeight() > 0f) {
          fixedSum += p.getFixedHeight();
        } else {
          fractionSum += p.getHeightFraction();
        }
      }
      for (Panel p : panels) {
        float panelHeight = remainingHeight * p.getHeightFraction();
        if (p.getFixedHeight() > 0f) {
          panelHeight = Math.min(p.getFixedHeight(), remainingHeight);
        } else if (fixedSum > 0f && fractionSum > 0f) {
          panelHeight = Math.max(0f, remainingHeight - fixedSum) * p.getHeightFraction() / fractionSum;
        }
        CustomPanelAPI panelComponent = mainPanel.createCustomPanel(width, panelHeight, plugin);
        p.setPlugin(plugin);
        p.setTabCount(inter.getTabNames().size());
        p.createPanel(panelComponent, width, panelHeight);
        mainPanel.addComponent(panelComponent).inTL(0, yOffset);
        yOffset += panelHeight;
      }
      panel.addComponent(mainPanel).inTL(0, 0);
    }

    public static boolean isTabButton(Object buttonId) {
      return buttonId instanceof String && ((String) buttonId).startsWith(TAB_PREFIX);
    }

    public static String getTabName(Object buttonId) {
      return ((String) buttonId).substring(TAB_PREFIX.length());
    }

  }

  public String activeTab();

  public List<String> getTabNames();

  public Map<String, List<Panel>> getPanelsForTabs();
}

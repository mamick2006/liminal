package liminal.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

import liminal.LGlobals;
import liminal.crossmod.SotfSeam;
import liminal.faction.Autofit;
import liminal.faction.LiminalCampaignPlugin;
import liminal.faction.LiminalFactionFleetEventListener;
import liminal.instructions.GachaOnlyInstructions;
import liminal.instructions.InstructionsManager;
import liminal.instructions.TemplateCapacityScript;
import liminal.listener.LiminalDialogListener;
import liminal.start.StartPlace;
import liminal.topbar.LiminalMgmtInjector;
import liminal.ui.LiminalMgmtScreen;
import liminal.ui.LiminalConsolePanel;
import liminal.LiminalSettings;
import liminal.LiminalStrings;

public class LiminalPlugin extends BaseModPlugin {
  @Override
  public void onApplicationLoad() {
    if (Global.getSettings().getModManager().isModEnabled("secretsofthefrontier")) {
      if (SotfSeam.exemptFromGuilt(LGlobals.liminalFactionId)) {
        Global.getLogger(this.getClass()).info("Liminal: added the Entity to Secrets of the Frontier's"
            + " guilt-free factions.");
      } else {
        Global.getLogger(this.getClass()).warn("Liminal: Secrets of the Frontier is enabled, but its"
            + " guilt-free faction list (" + SotfSeam.TRACKER + ".GUILTY_FACTIONS) was not found, so"
            + " saturation bombing an Entity world brings that mod's guilt, Sierra's reaction and the"
            + " Dustkeepers' hatred.");
      }
    }
  }

  @Override
  public void onNewGame() {
  }

  @Override
  public void beforeGameSave() {
    InstructionsManager.endResearchVisit();
  }

  @Override
  public void onGameLoad(boolean newGame) {
    Global.getLogger(this.getClass()).info("Loaded Liminal!");
    LiminalConsolePanel.resetStaticsForLoad();
    LiminalMgmtInjector.resetStaticsForLoad();
    LiminalMgmtScreen.resetStaticsForLoad();
    Autofit.resetStaticsForLoad();
    GachaOnlyInstructions.resetStaticsForLoad();
    InstructionsManager.reconcileOnLoad();
    InstructionsManager.refreshOnLoad();
    if (!Global.getSector().hasTransientScript(TemplateCapacityScript.class)) {
      TemplateCapacityScript recount = new TemplateCapacityScript();
      Global.getSector().addTransientScript(recount);
      Global.getSector().getListenerManager().addListener(recount, true);
    }
    Global.getSector().registerPlugin(new LiminalCampaignPlugin());
    LiminalFactionFleetEventListener listener = new LiminalFactionFleetEventListener();
    Global.getSector().addTransientListener(listener);
    Global.getSector().getListenerManager().addListener(listener, true);
    Global.getSector().getListenerManager().addListener(new LiminalDialogListener(), true);
    try {
      StartPlace.decide();
      if (!Global.getSector().getCharacterData().getMemoryWithoutUpdate()
          .getBoolean("$liminal_hullmods_enabled")) {
        StartPlace.tell(LiminalStrings.intel("msg_start_place"),
            Global.getSettings().getSpriteName("icons", "liminal_intel_research"), LGlobals.liminalityColor);
      }
    } catch (Throwable t) {
      Global.getLogger(this.getClass()).warn("Liminal: could not work out where the research starts in"
          + " this sector, so it starts at the Galatia Academy if the sector has one.", t);
    }
    boolean mgmtIntelOnly = LiminalSettings.getBoolean("liminal_mgmt_intel_only", false);
    if (!mgmtIntelOnly) {
      try {
        if (!Global.getSector().hasTransientScript(LiminalMgmtInjector.class)) {
          Global.getSector().addTransientScript(new LiminalMgmtInjector());
        }
      } catch (Throwable ignore) {
      }
    }
  }
}

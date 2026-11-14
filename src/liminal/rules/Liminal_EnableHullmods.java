package liminal.rules;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.comm.CommMessageAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import liminal.LGlobals;
import liminal.LiminalStrings;
import liminal.instructions.InstructionsManager;
import liminal.instructions.InstructionsMonthlyCost;
import liminal.intel.LiminalCrisisIntel;
import liminal.intel.LiminalMessageMath;
import liminal.intel.LiminalityIntel;

public class Liminal_EnableHullmods extends BaseCommandPlugin {

  @Override
  public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params,
      Map<String, MemoryAPI> memoryMap) {
    InstructionsManager instructionsManager = InstructionsManager.getInstance();
    String firstLoadoutId = instructionsManager.getLoadoutIds().iterator().next();
    Global.getSector().getCharacterData().addHullMod(firstLoadoutId);

    LiminalityIntel researchIntel = new LiminalityIntel();
    Global.getSector().getIntelManager().addIntel(researchIntel, true);

    LiminalCrisisIntel crisisIntel = new LiminalCrisisIntel();
    Global.getSector().getIntelManager().addIntel(crisisIntel, true);
    crisisIntel.postInit();
    MessageIntel intel = new MessageIntel("Liminality enabled", LGlobals.liminalityColor);
    intel.addLine(LiminalStrings.intel("msg_liminality_enabled"), Misc.getTextColor(),
        new String[] { "Liminality",
            LiminalMessageMath.detailsPlace(liminal.topbar.LiminalMgmtInjector.isAlive()) },
        LGlobals.liminalityColor, Misc.getPositiveHighlightColor());
    intel.setIcon(Global.getSettings().getSpriteName("icons", "liminal_intel_research"));
    intel.setSound(BaseIntelPlugin.getSoundStandardPosting());
    Global.getSector().getCampaignUI().addMessage(intel, CommMessageAPI.MessageClickAction.INTEL_TAB, null);
    if (!Global.getSector().getListenerManager().hasListenerOfClass(InstructionsMonthlyCost.class)) {
      Global.getSector().getListenerManager().addListener(new InstructionsMonthlyCost());
    }
    return true;
  }
}

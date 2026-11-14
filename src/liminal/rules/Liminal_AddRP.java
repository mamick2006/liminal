package liminal.rules;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import liminal.LGlobals;
import liminal.LiminalStrings;
import liminal.instructions.InstructionsManager;

public class Liminal_AddRP extends BaseCommandPlugin {

  @Override
  public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params,
      Map<String, MemoryAPI> memoryMap) {
    if (dialog == null)
      return false;
    InstructionsManager im = InstructionsManager.getInstance();
    int rp = params.get(0).getInt(memoryMap);
    im.addResearchPoints(rp);

    TextPanelAPI text = dialog.getTextPanel();
    text.setFontSmallInsignia();
    String str = "" + rp;
    text.addParagraph(String.format(LiminalStrings.dialog("dialog_rp_gained"), str),
        Misc.getPositiveHighlightColor());
    text.highlightInLastPara(LGlobals.liminalityColor, str);
    text.setFontInsignia();
    return true;
  }
}

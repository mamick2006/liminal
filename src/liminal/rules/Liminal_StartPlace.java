package liminal.rules;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import liminal.start.StartPlace;

public class Liminal_StartPlace extends BaseCommandPlugin {

  @Override
  public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params,
      Map<String, MemoryAPI> memoryMap) {
    String mode = params.get(0).getString(memoryMap);
    MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
    if ("here".equals(mode)) {
      return dialog != null && StartPlace.isHere(dialog.getInteractionTarget(), local);
    }
    if ("name".equals(mode)) {
      local.set("$liminal_place", StartPlace.name(), 0);
      local.set("$liminal_place_short", StartPlace.shortName(), 0);
      return true;
    }
    return false;
  }
}

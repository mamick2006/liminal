package liminal.rules;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
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
    if ("start".equals(mode)) {
      return dialog != null
          && StartPlace.offersStart(dialog.getInteractionTarget(), local);
    }
    if ("name".equals(mode)) {
      if (params.size() > 1 && "local".equals(params.get(1).getString(memoryMap)) && dialog != null) {
        SectorEntityToken target = dialog.getInteractionTarget();
        local.set("$liminal_place", StartPlace.nameOfDialog(target, local, false), 0);
        local.set("$liminal_place_short", StartPlace.nameOfDialog(target, local, true), 0);
        return true;
      }
      local.set("$liminal_place", StartPlace.name(), 0);
      local.set("$liminal_place_short", StartPlace.shortName(), 0);
      return true;
    }
    return false;
  }
}

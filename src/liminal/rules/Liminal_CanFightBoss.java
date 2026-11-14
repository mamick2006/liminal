package liminal.rules;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import liminal.intel.LiminalCrisisIntel;

public class Liminal_CanFightBoss extends BaseCommandPlugin {

  @Override
  public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params,
      Map<String, MemoryAPI> memoryMap) {
    LiminalCrisisIntel intel = LiminalCrisisIntel.getIntel();
    if (intel == null) {
      return false;
    }
    return intel.canFightBoss();
  }
}

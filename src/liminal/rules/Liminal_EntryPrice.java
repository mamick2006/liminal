package liminal.rules;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.MutableValue;

import liminal.LiminalStrings;
import liminal.LiminalSurvival;

public class Liminal_EntryPrice extends BaseCommandPlugin {

  public static final int BASE_PRICE = 30000;

  public static int price() {
    return LiminalSurvival.price(BASE_PRICE);
  }

  @Override
  public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params,
      Map<String, MemoryAPI> memoryMap) {
    String mode = params.get(0).getString(memoryMap);
    int price = price();
    String shown = Misc.getWithDGS(price);
    MutableValue credits = Global.getSector().getPlayerFleet().getCargo().getCredits();
    if ("short".equals(mode)) {
      return credits.get() < price;
    }
    if ("set".equals(mode)) {
      memoryMap.get(MemKeys.LOCAL).set("$liminal_entry_price", shown, 0);
      return true;
    }
    if (dialog == null) {
      return false;
    }
    if ("tooltip".equals(mode)) {
      String option = params.get(1).getString(memoryMap);
      dialog.getOptionPanel().setTooltip(option,
          String.format(LiminalStrings.dialog("entry_price_tooltip"), shown));
      dialog.getOptionPanel().setTooltipHighlights(option, shown);
      return true;
    }
    if ("pay".equals(mode)) {
      credits.subtract(price);
      AddRemoveCommodity.addCreditsLossText(price, dialog.getTextPanel());
      return true;
    }
    return false;
  }
}

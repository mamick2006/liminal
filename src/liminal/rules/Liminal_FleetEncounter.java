package liminal.rules;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;

import liminal.LGlobals;
import liminal.LiminalStrings;
import liminal.faction.Autofit;
import liminal.faction.LUtils;

public class Liminal_FleetEncounter extends BaseCommandPlugin {

  @Override
  public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params,
      final Map<String, MemoryAPI> memoryMap) {
    if (dialog == null) {
      return false;
    }
    if (dialog.getInteractionTarget() == null) {
      return false;
    }
    if (dialog.getInteractionTarget() instanceof CampaignFleetAPI) {
      CampaignFleetAPI fleet = (CampaignFleetAPI) dialog.getInteractionTarget();
      if (fleet.isPlayerFleet()) {
        return true;
      }
      if (fleet.getFaction() == null) {
        return true;
      }
      if (fleet.getFaction().getId().equals(LGlobals.liminalFactionId)) {
        return true;
      }
      if (fleet.getFaction().isPlayerFaction()) {
        return true;
      }
      TextPanelAPI text = dialog.getTextPanel();

      boolean hasLiminality = false;
      for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
        for (String hullmod : m.getVariant().getHullMods()) {
          if (hullmod.contains("liminal")) {
            text.addParagraph(m.getShipName() + " is influenced by the Liminality");
            text.highlightInLastPara(LGlobals.liminalityColor, "influenced by the Liminality");
            hasLiminality = true;
          }
        }
      }

      if (hasLiminality) {
        return true;
      }

      float chance = LUtils.getAllShipHullmodChance();
      String hullmod = LUtils.getAllShipHullmod();
      for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
        if (m.isFighterWing())
          continue;
        if (Math.random() < chance) {
          ShipVariantAPI variant = m.getVariant();
          variant.addPermaMod(hullmod);
          int numBays = Global.getSettings().computeNumFighterBays(variant);
          for (int i = variant.getWings().size(); i < numBays; i++) {
            variant.setWingId(i, Autofit.getRandomWing().getId());
          }
          text.addParagraph(m.getShipName() + LiminalStrings.dialog("fleet_influenced_suffix"));
          text.highlightInLastPara(LGlobals.liminalityColor, "influenced by the Liminality");
        }
      }

    }

    return true;
  }
}

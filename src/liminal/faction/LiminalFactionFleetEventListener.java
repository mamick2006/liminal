package liminal.faction;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.listeners.CoreAutoresolveListener;
import com.fs.starfarer.api.campaign.listeners.ShipRecoveryListener;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.BattleAutoresolverPluginImpl.FleetAutoresolveData;
import com.fs.starfarer.api.util.Misc;

import liminal.LGlobals;
import liminal.intel.LiminalCrisisIntel;

public class LiminalFactionFleetEventListener extends BaseCampaignEventListener
    implements ShipRecoveryListener, CoreAutoresolveListener {

  public LiminalFactionFleetEventListener() {
    super(false);
  }

  @Override
  public void reportBattleOccurred(CampaignFleetAPI fleet, BattleAPI battle) {

    if (!battle.isPlayerInvolved())
      return;

    float totalMultDestroyed = 0;
    int totalShipsDestroyed = 0;
    for (CampaignFleetAPI otherFleet : battle.getNonPlayerSideSnapshot()) {
      if (!LGlobals.liminalFactionId.equals(otherFleet.getFaction().getId()))
        continue;

      for (FleetMemberAPI loss : Misc.getSnapshotMembersLost(otherFleet)) {
        totalMultDestroyed += Misc.getSizeNum(loss.getHullSpec().getHullSize());
        totalShipsDestroyed++;
      }
    }

    LiminalCrisisIntel intel = LiminalCrisisIntel.getIntel();
    if (intel != null && totalMultDestroyed > 0) {
      intel.updateDestroyedProgress(totalMultDestroyed, totalShipsDestroyed);
    }
  }

  @Override
  public void reportBattleFinished(CampaignFleetAPI fleet, BattleAPI battle) {
    Autofit.cleanup();
  }

  @Override
  public void reportShipsRecovered(List<FleetMemberAPI> ships, InteractionDialogAPI dialog) {
    Autofit.cleanup();

    for (FleetMemberAPI m : ships) {
      ShipVariantAPI v = m.getVariant();

      boolean removed = false;
      List<String> toRemove = new ArrayList<>();
      for (String hullmod : v.getHullMods()) {
        if (hullmod.contains("liminal_connection")) {
          toRemove.add(hullmod);
          removed = true;
        }
      }
      for (String hullmod : toRemove) {
        v.removePermaMod(hullmod);
      }
      if (removed) {
        v.addPermaMod("liminal_connection_25");
      }
    }

  }

  @Override
  public void modifyDataForFleet(FleetAutoresolveData data) {
    if (data.fleet.getFaction().getId().equals(LGlobals.liminalFactionId)) {
      data.fightingStrength *= 4f;
    }
  }

}

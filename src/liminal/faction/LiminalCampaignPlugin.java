package liminal.faction;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

public class LiminalCampaignPlugin extends BaseCampaignPlugin {
  @Override
  public String getId() {
    return "liminal.faction.LiminalCampaignPlugin";
  }

  @Override
  public boolean isTransient() {
    return true;
  }

  @Override
  public PluginPick<BattleCreationPlugin> pickBattleCreationPlugin(SectorEntityToken interactionTarget) {
    boolean hasLiminalShip = false;
    for (FleetMemberAPI member : ((CampaignFleetAPI) interactionTarget).getFleetData()
        .getMembersListCopy()) {
      if (Autofit.isLiminalShipOrBoss(member.getVariant())) {
        hasLiminalShip = true;
        break;
      }
    }
    if (hasLiminalShip) {
      return new PluginPick<BattleCreationPlugin>(new AutofitPlugin(), PickPriority.HIGHEST);
    }
    return super.pickBattleCreationPlugin(interactionTarget);
  }
}

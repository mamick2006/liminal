package liminal.crossmod;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.util.Misc;

import liminal.LiminalStrings;

public class LinkageItem extends BaseSpecialItemPlugin {

  @Override
  public boolean hasRightClickAction() {
    return CqSeam.isKnownFurnishing(getId());
  }

  @Override
  public boolean shouldRemoveOnRightClickAction() {
    return CqSeam.isAvailable() && CqSeam.isKnownFurnishing(getId()) && !CqSeam.isOwned(getId());
  }

  @Override
  public void performRightClickAction() {
    if (CqSeam.grant(getId())) {
      return;
    }
    if (!CqSeam.isInstalled()) {
      say(LiminalStrings.mgmt("linkage_needs_quarters"), true);
      return;
    }
    say(LiminalStrings.mgmt("linkage_already_collected"), false);
  }

  private void say(String text, boolean negative) {
    try {
      Global.getSector().getCampaignUI().getMessageDisplay().addMessage(text,
          negative ? Misc.getNegativeHighlightColor() : Misc.getGrayColor());
    } catch (Throwable t) {
    }
  }
}

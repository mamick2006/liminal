package liminal.scripts;

import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;

import com.fs.starfarer.api.Global;

import liminal.ui.FightBoss;

public class LiminalFightBoss implements BaseCommand {
  @Override
  public CommandResult runCommand(@NotNull String args, @NotNull BaseCommand.CommandContext context) {
    Global.getSector().getCampaignUI().showInteractionDialog(new FightBoss(),
        null);
    return CommandResult.SUCCESS;
  }
}

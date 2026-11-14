package liminal.scripts;

import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import liminal.LGlobals;
import liminal.LiminalStrings;

public class LiminalOverrideRestrictions implements BaseCommand {
  @Override
  public CommandResult runCommand(@NotNull String args, @NotNull BaseCommand.CommandContext context) {
    LGlobals.ignoreRestrictions = !LGlobals.ignoreRestrictions;
    Console.showMessage(LiminalStrings.console("console_override_restrictions")
        + (LGlobals.ignoreRestrictions ? "enabled." : "disabled."));
    return CommandResult.SUCCESS;
  }
}

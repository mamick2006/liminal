package liminal.scripts;

import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import liminal.instructions.InstructionsManager;

public class LiminalAddRP implements BaseCommand {
  @Override
  public CommandResult runCommand(@NotNull String args, @NotNull BaseCommand.CommandContext context) {
    String[] argList = args.split("\\s+");

    if (argList.length != 1) {
      return badSyntax();
    }
    int amount;

    try {
      amount = Integer.parseInt(argList[0]);
      InstructionsManager.getInstance().addResearchPoints(amount);
      Console.showMessage("Added " + amount + " research points");
    } catch (NumberFormatException e) {
      return badSyntax();
    }
    return CommandResult.SUCCESS;
  }

  public CommandResult badSyntax() {
    Console.showMessage("AddResearchPoints amount");
    return CommandResult.BAD_SYNTAX;
  }
}

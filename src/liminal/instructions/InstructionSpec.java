package liminal.instructions;

public class InstructionSpec {

  public final String id;
  public final Instruction.Group group;
  public final int opCost;
  public final int rpMult;
  public final int tier;
  public final String displayName;
  public final String description;
  public final String effect;
  public final boolean template;
  public final String rowSource;

  public InstructionSpec(String id, Instruction.Group group, int opCost, int rpMult, int tier,
      String displayName, String description, String effect, boolean template,
      String rowSource) {
    this.id = id;
    this.group = group;
    this.opCost = opCost;
    this.rpMult = rpMult;
    this.tier = tier;
    this.displayName = displayName;
    this.description = description;
    this.effect = effect;
    this.template = template;
    this.rowSource = rowSource;
  }
}

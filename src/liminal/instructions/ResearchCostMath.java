package liminal.instructions;

public final class ResearchCostMath {

  private ResearchCostMath() {
  }

  public static int costFromLevel(int level, int rpMult) {
    if (level < 0 || rpMult <= 0) {
      return 0;
    }
    return (int) (Math.log(1.7 + level) / Math.log(1.7) * rpMult);
  }
}

package liminal.rules;

public enum StagingGroundDefense {
  SHUTTLE_SWARM(true),
  WARSHIP_SWARM(true),
  ZIGGURATS(false),
  COMBINED(false);

  private final boolean swarm;

  StagingGroundDefense(boolean swarm) {
    this.swarm = swarm;
  }

  public boolean isSwarm() {
    return swarm;
  }

  public static StagingGroundDefense forRemaining(int remaining) {
    switch (remaining) {
      case 4:
        return SHUTTLE_SWARM;
      case 3:
        return WARSHIP_SWARM;
      case 2:
        return ZIGGURATS;
      case 1:
        return COMBINED;
      default:
        return null;
    }
  }

  public static boolean isSwarm(int remaining) {
    StagingGroundDefense defense = forRemaining(remaining);
    return defense != null && defense.swarm;
  }
}

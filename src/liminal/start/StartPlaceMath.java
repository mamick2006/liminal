package liminal.start;

public final class StartPlaceMath {

  private StartPlaceMath() {
  }

  public static int pick(String[] ids, int[] sizes, boolean[] open) {
    int best = -1;
    for (int i = 0; i < ids.length; i++) {
      if (best < 0 || better(i, best, ids, sizes, open)) {
        best = i;
      }
    }
    return best;
  }

  private static boolean better(int a, int b, String[] ids, int[] sizes, boolean[] open) {
    if (open[a] != open[b]) {
      return open[a];
    }
    if (sizes[a] != sizes[b]) {
      return sizes[a] > sizes[b];
    }
    return ids[a].compareTo(ids[b]) < 0;
  }
}

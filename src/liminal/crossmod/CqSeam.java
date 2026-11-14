package liminal.crossmod;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class CqSeam {

  private static final String API = "cq.api.CaptainsQuartersAPI";

  private static final Class<?> CQ = find();
  private static final MethodHandle IS_AVAILABLE = method("isAvailable");
  private static final MethodHandle IS_KNOWN_FURNISHING = method("isKnownFurnishing", String.class);
  private static final MethodHandle IS_OWNED = method("isOwned", String.class);
  private static final MethodHandle GRANT = method("grant", String.class);

  private CqSeam() {
  }

  public static boolean isInstalled() {
    return CQ != null;
  }

  public static boolean isAvailable() {
    return ask(IS_AVAILABLE);
  }

  public static boolean isKnownFurnishing(String itemId) {
    return ask(IS_KNOWN_FURNISHING, itemId);
  }

  public static boolean isOwned(String itemId) {
    return ask(IS_OWNED, itemId);
  }

  public static boolean grant(String itemId) {
    return ask(GRANT, itemId);
  }

  private static Class<?> find() {
    try {
      return Class.forName(API);
    } catch (Throwable t) {
      return null;
    }
  }

  private static MethodHandle method(String name, Class<?>... params) {
    if (CQ == null) {
      return null;
    }
    try {
      return MethodHandles.publicLookup()
          .findStatic(CQ, name, MethodType.methodType(boolean.class, params));
    } catch (Throwable t) {
      return null;
    }
  }

  private static boolean ask(MethodHandle handle, Object... args) {
    if (handle == null) {
      return false;
    }
    try {
      return (Boolean) handle.invokeWithArguments(args);
    } catch (Throwable t) {
      return false;
    }
  }
}

package liminal.topbar;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LiminalMgmtShell {

  private LiminalMgmtShell() {
  }

  static final String TAB_METHOD = "tabClosed";
  private static final String TAB_METHOD_DESC = "()V";
  public static final String RULE_PANEL = "panel class";
  public static final String RULE_TAB = "tab interface";

  private static final Map<String, Class<?>> DEFINED = new HashMap<String, Class<?>>();

  public static synchronized MethodHandle define(MethodHandles.Lookup lookup, String simpleName,
      Class<?> coreClass, Class<?> panelBase, Class<?> sink, String sinkMethod)
      throws Exception {
    String name = lookup.lookupClass().getPackageName() + "." + simpleName;
    Class<?> shell = DEFINED.get(name);
    if (shell == null) {
      Class<?> tab = tabInterface(coreClass);
      checkPanelBase(panelBase);
      MethodHandles.publicLookup().findStatic(sink, sinkMethod,
          MethodType.methodType(void.class, Object.class));
      shell = lookup.defineClass(shellBytes(internal(name), internal(panelBase.getName()),
          internal(tab.getName()), internal(sink.getName()), sinkMethod));
      DEFINED.put(name, shell);
    }
    return lookup.findConstructor(shell,
        MethodType.methodType(void.class, float.class, float.class));
  }

  public static Class<?> tabInterface(Class<?> coreClass) throws Exception {
    List<String> found = new ArrayList<String>();
    List<String> seen = new ArrayList<String>();
    StringBuilder refusals = new StringBuilder();
    ClassLoader loader = coreClass.getClassLoader();
    for (Class<?> c = coreClass; c != null && c != Object.class; c = c.getSuperclass()) {
      List<String> owners;
      try {
        owners = interfaceMethodOwners(classBytes(c), TAB_METHOD, TAB_METHOD_DESC);
      } catch (Exception e) {
        throw new ClassNotFoundException(ruleFailed(RULE_TAB, "the class file of " + c.getName()
            + " could not be read (" + e + ")"));
      }
      for (String owner : owners) {
        if (seen.contains(owner)) {
          continue;
        }
        seen.add(owner);
        String refused = tabInterfaceProblem(owner, loader);
        if (refused == null) {
          found.add(owner);
        } else {
          refusals.append(owner).append(' ').append(refused).append("; ");
        }
      }
    }
    if (found.size() == 1) {
      return Class.forName(found.get(0), false, loader);
    }
    if (found.isEmpty()) {
      throw new ClassNotFoundException(ruleFailed(RULE_TAB, refusals + coreClass.getName()
          + " calls " + TAB_METHOD + "() on no public one-method interface"));
    }
    throw new ClassNotFoundException(ruleFailed(RULE_TAB, coreClass.getName() + " calls "
        + TAB_METHOD + "() on " + found.size() + " one-method interfaces, " + found
        + ", and exactly one is needed"));
  }

  static String ruleFailed(String rule, String what) {
    return "Manage lookup '" + rule + "' failed: " + what + ". The game's core UI changed shape.";
  }

  private static String tabInterfaceProblem(String name, ClassLoader loader) {
    Class<?> c;
    try {
      c = Class.forName(name, false, loader);
    } catch (Throwable t) {
      return "does not load (" + t + ")";
    }
    if (!c.isInterface() || (c.getModifiers() & 0x0001) == 0) {
      return "is not a public interface";
    }
    try {
      MethodHandles.publicLookup().findVirtual(c, TAB_METHOD, MethodType.methodType(void.class));
      MethodHandleProxies.asInterfaceInstance(c, MethodHandles.zero(void.class));
    } catch (Throwable t) {
      return "is not a one-method " + TAB_METHOD + "() interface (" + t + ")";
    }
    return null;
  }

  private static void checkPanelBase(Class<?> panelBase) throws Exception {
    if (panelBase.isInterface() || (panelBase.getModifiers() & 0x0411) != 0x0001) {
      throw new ClassNotFoundException(ruleFailed(RULE_PANEL, panelBase.getName()
          + " is not a public class the Manage screen can extend"));
    }
    try {
      MethodHandles.publicLookup().findConstructor(panelBase,
          MethodType.methodType(void.class, float.class, float.class));
    } catch (Exception e) {
      throw new ClassNotFoundException(ruleFailed(RULE_PANEL, panelBase.getName()
          + " has no public (float, float) constructor (" + e + ")"));
    }
  }

  static byte[] classBytes(Class<?> c) throws IOException {
    String name = c.getName();
    String file = name.substring(name.lastIndexOf('.') + 1) + ".class";
    InputStream in = c.getResourceAsStream(file);
    if (in == null) {
      throw new IOException("no class file " + file);
    }
    try {
      byte[] buf = new byte[65536];
      int n = 0;
      for (;;) {
        int got = in.read(buf, n, buf.length - n);
        if (got < 0) {
          return Arrays.copyOf(buf, n);
        }
        n += got;
        if (n == buf.length) {
          buf = Arrays.copyOf(buf, n * 2);
        }
      }
    } finally {
      in.close();
    }
  }

  private static int[] poolOffsets(byte[] b) {
    if (b.length < 10 || u2(b, 0) != 0xCAFE || u2(b, 2) != 0xBABE) {
      throw new IllegalArgumentException("not a class file");
    }
    int count = u2(b, 8);
    int[] at = new int[count + 1];
    int p = 10;
    for (int i = 1; i < count; i++) {
      at[i] = p;
      int tag = b[p] & 0xFF;
      switch (tag) {
        case 1:
          p += 3 + u2(b, p + 1);
          break;
        case 3: case 4: case 9: case 10: case 11: case 12: case 17: case 18:
          p += 5;
          break;
        case 5: case 6:
          p += 9;
          i++;
          break;
        case 7: case 8: case 16: case 19: case 20:
          p += 3;
          break;
        case 15:
          p += 4;
          break;
        default:
          throw new IllegalArgumentException("unknown constant pool tag " + tag);
      }
    }
    at[count] = p;
    return at;
  }

  static List<String> interfaceMethodOwners(byte[] classBytes, String name, String desc) {
    byte[] b = classBytes;
    int[] at = poolOffsets(b);
    List<String> owners = new ArrayList<String>();
    for (int i = 1; i < at.length - 1; i++) {
      if (at[i] == 0 || b[at[i]] != 11) {
        continue;
      }
      int nameAndType = at[u2(b, at[i] + 3)];
      if (!name.equals(utf(b, at[u2(b, nameAndType + 1)]))
          || !desc.equals(utf(b, at[u2(b, nameAndType + 3)]))) {
        continue;
      }
      int owner = at[u2(b, at[i] + 1)];
      String dotted = utf(b, at[u2(b, owner + 1)]).replace('/', '.');
      if (!owners.contains(dotted)) {
        owners.add(dotted);
      }
    }
    return owners;
  }

  static String className(byte[] classBytes) {
    byte[] b = classBytes;
    int[] at = poolOffsets(b);
    int thisClass = at[u2(b, at[at.length - 1] + 2)];
    return utf(b, at[u2(b, thisClass + 1)]).replace('/', '.');
  }

  private static int u2(byte[] b, int at) {
    return (b[at] & 0xFF) << 8 | (b[at + 1] & 0xFF);
  }

  private static String utf(byte[] b, int at) {
    int len = u2(b, at + 1);
    StringBuilder s = new StringBuilder(len);
    int p = at + 3;
    int end = p + len;
    while (p < end) {
      int c = b[p++] & 0xFF;
      if (c < 0x80) {
        s.append((char) c);
      } else if (c < 0xE0) {
        s.append((char) ((c & 0x1F) << 6 | (b[p++] & 0x3F)));
      } else {
        s.append((char) ((c & 0x0F) << 12 | (b[p] & 0x3F) << 6 | (b[p + 1] & 0x3F)));
        p += 2;
      }
    }
    return s.toString();
  }

  private static String internal(String dotted) {
    return dotted.replace('.', '/');
  }

  static byte[] shellBytes(String self, String superName, String iface, String sink,
      String sinkMethod) {
    Out o = new Out();
    o.u4(0xCAFEBABE);
    o.u2(0);
    o.u2(52);
    o.u2(20);
    o.utf(self);
    o.pair(7, 1);
    o.utf(superName);
    o.pair(7, 3);
    o.utf(iface);
    o.pair(7, 5);
    o.utf("<init>");
    o.utf("(FF)V");
    o.pair(12, 7, 8);
    o.pair(10, 4, 9);
    o.utf(TAB_METHOD);
    o.utf(TAB_METHOD_DESC);
    o.utf(sink);
    o.pair(7, 13);
    o.utf(sinkMethod);
    o.utf("(Ljava/lang/Object;)V");
    o.pair(12, 15, 16);
    o.pair(10, 14, 17);
    o.utf("Code");
    o.u2(0x0031);
    o.u2(2);
    o.u2(4);
    o.u2(1);
    o.u2(6);
    o.u2(0);
    o.u2(2);
    o.method(7, 8, 3, 3, new int[] { 0x2A, 0x23, 0x24, 0xB7, 0, 10, 0xB1 });
    o.method(11, 12, 1, 1, new int[] { 0x2A, 0xB8, 0, 18, 0xB1 });
    o.u2(0);
    return o.bytes();
  }

  private static final class Out {
    private byte[] buf = new byte[512];
    private int n;

    void u1(int v) {
      if (n == buf.length) {
        buf = Arrays.copyOf(buf, n * 2);
      }
      buf[n++] = (byte) v;
    }

    void u2(int v) {
      u1(v >> 8);
      u1(v);
    }

    void u4(int v) {
      u2(v >>> 16);
      u2(v);
    }

    void utf(String s) {
      u1(1);
      int lengthAt = n;
      u2(0);
      for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        if (c >= 0x01 && c <= 0x7F) {
          u1(c);
        } else if (c <= 0x7FF) {
          u1(0xC0 | c >> 6);
          u1(0x80 | c & 0x3F);
        } else {
          u1(0xE0 | c >> 12);
          u1(0x80 | c >> 6 & 0x3F);
          u1(0x80 | c & 0x3F);
        }
      }
      int len = n - lengthAt - 2;
      buf[lengthAt] = (byte) (len >> 8);
      buf[lengthAt + 1] = (byte) len;
    }

    void pair(int tag, int... indexes) {
      u1(tag);
      for (int index : indexes) {
        u2(index);
      }
    }

    void method(int name, int desc, int maxStack, int maxLocals, int[] code) {
      u2(0x0001);
      u2(name);
      u2(desc);
      u2(1);
      u2(19);
      u4(12 + code.length);
      u2(maxStack);
      u2(maxLocals);
      u4(code.length);
      for (int op : code) {
        u1(op);
      }
      u2(0);
      u2(0);
    }

    byte[] bytes() {
      return Arrays.copyOf(buf, n);
    }
  }
}

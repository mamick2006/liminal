package liminal.ui;

import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

public final class LiminalIllustration implements CustomUIPanelPlugin {

  private static final float LEFT_X = 790f;
  private static final float RIGHT_GUTTER = 24f;
  private static final float PAD = 6f;
  private static final float MIN_W = 240f;
  private static final float MIN_H = 160f;

  private final SpriteAPI sprite;
  private final float imageAspect;
  private final float texXFull;
  private final float texYFull;
  private final float texWFull;
  private final float texHFull;
  private transient PositionAPI pos;

  public static boolean fillRightOfEventUI(CustomPanelAPI host, String spriteKey, float width,
      float height) {
    try {
      float w = width - LEFT_X - RIGHT_GUTTER;
      float h = height - 2f * PAD;
      if (w < MIN_W || h < MIN_H) {
        return false;
      }
      String name = Global.getSettings().getSpriteName("illustrations", spriteKey);
      if (name == null || name.isEmpty()) {
        return false;
      }
      SpriteAPI sprite = Global.getSettings().getSprite(name);
      if (sprite == null || sprite.getWidth() <= 0f || sprite.getHeight() <= 0f) {
        return false;
      }
      CustomPanelAPI panel = host.createCustomPanel(w, h, new LiminalIllustration(sprite));
      host.addComponent(panel).inTL(LEFT_X, PAD);
      return true;
    } catch (Throwable ignore) {
      return false;
    }
  }

  private LiminalIllustration(SpriteAPI sprite) {
    this.sprite = sprite;
    this.imageAspect = sprite.getWidth() / sprite.getHeight();
    this.texXFull = sprite.getTexX();
    this.texYFull = sprite.getTexY();
    this.texWFull = sprite.getTexWidth();
    this.texHFull = sprite.getTexHeight();
  }

  @Override
  public void positionChanged(PositionAPI position) {
    this.pos = position;
    float w = position.getWidth();
    float h = position.getHeight();
    if (w <= 0f || h <= 0f) {
      return;
    }
    float rectAspect = w / h;
    float cropW = 1f;
    float cropH = 1f;
    if (imageAspect > rectAspect) {
      cropW = rectAspect / imageAspect;
    } else {
      cropH = imageAspect / rectAspect;
    }
    sprite.setTexX(texXFull + texWFull * (1f - cropW) / 2f);
    sprite.setTexWidth(texWFull * cropW);
    sprite.setTexY(texYFull + texHFull * (1f - cropH) / 2f);
    sprite.setTexHeight(texHFull * cropH);
    sprite.setSize(w, h);
  }

  @Override
  public void renderBelow(float alphaMult) {
    if (pos == null || alphaMult <= 0f) {
      return;
    }
    sprite.setAlphaMult(alphaMult);
    sprite.render(pos.getX(), pos.getY());
  }

  @Override
  public void render(float alphaMult) {
  }

  @Override
  public void advance(float amount) {
  }

  @Override
  public void processInput(List<InputEventAPI> events) {
  }

  @Override
  public void buttonPressed(Object buttonId) {
  }
}

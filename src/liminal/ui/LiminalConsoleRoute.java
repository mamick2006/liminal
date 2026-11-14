package liminal.ui;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;

final class LiminalConsoleRoute extends BaseCustomUIPanelPlugin {

  static final LiminalConsoleRoute INSTANCE = new LiminalConsoleRoute();

  private LiminalConsoleRoute() {
  }

  @Override
  public void buttonPressed(Object buttonId) {
    LiminalConsolePanel.routeButton(buttonId);
  }
}

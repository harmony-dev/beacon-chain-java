package org.ethereum.beacon.validator.api.controller;

public class ControllerRoute {
  private final String path;
  private final Controller controller;

  private ControllerRoute(String path, Controller controller) {
    this.path = path;
    this.controller = controller;
  }

  public static ControllerRoute of(String path, Controller controller) {
    return new ControllerRoute(path, controller);
  }

  public String getPath() {
    return path;
  }

  public Controller getController() {
    return controller;
  }
}

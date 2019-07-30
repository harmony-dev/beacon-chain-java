package org.ethereum.beacon.validator.api.controller;

public class ControllerRoute {
  private final RequestType requestType;
  private final String path;
  private final Controller controller;

  private ControllerRoute(RequestType requestType, String path, Controller controller) {
    this.requestType = requestType;
    this.path = path;
    this.controller = controller;
  }

  public static ControllerRoute of(RequestType requestType, String path, Controller controller) {
    return new ControllerRoute(requestType, path, controller);
  }

  public RequestType getRequestType() {
    return requestType;
  }

  public String getPath() {
    return path;
  }

  public Controller getController() {
    return controller;
  }

  public enum RequestType {
    GET,
    POST
  };
}

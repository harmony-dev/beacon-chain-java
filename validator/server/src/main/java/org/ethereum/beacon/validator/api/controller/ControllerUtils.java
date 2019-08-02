package org.ethereum.beacon.validator.api.controller;

import io.vertx.core.MultiMap;
import org.ethereum.beacon.validator.api.InvalidInputException;

import java.util.List;

public abstract class ControllerUtils {
  static String getParamString(String param, MultiMap params) throws InvalidInputException {
    try {
      assert params.contains(param);
      assert params.getAll(param).size() == 1;
      return params.get(param);
    } catch (AssertionError e) {
      throw new InvalidInputException(
          String.format("Parameters map doesn't contain 1 required param `%s`", param));
    }
  }

  static List<String> getParamStringList(String param, MultiMap params)
      throws InvalidInputException {
    try {
      assert params.contains(param);
      return params.getAll(param);
    } catch (AssertionError e) {
      throw new InvalidInputException(
          String.format("Parameters map doesn't contain required param `%s`", param));
    }
  }
}

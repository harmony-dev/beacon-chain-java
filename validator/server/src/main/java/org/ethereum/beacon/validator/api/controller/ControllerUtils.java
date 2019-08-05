package org.ethereum.beacon.validator.api.controller;

import io.vertx.core.MultiMap;
import org.ethereum.beacon.validator.api.InvalidInputException;

import java.util.List;

public abstract class ControllerUtils {
  static String getParamString(String param, MultiMap params) throws InvalidInputException {
    if (!params.contains(param)) {
      throwNoParamInMap(param);
    }
    if (params.getAll(param).size() != 1) {
      throwMoreThanOneParamInMap(param);
    }
    return params.get(param);
  }

  static List<String> getParamStringList(String param, MultiMap params)
      throws InvalidInputException {
    if (!params.contains(param)) {
      throwNoParamInMap(param);
    }
    return params.getAll(param);
  }

  private static void throwNoParamInMap(String param) {
    throw new InvalidInputException(
        String.format("Parameters map doesn't contain required param `%s`", param));
  }

  private static void throwMoreThanOneParamInMap(String param) {
    throw new InvalidInputException(
        String.format(
            "Parameters map contains more than one value for param `%s` but shouldn't", param));
  }
}

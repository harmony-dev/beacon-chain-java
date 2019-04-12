package org.ethereum.beacon.ssz;

import java.util.function.Function;

public interface ExternalVarResolver extends Function<String, Object> {

  class ExternalVariableNotDefined extends SSZSchemeException {

    ExternalVariableNotDefined(String s) {
      super(s);
    }
  }

  class ExternalVariableInvalidType extends SSZSchemeException {

    public ExternalVariableInvalidType(String message) {
      super(message);
    }
  }

  default <T> T resolveOrThrow(String varName, Class<T> type) {
    Object ret = apply(varName);
    if (ret == null) {
      throw new ExternalVariableNotDefined("Mandatory variable not defined: " + varName);
    }
    if (!type.isInstance(ret)) {
      throw new ExternalVariableInvalidType(
          "Variable value type (" + ret.getClass() + ") is not expected: " + type);
    }
    return (T) ret;
  }
}

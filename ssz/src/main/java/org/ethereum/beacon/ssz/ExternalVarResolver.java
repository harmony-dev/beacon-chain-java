package org.ethereum.beacon.ssz;

import org.ethereum.beacon.ssz.annotation.SSZ;

import java.util.function.Function;

/**
 * Resolves external runtime property value.
 * For example see {@link SSZ#vectorSize()} placeholder
 */
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

  /**
   * Resolves the variable of specific type.
   * @throws SSZSchemeException if variable is absent or of a wrong type
   */
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

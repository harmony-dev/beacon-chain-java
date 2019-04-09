package org.ethereum.beacon.core.spec;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.sun.istack.internal.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class StringConstantsResolver  {
  private final SpecConstants constants;
  private final Converter<String, String> caseConverter;
  private final Map<String, Method> constMethods = new HashMap<>();

  public StringConstantsResolver(SpecConstants constants) {
    this.constants = constants;
    caseConverter = CaseFormat.UPPER_UNDERSCORE.converterTo(CaseFormat.UPPER_CAMEL);
    for (Method method : constants.getClass().getMethods()) {
      if (method.getParameterTypes().length == 0
          && method.getName().startsWith("get")
          && Number.class.isAssignableFrom(method.getReturnType())) {
        constMethods.put(method.getName(), method);
      }
    }
  }

  /**
   * Name should be in the original spec notation (upper underscore), like TARGET_COMMITTEE_SIZE
   * @return <code>empty</code> if the constant not found
   */
  public Optional<Number> resolveByName(@NotNull String constName) {
    String convertedName = caseConverter.convert(constName);
    Method getter = constMethods.get("get" + convertedName);
    if (getter == null) {
      return Optional.empty();
    }
    try {
      return Optional.of((Number) getter.invoke(constants));
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("Couldn't retrieve constant " + constName, e);
    }
  }
}

package org.ethereum.beacon.core.ssz;

import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.spec.StringConstantsResolver;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.ssz.access.basic.BytesCodec;
import org.ethereum.beacon.ssz.access.basic.HashCodec;
import org.ethereum.beacon.ssz.access.basic.UIntCodec;
import org.ethereum.beacon.ssz.access.list.BytesValueAccessor;
import org.ethereum.beacon.ssz.access.list.ReadListAccessor;

public class DefaultSSZ {

  public static SSZSerializer createSSZSerializer() {
    return createCommonSSZBuilder().buildSerializer();
  }

  public static SSZSerializer createSSZSerializer(SpecConstants specConstants) {
    return createCommonSSZBuilder(specConstants).buildSerializer();
  }

  public static SSZBuilder createCommonSSZBuilder(SpecConstants specConstants) {
    StringConstantsResolver constantsResolver = new StringConstantsResolver(specConstants);
    return createCommonSSZBuilder()
        .withExternalVarResolver(varName -> {
          if (varName.startsWith("spec.")) {
            return constantsResolver.resolveByName(varName.substring("spec.".length()));
          }
          return null;
        });
  }

  public static SSZBuilder createCommonSSZBuilder() {
    return new SSZBuilder()
        .addDefaultBasicCodecs()
        .addBasicCodecs(new UIntCodec())
        .addBasicCodecs(new BytesCodec())
        .addBasicCodecs(new HashCodec())
        .addDefaultListAccessors()
        .addListAccessors(new ReadListAccessor())
        .addListAccessors(new BytesValueAccessor());
  }
}

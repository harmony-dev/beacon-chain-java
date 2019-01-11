package org.ethereum.beacon.util.ssz;

import org.ethereum.beacon.util.ssz.type.BooleanPrimitive;
import org.ethereum.beacon.util.ssz.type.BytesPrimitive;
import org.ethereum.beacon.util.ssz.type.SSZCodec;
import org.ethereum.beacon.util.ssz.type.StringPrimitive;
import org.ethereum.beacon.util.ssz.type.UIntPrimitive;

public class SSZSerializerBuilder {
  private SSZSerializer sszSerializer;

  public SSZSerializerBuilder(SSZSchemeBuilder schemeBuilder) {
    this.sszSerializer = new SSZSerializer(schemeBuilder);
  }

  public void addEncoderDecoder(SSZCodec encoderDecoder) {
    sszSerializer.registerClassTypes(encoderDecoder.getSupportedClassTypes(), encoderDecoder);
    sszSerializer.registerTypes(encoderDecoder.getSupportedTypes(), encoderDecoder);
  }

  /**
   * Creates {@link SSZSerializer} with set of {@link SSZCodec}'s
   * covering all primitive java types: integer numerics, strings, booleans, bytes
   * Uses {@link SSZAnnotationSchemeBuilder} for SSZ model scheme building
   * @return almost baked serializer, need to run build only.
   */
  public static SSZSerializerBuilder getBakedAnnotationBuilder() {
    SSZSerializerBuilder builder = new SSZSerializerBuilder(new SSZAnnotationSchemeBuilder());
    builder.addEncoderDecoder(new UIntPrimitive());
    builder.addEncoderDecoder(new BytesPrimitive());
    builder.addEncoderDecoder(new BooleanPrimitive());
    builder.addEncoderDecoder(new StringPrimitive());

    return builder;
  }

  public SSZSerializer build() {
    return sszSerializer;
  }
}

package org.ethereum.beacon.types.ssz;

import org.ethereum.beacon.util.ssz.SSZAnnotationSchemeBuilder;
import org.ethereum.beacon.util.ssz.SSZSchemeBuilder;
import org.ethereum.beacon.util.ssz.SSZSerializer;
import org.ethereum.beacon.util.ssz.type.SSZEncoderDecoder;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class Serializer extends SSZSerializer {

  private final static Serializer ANNOTATION_SERIALIZER = new Serializer(new SSZAnnotationSchemeBuilder())
      .withSSZEncoderDecoder(new SSZHash())
      .withSSZEncoderDecoder(new SSZUInt())
      .withSSZEncoderDecoder(new SSZBytesValue());

  public Serializer(SSZSchemeBuilder schemeBuilder) {
    super(schemeBuilder);
  }

  public static Serializer annotationSerializer() {
    return ANNOTATION_SERIALIZER;
  }

  @Override
  public Serializer withSSZEncoderDecoder(SSZEncoderDecoder encoderDecoder) {
    super.withSSZEncoderDecoder(encoderDecoder);
    return this;
  }

  public BytesValue encode2(Object input) {
    return BytesValue.wrap(encode(input));
  }

  public Object decode(BytesValue data, Class clazz) {
    return decode(data.getArrayUnsafe(), clazz);
  }
}

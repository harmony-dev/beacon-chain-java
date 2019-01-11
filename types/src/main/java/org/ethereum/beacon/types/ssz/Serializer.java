package org.ethereum.beacon.types.ssz;

import org.ethereum.beacon.util.ssz.SSZSerializer;
import org.ethereum.beacon.util.ssz.SSZSerializerBuilder;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class Serializer {

  private final static SSZSerializer ANNOTATION_SERIALIZER;
  static {
    SSZSerializerBuilder builder = SSZSerializerBuilder.getBakedAnnotationBuilder();
    builder.addEncoderDecoder(new SSZHash());
    builder.addEncoderDecoder(new SSZUInt());
    builder.addEncoderDecoder(new SSZBytesValue());
    ANNOTATION_SERIALIZER = builder.build();
  }
  private final static Serializer INSTANCE = new Serializer();

  public Serializer() {
  }

  public static Serializer annotationSerializer() {
    return INSTANCE;
  }

  public BytesValue encode2(Object input) {
    return BytesValue.wrap(ANNOTATION_SERIALIZER.encode(input));
  }

  public Object decode(BytesValue data, Class clazz) {
    return ANNOTATION_SERIALIZER.decode(data.getArrayUnsafe(), clazz);
  }
}

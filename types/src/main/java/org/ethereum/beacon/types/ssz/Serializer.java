package org.ethereum.beacon.types.ssz;

import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.ssz.SSZSerializerBuilder;
import tech.pegasys.artemis.util.bytes.BytesValue;

/**
 * <p>Extension of {@link SSZSerializer} designed to work with
 * encoded data in form of {@link BytesValue} instead of default
 * bytes[]. So it serializes instance to {@link BytesValue} and
 * restores it from this type.</p>
 *
 * <p>Includes several codecs for non-primitive types from
 * tech.pegasys.artemis.util package in {@link #annotationSerializer()}</p>
 */
public class Serializer {

  private final static SSZSerializer ANNOTATION_SERIALIZER;
  static {
    SSZSerializerBuilder builder = SSZSerializerBuilder.getBakedAnnotationBuilder();
    builder.addCodec(new SSZHash());
    builder.addCodec(new SSZUInt());
    builder.addCodec(new SSZBytesValue());
    ANNOTATION_SERIALIZER = builder.build();
  }
  private final static Serializer INSTANCE = new Serializer();

  private Serializer() {
  }

  /**
   * Prebuilt SSZ serializer with all codecs enabled
   * @return cached serializer instance
   */
  public static Serializer annotationSerializer() {
    return INSTANCE;
  }

  /**
   * Encodes object of SSZ model to {@link BytesValue}
   * @param input   SSZ model instance, marked with {@link org.ethereum.beacon.ssz.annotation.SSZSerializable}
   *                and other SSZ markings
   * @return {@link BytesValue} SSZ encoding of input onject
   */
  public BytesValue encode2(Object input) {
    return BytesValue.wrap(ANNOTATION_SERIALIZER.encode(input));
  }

  /**
   * Decodes encoded SSZ data to instance of some SSZ model
   * @param data    Encoded data
   * @param clazz   Java class with SSZ model markings
   * @return SSZ model instance filled with input encoded data
   */
  public Object decode(BytesValue data, Class clazz) {
    return ANNOTATION_SERIALIZER.decode(data.getArrayUnsafe(), clazz);
  }
}

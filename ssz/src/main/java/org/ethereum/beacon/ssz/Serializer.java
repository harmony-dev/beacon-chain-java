package org.ethereum.beacon.ssz;

import org.ethereum.beacon.ssz.type.BytesCodec;
import org.ethereum.beacon.ssz.type.HashCodec;
import org.ethereum.beacon.ssz.type.UIntCodec;
import tech.pegasys.artemis.util.bytes.BytesValue;

/**
 * Extension of {@link SSZSerializer} designed to work with encoded data in form of {@link
 * BytesValue} instead of default bytes[]. So it serializes instance to {@link BytesValue} and
 * restores it from this type.
 *
 * <p>Includes several codecs for non-primitive types from tech.pegasys.artemis.util package in
 * {@link #annotationSerializer()}
 */
public class Serializer {

  private static final SSZSerializer ANNOTATION_SERIALIZER;
  private static final Serializer INSTANCE = new Serializer();

  static {
    SSZSerializerBuilder builder = new SSZSerializerBuilder();
    builder.initWithExplicitAnnotations();
    builder.addCodec(new HashCodec());
    builder.addCodec(new UIntCodec());
    builder.addCodec(new BytesCodec());
    ANNOTATION_SERIALIZER = builder.build();
  }

  private Serializer() {}

  /**
   * Prebuilt SSZ serializer with all codecs enabled
   *
   * @return cached serializer instance
   */
  public static Serializer annotationSerializer() {
    return INSTANCE;
  }

  /**
   * Encodes object of SSZ model to {@link BytesValue}
   *
   * @param input SSZ model instance, marked with {@link
   *     org.ethereum.beacon.ssz.annotation.SSZSerializable} and other SSZ markings
   * @return {@link BytesValue} SSZ encoding of input onject
   */
  public BytesValue encode2(Object input) {
    return BytesValue.wrap(ANNOTATION_SERIALIZER.encode(input));
  }

  /**
   * Decodes encoded SSZ data to instance of some SSZ model
   *
   * @param data Encoded data
   * @param clazz Java class with SSZ model markings
   * @return SSZ model instance filled with input encoded data
   */
  public Object decode(BytesValue data, Class clazz) {
    return ANNOTATION_SERIALIZER.decode(data.getArrayUnsafe(), clazz);
  }
}

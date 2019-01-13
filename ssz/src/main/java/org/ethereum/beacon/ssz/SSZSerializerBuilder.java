package org.ethereum.beacon.ssz;

import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.ssz.annotation.SSZTransient;
import org.ethereum.beacon.ssz.type.BooleanPrimitive;
import org.ethereum.beacon.ssz.type.BytesPrimitive;
import org.ethereum.beacon.ssz.type.SSZCodec;
import org.ethereum.beacon.ssz.type.StringPrimitive;
import org.ethereum.beacon.ssz.type.UIntPrimitive;


/**
 * <p>SSZ Builder is designed to create {@link SSZSerializer} up to your needs.</p>
 *
 * <p>It uses {@link SSZAnnotationSchemeBuilder}to create SSZ model
 * from Java class with annotations</p>
 * <p>
 * Following annotations are used for this:
 * <ul>
 * <li>{@link SSZSerializable} - Class which stores SSZ serializable data should be
 * annotated with it, any type which is not java.lang.*</li>
 * <li>{@link SSZ} - any field with Java type couldn't be automatically mapped to SSZ type,
 * or with mapping that overrides standard, should be annotated with it. For standard
 * mappings check {@link SSZ#type()} Javadoc.</li>
 * <li>{@link SSZTransient} - Fields that should not be used in serialization
 * should be marked with such annotation</li>
 * </ul>
 * </p>
 *
 * <p>Final {@link SSZSerializer} could be built with {@link #build()} method.</p>
 * <p>For serialization use {@link SSZSerializer#encode(Object)}. SSZ serializer
 * uses getters for all non-transient fields to get current values.</p>
 * <p>For deserialization, to restore instance use {@link SSZSerializer#decode(byte[], Class)}.
 * It will try to find constructor with all non-transient field types and the same order,
 * to restore object. If failed, it will try to create empty instance from no-fields
 * constructor and set each one by one by appropriate setter.
 * If at least one field is failed to be set, {@link SSZSchemeException} is thrown.</p>
 */
public class SSZSerializerBuilder {
  private SSZSerializer sszSerializer = null;
  private SSZCodecResolver sszCodecResolver = null;

  public SSZSerializerBuilder() {
  }

  public SSZSerializerBuilder(SSZSchemeBuilder schemeBuilder, SSZCodecResolver codecResolver,
                              SSZModelFactory sszModelFactory) {
    this.sszCodecResolver = codecResolver;
    this.sszSerializer = new SSZSerializer(schemeBuilder, codecResolver, sszModelFactory);
  }

  public SSZSerializerBuilder initWith(SSZSchemeBuilder schemeBuilder, SSZCodecResolver codecResolver,
                                       SSZModelFactory sszModelFactory) {
    if (sszSerializer != null) {
      throw new RuntimeException("Already initialized!");
    }

    this.sszSerializer = new SSZSerializer(schemeBuilder, codecResolver, sszModelFactory);
    this.sszCodecResolver = codecResolver;
    return this;
  }

  /**
   * <p>{@link SSZSerializer} built with {@link SSZAnnotationSchemeBuilder}
   * which requires {@link SSZ} annotation at each model field</p>
   * @return {@link SSZSerializerBuilder} without codecs
   */
  public SSZSerializerBuilder initWithExplicitAnnotations() {
    this.sszCodecResolver = new SSZCodecRoulette();
    return initWith(new SSZAnnotationSchemeBuilder(), sszCodecResolver, new SSZModelCreator());
  }

  /**
   * <p>{@link SSZSerializer} built with {@link SSZAnnotationSchemeBuilder}
   * which doesn't require {@link SSZ} annotation at each model field,
   * mark all fields that should be skipped with {@link SSZTransient}</p>
   * @return {@link SSZSerializerBuilder} without codecs
   */
  public SSZSerializerBuilder initWithNonExplicitAnnotations() {
    this.sszCodecResolver = new SSZCodecRoulette();
    return initWith(new SSZAnnotationSchemeBuilder(false), sszCodecResolver, new SSZModelCreator());
  }

  public void addCodec(SSZCodec codec) {
    if (sszCodecResolver == null) {
      throw new RuntimeException("initWith* method should be called first");
    }
    sszCodecResolver.registerCodec(codec.getSupportedClasses(), codec.getSupportedSSZTypes(), codec);
  }

  /**
   * Adds {@link SSZCodec}'s  to handle almost all Java primitive types
   */
  public void addPrimitivesCodecs() {
    this.addCodec(new UIntPrimitive());
    this.addCodec(new BytesPrimitive());
    this.addCodec(new BooleanPrimitive());
    this.addCodec(new StringPrimitive());
  }

  /**
   * Creates {@link SSZSerializer} with set of {@link SSZCodec}'s
   * covering all primitive java types: integer numerics, strings, booleans, bytes
   * Uses {@link SSZAnnotationSchemeBuilder} for SSZ model scheme building
   * @return almost baked serializer, need to run build only.
   */
  public static SSZSerializerBuilder getBakedAnnotationBuilder() {
    SSZSerializerBuilder builder = new SSZSerializerBuilder().initWithNonExplicitAnnotations();
    builder.addPrimitivesCodecs();

    return builder;
  }

  /**
   * Finalizes build of {@link SSZSerializer} with builder
   * @return {@link SSZSerializer}
   */
  public SSZSerializer build() {
    if (sszSerializer == null) {
      throw new RuntimeException("initWith* method should be called first");
    }

    return sszSerializer;
  }
}

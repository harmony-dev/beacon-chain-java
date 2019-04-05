package org.ethereum.beacon.ssz;

import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.ssz.annotation.SSZTransient;
import org.ethereum.beacon.ssz.creator.ConstructorObjCreator;
import org.ethereum.beacon.ssz.creator.SSZModelFactory;
import org.ethereum.beacon.ssz.creator.SettersObjCreator;
import org.ethereum.beacon.ssz.type.BooleanPrimitive;
import org.ethereum.beacon.ssz.type.BytesPrimitive;
import org.ethereum.beacon.ssz.type.SSZCodec;
import org.ethereum.beacon.ssz.type.StringPrimitive;
import org.ethereum.beacon.ssz.type.UIntPrimitive;
import org.javatuples.Triplet;

import java.util.function.Function;

/**
 * SSZ Builder is designed to create {@link SSZSerializer} up to your needs.
 *
 * <p>It uses {@link SSZAnnotationSchemeBuilder}to create SSZ model from Java class with annotations
 *
 * <p>Following annotations are used for this:
 *
 * <ul>
 *   <li>{@link SSZSerializable} - Class which stores SSZ serializable data should be annotated with
 *       it, any type which is not java.lang.*
 *   <li>{@link SSZ} - any field with Java type couldn't be automatically mapped to SSZ type, or
 *       with mapping that overrides standard, should be annotated with it. For standard mappings
 *       check {@link SSZ#type()} Javadoc.
 *   <li>{@link SSZTransient} - Fields that should not be used in serialization should be marked
 *       with such annotation
 * </ul>
 *
 * <p>Final {@link SSZSerializer} could be built with {@link #build()} method.
 *
 * <p>For serialization use {@link SSZSerializer#encode(Object)}. SSZ serializer uses getters for
 * all non-transient fields to get current values.
 *
 * <p>For deserialization, to restore instance use {@link SSZSerializer#decode(byte[], Class)}. It
 * will try to find constructor with all non-transient field types and the same order, to restore
 * object. If failed, it will try to create empty instance from no-fields constructor and set each
 * one by one by appropriate setter. If at least one field is failed to be set, {@link
 * SSZSchemeException} is thrown.
 */
public class SSZSerializerBuilder {

  private static final int SSZ_SCHEMES_CACHE_CAPACITY = 128;

  private SSZSerializer sszSerializer = null;

  private SSZCodecResolver sszCodecResolver = null;

  private SSZSchemeBuilder sszSchemeBuilder = null;

  private SSZModelFactory sszModelFactory = null;

  public SSZSerializerBuilder() {}

  /**
   * Creates {@link SSZSerializer} with set of {@link SSZCodec}'s covering all primitive java types:
   * integer numerics, strings, booleans, bytes Uses {@link SSZAnnotationSchemeBuilder} for SSZ
   * model scheme building
   *
   * @return almost baked serializer, need to run build only.
   */
  public static SSZSerializerBuilder getBakedAnnotationBuilder() {
    SSZSerializerBuilder builder = new SSZSerializerBuilder().initWithNonExplicitAnnotations();
    builder.addPrimitivesCodecs();

    return builder;
  }

  public void addSchemeBuilderCache() {
    if (sszSchemeBuilder == null) {
      throw new RuntimeException("Scheme builder is not defined yet");
    }
    if (!(sszSchemeBuilder instanceof SSZAnnotationSchemeBuilder)) {
      throw new RuntimeException("Only SSZAnnotationSchemeBuilder is supported for adding cache");
    } else {
      ((SSZAnnotationSchemeBuilder) sszSchemeBuilder).withCache(SSZ_SCHEMES_CACHE_CAPACITY);
    }
  }

  private void checkAlreadyInitialized() {
    if (this.sszSerializer != null) {
      throw new RuntimeException("Already initialized!");
    }
  }

  /**
   * Final {@link SSZSerializer} will use user provided {@link SSZCodecResolver} for resolving
   * appropriate ssz codec when {@link SSZSerializerBuilder#build()} called in the end
   *
   * @param codecResolver Codec resolver
   * @return semi-built {@link SSZSerializerBuilder}
   */
  public SSZSerializerBuilder withSSZCodecResolver(SSZCodecResolver codecResolver) {
    checkAlreadyInitialized();
    this.sszCodecResolver = codecResolver;
    return this;
  }

  /**
   * Final {@link SSZSerializer} will use user provided {@link SSZSchemeBuilder} for creating ssz
   * scheme ob objects when {@link SSZSerializerBuilder#build()} called in the end
   *
   * @param schemeBuilder Scheme builder
   * @return semi-built {@link SSZSerializerBuilder}
   */
  public SSZSerializerBuilder withSSZSchemeBuilder(SSZSchemeBuilder schemeBuilder) {
    checkAlreadyInitialized();
    this.sszSchemeBuilder = schemeBuilder;
    return this;
  }

  /**
   * Final {@link SSZSerializer} will use user provided {@link SSZModelFactory} for object
   * instantiation when {@link SSZSerializerBuilder#build()} called in the end
   *
   * @param modelFactory Model factory
   * @return semi-built {@link SSZSerializerBuilder}
   */
  public SSZSerializerBuilder withSSZModelFactory(SSZModelFactory modelFactory) {
    checkAlreadyInitialized();
    this.sszModelFactory = modelFactory;
    return this;
  }

  /**
   * Uses {@link SSZModelFactory} which tries to create model instance by one constructor with all
   * input fields included. If such public constructor is not found, it tries to instantiate object
   * with empty constructor and set all fields directly or using standard setter.
   *
   * @return semi-built {@link SSZSerializerBuilder}
   */
  public SSZSerializerBuilder withDefaultSSZModelFactory() {
    checkAlreadyInitialized();
    this.sszModelFactory = createDefaultModelCreator();
    return this;
  }

  private SSZSerializerBuilder initWith(
      SSZSchemeBuilder schemeBuilder,
      SSZCodecResolver codecResolver,
      SSZModelFactory sszModelFactory) {
    checkAlreadyInitialized();
    this.sszSerializer = new SSZSerializer(schemeBuilder, codecResolver, sszModelFactory, null);
    this.sszCodecResolver = codecResolver;
    return this;
  }

  /**
   * {@link SSZSerializer} built with {@link SSZAnnotationSchemeBuilder} which requires {@link SSZ}
   * annotation at each model field
   *
   * <p>Uses {@link SSZModelFactory} which tries to create model instance by one constructor with
   * all input fields included. If such public constructor is not found, it tries to instantiate
   * object with empty constructor and set all fields directly or using standard setter.
   *
   * @return {@link SSZSerializerBuilder} without codecs
   */
  public SSZSerializerBuilder initWithExplicitAnnotations() {
    this.sszSchemeBuilder = new SSZAnnotationSchemeBuilder();
    return initWith(sszSchemeBuilder, sszCodecResolver, createDefaultModelCreator());
  }

  private SSZModelFactory createDefaultModelCreator() {
    return new SSZModelFactory(new ConstructorObjCreator(),new SettersObjCreator());
  }

  /**
   * {@link SSZSerializer} built with {@link SSZAnnotationSchemeBuilder} which doesn't require
   * {@link SSZ} annotation at each model field, mark all fields that should be skipped with {@link
   * SSZTransient}
   *
   * @return {@link SSZSerializerBuilder} without codecs
   */
  public SSZSerializerBuilder initWithNonExplicitAnnotations() {
    this.sszSchemeBuilder = new SSZAnnotationSchemeBuilder(false);
    return initWith(sszSchemeBuilder, sszCodecResolver, createDefaultModelCreator());
  }

  public SSZSerializerBuilder addCodec(SSZCodec codec) {
    if (sszCodecResolver == null) {
      throw new RuntimeException("initWith* method should be called first");
    }
    sszCodecResolver.registerCodec(
        codec.getSupportedClasses(), codec.getSupportedSSZTypes(), codec);

    return this;
  }

  /** Adds {@link SSZCodec}'s to handle almost all Java primitive types */
  public SSZSerializerBuilder addPrimitivesCodecs() {
    this.addCodec(new UIntPrimitive());
    this.addCodec(new BytesPrimitive());
    this.addCodec(new BooleanPrimitive());
    this.addCodec(new StringPrimitive());
    return this;
  }

  /**
   * Finalizes build of {@link SSZSerializer} with builder
   *
   * @return {@link SSZSerializer}
   */
  public SSZSerializer build() {
    if (sszSerializer == null) {
      if (sszCodecResolver != null && sszModelFactory != null && sszSchemeBuilder != null) {
        this.sszSerializer = new SSZSerializer(sszSchemeBuilder, sszCodecResolver, sszModelFactory, null);
      } else {
        throw new RuntimeException("initWith* or all with* methods should be called first");
      }
    }

    return sszSerializer;
  }

  /**
   * Finalizes build of custom extension of {@link SSZSerializer} with builder
   *
   * @return Custom {@link SSZSerializer}
   */
  public SSZSerializer buildCustom(
      Function<Triplet<SSZSchemeBuilder, SSZCodecResolver, SSZModelFactory>, SSZSerializer>
          serializerCreator) {
    checkAlreadyInitialized();
    this.sszSerializer =
        serializerCreator.apply(Triplet.with(sszSchemeBuilder, sszCodecResolver, sszModelFactory));

    return sszSerializer;
  }
}

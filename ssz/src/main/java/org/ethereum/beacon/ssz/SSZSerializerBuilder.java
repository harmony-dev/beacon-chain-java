package org.ethereum.beacon.ssz;

import org.ethereum.beacon.ssz.type.BooleanPrimitive;
import org.ethereum.beacon.ssz.type.BytesPrimitive;
import org.ethereum.beacon.ssz.type.SSZCodec;
import org.ethereum.beacon.ssz.type.StringPrimitive;
import org.ethereum.beacon.ssz.type.UIntPrimitive;

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

  public SSZSerializerBuilder initWithExplicitAnnotations() {
    this.sszCodecResolver = new SSZCodecRoulette();
    return initWith(new SSZAnnotationSchemeBuilder(), sszCodecResolver, new SSZModelCreator());
  }

  public SSZSerializerBuilder initWithNonExplicitAnnotations() {
    this.sszCodecResolver = new SSZCodecRoulette();
    return initWith(new SSZAnnotationSchemeBuilder(false), sszCodecResolver, new SSZModelCreator());
  }

  public void addCodec(SSZCodec codec) {
    if (sszCodecResolver == null) {
      throw new RuntimeException("initWith* method should be called first");
    }
    sszCodecResolver.registerCodec(codec.getSupportedClasses(), codec.getSupportedTypes(), codec);
  }

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

  public SSZSerializer build() {
    if (sszSerializer == null) {
      throw new RuntimeException("initWith* method should be called first");
    }

    return sszSerializer;
  }
}

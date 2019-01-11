package org.ethereum.beacon.util.ssz;

import org.ethereum.beacon.util.ssz.type.BooleanPrimitive;
import org.ethereum.beacon.util.ssz.type.BytesPrimitive;
import org.ethereum.beacon.util.ssz.type.SSZCodec;
import org.ethereum.beacon.util.ssz.type.StringPrimitive;
import org.ethereum.beacon.util.ssz.type.UIntPrimitive;

public class SSZSerializerBuilder {
  private SSZSerializer sszSerializer = null;

  public SSZSerializerBuilder() {
  }

  public SSZSerializerBuilder(SSZSchemeBuilder schemeBuilder) {
    this.sszSerializer = new SSZSerializer(schemeBuilder);
  }

  public SSZSerializerBuilder initWithSchemeBuilder(SSZSchemeBuilder schemeBuilder) {
    if (sszSerializer != null) {
      throw new RuntimeException("Already initialized!");
    }

    this.sszSerializer = new SSZSerializer(schemeBuilder);
    return this;
  }

  public SSZSerializerBuilder initWithExplicitAnnotations() {
    return initWithSchemeBuilder(new SSZAnnotationSchemeBuilder());
  }

  public SSZSerializerBuilder initWithNonExplicitAnnotations() {
    return initWithSchemeBuilder(new SSZAnnotationSchemeBuilder(false));
  }

  public void addCodec(SSZCodec codec) {
    if (sszSerializer == null) {
      throw new RuntimeException("initWith* method should be called first");
    }
    sszSerializer.registerCodec(codec.getSupportedClasses(), codec.getSupportedTypes(), codec);
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

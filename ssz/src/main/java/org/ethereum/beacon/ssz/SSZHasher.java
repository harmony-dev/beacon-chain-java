package org.ethereum.beacon.ssz;

import net.consensys.cava.bytes.Bytes;
import java.util.function.Function;

public class SSZHasher {
  private SSZSerializer hasher;

  private Function<Bytes, Bytes> hashFunction;

  public SSZHasher(Function<Bytes, Bytes> hashFunction) {
    this.hashFunction = hashFunction;
    SSZSerializerBuilder builder = new SSZSerializerBuilder();
    SSZCodecResolver hasher = new SSZCodecHasher(hashFunction);
    // TODO: we should have explicit annotations alternative
    builder.initWith(new SSZAnnotationSchemeBuilder(false), hasher, createDefaultModelCreator());
    this.hasher = builder.build();
  }

  private SSZModelFactory createDefaultModelCreator() {
    return new SSZModelCreator()
        .registerObjCreator(new ConstructorObjCreator())
        .registerObjCreator(new SettersObjCreator());
  }

  public Bytes calc(Object input) {
    return hashFunction.apply(Bytes.wrap(hasher.encode(input)));
  }
}

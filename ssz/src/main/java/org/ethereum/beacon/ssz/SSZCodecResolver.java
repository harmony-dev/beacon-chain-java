package org.ethereum.beacon.ssz;

import net.consensys.cava.ssz.BytesSSZReaderProxy;
import org.ethereum.beacon.ssz.type.SSZCodec;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import java.io.OutputStream;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public interface SSZCodecResolver {

  void registerCodec(Set<Class> classes, Set<String> types, SSZCodec codec);

  Consumer<Triplet<Object, OutputStream, SSZSerializer>> resolveEncodeFunction(
      SSZSchemeBuilder.SSZScheme.SSZField field);

  Function<Pair<BytesSSZReaderProxy, SSZSerializer>, Object> resolveDecodeFunction(
      SSZSchemeBuilder.SSZScheme.SSZField field);
}

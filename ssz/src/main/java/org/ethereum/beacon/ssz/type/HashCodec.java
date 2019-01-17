package org.ethereum.beacon.ssz.type;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.ssz.BytesSSZReaderProxy;
import net.consensys.cava.ssz.SSZ;
import net.consensys.cava.ssz.SSZException;
import org.ethereum.beacon.types.Hash48;
import org.ethereum.beacon.ssz.SSZSchemeBuilder;
import org.ethereum.beacon.ssz.SSZSchemeException;
import org.ethereum.beacon.ssz.type.SSZCodec;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.BytesValue;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SSZ Codec designed to work with fixed size bytes
 * data classes representing hashes,
 * check list in {@link #getSupportedClasses()}
 */
public class HashCodec implements SSZCodec {

  private static Set<String> supportedTypes = new HashSet<>();

  private static Set<Class> supportedClassTypes = new HashSet<>();
  static {
    supportedClassTypes.add(Hash32.class);
    supportedClassTypes.add(Hash48.class);
  }

  @Override
  public Set<String> getSupportedSSZTypes() {
    return supportedTypes;
  }

  @Override
  public Set<Class> getSupportedClasses() {
    return supportedClassTypes;
  }

  @Override
  public void encode(Object value, SSZSchemeBuilder.SSZScheme.SSZField field, OutputStream result) {
    HashType hashType = parseFieldType(field);
    Bytes res = null;
    BytesValue data = (BytesValue) value;
    res = SSZ.encodeHash(Bytes.of(data.getArrayUnsafe()));

    try {
      result.write(res.toArrayUnsafe());
    } catch (IOException e) {
      String error = String.format("Failed to write data of type %s to stream", field.type);
      throw new SSZException(error, e);
    }
  }

  @Override
  public void encodeList(List<Object> value, SSZSchemeBuilder.SSZScheme.SSZField field, OutputStream result) {
    HashType hashType = parseFieldType(field);
    Bytes[] data = repackBytesList((List<BytesValue>) (List<?>) value);

    try {
       result.write(SSZ.encodeHashList(data).toArrayUnsafe());
    } catch (IOException ex) {
      String error = String.format("Failed to write data from field \"%s\" to stream",
          field.name);
      throw new SSZException(error, ex);
    }
  }

  private static Bytes[] repackBytesList(List<BytesValue> list) {
    Bytes[] data = new Bytes[list.size()];
    for (int i = 0; i < list.size(); i++) {
      byte[] el = list.get(i).getArrayUnsafe();
      data[i] = Bytes.of(el);
    }

    return data;
  }

  @Override
  public Object decode(SSZSchemeBuilder.SSZScheme.SSZField field, BytesSSZReaderProxy reader) {
    HashType hashType = parseFieldType(field);

    try {
      switch (hashType.size) {
        case 32: {
          return Hash32.wrap(Bytes32.wrap(reader.readHash(hashType.size).toArrayUnsafe()));
        }
        case 48: {
          return Hash48.wrap(Bytes48.wrap(reader.readHash(hashType.size).toArrayUnsafe()));
        }
      }
    } catch (Exception ex) {
      String error = String.format("Failed to read data from stream to field \"%s\"",
          field.name);
      throw new SSZException(error, ex);
    }

    return throwUnsupportedType(field);
  }

  @Override
  public List<Object> decodeList(SSZSchemeBuilder.SSZScheme.SSZField field, BytesSSZReaderProxy reader) {
    HashType hashType = parseFieldType(field);

    List<Bytes> bytesList = reader.readHashList(hashType.size);
    List<BytesValue> res = null;
    try {
      switch (hashType.size) {
        case 32: {
          res = bytesList.stream()
              .map(Bytes::toArrayUnsafe)
              .map(Bytes32::wrap)
              .map(Hash32::wrap)
              .collect(Collectors.toList());
          break;
        }
        case 48: {
          res = bytesList.stream()
              .map(Bytes::toArrayUnsafe)
              .map(Bytes48::wrap)
              .map(Hash48::wrap)
              .collect(Collectors.toList());
          break;
        }
      }
    } catch (Exception ex) {
      String error = String.format("Failed to read list data from stream to field \"%s\"",
          field.name);
      throw new SSZException(error, ex);
    }

    return (List<Object>) (List<?>)  res;
  }

  static class HashType {
    final int size;

    HashType(int size) {
      this.size = size;
    }

    static HashType of(int size) {
      HashType res = new HashType(size);
      return res;
    }
  }

  private HashType parseFieldType(SSZSchemeBuilder.SSZScheme.SSZField field) {
    if (field.type.equals(Hash32.class)) {
      return HashType.of(32);
    } else if (field.type.equals(Hash48.class)) {
      return HashType.of(48);
    }

    throw new SSZSchemeException(String.format("Hash of class %s is not supported", field.type));
  }
}

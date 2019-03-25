package org.ethereum.beacon.ssz;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.ssz.BytesSSZReaderProxy;
import net.consensys.cava.ssz.SSZ;
import net.consensys.cava.ssz.SSZException;
import net.consensys.cava.units.bigints.UInt256;
import org.ethereum.beacon.ssz.type.SSZCodec;
import org.ethereum.beacon.ssz.type.SubclassCodec;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Implementation of {@link SSZCodecResolver} which implements SSZ Hash function
 *
 * <p>For more info check <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/simple-serialize.md#tree-hash">SSZ
 * Tree hash</a>
 */
public class SSZCodecHasher implements SSZCodecResolver {

  static final int BYTES_PER_CHUNK = 32;

  static final Bytes EMPTY_CHUNK = Bytes.of(new byte[BYTES_PER_CHUNK]);

  private Function<Bytes, Bytes> hashFunction;

  private Map<Class, List<CodecEntry>> registeredClassHandlers = new HashMap<>();

  public SSZCodecHasher(Function<Bytes, Bytes> hashFunction) {
    this.hashFunction = hashFunction;
  }

  public static SSZCodecHasher createWithHashFunction(
      Function<BytesValue, ? extends BytesValue> hashFunction) {
    return new SSZCodecHasher(bytes -> {
      BytesValue input = BytesValue.of(bytes.toArrayUnsafe());
      return Bytes.wrap(hashFunction.apply(input).getArrayUnsafe());
    });
  }

  public Consumer<Triplet<Object, OutputStream, BytesSerializer>> resolveEncodeFunction(
      SSZSchemeBuilder.SSZScheme.SSZField field) {
    SSZCodec encoder = resolveCodec(field);

    if (field.multipleType.equals(SSZSchemeBuilder.SSZScheme.MultipleType.NONE)) {
      if (encoder != null) {
        return objects -> {
          Object value = objects.getValue0();
          OutputStream res = objects.getValue1();
          ByteArrayOutputStream tmp = new ByteArrayOutputStream();
          encoder.encode(value, field, tmp);
          try {
            res.write(hash_tree_root_element(Bytes.wrap(tmp.toByteArray())).toArrayUnsafe());
          } catch (IOException e) {
            throw new SSZException("Failed to write data length to stream", e);
          }
        };
      } else {
        return objects -> {
          Object value = objects.getValue0();
          OutputStream res = objects.getValue1();
          BytesSerializer sszSerializer = objects.getValue2();
          encodeContainer(value, field, res, sszSerializer);
        };
      }
    } else if (field.multipleType.equals(SSZSchemeBuilder.SSZScheme.MultipleType.LIST)) {
      return objects -> {
        Object value = objects.getValue0();
        OutputStream res = objects.getValue1();
        Bytes[] elements;
        if (encoder != null) {
          List<Object> valuesList = (List<Object>) value;
          Bytes[] listElements = new Bytes[valuesList.size()];
          for (int i = 0; i < valuesList.size(); i++) {
            Object obj = valuesList.get(i);
            ByteArrayOutputStream tmp = new ByteArrayOutputStream();
            encoder.encode(obj, field, tmp);
            listElements[i] = Bytes.wrap(tmp.toByteArray());
          }
          elements = listElements;
        } else {
          BytesSerializer sszSerializer = objects.getValue2();
          elements = packContainerList((List<Object>) value, field, sszSerializer);
        }
        try {
          res.write(hash_tree_root_list(elements).toArrayUnsafe());
        } catch (IOException e) {
          throw new SSZException("Failed to write data length to stream", e);
        }
      };
    } else if (field.multipleType.equals(SSZSchemeBuilder.SSZScheme.MultipleType.ARRAY)) {
      return objects -> {
        Object value = objects.getValue0();
        OutputStream res = objects.getValue1();
        Bytes[] elements;
        if (encoder != null) {
          Object[] valuesArray = (Object[]) value;
          Bytes[] arrayElements = new Bytes[valuesArray.length];
          for (int i = 0; i < valuesArray.length; i++) {
            Object obj = valuesArray[i];
            ByteArrayOutputStream tmp = new ByteArrayOutputStream();
            encoder.encode(obj, field, tmp);
            arrayElements[i] = Bytes.wrap(tmp.toByteArray());
          }
          elements = arrayElements;
        } else {
          BytesSerializer sszSerializer = objects.getValue2();
          elements = packContainerList(Arrays.asList((Object[]) value), field, sszSerializer);
        }
        try {
          res.write(hash_tree_root_list(elements).toArrayUnsafe());
        } catch (IOException e) {
          throw new SSZException("Failed to write data to stream", e);
        }
      };
    }

    throw new SSZSchemeException(
        String.format("Function not resolved for encoding field %s", field));
  }

  private void encodeContainer(
      Object value,
      SSZSchemeBuilder.SSZScheme.SSZField field,
      OutputStream result,
      BytesSerializer sszSerializer) {
    byte[] data = sszSerializer.encode(value, field.type);

    if (!field.notAContainer) {
      try {
        // Prepend data with its length
        result.write(net.consensys.cava.ssz.SSZ.encodeInt32(data.length).toArrayUnsafe());
      } catch (IOException e) {
        throw new SSZException("Failed to write data length to stream", e);
      }
    }

    try {
      result.write(data);
    } catch (IOException e) {
      String error =
          String.format("Failed to write container from field \"%s\" to stream", field.name);
      throw new SSZException(error, e);
    }
  }

  private Bytes[] packContainerList(
      List<Object> values, SSZSchemeBuilder.SSZScheme.SSZField field, BytesSerializer sszSerializer) {
    Bytes[] res = new Bytes[values.size()];
    for (int i = 0; i < values.size(); ++i) {
      byte[] data = sszSerializer.encode(values.get(i), field.type);
      Bytes curValue;
      if (field.notAContainer) {
        curValue = Bytes.of(data).slice(4);
      } else {
        curValue = Bytes.of(data);
      }
      res[i] = curValue;
    }

    return res;
  }

  public Function<Pair<BytesSSZReaderProxy, BytesSerializer>, Object> resolveDecodeFunction(
      SSZSchemeBuilder.SSZScheme.SSZField field) {
    throw new SSZException("Decode is not supported for hash");
  }

  private SSZCodec resolveCodec(SSZSchemeBuilder.SSZScheme.SSZField field) {
    Class<?> type = field.type;
    boolean subclassCodec = false;
    if (!SubclassCodec.getSerializableClass(type).equals(type)) {
      type = SubclassCodec.getSerializableClass(type);
      subclassCodec = true;
    }

    SSZCodec codec = null;
    if (registeredClassHandlers.containsKey(type)) {
      List<CodecEntry> codecs = registeredClassHandlers.get(type);
      if (field.extraType == null || field.extraType.isEmpty()) {
        codec = codecs.get(0).codec;
      } else {
        for (CodecEntry codecEntry : codecs) {
          if (codecEntry.types.contains(field.extraType)) {
            codec = codecEntry.codec;
            break;
          }
        }
      }
    }

    if (codec != null && subclassCodec) {
      codec = new SubclassCodec(codec);
    }

    return codec;
  }

  /**
   * Registers codecs to be used for
   *
   * @param classes Classes, resolving is performed with class at first
   * @param types Text type, one class could be interpreted to several types. Several codecs could
   *     handle one class. Empty/null type is occupied by first class codec. Type is looked up in
   *     codecs one by one.
   * @param codec Codec able to encode/decode of specific class/types
   */
  public void registerCodec(Set<Class> classes, Set<String> types, SSZCodec codec) {
    for (Class clazz : classes) {
      if (registeredClassHandlers.get(clazz) != null) {
        registeredClassHandlers.get(clazz).add(new CodecEntry(codec, types));
      } else {
        registeredClassHandlers.put(
            clazz, new ArrayList<>(Collections.singletonList(new CodecEntry(codec, types))));
      }
    }
  }

  /**
   * Given ordered objects of the same basic type, serialize them, pack them into
   * BYTES_PER_CHUNK-byte chunks, right-pad the last chunk with zero bytes, and return the chunks.
   *
   */
  List<Bytes> pack(Bytes[] lst) {
    List<Bytes> chunkz = new ArrayList<>();
    // Handle empty list case
    if (lst.length == 0) {
      chunkz.add(EMPTY_CHUNK);
    } else {
      int currentItem = 0;
      int itemPosition = 0;
      while (currentItem < lst.length) {
        int chunkPosition = 0;
        byte[] currentChunk = new byte[BYTES_PER_CHUNK];
        while (chunkPosition < BYTES_PER_CHUNK) {
          int len =
              Math.min(BYTES_PER_CHUNK - chunkPosition, lst[currentItem].size() - itemPosition);
          System.arraycopy(
              lst[currentItem].toArray(), itemPosition, currentChunk, chunkPosition, len);
          chunkPosition += len;
          itemPosition += len;
          if (itemPosition == lst[currentItem].size()) {
            ++currentItem;
            itemPosition = 0;
          }
          if (currentItem == lst.length || chunkPosition == BYTES_PER_CHUNK) {
            chunkz.add(Bytes.wrap(currentChunk));
            chunkPosition = BYTES_PER_CHUNK;
          }
        }
        ++currentItem;
      }
    }

    return chunkz;
  }

  /**
   * Given ordered BYTES_PER_CHUNK-byte chunks, if necessary append zero chunks so that the number
   * of chunks is a power of two, Merkleize the chunks, and return the root.
   */
  Bytes merkleize(List<Bytes> chunkz) {
    for (int i = chunkz.size(); i < next_power_of_2(chunkz.size()); ++i) {
      chunkz.add(EMPTY_CHUNK);
    }
    while (chunkz.size() > 1) {
      List<Bytes> tempChunkz = new ArrayList<>();
      for (int i = 0; i < chunkz.size(); i += 2) {
        Bytes curChunk = hashFunction.apply(Bytes.concatenate(chunkz.get(i), chunkz.get(i + 1)));
        tempChunkz.add(curChunk);
      }
      chunkz = tempChunkz;
    }

    return chunkz.get(0);
  }

  private long next_power_of_2(int x) {
    if (x <= 1) {
      return 1;
    } else {
      return Long.highestOneBit(x - 1) << 1;
    }
  }

  /**
   * Given a Merkle root and a length (uint256 little-endian serialization) return
   * hash(root + length).
   */
  Bytes mix_in_length(Bytes root, int length) {
    Bytes len = SSZ.encodeUInt256(UInt256.valueOf(length));
    return hashFunction.apply(Bytes.concatenate(root, len));
  }

  Bytes zpad(Bytes input, int length) {
    return Bytes.concatenate(input, Bytes.wrap(new byte[length - input.size()]));
  }

  private Bytes hash_tree_root_list(Bytes[] lst) {
    return mix_in_length(merkleize(pack(lst)), lst.length);
  }

  private Bytes hash_tree_root_container(Bytes[] lst) {
    List<Bytes> values = new ArrayList<>();
    for (int i = 0; i < lst.length; ++i) {
      values.add(hash_tree_root_element(lst[i]));
    }
    return merkleize(values);
  }

  Bytes hash_tree_root_element(Bytes el) {
    return merkleize(pack(new Bytes[]{el}));
  }

  private Bytes hash_tree_root_containers_list(Bytes[] lst) {
    List<Bytes> values = new ArrayList<>();
    for (int i = 0; i < lst.length; ++i) {
      values.add(hash_tree_root_element(lst[i]));
    }
    return mix_in_length(merkleize(values), lst.length);
  }

  class CodecEntry {
    SSZCodec codec;
    Set<String> types;

    public CodecEntry(SSZCodec codec, Set<String> types) {
      this.codec = codec;
      this.types = types;
    }
  }
}

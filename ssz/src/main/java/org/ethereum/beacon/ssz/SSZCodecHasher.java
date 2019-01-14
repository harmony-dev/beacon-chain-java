package org.ethereum.beacon.ssz;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.ssz.BytesSSZReaderProxy;
import net.consensys.cava.ssz.SSZ;
import net.consensys.cava.ssz.SSZException;
import org.ethereum.beacon.ssz.type.SSZCodec;
import org.javatuples.Pair;
import org.javatuples.Triplet;
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
 * <p>Implementation of {@link SSZCodecResolver} which implements SSZ Hash function</p>
 * <p>For more info check https://github.com/ethereum/eth2.0-specs/blob/master/specs/simple-serialize.md#tree-hash</p>
 */
public class SSZCodecHasher implements SSZCodecResolver {

  private final static int SSZ_CHUNK_SIZE = 32;

  private final static Bytes EMPTY_CHUNK = Bytes.of(new byte[SSZ_CHUNK_SIZE]);

  private Function<Bytes, Bytes> hashFunction;

  private Map<Class, List<CodecEntry>> registeredClassHandlers = new HashMap<>();

  public SSZCodecHasher(Function<Bytes, Bytes> hashFunction) {
    this.hashFunction = hashFunction;
  }

  public Consumer<Triplet<Object, OutputStream, SSZSerializer>> resolveEncodeFunction(SSZSchemeBuilder.SSZScheme.SSZField field) {
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
          SSZSerializer sszSerializer = objects.getValue2();
          encodeContainer(value, field, res, sszSerializer);
        };
      }
    } else if (field.multipleType.equals(SSZSchemeBuilder.SSZScheme.MultipleType.LIST))  {
      return objects -> {
        Object value = objects.getValue0();
        OutputStream res = objects.getValue1();
        Bytes[] elements;
        if (encoder != null) {
          List<Bytes> listElements = new ArrayList<>();
          for (Object obj : (List<Object>) value) {
            ByteArrayOutputStream tmp = new ByteArrayOutputStream();
            encoder.encode(obj, field, tmp);
            listElements.add(Bytes.wrap(tmp.toByteArray()));
          }
          elements = (Bytes[]) listElements.toArray();
        } else {
          SSZSerializer sszSerializer = objects.getValue2();
          elements = packContainerList((List<Object>) value, field, sszSerializer);
        }
        try {
          res.write(hash_tree_root_list(elements).toArrayUnsafe());
        } catch (IOException e) {
          throw new SSZException("Failed to write data length to stream", e);
        }
      };
    } else if (field.multipleType.equals(SSZSchemeBuilder.SSZScheme.MultipleType.ARRAY))  {
      return objects -> {
        Object value = objects.getValue0();
        OutputStream res = objects.getValue1();
        Bytes[] elements;
        if (encoder != null) {
          List<Bytes> listElements = new ArrayList<>();
          for (Object obj : (Object[]) value) {
            ByteArrayOutputStream tmp = new ByteArrayOutputStream();
            encoder.encode(obj, field, tmp);
            listElements.add(Bytes.wrap(tmp.toByteArray()));
          }
          elements = (Bytes[]) listElements.toArray();
        } else {
          SSZSerializer sszSerializer = objects.getValue2();
          elements = packContainerList(Arrays.asList((Object[]) value), field, sszSerializer);
        }
        try {
          res.write(hash_tree_root_list(elements).toArrayUnsafe());
        } catch (IOException e) {
          throw new SSZException("Failed to write data to stream", e);
        }
      };
    }

    throw new SSZSchemeException(String.format("Function not resolved for encoding field %s", field));
  }

  private void encodeContainer(Object value, SSZSchemeBuilder.SSZScheme.SSZField field,
                               OutputStream result, SSZSerializer sszSerializer) {
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
      String error = String.format("Failed to write container from field \"%s\" to stream",
          field.name);
      throw new SSZException(error, e);
    }
  }

  private Bytes[] packContainerList(List<Object> values, SSZSchemeBuilder.SSZScheme.SSZField field,
                                    SSZSerializer sszSerializer) {
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

  public Function<Pair<BytesSSZReaderProxy, SSZSerializer>, Object> resolveDecodeFunction(SSZSchemeBuilder.SSZScheme.SSZField field) {
    throw new SSZException("Decode is not supported for hash");
  }

  private SSZCodec resolveCodec(SSZSchemeBuilder.SSZScheme.SSZField field) {
    if (registeredClassHandlers.containsKey(field.type)) {
      List<CodecEntry> codecs = registeredClassHandlers.get(field.type);
      if (field.extraType == null || field.extraType.isEmpty()) {
        return codecs.get(0).codec;
      } else {
        for (CodecEntry codecEntry : codecs) {
          if (codecEntry.types.contains(field.extraType)) {
            return codecEntry.codec;
          }
        }
      }
    }

    return null;
  }

  /**
   * Registers codecs to be used for
   * @param classes        Classes, resolving is performed with class at first
   * @param types          Text type, one class could be interpreted to several types.
   *                       Several codecs could handle one class.
   *                       Empty/null type is occupied by first class codec.
   *                       Type is looked up in codecs one by one.
   * @param codec          Codec able to encode/decode of specific class/types
   */
  public void registerCodec(Set<Class> classes, Set<String> types, SSZCodec codec) {
    for (Class clazz : classes) {
      if (registeredClassHandlers.get(clazz) != null) {
        registeredClassHandlers.get(clazz).add(new CodecEntry(codec, types));
      } else {
        registeredClassHandlers.put(clazz,
            new ArrayList<>(Collections.singletonList(new CodecEntry(codec, types))));
      }
    }
  }

  /**
   * Merkle tree hash of a list of homogenous, non-empty items
   * @param lst
   * @return
   */
  private Bytes merkle_hash(Bytes[] lst) {
    // Store length of list (to compensate for non-bijectiveness of padding)
    Bytes dataLen = SSZ.encodeInt32(lst.length);

    List<Bytes> chunkz = new ArrayList<>();
    // Handle empty list case
    if (dataLen.isZero()) {
      chunkz.add(EMPTY_CHUNK);
    } else if (lst[0].size() < SSZ_CHUNK_SIZE) {
      // See how many items fit in a chunk
      int itemsPerChunk = SSZ_CHUNK_SIZE / lst[0].size();
      // Build a list of chunks based on the number of items in the chunk
      for (int i = 0; i < lst.length; i += itemsPerChunk) {
        Bytes[] sliced = new Bytes[itemsPerChunk];
        System.arraycopy(lst, i, sliced, 0, itemsPerChunk);
        chunkz.add(Bytes.concatenate(sliced));
      }
    } else {
      // Leave large items alone
      chunkz.addAll(Arrays.asList(lst));
    }

    // Tree-hash
    while (chunkz.size() > 1) {
      if (chunkz.size() % 2 == 1) {
        chunkz.add(EMPTY_CHUNK);
      }
      List<Bytes> tempChunkz = new ArrayList<>();
      for (int i = 0; i < chunkz.size(); i += 2) {
        Bytes curChunk = hashFunction.apply(Bytes.concatenate(chunkz.get(i), chunkz.get(i+1)));
        tempChunkz.add(curChunk);
      }
      chunkz = tempChunkz;
    }

    return hashFunction.apply(Bytes.concatenate(chunkz.get(0), dataLen));
  }

  private Bytes hash_tree_root_list(Bytes[] lst) {
    Bytes[] res = new Bytes[lst.length];
    for (int i = 0; i < lst.length; ++i) {
      res[i] = hash_tree_root_element(lst[i]);
    }
    return merkle_hash(res);
  }

  private Bytes hash_tree_root_element(Bytes el) {
    if (el.size() < SSZ_CHUNK_SIZE) {
      return el;
    } else {
      return hashFunction.apply(el);
    }
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

package org.ethereum.beacon.ssz;

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
import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.ssz.BytesSSZReaderProxy;
import net.consensys.cava.ssz.SSZ;
import net.consensys.cava.ssz.SSZException;
import org.ethereum.beacon.ssz.type.SSZCodec;
import org.ethereum.beacon.ssz.type.SubclassCodec;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import tech.pegasys.artemis.util.bytes.BytesValue;

/**
 * Implementation of {@link SSZCodecResolver} which implements SSZ Hash function
 *
 * <p>For more info check <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/simple-serialize.md#tree-hash">SSZ
 * Tree hash</a>
 */
public class SSZCodecHasher implements SSZCodecResolver {

  static final int SSZ_CHUNK_SIZE = 32;

  private static final Bytes EMPTY_CHUNK = Bytes.of(new byte[SSZ_CHUNK_SIZE]);

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
            res.write(hash_tree_root_internal(Bytes.wrap(tmp.toByteArray())).toArrayUnsafe());
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
   * Merkle tree hash of a list of homogenous, non-empty items
   *
   * @param lst
   * @return
   */
  Bytes merkle_hash(Bytes[] lst) {
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
        int chunkLen = Math.min(itemsPerChunk, lst.length - i);
        Bytes[] lstSlice = new Bytes[chunkLen];
        System.arraycopy(lst, i, lstSlice, 0, chunkLen);
        Bytes chunkBeforePad = Bytes.concatenate(lstSlice);
        chunkz.add(zpad(chunkBeforePad, SSZ_CHUNK_SIZE));
      }
    } else {
      // Leave large items alone
      chunkz.addAll(Arrays.asList(lst));
    }

    // Merkleise
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

    return hashFunction.apply(Bytes.concatenate(chunkz.get(0), dataLen));
  }

  private long next_power_of_2(int x) {
    if (x == 0) {
      return 1;
    } else {
      return Double.valueOf(Math.pow(2, bit_length(x - 1))).longValue();
    }
  }

  private int bit_length(int val) {
    String bin = Integer.toBinaryString(val);
    for (int i = 0; i < bin.length(); ++i) {
      if (bin.charAt(i) != '0') {
        return bin.length() - i;
      }
    }

    return 0;
  }

  Bytes zpad(Bytes input, int length) {
    try {
    return Bytes.concatenate(input, Bytes.wrap(new byte[length - input.size()]));
    } catch (Exception ex) {
      System.out.println("");
      return null;
    }
  }

  private Bytes hash_tree_root_list(Bytes[] lst) {
    Bytes[] res = new Bytes[lst.length];
    for (int i = 0; i < lst.length; ++i) {
      res[i] = hash_tree_root_internal(lst[i]);
    }
    return merkle_hash(res);
  }

  Bytes hash_tree_root_internal(Bytes el) {
    if (el.size() <= SSZ_CHUNK_SIZE) {
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

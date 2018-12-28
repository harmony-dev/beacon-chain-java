package net.consensys.cava.ssz;

import javafx.util.Pair;
import net.consensys.cava.bytes.Bytes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static net.consensys.cava.ssz.SSZSerializer.LENGTH_PREFIX_BYTE_SIZE;
import static net.consensys.cava.ssz.SSZSerializer.SSZType.Type.ADDRESS;
import static net.consensys.cava.ssz.SSZSerializer.SSZType.Type.BIGINT;
import static net.consensys.cava.ssz.SSZSerializer.SSZType.Type.BOOLEAN;
import static net.consensys.cava.ssz.SSZSerializer.SSZType.Type.BYTES;
import static net.consensys.cava.ssz.SSZSerializer.SSZType.Type.CONTAINER;
import static net.consensys.cava.ssz.SSZSerializer.SSZType.Type.HASH;
import static net.consensys.cava.ssz.SSZSerializer.SSZType.Type.INT;
import static net.consensys.cava.ssz.SSZSerializer.SSZType.Type.LIST;
import static net.consensys.cava.ssz.SSZSerializer.SSZType.Type.LONG;
import static net.consensys.cava.ssz.SSZSerializer.SSZType.Type.STRING;
import static net.consensys.cava.ssz.SSZSchemeBuilder.SSZScheme.SSZField;

/**
 * <p>Used for deserialization of SSZ data (restoring instance of class)</p>
 * <p>For more information check {@link SSZSerializer}</p>
 */
public class SSZDecoder {
  private static Map<SSZSerializer.SSZType.Type, Function<DecodeInputBox, Object>> decodeHandlers
      = new HashMap<>();

  static {
    decodeHandlers.put(HASH, c -> c.reader.readHash(c.field.sszType.size).toArrayUnsafe());
    decodeHandlers.put(BYTES,  c -> (c.field.sszType.size == null)
        ? c.reader.readBytes().toArrayUnsafe()
        : c.reader.readBytes(c.field.sszType.size).toArrayUnsafe());
    decodeHandlers.put(ADDRESS,  c -> c.reader.readAddress().toArrayUnsafe());
    decodeHandlers.put(STRING,  c -> c.reader.readString());
    decodeHandlers.put(BOOLEAN,  c -> c.reader.readBoolean());
    decodeHandlers.put(INT,  c -> c.reader.readInt(c.field.sszType.size));
    decodeHandlers.put(LONG,  c -> c.reader.readLong(c.field.sszType.size));
    decodeHandlers.put(BIGINT,  c -> c.reader.readUnsignedBigInteger(c.field.sszType.size));
    decodeHandlers.put(LIST, SSZDecoder::decodeList);
    decodeHandlers.put(CONTAINER, SSZDecoder::decodeContainer);
  }

  private static Object decodeContainer(DecodeInputBox c) {
    return decodeContainerImpl(c).getKey();
  }

  private static Pair<Object, Integer> decodeContainerImpl(DecodeInputBox c) {
    BytesSSZReader reader = c.reader;
    Bytes data = reader.readBytes();
    int dataSize = data.size();

    if (c.field.skipContainer) {
      Bytes lengthPrefix = SSZ.encodeUInt32(dataSize);
      byte[] container = Bytes.concatenate(lengthPrefix, data).toArrayUnsafe();
      return new Pair<>(SSZSerializer.decode(container, c.field.type), dataSize);
    } else {
      return new Pair<>(SSZSerializer.decode(data.toArrayUnsafe(), c.field.type),
          dataSize + LENGTH_PREFIX_BYTE_SIZE);
    }
  }

  private static Object decodeList(DecodeInputBox c) {
    BytesSSZReader reader = c.reader;
    Integer size = c.field.sszType.size;

    switch (c.field.sszType.type) {
      case BYTES: {
        return reader.readByteArrayList();
      }
      case HASH: {
        checkSizePresence(c.field);
        return reader.readHashList(size);
      }
      case ADDRESS: {
        return reader.readAddressList();
      }
      case STRING: {
        return reader.readStringList();
      }
      case BOOLEAN: {
        return reader.readBooleanList();
      }
      case INT: {
        checkSizePresence(c.field);
        return reader.readIntList(size);
      }
      case LONG: {
        checkSizePresence(c.field);
        return reader.readLongIntList(size);
      }
      case BIGINT: {
        checkSizePresence(c.field);
        return reader.readUnsignedBigIntegerList(size);
      }
      case CONTAINER: {
        int remainingData = reader.readInt32();
        List<Object> res = new ArrayList<>();
        while (remainingData > 0) {
          Pair<Object, Integer> decodeRes = decodeContainerImpl(c);
          res.add(decodeRes.getKey());
          remainingData -= decodeRes.getValue();
        }
        return res;
      }
      default: {
        String error = String.format("Decoding of list with type %s is not implemented yet",
            c.field.sszType.type);
        throw new RuntimeException(error);
      }
    }
  }

  private static void checkSizePresence(SSZField field) {
    if (field.sszType.size == null) {
      String error = String.format("Size of data type %s is required for decoding",
          field.sszType.type);
      throw new RuntimeException(error);
    }
  }

  public static Object decodeField(SSZField field, BytesSSZReader reader) {
    Function<DecodeInputBox, Object> handler;
    if (field.type.equals(List.class)) {
      handler = decodeHandlers.get(LIST);
    } else {
      if (field.skipContainer != null) {
        handler = decodeHandlers.get(CONTAINER);
      } else {
        handler = decodeHandlers.get(field.sszType.type);
      }
    }

    return handler.apply(new DecodeInputBox(field, reader));
  }

  static class DecodeInputBox {
    SSZField field;
    BytesSSZReader reader;

    DecodeInputBox(SSZField field, BytesSSZReader reader) {
      this.field = field;
      this.reader = reader;
    }
  }
}

package org.ethereum.beacon.wire.impl.libp2p.encoding;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.CorruptedFrameException;

public class Util {

  /**
   * Encodes int as Protobuf varint
   * Copied from https://github.com/netty/netty/blob/00afb19d7a37de21b35ce4f6cb3fa7f74809f2ab/codec/src/main/java/io/netty/handler/codec/protobuf/ProtobufVarint32LengthFieldPrepender.java#L58
   */
  public static void writeRawVarint32(ByteBuf out, int value) {
    while (true) {
      if ((value & ~0x7F) == 0) {
        out.writeByte(value);
        return;
      } else {
        out.writeByte((value & 0x7F) | 0x80);
        value >>>= 7;
      }
    }
  }

  /**
   * Decodes Protobuf varint
   * Copied from: https://github.com/netty/netty/blob/00afb19d7a37de21b35ce4f6cb3fa7f74809f2ab/codec/src/main/java/io/netty/handler/codec/protobuf/ProtobufVarint32FrameDecoder.java#L73
   */
  public static int readRawVarint32(ByteBuf buffer) {
    if (!buffer.isReadable()) {
      return 0;
    }
    buffer.markReaderIndex();
    byte tmp = buffer.readByte();
    if (tmp >= 0) {
      return tmp;
    } else {
      int result = tmp & 127;
      if (!buffer.isReadable()) {
        buffer.resetReaderIndex();
        return 0;
      }
      if ((tmp = buffer.readByte()) >= 0) {
        result |= tmp << 7;
      } else {
        result |= (tmp & 127) << 7;
        if (!buffer.isReadable()) {
          buffer.resetReaderIndex();
          return 0;
        }
        if ((tmp = buffer.readByte()) >= 0) {
          result |= tmp << 14;
        } else {
          result |= (tmp & 127) << 14;
          if (!buffer.isReadable()) {
            buffer.resetReaderIndex();
            return 0;
          }
          if ((tmp = buffer.readByte()) >= 0) {
            result |= tmp << 21;
          } else {
            result |= (tmp & 127) << 21;
            if (!buffer.isReadable()) {
              buffer.resetReaderIndex();
              return 0;
            }
            result |= (tmp = buffer.readByte()) << 28;
            if (tmp < 0) {
              throw new CorruptedFrameException("malformed varint.");
            }
          }
        }
      }
      return result;
    }
  }
}

/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package net.consensys.cava.ssz;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.units.bigints.UInt256;
import java.math.BigInteger;
import java.util.List;

public class BytesSSZReaderProxy {
  private BytesSSZReader reader;

  public BytesSSZReaderProxy(Bytes bytes) {
    this(new BytesSSZReader(bytes));
  }

  private BytesSSZReaderProxy(BytesSSZReader reader) {
    this.reader = reader;
  }

  public Bytes readBytes() {
    return reader.readBytes();
  }

  public byte[] readByteArray() {
    return reader.readByteArray();
  }

  public byte[] readByteArray(int limit) {
    return reader.readByteArray(limit);
  }

  public String readString() {
    return reader.readString();
  }

  public String readString(int limit) {
    return reader.readString(limit);
  }

  public int readInt8() {
    return reader.readInt8();
  }

  public int readInt16() {
    return reader.readInt16();
  }

  public int readInt32() {
    return reader.readInt32();
  }

  public long readInt64() {
    return reader.readInt64();
  }

  public int readUInt(int bitLength) {
    return reader.readUInt(bitLength);
  }

  public long readULong(int bitLength) {
    return reader.readULong(bitLength);
  }

  public int readUInt8() {
    return reader.readUInt8();
  }

  public int readUInt16() {
    return reader.readUInt16();
  }

  public long readUInt32() {
    return reader.readUInt32();
  }

  public long readUInt64() {
    return reader.readUInt64();
  }

  public boolean readBoolean() {
    return reader.readBoolean();
  }

  public List<Bytes> readBytesList() {
    return reader.readBytesList();
  }

  public List<byte[]> readByteArrayList() {
    return reader.readByteArrayList();
  }

  public List<byte[]> readByteArrayList(int limit) {
    return reader.readByteArrayList(limit);
  }

  public List<String> readStringList() {
    return reader.readStringList();
  }

  public List<Integer> readInt8List() {
    return reader.readInt8List();
  }

  public List<Integer> readInt16List() {
    return reader.readInt16List();
  }

  public List<Integer> readInt32List() {
    return reader.readInt32List();
  }

  public List<Long> readInt64List() {
    return reader.readInt64List();
  }

  public List<Integer> readUIntList(int bitLength) {
    return reader.readUIntList(bitLength);
  }

  public List<Long> readULongIntList(int bitLength) {
    return reader.readULongIntList(bitLength);
  }

  public List<Integer> readUInt8List() {
    return reader.readUInt8List();
  }

  public List<Integer> readUInt16List() {
    return reader.readUInt16List();
  }

  public List<Long> readUInt32List() {
    return reader.readUInt32List();
  }

  public List<Long> readUInt64List() {
    return reader.readUInt64List();
  }

  public Bytes readBytes(int limit) {
    return reader.readBytes(limit);
  }

  public int readInt(int bitLength) {
    return reader.readInt(bitLength);
  }

  public long readLong(int bitLength) {
    return reader.readLong(bitLength);
  }

  public BigInteger readBigInteger(int bitLength) {
    return reader.readBigInteger(bitLength);
  }

  public BigInteger readUnsignedBigInteger(int bitLength) {
    return reader.readUnsignedBigInteger(bitLength);
  }

  public UInt256 readUInt256() {
    return reader.readUInt256();
  }

  public Bytes readAddress() {
    return reader.readAddress();
  }

  public Bytes readHash(int hashLength) {
    return reader.readHash(hashLength);
  }

  public List<Bytes> readBytesList(int limit) {
    return reader.readBytesList(limit);
  }

  public List<String> readStringList(int limit) {
    return reader.readStringList(limit);
  }

  public List<Integer> readIntList(int bitLength) {
    return reader.readIntList(bitLength);
  }

  public List<Long> readLongIntList(int bitLength) {
    return reader.readLongIntList(bitLength);
  }

  public List<BigInteger> readBigIntegerList(int bitLength) {
    return reader.readBigIntegerList(bitLength);
  }

  public List<BigInteger> readUnsignedBigIntegerList(int bitLength) {
    return reader.readUnsignedBigIntegerList(bitLength);
  }

  public List<UInt256> readUInt256List() {
    return reader.readUInt256List();
  }

  public List<Bytes> readAddressList() {
    return reader.readAddressList();
  }

  public List<Bytes> readHashList(int hashLength) {
    return reader.readHashList(hashLength);
  }

  public List<Boolean> readBooleanList() {
    return reader.readBooleanList();
  }

  public boolean isComplete() {
    return reader.isComplete();
  }
}

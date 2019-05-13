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
    // FIXME: uses length prefix
    return reader.readBytes();
  }

  public String readString() {
    // FIXME: uses length prefix
    return reader.readString();
  }

  public int readUInt(int bitLength) {
    return reader.readUInt(bitLength);
  }

  public long readULong(int bitLength) {
    return reader.readULong(bitLength);
  }

  public boolean readBoolean() {
    return reader.readBoolean();
  }

  public BigInteger readUnsignedBigInteger(int bitLength) {
    return reader.readUnsignedBigInteger(bitLength);
  }

  public Bytes readHash(int hashLength) {
    return reader.readHash(hashLength);
  }
}

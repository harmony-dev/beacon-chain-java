/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.ethereum.beacon.db.source.impl;

import org.ethereum.beacon.db.source.CodecSource;
import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.source.HoleyList;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.BytesValues;

import javax.annotation.Nonnull;
import java.util.AbstractList;
import java.util.Optional;
import java.util.function.Function;

/**
 * Stores List structure in Source structure
 */
public class DataSourceList<V> implements HoleyList<V> {
  private static final BytesValue SIZE_KEY = BytesValue.fromHexString("FFFFFFFFFFFFFFFF");

  private final DataSource<BytesValue, BytesValue> src;
  private final DataSource<BytesValue, V> valSsrc;
  private long size = -1;

  public DataSourceList(DataSource<BytesValue, BytesValue> src,
                        @Nonnull final Function<V, BytesValue> valueCoder,
                        @Nonnull final Function<BytesValue, V> valueDecoder) {
    this.src = src;
    valSsrc = new CodecSource.ValueOnly<>(src, valueCoder, valueDecoder);
  }

  @Override
  public void put(long idx, V value) {
    if (value == null) return;
    if (idx >= size()) {
      setSize(idx + 1);
    }
    valSsrc.put(BytesValues.toMinimalBytes(idx), value);
  }

  @Override
  public Optional<V> get(long idx) {
    if (idx < 0 || idx >= size()) return Optional.empty();
    return valSsrc.get(BytesValues.toMinimalBytes(idx));
  }

  @Override
  public long size() {
    if (size < 0) {
      size = src.get(SIZE_KEY).map(BytesValues::extractLong).orElse(0L);
    }
    return size;
  }

  private void setSize(long newSize) {
    size = newSize;
    src.put(SIZE_KEY, BytesValues.toMinimalBytes(newSize));
  }
}

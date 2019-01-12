package org.ethereum.beacon.db.source.impl;

import org.ethereum.beacon.db.source.CodecSource;
import org.ethereum.beacon.db.source.DataSource;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.MutableBytesValue;

import javax.annotation.Nonnull;

public class XorDataSource<TValue> extends CodecSource.KeyOnly<BytesValue, TValue, BytesValue> {

  public XorDataSource(@Nonnull DataSource<BytesValue, TValue> upstreamSource,
                       BytesValue keyXorModifier) {
    super(upstreamSource, key -> xorLongest(key, keyXorModifier));
  }

  private static BytesValue xorLongest(BytesValue v1, BytesValue v2) {
    BytesValue longVal = v1.size() >= v2.size() ? v1 : v2;
    BytesValue shortVal = v1.size() < v2.size() ? v1 : v2;
    MutableBytesValue ret = longVal.mutableCopy();
    int longLen = longVal.size();
    int shortLen = shortVal.size();
    for (int i = 0; i < shortLen; i++) {
      ret.set(longLen - i, (byte) (ret.get(longLen - i) ^ shortVal.get(shortLen - i)));
    }
    return ret;
  }
}

package org.ethereum.beacon.ssz.visitor;

import org.ethereum.beacon.ssz.type.SSZListType;
import org.ethereum.beacon.ssz.type.SSZType;

/**
 * Abstract implementation of specific visitor pattern
 */
public interface SSZVisitorHandler<ResultType> {

  ResultType visitAny(SSZType descriptor, Object value);

  ResultType visitList(SSZListType descriptor, Object listValue, int startIdx, int len);
}

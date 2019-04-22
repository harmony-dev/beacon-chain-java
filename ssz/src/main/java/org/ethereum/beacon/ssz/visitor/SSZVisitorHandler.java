package org.ethereum.beacon.ssz.visitor;

import org.ethereum.beacon.ssz.type.SSZType;

/**
 * Abstract implementation of specific visitor pattern
 */
public interface SSZVisitorHandler<ResultType> {

  ResultType visitAny(SSZType descriptor, Object value);
}

package org.ethereum.beacon.discovery.mock;

import org.ethereum.beacon.discovery.enr.EnrSchemeV4Interpreter;
import org.ethereum.beacon.discovery.enr.NodeRecord;

public class EnrSchemeV4InterpreterMock extends EnrSchemeV4Interpreter {
  @Override
  public void verify(NodeRecord nodeRecord) {
    // Don't verify ECDSA
  }
}

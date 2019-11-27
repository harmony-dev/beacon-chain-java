package org.ethereum.beacon.discovery.mock;

import org.ethereum.beacon.discovery.enr.IdentitySchemaV4Interpreter;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import tech.pegasys.artemis.util.bytes.Bytes96;

public class IdentitySchemaV4InterpreterMock extends IdentitySchemaV4Interpreter {
  @Override
  public void verify(NodeRecord nodeRecord) {
    // Don't verify signature
  }

  @Override
  public void sign(NodeRecord nodeRecord, Object signOptions) {
    nodeRecord.setSignature(Bytes96.ZERO);
  }
}

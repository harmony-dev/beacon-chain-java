package org.ethereum.beacon.discovery.packet;

import org.ethereum.beacon.crypto.Hashes;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes32s;
import tech.pegasys.artemis.util.bytes.BytesValue;

/** Default packet form until its goal is known */
public class UnknownPacket extends AbstractPacket {
  private static final int MAX_SIZE = 1280;

  public UnknownPacket(BytesValue bytes) {
    super(bytes);
  }

  public MessagePacket getMessagePacket() {
    return new MessagePacket(getBytes());
  }

  public AuthHeaderMessagePacket getAuthHeaderMessagePacket() {
    return new AuthHeaderMessagePacket(getBytes());
  }

  public RandomPacket getRandomPacket() {
    return new RandomPacket(getBytes());
  }

  public WhoAreYouPacket getWhoAreYouPacket() {
    return new WhoAreYouPacket(getBytes());
  }

  public boolean isWhoAreYouPacket(Bytes32 destNodeId) {
    return WhoAreYouPacket.getStartMagic(destNodeId).equals(getBytes().slice(0, 32));
  }

  // tag              = xor(sha256(dest-node-id), src-node-id)
  // dest-node-id     = 32-byte node ID of B
  // src-node-id      = 32-byte node ID of A
  //
  // The recipient can recover the sender's ID by performing the same calculation in reverse.
  //
  // src-node-id      = xor(sha256(dest-node-id), tag)
  public Bytes32 getSourceNodeId(Bytes32 destNodeId) {
    assert !isWhoAreYouPacket(destNodeId);
    BytesValue xorTag = getBytes().slice(0, 32);
    return Bytes32s.xor(Hashes.sha256(destNodeId), Bytes32.wrap(xorTag, 0));
  }

  public void verify() {
    if (getBytes().size() > MAX_SIZE) {
      throw new RuntimeException(String.format("Packets should not exceed %s bytes", MAX_SIZE));
    }
  }

  @Override
  public String toString() {
    return "UnknownPacket{"
        + (getBytes().size() < 200
            ? getBytes()
            : getBytes().slice(0, 190) + "..." + "(" + getBytes().size() + " bytes)")
        + "}";
  }
}

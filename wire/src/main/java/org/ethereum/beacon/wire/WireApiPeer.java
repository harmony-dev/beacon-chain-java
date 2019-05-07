package org.ethereum.beacon.wire;

import org.ethereum.beacon.wire.message.payload.GoodbyeMessage;
import org.ethereum.beacon.wire.message.payload.HelloMessage;

public interface WireApiPeer {

  void hello(HelloMessage message);

  void goodbye(GoodbyeMessage message);

}

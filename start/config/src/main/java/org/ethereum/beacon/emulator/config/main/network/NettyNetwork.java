package org.ethereum.beacon.emulator.config.main.network;

import java.util.List;

public class NettyNetwork extends Network {
  private Integer listenPort;
  private List<String> activePeers;

  public Integer getListenPort() {
    return listenPort;
  }

  public void setListenPort(Integer listenPort) {
    this.listenPort = listenPort;
  }

  public List<String> getActivePeers() {
    return activePeers;
  }

  public void setActivePeers(List<String> activePeers) {
    this.activePeers = activePeers;
  }
}

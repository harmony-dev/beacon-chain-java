package org.ethereum.beacon.emulator.config.main.network;

import java.util.ArrayList;
import java.util.List;

public class Libp2pNetwork extends Network {

  public static class GossipOptions {
    Integer gossipD;
    Integer gossipDLow;
    Integer gossipDHigh;
    Integer gossipDLazy;
    Integer fanoutTTL;
    Integer gossipAdvertise;
    Integer gossipHistory;
    Integer heartbeatIntervalMillis;

    public Integer getGossipD() {
      return gossipD;
    }

    public void setGossipD(Integer gossipD) {
      this.gossipD = gossipD;
    }

    public Integer getGossipDLow() {
      return gossipDLow;
    }

    public void setGossipDLow(Integer gossipDLow) {
      this.gossipDLow = gossipDLow;
    }

    public Integer getGossipDHigh() {
      return gossipDHigh;
    }

    public void setGossipDHigh(Integer gossipDHigh) {
      this.gossipDHigh = gossipDHigh;
    }

    public Integer getGossipDLazy() {
      return gossipDLazy;
    }

    public void setGossipDLazy(Integer gossipDLazy) {
      this.gossipDLazy = gossipDLazy;
    }

    public Integer getFanoutTTL() {
      return fanoutTTL;
    }

    public void setFanoutTTL(Integer fanoutTTL) {
      this.fanoutTTL = fanoutTTL;
    }

    public Integer getGossipAdvertise() {
      return gossipAdvertise;
    }

    public void setGossipAdvertise(Integer gossipAdvertise) {
      this.gossipAdvertise = gossipAdvertise;
    }

    public Integer getGossipHistory() {
      return gossipHistory;
    }

    public void setGossipHistory(Integer gossipHistory) {
      this.gossipHistory = gossipHistory;
    }

    public Integer getHeartbeatIntervalMillis() {
      return heartbeatIntervalMillis;
    }

    public void setHeartbeatIntervalMillis(Integer heartbeatIntervalMillis) {
      this.heartbeatIntervalMillis = heartbeatIntervalMillis;
    }
  }

  private Integer listenPort;
  private String privateKey;
  private List<String> activePeers = new ArrayList<>();
  private GossipOptions gossipOptions;

  public Integer getListenPort() {
    return listenPort;
  }

  public void setListenPort(Integer listenPort) {
    this.listenPort = listenPort;
  }

  public List<String> getActivePeers() {
    return activePeers;
  }

  public void setActivePeers(
      List<String> activePeers) {
    this.activePeers = activePeers;
  }

  public GossipOptions getGossipOptions() {
    return gossipOptions;
  }

  public void setGossipOptions(
      GossipOptions gossipOptions) {
    this.gossipOptions = gossipOptions;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public void setPrivateKey(String privateKey) {
    this.privateKey = privateKey;
  }
}

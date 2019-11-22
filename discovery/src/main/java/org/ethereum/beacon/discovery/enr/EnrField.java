package org.ethereum.beacon.discovery.enr;

public interface EnrField {
  // Schema id
  String ID = "id";
  // IPv4 address
  String IP_V4 = "ip";
  // TCP port, integer
  String TCP_V4 = "tcp";
  // UDP port, integer
  String UDP_V4 = "udp";
  // IPv6 address
  String IP_V6 = "ip6";
  // IPv6-specific TCP port
  String TCP_V6 = "tcp6";
  // IPv6-specific UDP port
  String UDP_V6 = "udp6";
}

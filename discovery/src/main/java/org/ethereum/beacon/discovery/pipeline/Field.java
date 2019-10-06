package org.ethereum.beacon.discovery.pipeline;

public enum Field {
  NEED_CONTEXT, // Node id, requests context resolving
  CONTEXT, // Node context
  INCOMING, // Raw incoming data
  PACKET_UNKNOWN, // Unknown packet
  PACKET_WHOAREYOU, // WhoAreYou packet
  PACKET_AUTH_HEADER_MESSAGE, // Auth header message packet
  PACKET_MESSAGE, // Standard message packet
  MESSAGE, // Message extracted from the packet
  NODE, // Sender/recipient node
  BAD_PACKET, // Bad, rejected packet
  BAD_PACKET_EXCEPTION, // Stores exception for bad packet
  TASK, // Task to perform
  FUTURE, // Completable future

}

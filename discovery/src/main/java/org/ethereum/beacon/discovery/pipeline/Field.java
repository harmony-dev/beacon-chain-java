package org.ethereum.beacon.discovery.pipeline;

public enum Field {
  SESSION_LOOKUP, // Node id, requests session lookup
  SESSION, // Node session
  INCOMING, // Raw incoming data
  PACKET_UNKNOWN, // Unknown packet
  PACKET_WHOAREYOU, // WhoAreYou packet
  PACKET_AUTH_HEADER_MESSAGE, // Auth header message packet
  PACKET_MESSAGE, // Standard message packet
  MESSAGE, // Message extracted from the packet
  NODE, // Sender/recipient node
  BAD_PACKET, // Bad, rejected packet
  BAD_MESSAGE, // Bad, rejected message
  BAD_EXCEPTION, // Stores exception for bad packet or message
  TASK, // Task to perform
  TASK_OPTIONS, // Task options
  FUTURE, // Completable future
}

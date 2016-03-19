@echo off

java -cp pcap4j-core.jar;pcap4j-packetfactory-propertiesbased.jar;jna.jar;slf4j-api.jar;..\target\zundoko-protocol-0.0.1-SNAPSHOT.jar com.github.kaitoy.zundoko.protocol.ZundokoServer

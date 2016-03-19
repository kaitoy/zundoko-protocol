package com.github.kaitoy.zundoko.protocol;

import java.io.IOException;
import java.net.UnknownHostException;
import org.pcap4j.core.BpfProgram.BpfCompileMode;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.Packet.Builder;
import org.pcap4j.util.MacAddress;
import org.pcap4j.util.NifSelector;

/**
 * @author Kaito Yamada
 */
public final class ZundokoServer {

  /**
   *
   * @param args
   * @throws PcapNativeException
   * @throws NotOpenException
   * @throws UnknownHostException
   */
  public static void main(
    String[] args
  ) throws PcapNativeException, NotOpenException, UnknownHostException {
    PcapNetworkInterface nif;
    try {
      nif = new NifSelector().selectNetworkInterface();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    if (nif == null) {
      return;
    }
    System.out.println(nif.getName() + "(" + nif.getDescription() + ")");

    System.setProperty(
      "org.pcap4j.packet.Packet.classFor.org.pcap4j.packet.namednumber.EtherType."
        + ZundokoPacket.ETHER_TYPE.valueAsString(),
      "com.github.kaitoy.zundoko.protocol.ZundokoPacket"
    );

    new ZundokoServer().start(nif);
  }

  /**
   * Starts this server.
   * @param nif
   * @throws PcapNativeException
   * @throws NotOpenException
   */
  public void start(PcapNetworkInterface nif) throws PcapNativeException, NotOpenException {
    PcapHandle handle = nif.openLive(65536, PromiscuousMode.PROMISCUOUS, 10);
    MacAddress addr = (MacAddress)nif.getLinkLayerAddresses().get(0);
    handle.setFilter("ether dst " + Pcaps.toBpfString(addr), BpfCompileMode.OPTIMIZE);

    try {
      handle.loop(-1, new ZundokoHandler(handle));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * @author Kaito Yamada
   */
  private static final class ZundokoHandler implements PacketListener {

    private final PcapHandle ph;
    private volatile int zunCounter;

    ZundokoHandler(PcapHandle ph) {
      this.ph = ph;
    }

    public void gotPacket(Packet packet) {
      ZundokoPacket zp = packet.get(ZundokoPacket.class);
      if (zp == null) {
        return;
      }

      String zundoko = zp.getHeader().getZundoko();
      System.out.println(zundoko);
      synchronized(this) {
        if (zundoko.equals("ズン")) {
          zunCounter++;
        }
        else if (zundoko.equals("ドコ")) {
          if (zunCounter >= 4) {
            Builder b = packet.getBuilder();

            EthernetPacket ep = packet.get(EthernetPacket.class);
            EthernetPacket.Builder eb = b.get(EthernetPacket.Builder.class);
            eb.dstAddr(ep.getHeader().getSrcAddr())
              .srcAddr(ep.getHeader().getDstAddr())
              .paddingAtBuild(true);

            ZundokoPacket.Builder zb = b.get(ZundokoPacket.Builder.class);
            zb.zundoko("キ・ヨ・シ！");

            try {
              System.out.println("キ・ヨ・シ！");
              ph.sendPacket(b.build());
            } catch (PcapNativeException e) {
              e.printStackTrace();
            } catch (NotOpenException e) {
              e.printStackTrace();
            }
          }
          zunCounter = 0;
        }
        else {
          zunCounter = 0;
        }
      }
    }

  }

}

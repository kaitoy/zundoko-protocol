package com.github.kaitoy.zundoko.protocol;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import org.pcap4j.util.MacAddress;
import org.pcap4j.util.NifSelector;

/**
 * @author Kaito Yamada
 */
public final class ZundokoClient {

  /**
   * Usage: com.github.kaitoy.zundoko.client.Client <Mac addr>
   * @param args
   * @throws InterruptedException
   * @throws PcapNativeException
   * @throws NotOpenException
   */
  public static void main(
    String[] args
  ) throws InterruptedException, PcapNativeException, NotOpenException {
    if (args.length != 1) {
      System.out.println("Usage: com.github.kaitoy.zundoko.Client <Dst Mac addr>");
      return;
    }
    MacAddress addr = MacAddress.getByName(args[0]);

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

    new ZundokoClient().start(nif, addr);
  }

  /**
   * Starts this client.
   *
   * @param nif
   * @param dstAddr
   * @throws InterruptedException
   * @throws PcapNativeException
   * @throws NotOpenException
   */
  public void start(
    PcapNetworkInterface nif,
    MacAddress dstAddr
  ) throws InterruptedException, PcapNativeException, NotOpenException {
    PcapHandle handle = nif.openLive(65536, PromiscuousMode.PROMISCUOUS, 10);
    MacAddress srcAddr = (MacAddress)nif.getLinkLayerAddresses().get(0);
    handle.setFilter("ether dst " + Pcaps.toBpfString(srcAddr), BpfCompileMode.OPTIMIZE);

    ExecutorService pool = Executors.newSingleThreadExecutor();
    Task t = new Task(handle, new ZundokoHandler());
    pool.execute(t);

    ZundokoPacket.Builder zb = new ZundokoPacket.Builder();
    EthernetPacket.Builder eb = new EthernetPacket.Builder();
    eb.dstAddr(dstAddr)
      .srcAddr(srcAddr)
      .type(ZundokoPacket.ETHER_TYPE)
      .payloadBuilder(zb)
      .paddingAtBuild(true);

    while (true) {
      String zundoko;
      if (Math.random() >= 0.4) {
        zundoko = "ズン";
      }
      else {
        zundoko = "ドコ";
      }
      zb.zundoko(zundoko);

      System.out.println(zundoko);
      handle.sendPacket(eb.build());
      Thread.sleep(1000);
    }
  }

  private static final class ZundokoHandler implements PacketListener {

    public void gotPacket(Packet packet) {
      ZundokoPacket zp = packet.get(ZundokoPacket.class);
      if (zp == null) {
        return;
      }

      String zundoko = zp.getHeader().getZundoko();
      System.out.println(zundoko);
    }

  }

  private static final class Task implements Runnable {

    private PcapHandle handle;
    private PacketListener listener;

    public Task(PcapHandle handle, PacketListener listener) {
      this.handle = handle;
      this.listener = listener;
    }

    public void run() {
      try {
        handle.loop(-1, listener);
      } catch (PcapNativeException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (NotOpenException e) {
        e.printStackTrace();
      }
    }

  }

}

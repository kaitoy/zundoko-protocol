package com.github.kaitoy.zundoko.protocol;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.pcap4j.packet.AbstractPacket;
import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.namednumber.EtherType;
import org.pcap4j.util.ByteArrays;

/**
 * @author Kaito Yamada
 */
public class ZundokoPacket extends AbstractPacket {

  /**
   *
   */
  private static final long serialVersionUID = 6756412180335244379L;

  public static final EtherType ETHER_TYPE = new EtherType((short)0x01FF, "Zundoko");

  static {
    EtherType.register(ETHER_TYPE);
  }

  private final ZundokoHeader header;

  /**
   * A static factory method.
   * This method validates the arguments by {@link ByteArrays#validateBounds(byte[], int, int)},
   * which may throw exceptions undocumented here.
   *
   * @param rawData rawData
   * @param offset offset
   * @param length length
   * @return a new ZundokoPacket object.
   * @throws IllegalRawDataException if parsing the raw data fails.
   */
  public static ZundokoPacket newPacket(
    byte[] rawData, int offset, int length
  ) throws IllegalRawDataException {
    ByteArrays.validateBounds(rawData, offset, length);
    return new ZundokoPacket(rawData, offset, length);
  }

  private ZundokoPacket(byte[] rawData, int offset, int length) throws IllegalRawDataException {
    this.header = new ZundokoHeader(rawData, offset, length);
  }

  private ZundokoPacket(Builder builder) {
    if (builder == null || builder.zundoko == null) {
      StringBuilder sb = new StringBuilder();
      sb.append("builder: ").append(builder)
        .append(" builder.zundoko: ").append(builder.zundoko);
      throw new NullPointerException(sb.toString());
    }

    this.header = new ZundokoHeader(builder);
  }

  @Override
  public ZundokoHeader getHeader() {
    return header;
  }

  @Override
  public Builder getBuilder() {
    return new Builder(this);
  }

  /**
   * @author Kaito Yamada
   */
  public static final class Builder extends AbstractBuilder {

    private String zundoko;

    /**
     *
     */
    public Builder() {}

    private Builder(ZundokoPacket packet) {
      this.zundoko = packet.header.zundoko;
    }

    public Builder zundoko(String zundoko) {
      this.zundoko = zundoko;
      return this;
    }

    @Override
    public ZundokoPacket build() {
      return new ZundokoPacket(this);
    }

  }

  /**
   * @author Kaito Yamada
   */
  public static final class ZundokoHeader extends AbstractHeader {

    /*
     *  0                            15                              31
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |             zundoko (null-terminated string)                  |
     * |                                                               |
     * |                                                               |
     * |                                                               |
     * |                                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *
     */

    /**
     *
     */
    private static final long serialVersionUID = 647053523582039050L;

    private static final int ZUNDOKO_HEADER_SIZE = 20;

    private final String zundoko;

    private ZundokoHeader(
      byte[] rawData, int offset, int length
    ) throws IllegalRawDataException {
      if (length < ZUNDOKO_HEADER_SIZE) {
        StringBuilder sb = new StringBuilder(110);
        sb.append("The data is too short to build a Zundoko header. ")
          .append("It must be at least ")
          .append(ZUNDOKO_HEADER_SIZE)
          .append(" bytes. data: ")
          .append(ByteArrays.toHexString(rawData, " "))
          .append(", offset: ")
          .append(offset)
          .append(", length: ")
          .append(length);
        throw new IllegalRawDataException(sb.toString());
      }

      int i;
      for (i = 0; i < ZUNDOKO_HEADER_SIZE; i++) {
        if (rawData[offset + i] == (byte)0) {
          break;
        }
      }
      if (i == ZUNDOKO_HEADER_SIZE) {
        StringBuilder sb = new StringBuilder(110);
        sb.append("zundoko must be null-terminated. data: ")
          .append(ByteArrays.toHexString(rawData, " "))
          .append(", offset: ")
          .append(offset)
          .append(", length: ")
          .append(length);
        throw new IllegalRawDataException(sb.toString());
      }

      if (i == 0) {
        this.zundoko = "";
      }
      else {
        try {
          this.zundoko = new String(rawData, offset, i, "UTF-8");
        } catch (UnsupportedEncodingException e) {
          throw new AssertionError("Never get here.");
        }
      }
    }

    private ZundokoHeader(Builder builder) {
      try {
        if (builder.zundoko.getBytes("UTF-8").length >= ZUNDOKO_HEADER_SIZE) {
          throw new IllegalArgumentException(
                  "Too long zundoko. builder.zundoko: " + builder.zundoko
                );
        }
      } catch (UnsupportedEncodingException e) {
        throw new AssertionError("Never get here.");
      }
      this.zundoko = builder.zundoko;
    }

    /**
     *
     * @return zundoko
     */
    public String getZundoko() {
      return zundoko;
    }

    @Override
    protected List<byte[]> getRawFields() {
      try {
        List<byte[]> rawFields = new ArrayList<byte[]>();
        byte[] tmp = new byte[ZUNDOKO_HEADER_SIZE];
        if (zundoko.length() > 0) {
          byte[] zundokoBytes = zundoko.getBytes("UTF-8");
          System.arraycopy(zundokoBytes, 0, tmp, 0, zundokoBytes.length);
        }
        rawFields.add(tmp);
        return rawFields;
      } catch (UnsupportedEncodingException e) {
        throw new AssertionError("Never get here.");
      }
    }

    @Override
    public int length() { return ZUNDOKO_HEADER_SIZE; }

    @Override
    protected String buildString() {
      StringBuilder sb = new StringBuilder();
      String ls = System.getProperty("line.separator");

      sb.append("[Zundoko Header (")
        .append(length())
        .append(" bytes)]")
        .append(ls);
      sb.append("  Zundoko: ")
        .append(zundoko)
        .append(ls);

      return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) { return true; }
      if (!this.getClass().isInstance(obj)) { return false; }

      ZundokoHeader other = (ZundokoHeader)obj;
      return zundoko.equals(other.zundoko);
    }

    @Override
    protected int calcHashCode() {
      int result = 17;
      result = 31 * result + zundoko.hashCode();
      return result;
    }

  }

}

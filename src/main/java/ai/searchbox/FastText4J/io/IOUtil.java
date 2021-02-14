package ai.searchbox.FastText4J.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class IOUtil {
  private int string_buf_size_ = 128;
  
  private byte[] bool_bytes_ = new byte[1];
  
  private byte[] int_bytes_ = new byte[4];
  
  private byte[] long_bytes_ = new byte[8];
  
  private byte[] float_bytes_ = new byte[4];
  
  private byte[] double_bytes_ = new byte[8];
  
  private byte[] string_bytes_ = new byte[this.string_buf_size_];
  
  private StringBuilder stringBuilder_ = new StringBuilder();
  
  private ByteBuffer float_array_bytebuffer_ = null;
  
  private byte[] float_array_bytes_ = null;
  
  public void setStringBufferSize(int size) {
    this.string_buf_size_ = size;
    this.string_bytes_ = new byte[this.string_buf_size_];
  }
  
  public void setFloatArrayBufferSize(int itemSize) {
    this.float_array_bytebuffer_ = ByteBuffer.allocate(itemSize * 4).order(ByteOrder.LITTLE_ENDIAN);
    this.float_array_bytes_ = new byte[itemSize * 4];
  }
  
  public byte readByte(InputStream is) throws IOException {
    return (byte)is.read();
  }
  
  public int readByteAsInt(InputStream is) throws IOException {
    return readByte(is) & 0xFF;
  }
  
  public boolean readBool(InputStream is) throws IOException {
    int ch = readByte(is);
    return (ch != 0);
  }
  
  public int readInt(InputStream is) throws IOException {
    is.read(this.int_bytes_);
    return getInt(this.int_bytes_);
  }
  
  public int getInt(byte[] b) {
    return (b[0] & 0xFF) << 0 | (b[1] & 0xFF) << 8 | (b[2] & 0xFF) << 16 | (b[3] & 0xFF) << 24;
  }
  
  public long readLong(InputStream is) throws IOException {
    is.read(this.long_bytes_);
    return getLong(this.long_bytes_);
  }
  
  public long getLong(byte[] b) {
    return (b[0] & 0xFFL) << 0L | (b[1] & 0xFFL) << 8L | (
      b[2] & 0xFFL) << 16L | (b[3] & 0xFFL) << 24L | (
      b[4] & 0xFFL) << 32L | (b[5] & 0xFFL) << 40L | (
      b[6] & 0xFFL) << 48L | (b[7] & 0xFFL) << 56L;
  }
  
  public float readFloat(InputStream is) throws IOException {
    is.read(this.float_bytes_);
    return getFloat(this.float_bytes_);
  }
  
  public void readFloat(InputStream is, float[] data) throws IOException {
    is.read(this.float_array_bytes_);
    this.float_array_bytebuffer_.clear();
    ((ByteBuffer)this.float_array_bytebuffer_.put(this.float_array_bytes_).flip()).asFloatBuffer().get(data);
  }
  
  public float getFloat(byte[] b) {
    return 
      Float.intBitsToFloat((b[0] & 0xFF) << 0 | (b[1] & 0xFF) << 8 | (b[2] & 0xFF) << 16 | (b[3] & 0xFF) << 24);
  }
  
  public double readDouble(InputStream is) throws IOException {
    is.read(this.double_bytes_);
    return getDouble(this.double_bytes_);
  }
  
  public double getDouble(byte[] b) {
    return Double.longBitsToDouble(getLong(b));
  }
  
  public String readString(InputStream is) throws IOException {
    int b = is.read();
    if (b < 0)
      return null; 
    int i = -1;
    this.stringBuilder_.setLength(0);
    while (b > -1 && b != 32 && b != 10 && b != 0) {
      this.string_bytes_[++i] = (byte)b;
      b = is.read();
      if (i == this.string_buf_size_ - 1) {
        this.stringBuilder_.append(new String(this.string_bytes_));
        i = -1;
      } 
    } 
    this.stringBuilder_.append(new String(this.string_bytes_, 0, i + 1));
    return this.stringBuilder_.toString();
  }
  
  public byte intToByte(int i) {
    return (byte)(i & 0xFF);
  }
  
  public byte[] intToByteArray(int i) {
    this.int_bytes_[0] = (byte)(i >> 0 & 0xFF);
    this.int_bytes_[1] = (byte)(i >> 8 & 0xFF);
    this.int_bytes_[2] = (byte)(i >> 16 & 0xFF);
    this.int_bytes_[3] = (byte)(i >> 24 & 0xFF);
    return this.int_bytes_;
  }
  
  public byte[] longToByteArray(long l) {
    this.long_bytes_[0] = (byte)(int)(l >> 0L & 0xFFL);
    this.long_bytes_[1] = (byte)(int)(l >> 8L & 0xFFL);
    this.long_bytes_[2] = (byte)(int)(l >> 16L & 0xFFL);
    this.long_bytes_[3] = (byte)(int)(l >> 24L & 0xFFL);
    this.long_bytes_[4] = (byte)(int)(l >> 32L & 0xFFL);
    this.long_bytes_[5] = (byte)(int)(l >> 40L & 0xFFL);
    this.long_bytes_[6] = (byte)(int)(l >> 48L & 0xFFL);
    this.long_bytes_[7] = (byte)(int)(l >> 56L & 0xFFL);
    return this.long_bytes_;
  }
  
  public byte[] floatToByteArray(float f) {
    return intToByteArray(Float.floatToIntBits(f));
  }
  
  public byte[] floatToByteArray(float[] f) {
    this.float_array_bytebuffer_.clear();
    this.float_array_bytebuffer_.asFloatBuffer().put(f);
    return this.float_array_bytebuffer_.array();
  }
  
  public byte[] doubleToByteArray(double d) {
    return longToByteArray(Double.doubleToRawLongBits(d));
  }
  
  public byte[] booleanToByteArray(boolean b) {
    return new byte[] { (byte)(b ? 1 : 0) };
  }
}

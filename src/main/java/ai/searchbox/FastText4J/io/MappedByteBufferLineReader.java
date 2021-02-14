package ai.searchbox.FastText4J.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class MappedByteBufferLineReader extends LineReader {
  private static int DEFAULT_BUFFER_SIZE = 1024;
  
  private volatile ByteBuffer byteBuffer_ = null;
  
  private RandomAccessFile raf_ = null;
  
  private FileChannel channel_ = null;
  
  private byte[] bytes_ = null;
  
  private int string_buf_size_ = DEFAULT_BUFFER_SIZE;
  
  private boolean fillLine_ = false;
  
  private StringBuilder sb_ = new StringBuilder();
  
  private List<String> tokens_ = new ArrayList<>();
  
  public MappedByteBufferLineReader(String filename, String charsetName) throws IOException, UnsupportedEncodingException {
    super(filename, charsetName);
    this.raf_ = new RandomAccessFile(this.file_, "r");
    this.channel_ = this.raf_.getChannel();
    this.byteBuffer_ = this.channel_.map(FileChannel.MapMode.READ_ONLY, 0L, this.channel_.size());
    this.bytes_ = new byte[this.string_buf_size_];
  }
  
  public MappedByteBufferLineReader(InputStream inputStream, String charsetName) throws UnsupportedEncodingException {
    this(inputStream, charsetName, DEFAULT_BUFFER_SIZE);
  }
  
  public MappedByteBufferLineReader(InputStream inputStream, String charsetName, int buf_size) throws UnsupportedEncodingException {
    super((inputStream instanceof BufferedInputStream) ? inputStream : new BufferedInputStream(inputStream), charsetName);
    this.string_buf_size_ = buf_size;
    this.byteBuffer_ = ByteBuffer.allocateDirect(this.string_buf_size_);
    this.bytes_ = new byte[this.string_buf_size_];
    if (inputStream == System.in)
      this.fillLine_ = true; 
  }
  
  public long skipLine(long n) throws IOException {
    if (n < 0L)
      throw new IllegalArgumentException("skip value is negative"); 
    long currentLine = 0L;
    long readLine = 0L;
    synchronized (this.lock) {
      ensureOpen();
      String line;
      while (currentLine < n && (line = getLine()) != null) {
        readLine++;
        if (line == null || line.isEmpty() || line.startsWith("#"))
          continue; 
        currentLine++;
      } 
    } 
    return readLine;
  }
  
  public String readLine() throws IOException {
    synchronized (this.lock) {
      ensureOpen();
      String lineString = getLine();
      while (lineString != null && (lineString.isEmpty() || lineString.startsWith("#")))
        lineString = getLine(); 
      return lineString;
    } 
  }
  
  public String[] readLineTokens() throws IOException {
    synchronized (this.lock) {
      ensureOpen();
      String[] tokens = getLineTokens();
      while (tokens != null && ((tokens.length == 1 && tokens[0].isEmpty()) || tokens[0].startsWith("#")))
        tokens = getLineTokens(); 
      return tokens;
    } 
  }
  
  public void rewind() throws IOException {
    synchronized (this.lock) {
      ensureOpen();
      if (this.raf_ != null) {
        this.raf_.seek(0L);
        this.channel_.position(0L);
      } 
      this.byteBuffer_.position(0);
    } 
  }
  
  public int read(char[] cbuf, int off, int len) throws IOException {
    synchronized (this.lock) {
      ensureOpen();
      if (off < 0 || off > cbuf.length || len < 0 || off + len > cbuf.length || off + len < 0)
        throw new IndexOutOfBoundsException(); 
      if (len == 0)
        return 0; 
      CharBuffer charBuffer = this.byteBuffer_.asCharBuffer();
      int length = Math.min(len, charBuffer.remaining());
      charBuffer.get(cbuf, off, length);
      if (this.inputStream_ != null) {
        off += length;
        while (off < len) {
          fillByteBuffer();
          if (!this.byteBuffer_.hasRemaining())
            break; 
          charBuffer = this.byteBuffer_.asCharBuffer();
          length = Math.min(len, charBuffer.remaining());
          charBuffer.get(cbuf, off, length);
          off += length;
        } 
      } 
      return (length == len) ? len : -1;
    } 
  }
  
  public void close() throws IOException {
    synchronized (this.lock) {
      if (this.raf_ != null) {
        this.raf_.close();
      } else if (this.inputStream_ != null) {
        this.inputStream_.close();
      } 
      this.channel_ = null;
      this.byteBuffer_ = null;
    } 
  }
  
  private void ensureOpen() throws IOException {
    if (this.byteBuffer_ == null)
      throw new IOException("Stream closed"); 
  }
  
  protected String getLine() throws IOException {
    fillByteBuffer();
    if (!this.byteBuffer_.hasRemaining())
      return null; 
    this.sb_.setLength(0);
    int b = -1;
    int i = -1;
    do {
      b = this.byteBuffer_.get();
      if ((b >= 10 && b <= 13) || b == 0)
        break; 
      this.bytes_[++i] = (byte)b;
      if (i == this.string_buf_size_ - 1) {
        this.sb_.append(new String(this.bytes_, this.charset_));
        i = -1;
      } 
      fillByteBuffer();
    } while (this.byteBuffer_.hasRemaining());
    this.sb_.append(new String(this.bytes_, 0, i + 1, this.charset_));
    return this.sb_.toString();
  }
  
  protected String[] getLineTokens() throws IOException {
    fillByteBuffer();
    if (!this.byteBuffer_.hasRemaining())
      return null; 
    this.tokens_.clear();
    this.sb_.setLength(0);
    int b = -1;
    int i = -1;
    do {
      b = this.byteBuffer_.get();
      if ((b >= 10 && b <= 13) || b == 0)
        break; 
      if (b == 9 || b == 32) {
        this.sb_.append(new String(this.bytes_, 0, i + 1, this.charset_));
        this.tokens_.add(this.sb_.toString());
        this.sb_.setLength(0);
        i = -1;
      } else {
        this.bytes_[++i] = (byte)b;
        if (i == this.string_buf_size_ - 1) {
          this.sb_.append(new String(this.bytes_, this.charset_));
          i = -1;
        } 
      } 
      fillByteBuffer();
    } while (this.byteBuffer_.hasRemaining());
    this.sb_.append(new String(this.bytes_, 0, i + 1, this.charset_));
    this.tokens_.add(this.sb_.toString());
    return this.tokens_.<String>toArray(new String[this.tokens_.size()]);
  }
  
  private void fillByteBuffer() throws IOException {
    if (this.inputStream_ == null || this.byteBuffer_.hasRemaining())
      return; 
    this.byteBuffer_.clear();
    for (int i = 0; i < this.string_buf_size_; i++) {
      int b = this.inputStream_.read();
      if (b < 0)
        break; 
      this.byteBuffer_.put((byte)b);
      if (this.fillLine_ && ((
        b >= 10 && b <= 13) || b == 0))
        break; 
    } 
    this.byteBuffer_.flip();
  }
}

package ai.searchbox.FastText4J.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public abstract class LineReader extends Reader {
  protected InputStream inputStream_ = null;
  
  protected File file_ = null;
  
  protected Charset charset_ = null;
  
  protected LineReader() {}
  
  protected LineReader(Object lock) {
    super(lock);
  }
  
  public LineReader(String filename, String charsetName) throws IOException, UnsupportedEncodingException {
    this();
    this.file_ = new File(filename);
    this.charset_ = Charset.forName(charsetName);
  }
  
  public LineReader(InputStream inputStream, String charsetName) throws UnsupportedEncodingException {
    this();
    this.inputStream_ = inputStream;
    this.charset_ = Charset.forName(charsetName);
  }
  
  public abstract long skipLine(long paramLong) throws IOException;
  
  public abstract String readLine() throws IOException;
  
  public abstract String[] readLineTokens() throws IOException;
  
  public abstract void rewind() throws IOException;
}

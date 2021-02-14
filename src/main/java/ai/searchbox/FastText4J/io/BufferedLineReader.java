package ai.searchbox.FastText4J.io;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public class BufferedLineReader extends LineReader {
  private static final Logger logger = Logger.getLogger(BufferedLineReader.class.getName());
  
  private String lineDelimitingRegex_ = " |\r|\t|\\v|\f|\000";
  
  private BufferedReader br_;
  
  public BufferedLineReader(String filename, String charsetName) throws IOException, UnsupportedEncodingException {
    super(filename, charsetName);
    FileInputStream fis = new FileInputStream(this.file_);
    this.br_ = new BufferedReader(new InputStreamReader(fis, this.charset_));
  }
  
  public BufferedLineReader(InputStream inputStream, String charsetName) throws UnsupportedEncodingException {
    super(inputStream, charsetName);
    this.br_ = new BufferedReader(new InputStreamReader(inputStream, this.charset_));
  }
  
  public long skipLine(long n) throws IOException {
    if (n < 0L)
      throw new IllegalArgumentException("skip value is negative"); 
    long currentLine = 0L;
    long readLine = 0L;
    synchronized (this.lock) {
      String line;
      while (currentLine < n && (line = this.br_.readLine()) != null) {
        readLine++;
        if (line == null || line.isEmpty() || line.startsWith("#"))
          continue; 
        currentLine++;
      } 
      return readLine;
    } 
  }
  
  public String readLine() throws IOException {
    synchronized (this.lock) {
      String lineString = this.br_.readLine();
      while (lineString != null && (lineString.isEmpty() || lineString.startsWith("#")))
        lineString = this.br_.readLine(); 
      return lineString;
    } 
  }
  
  public String[] readLineTokens() throws IOException {
    logger.debug("reading tokens");
    String line = readLine();
    logger.debug("line readed");
    if (line == null)
      return null; 
    return line.split(this.lineDelimitingRegex_, -1);
  }
  
  private String[] analyze(String text, Analyzer analyzer) throws IOException {
    int size = 0;
    TokenStream tokenStream1 = analyzer.tokenStream("all", text);
    tokenStream1.reset();
    while (tokenStream1.incrementToken())
      size++; 
    tokenStream1.close();
    logger.debug("size calculated");
    String[] result = new String[size];
    int index = 0;
    TokenStream tokenStream = analyzer.tokenStream("all", text);
    CharTermAttribute attr = (CharTermAttribute)tokenStream.addAttribute(CharTermAttribute.class);
    tokenStream.reset();
    while (tokenStream.incrementToken()) {
      result[index] = attr.toString();
      index++;
    } 
    tokenStream.close();
    logger.debug("tokens readed");
    return result;
  }
  
  public int read(char[] cbuf, int off, int len) throws IOException {
    synchronized (this.lock) {
      return this.br_.read(cbuf, off, len);
    } 
  }
  
  public void close() throws IOException {
    synchronized (this.lock) {
      if (this.br_ != null)
        this.br_.close(); 
    } 
  }
  
  public void rewind() throws IOException {
    synchronized (this.lock) {
      if (this.br_ != null)
        this.br_.close(); 
      if (this.file_ != null) {
        FileInputStream fis = new FileInputStream(this.file_);
        this.br_ = new BufferedReader(new InputStreamReader(fis, this.charset_));
      } else {
        throw new UnsupportedOperationException("InputStream rewind not supported");
      } 
    } 
  }
  
  public String getLineDelimitingRege() {
    return this.lineDelimitingRegex_;
  }
  
  public void setLineDelimitingRegex(String lineDelimitingRegex) {
    this.lineDelimitingRegex_ = lineDelimitingRegex;
  }
}

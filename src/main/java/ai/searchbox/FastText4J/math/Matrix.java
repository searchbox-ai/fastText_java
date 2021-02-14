package ai.searchbox.FastText4J.math;

import ai.searchbox.FastText4J.Utils;
import ai.searchbox.FastText4J.io.IOUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

public class Matrix {
  public int m = 0;
  
  public int n = 0;
  
  public float[][] data = null;
  
  public Matrix() {}
  
  public Matrix(int m, int n) {
    this.m = m;
    this.n = n;
    this.data = new float[m][n];
  }
  
  public void zero() {
    for (int i = 0; i < this.m; i++) {
      for (int j = 0; j < this.n; j++)
        this.data[i][j] = 0.0F; 
    } 
  }
  
  public void uniform(float a) {
    Random random = new Random(1L);
    for (int i = 0; i < this.m; i++) {
      for (int j = 0; j < this.n; j++)
        this.data[i][j] = Utils.randomFloat(random, -a, a); 
    } 
  }
  
  public void addToVector(Vector x, int t) {
    for (int j = 0; j < this.n; j++)
      x.data[j] = x.data[j] + this.data[t][j]; 
  }
  
  public void addRow(Vector vec, int i, float a) {
    Utils.checkArgument((i >= 0));
    Utils.checkArgument((i < this.m));
    Utils.checkArgument((vec.m == this.n));
    for (int j = 0; j < this.n; j++)
      this.data[i][j] = this.data[i][j] + a * vec.data[j]; 
  }
  
  public float dotRow(Vector vec, int i) {
    Utils.checkArgument((i >= 0));
    Utils.checkArgument((i < this.m));
    Utils.checkArgument((vec.m == this.n));
    float d = 0.0F;
    for (int j = 0; j < this.n; j++)
      d += this.data[i][j] * vec.data[j]; 
    return d;
  }
  
  public void load(InputStream input) throws IOException {
    IOUtil ioutil = new IOUtil();
    this.m = (int)ioutil.readLong(input);
    this.n = (int)ioutil.readLong(input);
    ioutil.setFloatArrayBufferSize(this.n);
    this.data = new float[this.m][this.n];
    for (int i = 0; i < this.m; i++)
      ioutil.readFloat(input, this.data[i]); 
  }
  
  public void save(OutputStream ofs) throws IOException {
    IOUtil ioutil = new IOUtil();
    ioutil.setFloatArrayBufferSize(this.n);
    ofs.write(ioutil.longToByteArray(this.m));
    ofs.write(ioutil.longToByteArray(this.n));
    for (int i = 0; i < this.m; i++)
      ofs.write(ioutil.floatToByteArray(this.data[i])); 
  }
  
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Matrix [data_=");
    if (this.data != null) {
      builder.append("[");
      for (int i = 0; i < this.m && i < 10; i++) {
        for (int j = 0; j < this.n && j < 10; j++)
          builder.append(this.data[i][j]).append(","); 
      } 
      builder.setLength(builder.length() - 1);
      builder.append("]");
    } else {
      builder.append("null");
    } 
    builder.append(", m_=");
    builder.append(this.m);
    builder.append(", n_=");
    builder.append(this.n);
    builder.append("]");
    return builder.toString();
  }
}

package ai.searchbox.FastText4J.math.quant;

import ai.searchbox.FastText4J.io.IOUtil;
import ai.searchbox.FastText4J.math.Vector;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

public class ProductQuantizer {
  private static final int SEED = 1234;
  
  private static final int NUM_BITS = 8;
  
  private static final int KSUB = 256;
  
  private static final int MAX_POINTS_PER_CLUSTER = 256;
  
  private static final int MAX_POINTS = 65536;
  
  private static final int NUM_ITER = 25;
  
  private static final double EPS = 1.0E-7D;
  
  int dim;
  
  int nsubq;
  
  int dsub;
  
  int lastdsub;
  
  float[] centroids;
  
  final Random rng = new Random(1234L);
  
  public float distL2(float[] x, float[] y, int d) {
    return distL2(x, y, d, 0, 0);
  }
  
  public float distL2(float[] x, float[] y, int d, int xpos, int ypos) {
    float dist = 0.0F;
    for (int i = 0; i < d; i++) {
      float tmp = x[i + xpos] - y[i + ypos];
      dist += tmp * tmp;
    } 
    return dist;
  }
  
  public int dim() {
    return this.dim;
  }
  
  public int dsub() {
    return this.dsub;
  }
  
  public int nsubq() {
    return this.nsubq;
  }
  
  public int lastdsub() {
    return this.lastdsub;
  }
  
  public float[] centroids() {
    return this.centroids;
  }
  
  public float getCentroid(int position) {
    return this.centroids[position];
  }
  
  public int getCentroidsPosition(int m, int i) {
    if (m == this.nsubq - 1)
      return m * 256 * this.dsub + i * this.lastdsub; 
    return (m * 256 + i) * this.dsub;
  }
  
  public void train(int n, float[] x) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  
  public void computeCode(float[] x, QCodes codes, int xBeginPosition, int codeBeginPosition) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  
  public void computeCodes(float[] x, QCodes codes, int m) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  
  public float mulCode(Vector x, QCodes codes, int t, float alpha) {
    float res = 0.0F;
    int d = this.dsub;
    int codePos = this.nsubq + t;
    for (int m = 0; m < this.nsubq; m++) {
      int c = getCentroidsPosition(m, codes.get(m + codePos));
      if (m == this.nsubq - 1)
        d = this.lastdsub; 
      for (int n = 0; n < d; n++)
        res += x.data[m * this.dsub + n] * this.centroids[c * n]; 
    } 
    return res * alpha;
  }
  
  public void addCode(Vector x, QCodes codes, int t, float alpha) {
    int d = this.dsub;
    int codePos = this.nsubq * t;
    for (int m = 0; m < this.nsubq; m++) {
      int c = getCentroidsPosition(m, codes.get(m + codePos));
      if (m == this.nsubq - 1)
        d = this.lastdsub; 
      for (int n = 0; n < d; n++)
        x.data[m * this.dsub + n] = x.data[m * this.dsub + n] + alpha * this.centroids[c + n]; 
    } 
  }
  
  public void save(OutputStream os) throws IOException {
    IOUtil io = new IOUtil();
    os.write(io.intToByteArray(this.dim));
    os.write(io.intToByteArray(this.nsubq));
    os.write(io.intToByteArray(this.dsub));
    os.write(io.intToByteArray(this.lastdsub));
    for (int i = 0; i < this.centroids.length; i++)
      os.write(io.floatToByteArray(this.centroids[i])); 
  }
  
  public void load(InputStream is) throws IOException {
    IOUtil io = new IOUtil();
    this.dim = io.readInt(is);
    this.nsubq = io.readInt(is);
    this.dsub = io.readInt(is);
    this.lastdsub = io.readInt(is);
    this.centroids = new float[this.dim * 256];
    for (int i = 0; i < this.centroids.length; i++)
      this.centroids[i] = io.readFloat(is); 
  }
  
  public static int findCentroidsSize(int dimension) {
    return dimension * 256;
  }
  
  private float assignCentroid(float[] x, int xStartPosition, int c0Position, QCodes codes, int codeStartPosition, int d) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  
  private void eStep(float[] x, int cPosition, QCodes codes, int d, int n) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  
  private void mStep(float[] x0, int cPosition, QCodes codes, int d, int n) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  
  private void kmeans(float[] x, int cPosition, int n, int d) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}

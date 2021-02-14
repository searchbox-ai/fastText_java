package ai.searchbox.FastText4J.math;

import ai.searchbox.FastText4J.Utils;
import ai.searchbox.FastText4J.io.IOUtil;
import ai.searchbox.FastText4J.math.quant.ProductQuantizer;
import ai.searchbox.FastText4J.math.quant.QCodeArray;
import ai.searchbox.FastText4J.math.quant.QCodes;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MatrixQ extends Matrix {
  QCodeArray codes;
  
  ProductQuantizer pq = new ProductQuantizer();
  
  QCodeArray normCodes;
  
  ProductQuantizer npq = new ProductQuantizer();
  
  boolean qnorm;
  
  public void addToVector(Vector x, int t) {
    float norm = 1.0F;
    if (this.qnorm) {
      int cPosition = this.npq.getCentroidsPosition(0, this.normCodes.get(t));
      norm = this.npq.getCentroid(cPosition);
    } 
    this.pq.addCode(x, (QCodes)this.codes, t, norm);
  }
  
  public float dotRow(Vector vec, int i) {
    Utils.checkArgument((i >= 0));
    Utils.checkArgument((i < this.m));
    Utils.checkArgument((vec.m == this.n));
    float norm = 1.0F;
    if (this.qnorm) {
      int cPosition = this.npq.getCentroidsPosition(0, this.normCodes.get(i));
      norm = this.npq.getCentroid(cPosition);
    } 
    return this.pq.mulCode(vec, (QCodes)this.codes, i, norm);
  }
  
  public void load(InputStream is) throws IOException {
    IOUtil ioutil = new IOUtil();
    this.qnorm = ioutil.readBool(is);
    this.m = (int)ioutil.readLong(is);
    this.n = (int)ioutil.readLong(is);
    int codeSize = ioutil.readInt(is);
    int[] rawCodes = new int[codeSize];
    for (int i = 0; i < codeSize; i++) {
      int c = ioutil.readByteAsInt(is);
      rawCodes[i] = c;
    } 
    this.codes = new QCodeArray(rawCodes);
    this.pq.load(is);
    if (this.qnorm) {
      int[] rawNormCodes = new int[this.m];
      for (int j = 0; j < this.m; j++) {
        int c = ioutil.readByteAsInt(is);
        rawNormCodes[j] = c;
      } 
      this.normCodes = new QCodeArray(rawNormCodes);
      this.npq.load(is);
    } 
  }
  
  public void save(OutputStream os) throws IOException {
    IOUtil ioutil = new IOUtil();
    os.write(ioutil.booleanToByteArray(this.qnorm));
    os.write(ioutil.longToByteArray(this.m));
    os.write(ioutil.longToByteArray(this.n));
    os.write(ioutil.intToByteArray(this.codes.size()));
    int i;
    for (i = 0; i < this.codes.size(); i++)
      os.write(ioutil.intToByte(this.codes.get(i))); 
    this.pq.save(os);
    if (this.qnorm) {
      for (i = 0; i < this.m; i++)
        os.write(ioutil.intToByte(this.normCodes.get(i))); 
      this.npq.save(os);
    } 
  }
}

package ai.searchbox.FastText4J.math.quant;

import ai.searchbox.FastText4J.Utils;

public class QCodeArray implements QCodes {
  private final int[] codes;
  
  public QCodeArray(QCodeArray qcodes) {
    this.codes = qcodes.codes;
  }
  
  public QCodeArray(int[] codes) {
    this.codes = codes;
  }
  
  public QCodeArray(int size) {
    this.codes = new int[size];
  }
  
  public int get(int i) {
    Utils.checkArgument((i >= 0));
    Utils.checkArgument((i < this.codes.length));
    return this.codes[i];
  }
  
  public int size() {
    return this.codes.length;
  }
  
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("QCodeArray(size=");
    builder.append(size());
    builder.append(", [");
    for (int i = 0; i < size(); i++)
      builder.append(get(i)).append(' '); 
    if (builder.length() > 1)
      builder.setLength(builder.length() - 1); 
    builder.append("])");
    return builder.toString();
  }
}

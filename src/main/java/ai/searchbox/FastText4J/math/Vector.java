package ai.searchbox.FastText4J.math;

import ai.searchbox.FastText4J.Utils;

public class Vector {
  public int m;
  
  public float[] data;
  
  public Vector(int size) {
    this.m = size;
    this.data = new float[this.m];
  }
  
  public Vector(float[] v) {
    this.m = v.length;
    this.data = new float[this.m];
    for (int i = 0; i < this.m; i++)
      set(i, v[i]); 
  }
  
  public int size() {
    return this.m;
  }
  
  public void zero() {
    for (int i = 0; i < this.m; i++)
      this.data[i] = 0.0F; 
  }
  
  public void mul(float a) {
    for (int i = 0; i < this.m; i++)
      this.data[i] = this.data[i] * a; 
  }
  
  public void addRow(Matrix A, int i) {
    Utils.checkArgument((i >= 0));
    Utils.checkArgument((i < A.m));
    Utils.checkArgument((this.m == A.n));
    A.addToVector(this, i);
  }
  
  public void addRow(Matrix A, int i, float a) {
    Utils.checkArgument((i >= 0));
    Utils.checkArgument((i < A.m));
    Utils.checkArgument((this.m == A.n));
    for (int j = 0; j < A.n; j++)
      this.data[j] = this.data[j] + a * A.data[i][j]; 
  }
  
  public void addVector(Vector source) {
    Utils.checkArgument((this.m == source.m));
    for (int i = 0; i < this.m; i++)
      this.data[i] = this.data[i] + source.get(i); 
  }
  
  public float norm() {
    float sum = 0.0F;
    for (int i = 0; i < this.m; i++)
      sum += this.data[i] * this.data[i]; 
    return (float)Math.sqrt(sum);
  }
  
  public void mul(Matrix A, Vector vec) {
    Utils.checkArgument((A.m == this.m));
    Utils.checkArgument((A.n == vec.m));
    for (int i = 0; i < this.m; i++) {
      this.data[i] = 0.0F;
      for (int j = 0; j < A.n; j++)
        this.data[i] = A.dotRow(vec, i); 
    } 
  }
  
  public int argmax() {
    float max = this.data[0];
    int argmax = 0;
    for (int i = 1; i < this.m; i++) {
      if (this.data[i] > max) {
        max = this.data[i];
        argmax = i;
      } 
    } 
    return argmax;
  }
  
  public float get(int i) {
    return this.data[i];
  }
  
  public void set(int i, float value) {
    this.data[i] = value;
  }
  
  public String toString() {
    StringBuilder builder = new StringBuilder();
    byte b;
    int i;
    float[] arrayOfFloat;
    for (i = (arrayOfFloat = this.data).length, b = 0; b < i; ) {
      float data = arrayOfFloat[b];
      builder.append(data).append(' ');
      b++;
    } 
    if (builder.length() > 1)
      builder.setLength(builder.length() - 1); 
    return builder.toString();
  }
}

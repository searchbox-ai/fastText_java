package ai.searchbox.FastText4J;

import ai.searchbox.FastText4J.io.LineReader;
import ai.searchbox.FastText4J.io.MappedByteBufferLineReader;
import ai.searchbox.FastText4J.math.Matrix;
import ai.searchbox.FastText4J.math.Vector;
import com.google.common.collect.MinMaxPriorityQueue;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Logger;

public class FastText {
  private static final Logger logger = Logger.getLogger(FastText.class.getName());
  
  public static int FASTTEXT_VERSION = 12;
  
  public static int FASTTEXT_FILEFORMAT_MAGIC_INT = 793712314;
  
  private long start_;
  
  int threadCount;
  
  long threadFileSize;
  
  private Args args_;
  
  private Dictionary dict_;
  
  private Model model_;
  
  private Matrix input_;
  
  private Matrix output_;
  
  private Matrix wordVectors = null;
  
  private Matrix wordVectorsOut = null;
  
  private boolean isQuant = false;
  
  private AtomicLong tokenCount_;
  
  private String charsetName_ = "UTF-8";
  
  private Class<? extends LineReader> lineReaderClass_ = (Class)MappedByteBufferLineReader.class;
  
  public Args getArgs() {
    return this.args_;
  }
  
  public void setArgs(Args args) {
    this.args_ = args;
    this.dict_ = new Dictionary(args);
  }
  
  public Dictionary dict() {
    return this.dict_;
  }
  
  public Vector getWordVectorIn(String word) {
    Vector vec = new Vector(this.args_.dim);
    vec.zero();
    List<Integer> ngrams = this.dict_.getNgrams(word);
    for (Integer it : ngrams)
      vec.addRow(this.input_, it.intValue()); 
    if (ngrams.size() > 0)
      vec.mul(1.0F / ngrams.size()); 
    return vec;
  }
  
  public Vector getWordVectorOut(String word) {
    int id = this.dict_.getId(word);
    Vector vec = new Vector(this.args_.dim);
    vec.zero();
    if (this.isQuant)
      return vec; 
    vec.addRow(this.output_, id);
    return vec;
  }
  
  public Vector getSentenceVector(List<String> sentence) {
    Vector svec = new Vector(this.args_.dim);
    svec.zero();
    if (this.args_.model == Args.ModelType.sup) {
      List<Integer> tokens = new ArrayList<>();
      List<Integer> labels = new ArrayList<>();
      this.dict_.getLine(sentence.<String>toArray(new String[sentence.size()]), tokens, labels, this.model_.rng);
      for (int i = 0; i < tokens.size(); i++)
        svec.addRow(this.input_, ((Integer)tokens.get(i)).intValue()); 
      if (!tokens.isEmpty())
        svec.mul(1.0F / tokens.size()); 
    } else {
      int count = 0;
      for (String word : sentence) {
        Vector vec = getWordVectorIn(word);
        svec.addVector(vec);
        count++;
      } 
      if (count > 0)
        svec.mul(1.0F / count); 
    } 
    return svec;
  }
  
  public Vector getSentenceVectorOut(List<String> sentence) {
    Vector svec = new Vector(this.args_.dim);
    svec.zero();
    int count = 0;
    for (String word : sentence) {
      Vector vec = getWordVectorOut(word);
      svec.addVector(vec);
      count++;
    } 
    if (count > 0)
      svec.mul(1.0F / count); 
    return svec;
  }
  
  public List<Pair<Float, String>> predict(String[] lineTokens, int k) {
    List<Pair<Float, String>> predictions = new ArrayList<>();
    List<Integer> words = new ArrayList<>();
    List<Integer> labels = new ArrayList<>();
    this.dict_.getLine(lineTokens, words, labels, this.model_.rng);
    this.dict_.addNgrams(words, this.args_.wordNgrams);
    if (words.isEmpty())
      return predictions; 
    List<Pair<Float, Integer>> modelPredictions = new ArrayList<>(k + 1);
    this.model_.predict(words, k, modelPredictions);
    for (Pair<Float, Integer> pair : modelPredictions)
      predictions.add(new Pair<>(pair.getKey(), this.dict_.getLabel(((Integer)pair.getValue()).intValue()))); 
    return predictions;
  }
  
  public List<FastTextSynonym> findNN(Vector queryVec, int k, Set<String> banSet) {
    return findNN(this.wordVectors, queryVec, k, banSet);
  }
  
  public List<FastTextSynonym> findNNOut(Vector queryVec, int k, Set<String> banSet) {
    return findNN(this.wordVectorsOut, queryVec, k, banSet);
  }
  
  public List<FastTextSynonym> findNN(Matrix wordVectors, Vector queryVec, int k, Set<String> banSet) {
    MinMaxPriorityQueue<Pair<Float, String>> heap = 
      MinMaxPriorityQueue.orderedBy(new HeapComparator())
      .expectedSize(this.dict_.nlabels())
      .create();
    float queryNorm = queryVec.norm();
    if (queryNorm > 0.0F)
      queryVec.mul(1.0F / queryNorm); 
    for (int i = 0; i < this.dict_.nwords(); i++) {
      String word = this.dict_.getWord(i);
      float dp = wordVectors.dotRow(queryVec, i);
      heap.add(new Pair<>(Float.valueOf(dp), word));
    } 
    List<FastTextSynonym> syns = new ArrayList<>();
    int j = 0;
    while (j < k && heap.size() > 0) {
      Pair<Float, String> synonym = (Pair<Float, String>)heap.pollFirst();
      boolean banned = banSet.contains(synonym.getValue());
      if (!banned) {
        syns.add(new FastTextSynonym(synonym.getValue(), ((Float)synonym.getKey()).floatValue()));
        j++;
      } 
    } 
    return syns;
  }
  
  public void saveModel() throws IOException {
    if (Utils.isEmpty(this.args_.output)) {
      if (this.args_.verbose > 1)
        System.out.println("output is empty, skip save model file"); 
      return;
    } 
    File file = new File(String.valueOf(this.args_.output) + ".bin");
    if (file.exists())
      file.delete(); 
    if (file.getParentFile() != null)
      file.getParentFile().mkdirs(); 
    if (this.args_.verbose > 1)
      System.out.println("Saving model to " + file.getCanonicalPath().toString()); 
    Exception exception1 = null, exception2 = null;
  }
  
  public void loadModel(String filename) throws IOException {
    logger.info("Loading " + filename);
    File file = new File(filename);
    if (!file.exists() || !file.isFile() || !file.canRead())
      throw new IOException("Model file cannot be opened for loading!"); 
    Exception exception1 = null, exception2 = null;
  }
  
  public void train() throws Exception {
    if ("-".equals(this.args_.input))
      throw new IOException("Cannot use stdin for training!"); 
    File file = new File(this.args_.input);
    if (!file.exists() || !file.isFile() || !file.canRead())
      throw new IOException("Input file cannot be opened! " + this.args_.input); 
    logger.debug("Building dict");
    this.dict_ = new Dictionary(this.args_);
    this.dict_.setCharsetName(this.charsetName_);
    this.dict_.setLineReaderClass(this.lineReaderClass_);
    this.dict_.readFromFile(this.args_.input);
    logger.debug("Building input matrix");
    if (!Utils.isEmpty(this.args_.pretrainedVectors)) {
      loadVecFile();
    } else {
      this.input_ = new Matrix(this.dict_.nwords() + this.args_.bucket, this.args_.dim);
      this.input_.uniform(1.0F / this.args_.dim);
    } 
    logger.debug("Building output matrix");
    int m = (this.args_.model == Args.ModelType.sup) ? this.dict_.nlabels() : this.dict_.nwords();
    this.output_ = new Matrix(m, this.args_.dim);
    this.output_.zero();
    this.start_ = System.currentTimeMillis();
    this.tokenCount_ = new AtomicLong(0L);
    long t0 = System.currentTimeMillis();
    this.threadFileSize = Utils.sizeLine(this.args_.input);
    this.threadCount = this.args_.thread;
    for (int i = 0; i < this.args_.thread; i++) {
      logger.debug("Spawning training thread");
      Thread t = new TrainThread(this, i);
      t.setUncaughtExceptionHandler(this.trainThreadExcpetionHandler);
      t.start();
    } 
    synchronized (this) {
      while (this.threadCount > 0) {
        try {
          wait();
        } catch (InterruptedException interruptedException) {}
      } 
    } 
    this.model_ = new Model(this.input_, this.output_, this.args_, 0);
    if (this.args_.verbose > 1) {
      long trainTime = (System.currentTimeMillis() - t0) / 1000L;
      System.out.printf("\nTrain time used: %d sec\n", new Object[] { Long.valueOf(trainTime) });
    } 
    logger.debug("Saving fasttext");
    saveModel();
    if (this.args_.model != Args.ModelType.sup)
      saveVecFile(); 
  }
  
  public void test(InputStream in, int k) throws IOException, Exception {
    int nexamples = 0, nlabels = 0;
    double precision = 0.0D;
    List<Integer> line = new ArrayList<>();
    List<Integer> labels = new ArrayList<>();
    LineReader lineReader = null;
    try {
      lineReader = this.lineReaderClass_.getConstructor(new Class[] { InputStream.class, String.class }).newInstance(new Object[] { in, this.charsetName_ });
      String[] lineTokens;
      while ((lineTokens = lineReader.readLineTokens()) != null && (
        lineTokens.length != 1 || !"quit".equals(lineTokens[0]))) {
        this.dict_.getLine(lineTokens, line, labels, this.model_.rng);
        this.dict_.addNgrams(line, this.args_.wordNgrams);
        if (labels.size() > 0 && line.size() > 0) {
          List<Pair<Float, Integer>> modelPredictions = new ArrayList<>();
          this.model_.predict(line, k, modelPredictions);
          for (Pair<Float, Integer> pair : modelPredictions) {
            if (labels.contains(pair.getValue()))
              precision++; 
          } 
          nexamples++;
          nlabels += labels.size();
        } 
      } 
    } finally {
      if (lineReader != null)
        lineReader.close(); 
    } 
    System.out.printf("P@%d: %.3f%n", new Object[] { Integer.valueOf(k), Double.valueOf(precision / (k * nexamples)) });
    System.out.printf("R@%d: %.3f%n", new Object[] { Integer.valueOf(k), Double.valueOf(precision / nlabels) });
    System.out.println("Number of examples: " + nexamples);
  }
  
  void cbow(Model model, float lr, List<Integer> line) {
    List<Integer> bow = new ArrayList<>();
    for (int w = 0; w < line.size(); w++) {
      bow.clear();
      int boundary = Utils.randomInt(model.rng, 1, this.args_.ws);
      for (int c = -boundary; c <= boundary; c++) {
        if (c != 0 && w + c >= 0 && w + c < line.size()) {
          List<Integer> ngrams = this.dict_.getNgrams(((Integer)line.get(w + c)).intValue());
          bow.addAll(ngrams);
        } 
      } 
      model.update(bow, ((Integer)line.get(w)).intValue(), lr);
    } 
  }
  
  void skipgram(Model model, float lr, List<Integer> line) {
    for (int w = 0; w < line.size(); w++) {
      int boundary = Utils.randomInt(model.rng, 1, this.args_.ws);
      List<Integer> ngrams = this.dict_.getNgrams(((Integer)line.get(w)).intValue());
      for (int c = -boundary; c <= boundary; c++) {
        if (c != 0 && w + c >= 0 && w + c < line.size())
          model.update(ngrams, ((Integer)line.get(w + c)).intValue(), lr); 
      } 
    } 
  }
  
  void supervised(Model model, float lr, List<Integer> line, List<Integer> labels) {
    if (labels.size() == 0 || line.size() == 0)
      return; 
    int i = Utils.randomInt(model.rng, 1, labels.size()) - 1;
    model.update(line, ((Integer)labels.get(i)).intValue(), lr);
  }
  
  void checkModel(int magic, int version) {
    if (magic != FASTTEXT_FILEFORMAT_MAGIC_INT)
      throw new IllegalArgumentException("Unhandled file format"); 
    if (version > FASTTEXT_VERSION)
      throw new IllegalArgumentException(
          "Input model version (" + version + ") doesn't match current version (" + FASTTEXT_VERSION + ")"); 
  }
  
  public void loadVecFile() throws IOException {
    loadVecFile(this.args_.pretrainedVectors);
  }
  
  public void loadVecFile(String path) throws IOException {
    Exception exception1 = null, exception2 = null;
  }
  
  public void saveVecFile() throws IOException {
    saveVecFile(String.valueOf(this.args_.output) + ".vec", true);
  }
  
  public void saveVecFile(String path, boolean in) throws IOException {
    File file = new File(path);
    if (file.exists())
      file.delete(); 
    if (file.getParentFile() != null)
      file.getParentFile().mkdirs(); 
    if (this.args_.verbose > 1)
      System.out.println("Saving Vectors to " + file.getCanonicalPath().toString()); 
    Exception exception1 = null, exception2 = null;
  }
  
  Thread.UncaughtExceptionHandler trainThreadExcpetionHandler = new Thread.UncaughtExceptionHandler() {
      public void uncaughtException(Thread th, Throwable ex) {
        ex.printStackTrace();
      }
    };
  
  private Matrix precomputeWordVectors(boolean in) {
    Matrix wordVectors = new Matrix(this.dict_.nwords(), this.args_.dim);
    wordVectors.zero();
    for (int i = 0; i < this.dict_.nwords(); i++) {
      String word = this.dict_.getWord(i);
      try {
        Vector vec = in ? getWordVectorIn(word) : getWordVectorOut(word);
        float norm = vec.norm();
        if (norm > 0.0F)
          wordVectors.addRow(vec, i, 1.0F / norm); 
      } catch (Exception e) {
        logger.error("Failed precomputing word vectors for " + word + " in in:" + in);
      } 
    } 
    return wordVectors;
  }
  
  public static class HeapComparator<T> implements Comparator<Pair<Float, T>> {
    public int compare(Pair<Float, T> p1, Pair<Float, T> p2) {
      if (((Float)p1.getKey()).equals(p2.getKey()))
        return 0; 
      if (((Float)p1.getKey()).floatValue() < ((Float)p2.getKey()).floatValue())
        return 1; 
      return -1;
    }
  }
  
  public class TrainThread extends Thread {
    final FastText ft;
    
    int threadId;
    
    public TrainThread(FastText ft, int threadId) {
      super("FT-TrainThread-" + threadId);
      this.ft = ft;
      this.threadId = threadId;
    }
    
    public void run() {
      if (FastText.this.args_.verbose > 2)
        System.out.println("thread: " + this.threadId + " RUNNING!"); 
      try {
        Exception exception2, exception1 = null;
      } catch (Exception e) {
        FastText.logger.error(e);
      } 
      synchronized (this.ft) {
        if (FastText.this.args_.verbose > 2)
          System.out.println("\nthread: " + this.threadId + " EXIT!"); 
        this.ft.threadCount--;
        this.ft.notify();
      } 
    }
    
    private void printInfo(float progress, float loss) throws Exception {
      float t = (float)(System.currentTimeMillis() - FastText.this.start_) / 1000.0F;
      float ws = (float)FastText.this.tokenCount_.get() / t;
      float wst = (float)FastText.this.tokenCount_.get() / t / FastText.this.args_.thread;
      float lr = (float)(FastText.this.args_.lr * (1.0F - progress));
      int eta = (int)(t / progress * (1.0F - progress));
      int etah = eta / 3600;
      int etam = (eta - etah * 3600) / 60;
      System.out.printf("\rProgress: %.1f%% words/sec: %d words/sec/thread: %d lr: %.6f loss: %.6f eta: %d h %d m", new Object[] { Float.valueOf(100.0F * progress), Integer.valueOf((int)ws), Integer.valueOf((int)wst), Float.valueOf(lr), Float.valueOf(loss), Integer.valueOf(etah), Integer.valueOf(etam) });
      System.out.println("ss");
    }
  }
  
  public class FastTextSynonym {
    private final String word;
    
    private final double cosineSimilarity;
    
    public FastTextSynonym(String word, double cosineSimilarity) {
      this.word = word;
      this.cosineSimilarity = cosineSimilarity;
    }
    
    public String word() {
      return this.word;
    }
    
    public double cosineSimilarity() {
      return this.cosineSimilarity;
    }
  }
  
  public class FastTextPrediction {
    private final String label;
    
    private final double logProbability;
    
    public FastTextPrediction(String label, double logProbability) {
      this.label = label;
      this.logProbability = logProbability;
    }
    
    public String label() {
      return this.label;
    }
    
    public double logProbability() {
      return this.logProbability;
    }
    
    public double probability() {
      return Math.exp(this.logProbability);
    }
  }
}

package ai.searchbox.FastText4J;

import ai.searchbox.FastText4J.io.IOUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Args {
  public String input;
  
  public String output;
  
  public String test;
  
  public double lr = 0.05D;
  
  public int lrUpdateRate = 100;
  
  public int dim = 100;
  
  public int ws = 5;
  
  public int epoch = 5;
  
  public int minCount = 5;
  
  public int minCountLabel = 0;
  
  public int neg = 5;
  
  public int wordNgrams = 1;
  
  public LossType loss = LossType.ns;
  
  public ModelType model = ModelType.sg;
  
  public int bucket = 2000000;
  
  public int minn = 3;
  
  public int maxn = 6;
  
  public int thread = 1;
  
  public double t = 1.0E-4D;
  
  public String label = "__label__";
  
  public int verbose = 2;
  
  public String pretrainedVectors = "";
  
  public void printHelp() {
    System.out.println("\nThe following arguments are mandatory:\n  -input              training file path\n  -output             output file path\n\nThe following arguments are optional:\n  -lr                 learning rate [" + 
        
        this.lr + "]\n" + 
        "  -lrUpdateRate       change the rate of updates for the learning rate [" + this.lrUpdateRate + "]\n" + 
        "  -dim                size of word vectors [" + this.dim + "]\n" + 
        "  -ws                 size of the context window [" + this.ws + "]\n" + 
        "  -epoch              number of epochs [" + this.epoch + "]\n" + 
        "  -minCount           minimal number of word occurences [" + this.minCount + "]\n" + 
        "  -minCountLabel      minimal number of label occurences [" + this.minCountLabel + "]\n" + 
        "  -neg                number of negatives sampled [" + this.neg + "]\n" + 
        "  -wordNgrams         max length of word ngram [" + this.wordNgrams + "]\n" + 
        "  -loss               loss function {ns, hs, softmax} [ns]\n" + 
        "  -bucket             number of buckets [" + this.bucket + "]\n" + 
        "  -minn               min length of char ngram [" + this.minn + "]\n" + 
        "  -maxn               max length of char ngram [" + this.maxn + "]\n" + 
        "  -thread             number of threads [" + this.thread + "]\n" + 
        "  -t                  sampling threshold [" + this.t + "]\n" + 
        "  -label              labels prefix [" + this.label + "]\n" + 
        "  -verbose            verbosity level [" + this.verbose + "]\n" + 
        "  -pretrainedVectors  pretrained word vectors for supervised learning []");
  }
  
  public void save(OutputStream ofs) throws IOException {
    IOUtil ioutil = new IOUtil();
    ofs.write(ioutil.intToByteArray(this.dim));
    ofs.write(ioutil.intToByteArray(this.ws));
    ofs.write(ioutil.intToByteArray(this.epoch));
    ofs.write(ioutil.intToByteArray(this.minCount));
    ofs.write(ioutil.intToByteArray(this.neg));
    ofs.write(ioutil.intToByteArray(this.wordNgrams));
    ofs.write(ioutil.intToByteArray(this.loss.value));
    ofs.write(ioutil.intToByteArray(this.model.value));
    ofs.write(ioutil.intToByteArray(this.bucket));
    ofs.write(ioutil.intToByteArray(this.minn));
    ofs.write(ioutil.intToByteArray(this.maxn));
    ofs.write(ioutil.intToByteArray(this.lrUpdateRate));
    ofs.write(ioutil.doubleToByteArray(this.t));
  }
  
  public void load(InputStream input) throws IOException {
    IOUtil ioutil = new IOUtil();
    this.dim = ioutil.readInt(input);
    this.ws = ioutil.readInt(input);
    this.epoch = ioutil.readInt(input);
    this.minCount = ioutil.readInt(input);
    this.neg = ioutil.readInt(input);
    this.wordNgrams = ioutil.readInt(input);
    this.loss = LossType.fromValue(ioutil.readInt(input));
    this.model = ModelType.fromValue(ioutil.readInt(input));
    this.bucket = ioutil.readInt(input);
    this.minn = ioutil.readInt(input);
    this.maxn = ioutil.readInt(input);
    this.lrUpdateRate = ioutil.readInt(input);
    this.t = ioutil.readDouble(input);
  }
  
  public void parseArgs(String[] args) {
    String command = args[0];
    if ("supervised".equalsIgnoreCase(command)) {
      this.model = ModelType.sup;
      this.loss = LossType.softmax;
      this.minCount = 1;
      this.minn = 0;
      this.maxn = 0;
      this.lr = 0.1D;
    } 
    if ("cbow".equalsIgnoreCase(command))
      this.model = ModelType.cbow; 
    if ("skipgram".equalsIgnoreCase(command))
      this.model = ModelType.sg; 
    int ai = 1;
    while (ai < args.length) {
      if (args[ai].charAt(0) != '-') {
        System.out.println("Provided argument without a dash! Usage:");
        printHelp();
        System.exit(1);
      } 
      if ("-h".equals(args[ai])) {
        System.out.println("Here is the help! Usage:");
        printHelp();
        System.exit(1);
      } else if ("-input".equals(args[ai])) {
        this.input = args[ai + 1];
      } else if ("-test".equals(args[ai])) {
        this.test = args[ai + 1];
      } else if ("-output".equals(args[ai])) {
        this.output = args[ai + 1];
      } else if ("-lr".equals(args[ai])) {
        this.lr = Double.parseDouble(args[ai + 1]);
      } else if ("-lrUpdateRate".equals(args[ai])) {
        this.lrUpdateRate = Integer.parseInt(args[ai + 1]);
      } else if ("-dim".equals(args[ai])) {
        this.dim = Integer.parseInt(args[ai + 1]);
      } else if ("-ws".equals(args[ai])) {
        this.ws = Integer.parseInt(args[ai + 1]);
      } else if ("-epoch".equals(args[ai])) {
        this.epoch = Integer.parseInt(args[ai + 1]);
      } else if ("-minCount".equals(args[ai])) {
        this.minCount = Integer.parseInt(args[ai + 1]);
      } else if ("-minCountLabel".equals(args[ai])) {
        this.minCountLabel = Integer.parseInt(args[ai + 1]);
      } else if ("-neg".equals(args[ai])) {
        this.neg = Integer.parseInt(args[ai + 1]);
      } else if ("-wordNgrams".equals(args[ai])) {
        this.wordNgrams = Integer.parseInt(args[ai + 1]);
      } else if ("-loss".equals(args[ai])) {
        if ("hs".equalsIgnoreCase(args[ai + 1])) {
          this.loss = LossType.hs;
        } else if ("ns".equalsIgnoreCase(args[ai + 1])) {
          this.loss = LossType.ns;
        } else if ("softmax".equalsIgnoreCase(args[ai + 1])) {
          this.loss = LossType.softmax;
        } else {
          System.out.println("Unknown loss: " + args[ai + 1]);
          printHelp();
          System.exit(1);
        } 
      } else if ("-bucket".equals(args[ai])) {
        this.bucket = Integer.parseInt(args[ai + 1]);
      } else if ("-minn".equals(args[ai])) {
        this.minn = Integer.parseInt(args[ai + 1]);
      } else if ("-maxn".equals(args[ai])) {
        this.maxn = Integer.parseInt(args[ai + 1]);
      } else if ("-thread".equals(args[ai])) {
        this.thread = Integer.parseInt(args[ai + 1]);
      } else if ("-t".equals(args[ai])) {
        this.t = Double.parseDouble(args[ai + 1]);
      } else if ("-label".equals(args[ai])) {
        this.label = args[ai + 1];
      } else if ("-verbose".equals(args[ai])) {
        this.verbose = Integer.parseInt(args[ai + 1]);
      } else if ("-pretrainedVectors".equals(args[ai])) {
        this.pretrainedVectors = args[ai + 1];
      } else {
        System.out.println("Unknown argument: " + args[ai]);
        printHelp();
        System.exit(1);
      } 
      ai += 2;
    } 
    if (Utils.isEmpty(this.input) || Utils.isEmpty(this.output)) {
      System.out.println("Empty input or output path.");
      printHelp();
      System.exit(1);
    } 
    if (this.wordNgrams <= 1 && this.maxn == 0)
      this.bucket = 0; 
  }
  
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Args [input=");
    builder.append(this.input);
    builder.append(", output=");
    builder.append(this.output);
    builder.append(", test=");
    builder.append(this.test);
    builder.append(", lr=");
    builder.append(this.lr);
    builder.append(", lrUpdateRate=");
    builder.append(this.lrUpdateRate);
    builder.append(", dim=");
    builder.append(this.dim);
    builder.append(", ws=");
    builder.append(this.ws);
    builder.append(", epoch=");
    builder.append(this.epoch);
    builder.append(", minCount=");
    builder.append(this.minCount);
    builder.append(", minCountLabel=");
    builder.append(this.minCountLabel);
    builder.append(", neg=");
    builder.append(this.neg);
    builder.append(", wordNgrams=");
    builder.append(this.wordNgrams);
    builder.append(", loss=");
    builder.append(this.loss);
    builder.append(", model=");
    builder.append(this.model);
    builder.append(", bucket=");
    builder.append(this.bucket);
    builder.append(", minn=");
    builder.append(this.minn);
    builder.append(", maxn=");
    builder.append(this.maxn);
    builder.append(", thread=");
    builder.append(this.thread);
    builder.append(", t=");
    builder.append(this.t);
    builder.append(", label=");
    builder.append(this.label);
    builder.append(", verbose=");
    builder.append(this.verbose);
    builder.append(", pretrainedVectors=");
    builder.append(this.pretrainedVectors);
    builder.append("]");
    return builder.toString();
  }
  
  public enum ModelType {
    cbow(1),
    sg(2),
    sup(3);
    
    private int value;
    
    public int getValue() {
      return this.value;
    }
    
    public static ModelType fromValue(int value) throws IllegalArgumentException {
      try {
        value--;
        return values()[value];
      } catch (ArrayIndexOutOfBoundsException e) {
        throw new IllegalArgumentException("Unknown model_name enum value :" + value);
      } 
    }
    
    ModelType(int value) {
      this.value = value;
    }
  }
  
  public enum LossType {
    hs(1),
    ns(2),
    softmax(3);
    
    private int value;
    
    public int getValue() {
      return this.value;
    }
    
    public static LossType fromValue(int value) throws IllegalArgumentException {
      try {
        value--;
        return values()[value];
      } catch (ArrayIndexOutOfBoundsException e) {
        throw new IllegalArgumentException("Unknown loss_name enum value :" + value);
      } 
    }
    
    LossType(int value) {
      this.value = value;
    }
  }
}

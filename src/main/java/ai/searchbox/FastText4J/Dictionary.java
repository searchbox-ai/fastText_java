package ai.searchbox.FastText4J;

import ai.searchbox.FastText4J.io.IOUtil;
import ai.searchbox.FastText4J.io.LineReader;
import ai.searchbox.FastText4J.io.MappedByteBufferLineReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.log4j.Logger;

public class Dictionary {
  private static final Logger logger = Logger.getLogger(Dictionary.class.getName());
  
  private static final int MAX_VOCAB_SIZE = 30000000;
  
  private static final int MAX_LINE_SIZE = 1024;
  
  private static final Integer WORDID_DEFAULT = Integer.valueOf(-1);
  
  private static final String EOS = "</s>";
  
  private static final String BOW = "<";
  
  private static final String EOW = ">";
  
  private int size = 0;
  
  private int nwords = 0;
  
  private long ntokens = 0L;
  
  private int nlabels = 0;
  
  protected long pruneIdxSize = -1L;
  
  Map<Integer, Integer> pruneIdx;
  
  private Args args_;
  
  private List<Entry> words;
  
  private Map<Long, Integer> word2int;
  
  private List<Float> pdiscard;
  
  private String charsetName_ = "UTF-8";
  
  private Class<? extends LineReader> lineReaderClass_ = (Class)MappedByteBufferLineReader.class;
  
  private transient Comparator<Entry> entry_comparator;
  
  public List<Entry> getWords() {
    return this.words;
  }
  
  public int nwords() {
    return this.nwords;
  }
  
  public int nlabels() {
    return this.nlabels;
  }
  
  public long ntokens() {
    return this.ntokens;
  }
  
  public Map<Long, Integer> getWord2int() {
    return this.word2int;
  }
  
  public List<Float> getPdiscard() {
    return this.pdiscard;
  }
  
  public int getSize() {
    return this.size;
  }
  
  public Args getArgs() {
    return this.args_;
  }
  
  public boolean isPruned() {
    return (this.pruneIdxSize >= 0L);
  }
  
  public String getCharsetName() {
    return this.charsetName_;
  }
  
  public void setCharsetName(String charsetName) {
    this.charsetName_ = charsetName;
  }
  
  public Class<? extends LineReader> getLineReaderClass() {
    return this.lineReaderClass_;
  }
  
  public void setLineReaderClass(Class<? extends LineReader> lineReaderClass) {
    this.lineReaderClass_ = lineReaderClass;
  }
  
  public int getId(String w) {
    long h = find(w);
    return ((Integer)Utils.<Long, Integer>mapGetOrDefault(this.word2int, Long.valueOf(h), WORDID_DEFAULT)).intValue();
  }
  
  public EntryType getType(int id) {
    Utils.checkArgument((id >= 0));
    Utils.checkArgument((id < this.size));
    return ((Entry)this.words.get(id)).type;
  }
  
  public String getWord(int id) {
    Utils.checkArgument((id >= 0));
    Utils.checkArgument((id < this.size));
    return ((Entry)this.words.get(id)).word;
  }
  
  public String getLabel(int lid) {
    Utils.checkArgument((lid >= 0));
    Utils.checkArgument((lid < this.nlabels));
    return ((Entry)this.words.get(lid + this.nwords)).word;
  }
  
  public long find(String w) {
    long h = hash(w) % 30000000L;
    Entry e = null;
    while (Utils.mapGetOrDefault(this.word2int, Long.valueOf(h), WORDID_DEFAULT) != WORDID_DEFAULT && (
      e = this.words.get(((Integer)this.word2int.get(Long.valueOf(h))).intValue())) != null && 
      !w.equals(e.word))
      h = (h + 1L) % 30000000L; 
    return h;
  }
  
  public void add(String w) {
    long h = find(w);
    if (Utils.mapGetOrDefault(this.word2int, Long.valueOf(h), WORDID_DEFAULT) == WORDID_DEFAULT) {
      Entry e = new Entry();
      e.word = w;
      e.count = 1L;
      e.type = w.startsWith(this.args_.label) ? EntryType.label : EntryType.word;
      this.words.add(e);
      this.word2int.put(Long.valueOf(h), Integer.valueOf(this.size++));
    } else {
      ((Entry)this.words.get(((Integer)this.word2int.get(Long.valueOf(h))).intValue())).count++;
    } 
    this.ntokens++;
  }
  
  public final List<Integer> getNgrams(String word) {
    if (!word.equals("</s>"))
      return computeNgrams("<" + word + ">"); 
    int id = getId(word);
    if (id != WORDID_DEFAULT.intValue())
      return getNgrams(id); 
    return new ArrayList<>();
  }
  
  public final List<Integer> getNgrams(int i) {
    Utils.checkArgument((i >= 0));
    Utils.checkArgument((i < this.nwords));
    return ((Entry)this.words.get(i)).subwords;
  }
  
  public void addNgrams(List<Integer> line, int n) {
    Utils.checkArgument((n > 0));
    int line_size = line.size();
    for (int i = 0; i < line_size; i++) {
      BigInteger h = BigInteger.valueOf(((Integer)line.get(i)).intValue());
      BigInteger r = BigInteger.valueOf(116049371L);
      BigInteger b = BigInteger.valueOf(this.args_.bucket);
      for (int j = i + 1; j < line_size && j < i + n; j++) {
        h = h.multiply(r).add(BigInteger.valueOf(((Integer)line.get(j)).intValue()));
        line.add(Integer.valueOf(this.nwords + h.remainder(b).intValue()));
      } 
    } 
  }
  
  public int getLine(String[] tokens, List<Integer> words, List<Integer> labels, Random urd) {
    words.clear();
    labels.clear();
    int ntokens = 0;
    if (tokens != null)
      for (int i = 0; i <= tokens.length; i++) {
        if (i >= tokens.length || !Utils.isEmpty(tokens[i])) {
          int wid = (i == tokens.length) ? getId("</s>") : getId(tokens[i]);
          if (wid >= 0) {
            ntokens++;
            EntryType type = getType(wid);
            if (type == EntryType.word && !discard(wid, Utils.randomFloat(urd, 0.0F, 1.0F)))
              words.add(Integer.valueOf(wid)); 
            if (type == EntryType.label)
              labels.add(Integer.valueOf(wid - this.nwords)); 
            if (words.size() > 1024 && this.args_.model != Args.ModelType.sup)
              break; 
          } 
        } 
      }  
    return ntokens;
  }
  
  public List<Long> countType(EntryType type) {
    int size = (EntryType.label == type) ? nlabels() : nwords();
    List<Long> counts = new ArrayList<>(size);
    for (Entry w : this.words) {
      if (w.type == type)
        counts.add(Long.valueOf(w.count)); 
    } 
    return counts;
  }
  
  public void readFromFile(String file) throws Exception {
    Exception exception1 = null, exception2 = null;
    try {
      LineReader lineReader = this.lineReaderClass_
        .getConstructor(new Class[] { String.class, String.class }).newInstance(new Object[] { file, this.charsetName_ });
      try {
        long minThreshold = 1L;
        String[] lineTokens;
        while ((lineTokens = lineReader.readLineTokens()) != null) {
          for (int i = 0; i <= lineTokens.length; i++) {
            if (i == lineTokens.length) {
              add("</s>");
            } else {
              if (Utils.isEmpty(lineTokens[i]))
                continue; 
              add(lineTokens[i]);
            } 
            if (this.size > 2.25E7D) {
              minThreshold++;
              threshold(minThreshold, minThreshold);
            } 
            if (this.ntokens % 1000000L == 0L && this.args_.verbose > 1)
              System.out.printf("\rRead %dM words", new Object[] { Long.valueOf(this.ntokens / 1000000L) }); 
            continue;
          } 
        } 
      } finally {
        if (lineReader != null)
          lineReader.close(); 
      } 
    } finally {
      exception2 = null;
      if (exception1 == null) {
        exception1 = exception2;
      } else if (exception1 != exception2) {
        exception1.addSuppressed(exception2);
      } 
    } 
    if (Args.ModelType.cbow == this.args_.model || Args.ModelType.sg == this.args_.model)
      initNgrams(); 
    if (this.args_.verbose > 0) {
      System.out.printf("\rRead %dM words\n", new Object[] { Long.valueOf(this.ntokens / 1000000L) });
      System.out.println("Number of words:  " + this.nwords);
      System.out.println("Number of labels: " + this.nlabels);
    } 
    if (this.size == 0)
      throw new Exception("Empty vocabulary. Try a smaller -minCount value."); 
  }
  
  public void load(InputStream is) throws IOException {
    IOUtil io = new IOUtil();
    this.size = io.readInt(is);
    this.nwords = io.readInt(is);
    this.nlabels = io.readInt(is);
    this.ntokens = io.readLong(is);
    this.pruneIdxSize = io.readLong(is);
    this.words = new ArrayList<>(this.size);
    this.word2int = new HashMap<>(this.size);
    int i;
    for (i = 0; i < this.size; i++) {
      Entry e = new Entry();
      e.word = io.readString(is);
      e.count = io.readLong(is);
      e.type = EntryType.fromValue(io.readByteAsInt(is));
      this.words.add(e);
      this.word2int.put(Long.valueOf(find(e.word)), Integer.valueOf(i));
    } 
    this.pruneIdx = new HashMap<>((int)Math.max(0L, this.pruneIdxSize));
    if (this.pruneIdxSize > 0L)
      for (i = 0; i < this.pruneIdxSize; i++) {
        int first = io.readInt(is);
        int second = io.readInt(is);
        this.pruneIdx.put(Integer.valueOf(first), Integer.valueOf(second));
      }  
    initTableDiscard();
    initNgrams();
  }
  
  public void save(OutputStream ofs) throws IOException {
    IOUtil io = new IOUtil();
    ofs.write(io.intToByteArray(this.size));
    ofs.write(io.intToByteArray(this.nwords));
    ofs.write(io.intToByteArray(this.nlabels));
    ofs.write(io.longToByteArray(this.ntokens));
    ofs.write(io.longToByteArray(this.pruneIdxSize));
    for (int i = 0; i < this.size; i++) {
      Entry e = this.words.get(i);
      ofs.write(e.word.getBytes());
      ofs.write(0);
      ofs.write(io.longToByteArray(e.count));
      ofs.write(io.intToByte(e.type.value));
    } 
  }
  
  public void threshold(long t, long tl) {
    Collections.sort(this.words, this.entry_comparator);
    Iterator<Entry> iterator = this.words.iterator();
    while (iterator.hasNext()) {
      Entry _entry = iterator.next();
      if ((EntryType.word == _entry.type && _entry.count < t) || (
        EntryType.label == _entry.type && _entry.count < tl))
        iterator.remove(); 
    } 
    ((ArrayList)this.words).trimToSize();
    this.size = 0;
    this.nwords = 0;
    this.nlabels = 0;
    this.word2int = new HashMap<>(this.words.size());
    for (Entry _entry : this.words) {
      long h = find(_entry.word);
      this.word2int.put(Long.valueOf(h), Integer.valueOf(this.size++));
      if (EntryType.word == _entry.type) {
        this.nwords++;
        continue;
      } 
      this.nlabels++;
    } 
  }
  
  public long hash(String str) {
    int h = -2128831035;
    byte b;
    int i;
    byte[] arrayOfByte;
    for (i = (arrayOfByte = str.getBytes()).length, b = 0; b < i; ) {
      byte strByte = arrayOfByte[b];
      h = (h ^ strByte) * 16777619;
      b++;
    } 
    return h & 0xFFFFFFFFL;
  }
  
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Dictionary [words_=");
    builder.append(this.words);
    builder.append(", pdiscard_=");
    builder.append(this.pdiscard);
    builder.append(", word2int_=");
    builder.append(this.word2int);
    builder.append(", size_=");
    builder.append(this.size);
    builder.append(", nwords_=");
    builder.append(this.nwords);
    builder.append(", nlabels_=");
    builder.append(this.nlabels);
    builder.append(", ntokens_=");
    builder.append(this.ntokens);
    builder.append("]");
    return builder.toString();
  }
  
  private void initTableDiscard() {
    this.pdiscard = new ArrayList<>(this.size);
    for (int i = 0; i < this.size; i++) {
      float f = (float)((Entry)this.words.get(i)).count / (float)this.ntokens;
      this.pdiscard.add(Float.valueOf((float)(Math.sqrt(this.args_.t / f) + this.args_.t / f)));
    } 
  }
  
  private void initNgrams() {
    for (int i = 0; i < this.size; i++) {
      String word = "<" + ((Entry)this.words.get(i)).word + ">";
      Entry e = this.words.get(i);
      e.subwords = new ArrayList<>();
      if (!((Entry)this.words.get(i)).word.equals("</s>"))
        e.subwords = computeNgrams(word); 
      e.subwords.add(Integer.valueOf(i));
    } 
  }
  
  private boolean charMatches(char ch) {
    return !(ch != ' ' && ch != '\t' && ch != '\n' && ch != '\f' && ch != '\r');
  }
  
  private boolean discard(int id, float rand) {
    Utils.checkArgument((id >= 0));
    Utils.checkArgument((id < this.nwords));
    return (this.args_.model == Args.ModelType.sup) ? false : ((rand > ((Float)this.pdiscard.get(id)).floatValue()));
  }
  
  private List<Integer> computeNgrams(String word) {
    List<Integer> ngrams = new ArrayList<>();
    if (word.equals("</s>"))
      return ngrams; 
    for (int i = 0; i < word.length(); i++) {
      StringBuilder ngram = new StringBuilder();
      if (!charMatches(word.charAt(i)))
        for (int j = i, n = 1; j < word.length() && n <= this.args_.maxn; n++) {
          ngram.append(word.charAt(j++));
          while (j < word.length() && charMatches(word.charAt(j)))
            ngram.append(word.charAt(j++)); 
          if (n >= this.args_.minn && (n != 1 || (i != 0 && j != word.length()))) {
            int h = (int)(this.nwords + hash(ngram.toString()) % this.args_.bucket);
            if (h < 0)
              logger.error("computeNgrams h<0: " + h + " on word: " + word); 
            pushHash(ngrams, h);
          } 
        }  
    } 
    return ngrams;
  }
  
  private void pushHash(List<Integer> hashes, int id) {
    if (this.pruneIdxSize == 0L || id < 0)
      return; 
    if (this.pruneIdxSize > 0L) {
      int pruneId = getPruning(id);
      if (pruneId >= 0) {
        id = pruneId;
      } else {
        return;
      } 
    } 
    hashes.add(Integer.valueOf(id));
  }
  
  private int getPruning(int id) {
    return ((Integer)this.pruneIdx.getOrDefault(Integer.valueOf(id), Integer.valueOf(-1))).intValue();
  }
  
  public Dictionary(Args args) {
    this.entry_comparator = new Comparator<Entry>() {
        public int compare(Dictionary.Entry o1, Dictionary.Entry o2) {
          int cmp = (o1.type.value < o2.type.value) ? -1 : ((o1.type.value == o2.type.value) ? 0 : 1);
          if (cmp == 0)
            cmp = (o2.count < o1.count) ? -1 : ((o2.count == o1.count) ? 0 : 1); 
          return cmp;
        }
      };
    this.args_ = args;
    this.words = new ArrayList<>(30000000);
    this.word2int = new HashMap<>(30000000);
  }
  
  public enum EntryType {
    word(0),
    label(1);
    
    private int value;
    
    public int getValue() {
      return this.value;
    }
    
    public static EntryType fromValue(int value) throws IllegalArgumentException {
      try {
        return values()[value];
      } catch (ArrayIndexOutOfBoundsException e) {
        throw new IllegalArgumentException("Unknown entry_type enum value :" + value);
      } 
    }
    
    public String toString() {
      return (this.value == 0) ? "word" : ((this.value == 1) ? "label" : "unknown");
    }
    
    EntryType(int value) {
      this.value = value;
    }
  }
  
  public class Entry {
    public String word;
    
    public Dictionary.EntryType type;
    
    public long count;
    
    public List<Integer> subwords;
    
    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("entry [word=");
      builder.append(this.word);
      builder.append(", count=");
      builder.append(this.count);
      builder.append(", type=");
      builder.append(this.type);
      builder.append(", subwords=");
      builder.append(this.subwords);
      builder.append("]");
      return builder.toString();
    }
  }
}

package fasttext;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.math3.distribution.UniformRealDistribution;

import fasttext.Args.model_name;
import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedInteger;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

public class Dictionary {

	private static final int MAX_VOCAB_SIZE = 30000000;
	private static final int MAX_LINE_SIZE = 1024;

	// private static final String EOS = "</s>";
	private static final String BOW = "<";
	private static final String EOW = ">";

	public enum entry_type {
		word(0), label(1);

		private int value;

		private entry_type(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}

		public static entry_type fromValue(int value) throws IllegalArgumentException {
			try {
				return entry_type.values()[value];
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new IllegalArgumentException("Unknown entry_type enum value :" + value);
			}
		}

		@Override
		public String toString() {
			return value == 0 ? "word" : value == 1 ? "label" : "unknown";
		}
	}

	public class entry {
		String word;
		long count;
		entry_type type;
		Vector<Integer> subwords;
	}

	private Vector<entry> words_;
	private Vector<Float> pdiscard_;
	private Long2IntOpenHashMap word2int_;
	private int size_;
	private int nwords_;
	private int nlabels_;
	private long ntokens_;

	private Args args_;

	public Dictionary(Args args) {
		args_ = args;
		size_ = 0;
		nwords_ = 0;
		nlabels_ = 0;
		ntokens_ = 0;
		word2int_ = new Long2IntOpenHashMap(MAX_VOCAB_SIZE);
		((Long2IntOpenHashMap) word2int_).defaultReturnValue(-1);
		words_ = new Vector<entry>(MAX_VOCAB_SIZE);
	}

	public long find(final String w) {
		long h = hash(w) % MAX_VOCAB_SIZE;
		entry e = null;
		while (word2int_.get(h) != -1 && ((e = words_.get(word2int_.get(h))) != null && !w.equals(e.word))) {
			h = (h + 1) % MAX_VOCAB_SIZE;
		}
		return h;
	}

	public void add(final String w) {
		long h = find(w);
		ntokens_++;
		if (word2int_.get(h) == -1) {
			entry e = new entry();
			e.word = w;
			e.count = 1;
			e.type = w.contains(args_.label) ? entry_type.label : entry_type.word;
			words_.add(e);
			word2int_.put(h, size_++);
		} else {
			words_.get(word2int_.get(h)).count++;
		}
	}

	public int nwords() {
		return nwords_;
	}

	public int nlabels() {
		return nlabels_;
	}

	public long ntokens() {
		return ntokens_;
	}

	public final Vector<Integer> getNgrams(int i) {
		Preconditions.checkArgument(i >= 0);
		Preconditions.checkArgument(i < nwords_);
		return words_.get(i).subwords;
	}

	public final Vector<Integer> getNgrams(final String word) {
		Vector<Integer> ngrams = new Vector<Integer>();
		int i = getId(word);
		if (i >= 0) {
			ngrams = words_.get(i).subwords;
		} else {
			computeNgrams(BOW + word + EOW, ngrams);
		}
		return ngrams;
	}

	public boolean discard(int id, float rand) {
		Preconditions.checkArgument(id >= 0);
		Preconditions.checkArgument(id < nwords_);
		if (args_.model == model_name.sup)
			return false;
		return rand > pdiscard_.get(id);
	}

	public int getId(final String w) {
		long h = find(w);
		return word2int_.get(h);
	}

	public entry_type getType(int id) {
		Preconditions.checkArgument(id >= 0);
		Preconditions.checkArgument(id < size_);
		return words_.get(id).type;
	}

	public String getWord(int id) {
		Preconditions.checkArgument(id >= 0);
		Preconditions.checkArgument(id < size_);
		return words_.get(id).word;
	}

	/**
	 * String FNV-1a Hash
	 * 
	 * @param str
	 * @return
	 */
	public static long hash(final String str) {
		int h = (int) 2166136261L;// 0xffffffc5;
		for (byte strByte : str.getBytes()) {
			h = (h ^ strByte) * 16777619; // FNV-1a
			// h = (h * 16777619) ^ strByte; //FNV-1
		}
		return UnsignedInteger.fromIntBits(h).longValue();
	}

	public void computeNgrams(final String word, Vector<Integer> ngrams) {
		for (int i = 0; i < word.length(); i++) {
			StringBuilder ngram = new StringBuilder();
			if (charMatches(word.charAt(i))) {
				continue;
			}
			for (int j = i, n = 1; j < word.length() && n <= args_.maxn; n++) {
				ngram.append(word.charAt(j++));
				while (j < word.length() && charMatches(word.charAt(j))) {
					ngram.append(word.charAt(j++));
				}
				if (n >= args_.minn && !(n == 1 && (i == 0 || j == word.length()))) {
					int h = (int) (hash(ngram.toString()) % args_.bucket);
					if (h < 0) {
						System.err.println("computeNgrams h<0: " + h + " on word: " + word);
					}
					ngrams.add(nwords_ + h);
				}
			}
		}
	}

	private boolean charMatches(char ch) {
		if (ch == ' ') {
			return true;
		} else if (ch == '\t') {
			return true;
		} else if (ch == '\n') {
			return true;
		} else if (ch == '\f') { // \x0B
			return true;
		} else if (ch == '\r') {
			return true;
		}
		return false;
	}

	public void initNgrams() {
		for (int i = 0; i < size_; i++) {
			String word = BOW + words_.get(i).word + EOW;
			entry e = words_.get(i);
			if (e.subwords == null) {
				e.subwords = new Vector<Integer>();
			}
			e.subwords.add(i);
			computeNgrams(word, e.subwords);
		}
	}

	public void readFromFile(String file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
		try {
			long minThreshold = 1;
			String line;
			while ((line = br.readLine()) != null) {
				if (Utils.isEmpty(line) || line.startsWith("#")) {
					continue;
				}
				String[] words = line.split("\\s+");
				for (String word : words) {
					add(word);
					if (ntokens_ % 1000000 == 0 && args_.verbose > 1) {
						System.out.printf("\rRead %dM words", ntokens_ / 1000000);
					}
					if (size_ > 0.75 * MAX_VOCAB_SIZE) {
						minThreshold++;
						threshold(minThreshold, minThreshold);
					}
				}
			}
		} finally {
			fis.close();
			br.close();
		}
		threshold(args_.minCount, args_.minCountLabel);
		initTableDiscard();
		if (model_name.cbow == args_.model || model_name.sg == args_.model) {
			initNgrams();
		}
		if (args_.verbose > 0) {
			System.out.printf("\rRead %dM words\n", ntokens_ / 1000000);
			System.out.println("Number of words:  " + nwords_);
			System.out.println("Number of labels: " + nlabels_);
		}
		if (size_ == 0) {
			System.err.println("Empty vocabulary. Try a smaller -minCount value.");
			System.exit(1);
		}
	}

	public void threshold(long t, long tl) {
		Collections.sort(words_, entry_comparator);
		Iterator<entry> iterator = words_.iterator();
		while (iterator.hasNext()) {
			entry _entry = iterator.next();
			if ((entry_type.word == _entry.type && _entry.count < t)
					|| (entry_type.label == _entry.type && _entry.count < tl)) {
				iterator.remove();
			}
		}
		size_ = 0;
		nwords_ = 0;
		nlabels_ = 0;
		word2int_.clear();
		for (entry _entry : words_) {
			long h = find(_entry.word);
			word2int_.put(h, size_++);
			if (entry_type.word == _entry.type) {
				nwords_++;
			}
			if (entry_type.label == _entry.type) {
				nlabels_++;
			}
		}
	}

	private transient Comparator<entry> entry_comparator = new Comparator<entry>() {
		@Override
		public int compare(entry o1, entry o2) {
			int cmp = o1.type.value > o2.type.value ? +1 : o1.type.value < o2.type.value ? -1 : 0;
			if (cmp == 0) {
				cmp = o2.count > o1.count ? +1 : o2.count < o1.count ? -1 : 0;
			}
			return cmp;
		}
	};

	public void initTableDiscard() {
		pdiscard_ = new Vector<Float>(size_);
		for (int i = 0; i < size_; i++) {
			float f = (float) (words_.get(i).count) / (float) ntokens_;
			pdiscard_.add((float) (Math.sqrt(args_.t / f) + args_.t / f));
		}
	}

	public Vector<Long> getCounts(entry_type type) {
		Vector<Long> counts = new Vector<Long>(words_.size());
		for (entry w : words_) {
			if (w.type == type)
				counts.add(w.count);
		}
		return counts;
	}

	public void addNgrams(Vector<Integer> line, int n) {
		int line_size = line.size();
		for (int i = 0; i < line_size; i++) {
			long h = (long) line.get(i);
			for (int j = i + 1; j < line_size && j < i + n; j++) {
				h = h * 116049371 + line.get(j);
				line.add(nwords_ + (int) (h % args_.bucket));
			}
		}
	}

	public int getLine(String line, Vector<Integer> words, Vector<Integer> labels, UniformRealDistribution urd) {
		if (!Utils.isEmpty(line)) {
			String[] tokens = line.split("\\s+");
			return getLine(tokens, words, labels, urd);
		}
		return 0;
	}

	public int getLine(String[] tokens, Vector<Integer> words, Vector<Integer> labels, UniformRealDistribution urd) {
		int ntokens = 0;
		words.clear();
		labels.clear();
		if (tokens != null) {
			for (String token : tokens) {
				ntokens++;
				// if (token.equals(EOS))
				// break;
				int wid = getId(token);
				if (wid < 0) {
					continue;
				}
				entry_type type = getType(wid);
				if (type == entry_type.word && !discard(wid, (float) urd.sample())) {
					words.add(wid);
				}
				if (type == entry_type.label) {
					labels.add(wid - nwords_);
				}
				if (words.size() > MAX_LINE_SIZE && args_.model != model_name.sup)
					break;
			}
		}
		return ntokens;
	}

	public String getLabel(int lid) {
		Preconditions.checkArgument(lid >= 0);
		Preconditions.checkArgument(lid < nlabels_);
		return words_.get(lid + nwords_).word;
	}

	public void save(OutputStream ofs) throws IOException {
		ofs.write(IOUtil.intToByteArray(size_));
		ofs.write(IOUtil.intToByteArray(nwords_));
		ofs.write(IOUtil.intToByteArray(nlabels_));
		ofs.write(IOUtil.longToByteArray(ntokens_));
		// Charset charset = Charset.forName("UTF-8");
		for (int i = 0; i < size_; i++) {
			entry e = words_.get(i);
			ofs.write(e.word.getBytes());
			ofs.write(0);
			ofs.write(IOUtil.longToByteArray(e.count));
			ofs.write(e.type.value & 0xFF);
		}
	}

	public void load(InputStream ifs) throws IOException {
		words_.clear();
		word2int_.clear();
		size_ = IOUtil.readInt(ifs);
		nwords_ = IOUtil.readInt(ifs);
		nlabels_ = IOUtil.readInt(ifs);
		ntokens_ = IOUtil.readLong(ifs);

		for (int i = 0; i < size_; i++) {
			entry e = new entry();
			e.word = IOUtil.readString((DataInputStream) ifs);
			e.count = IOUtil.readLong(ifs);
			e.type = entry_type.fromValue(((DataInputStream) ifs).readByte() & 0xFF);
			words_.add(e);
			word2int_.put(find(e.word), i);
		}
		initTableDiscard();
		if (model_name.cbow == args_.model || model_name.sg == args_.model) {
			initNgrams();
		}
	}

}
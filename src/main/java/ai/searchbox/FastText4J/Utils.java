package ai.searchbox.FastText4J;

import java.io.IOException;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;

public class Utils {
  private static final int SHUFFLE_THRESHOLD = 5;
  
  public static void checkArgument(boolean expression) {
    if (!expression)
      throw new IllegalArgumentException(); 
  }
  
  public static boolean isEmpty(String str) {
    return !(str != null && !str.isEmpty());
  }
  
  public static <K, V> V mapGetOrDefault(Map<K, V> map, K key, V defaultValue) {
    return map.containsKey(key) ? map.get(key) : defaultValue;
  }
  
  public static int randomInt(Random rnd, int lower, int upper) {
    checkArgument(((lower <= upper)) & ((lower > 0)));
    if (lower == upper)
      return lower; 
    return rnd.nextInt(upper - lower) + lower;
  }
  
  public static float randomFloat(Random rnd, float lower, float upper) {
    checkArgument((lower <= upper));
    if (lower == upper)
      return lower; 
    return rnd.nextFloat() * (upper - lower) + lower;
  }
  
  public static long sizeLine(String filename) throws IOException {
    Exception exception1 = null, exception2 = null;
    try {
    
    } finally {
      exception2 = null;
      if (exception1 == null) {
        exception1 = exception2;
      } else if (exception1 != exception2) {
        exception1.addSuppressed(exception2);
      } 
    } 
  }
  
  public static void shuffle(List<?> list, Random rnd) {
    int size = list.size();
    if (size < 5 || list instanceof java.util.RandomAccess) {
      for (int i = size; i > 1; i--)
        swap(list, i - 1, rnd.nextInt(i)); 
    } else {
      Object[] arr = list.toArray();
      for (int i = size; i > 1; i--)
        swap(arr, i - 1, rnd.nextInt(i)); 
      ListIterator<?> it = list.listIterator();
      for (int j = 0; j < arr.length; j++) {
        it.next();
        it.set(arr[j]);
      } 
    } 
  }
  
  public static void swap(Object[] arr, int i, int j) {
    Object tmp = arr[i];
    arr[i] = arr[j];
    arr[j] = tmp;
  }
  
  public static void swap(List<?> list, int i, int j) {
    List<?> l = list;
    l.set(i, l.set(j, l.get(i)));
  }
}

package tools.config;

public class OptionUtils {
  public static final Boolean getBooleanArrayOption(Boolean[] array, Boolean defaultBoolean) {
    return new ArrayParaTeller<Boolean>(defaultBoolean).tell(array);
  }

  public static final String getStringArrayOption(String[] array, String defaultStr) {
    return new ArrayParaTeller<String>(defaultStr).tell(array);
  }

}

class ArrayParaTeller<T> {
  T defaultPara;

  public ArrayParaTeller(T defaultPara) {
    this.defaultPara = defaultPara;
  }

  public T tell(T[] array) {
    if (array == null || array.length == 0) {
      return defaultPara;
    }
    return array[0];
  }
}

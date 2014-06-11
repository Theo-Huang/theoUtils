package tools.coding;

public class EnumHelper {
  public static final <T extends Enum<T>> T find(Class<T> enumType, String target, T defaultEnum) {
    target = target.trim();
    for (T enumConstan : enumType.getEnumConstants()) {
      if (enumConstan.name().equalsIgnoreCase(target)) {
        return enumConstan;
      }
    }
    return defaultEnum;
  }
}

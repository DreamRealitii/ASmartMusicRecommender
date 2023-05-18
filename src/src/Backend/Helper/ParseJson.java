package Backend.Helper;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ethan Carnahan
 * Extracts data from Json objects.
 * Throw RuntimeException on failure.
 */
public class ParseJson {

  //region Public methods

  // Gets a json value key:{object} from a json object.
  public static String getObject(String jsonObject, String key) {
    if (checkNull(jsonObject, key))
      return null;

    return "{" + getValue(jsonObject, key, "{}") + "}";
  }

  // Gets a json value key:[array] from a json object.
  public static String[] getArray(String jsonObject, String key) {
    if (checkNull(jsonObject, key))
      return null;

    String array = getValue(jsonObject, key, "[]");
    if (array.length() == 0)
      return new String[0];

    // Split array into elements.
    List<String> result = new ArrayList<>();
    int start = 0;
    int numBounds = 0;
    boolean inString = false;
    for (int i = 0; i < array.length(); i++) {
      // Found end of element, add to result.
      if (array.charAt(i) == ',' && numBounds == 0 && !inString) {
        result.add(array.substring(start, i));
        start = i + 1;
      }
      // Beginning/end of string.
      else if (array.charAt(i) == '\"') {
          inString = !inString;
      }
      // Beginning of array/object.
      else if (array.charAt(i) == '[' || array.charAt(i) == '{') {
          numBounds++;
      }
      // End of array/object.
      else if (array.charAt(i) == ']' || array.charAt(i) == '}') {
          numBounds--;
      }
    }
    // Add last element
    result.add(array.substring(start));

    return result.toArray(new String[0]);
  }

  // Gets a json value key:"string" from a json object.
  public static String getString(String jsonObject, String key) {
    if (checkNull(jsonObject, key))
      return null;

    return getValue(jsonObject, key, "\"\"");
  }

  public static Boolean getBool(String jsonObject, String key) {
    if (checkNull(jsonObject, key))
      return null;

    String result = getNumber(jsonObject, key);
      if (result.equals("true") || result.equals("false")) {
          return Boolean.parseBoolean(result);
      } else {
          throw new RuntimeException("ParseJson: Did not find boolean at key " + key);
      }
  }

  // Gets a json value key:integer from a json object.
  public static Integer getInt(String jsonObject, String key) {
    if (checkNull(jsonObject, key))
      return null;

    return Integer.parseInt(getNumber(jsonObject, key));
  }

  // Gets a json value key.double from a json object.
  public static Double getDouble(String jsonObject, String key) {
    if (checkNull(jsonObject, key))
      return null;

    return Double.parseDouble(getNumber(jsonObject, key));
  }

  //endregion

  //region Private methods

  // "main" method.
  // bounds are "" for strings, [] for arrays, and {} for objects.
  private static String getValue(String jsonObject, String key, String bounds) {
    String string = removeSpaces(jsonObject);

    int start = findStart(string, key, bounds);
    int end = findEnd(string, key, start - (bounds.length() / 2), bounds);

    return string.substring(start, end);
  }

  // Finds the beginning of a json value that isn't in a nested object.
  private static int findStart(String jsonObject, String key, String bounds) {
    int numBounds = 0;
    for (int i = 0; i < jsonObject.length(); i++) {
      if (jsonObject.charAt(i) == '{' || jsonObject.charAt(i) == '[')
        numBounds++;
      else if (jsonObject.charAt(i) == '}' || jsonObject.charAt(i) == ']')
        numBounds--;
      if (jsonObject.charAt(i) == '\"' && numBounds == 1) {
        // Check if string is key.
        int stringEnd = jsonObject.indexOf('\"', i+1);
        if (stringEnd == -1)
          throw new RuntimeException("ParseJson: Failed to find key " + key);
        String string = jsonObject.substring(i+1, stringEnd);
        if (string.equals(key))
          return i + key.length() + 3 + (bounds.length()/2);
      }
    }
    throw new RuntimeException("ParseJson: Failed to find key " + key);
  }

  // Finds the end of a json value.
  private static int findEnd(String jsonObject, String key, int start, String bounds) {
    // Check types.
      if (!bounds.isEmpty() && jsonObject.charAt(start) != bounds.charAt(0)) {
          throw new RuntimeException("ParseJson: Json type mismatch - " +
              jsonObject.charAt(start) + " != " + bounds.charAt(0));
      }

    // Find end of number/boolean.
    int result;
    if (bounds.isEmpty()) {
      result = jsonObject.indexOf(",", start + 1);
      // Value is at end of jsonObject.
        if (result == -1) {
            return jsonObject.length() - 1;
        } else {
            return result;
        }
    }

    // Find end of string value.
    if (bounds.charAt(1) == '\"') {
      result = jsonObject.indexOf("\"", start + 1);
        if (result == -1) {
            throw new RuntimeException("ParseJson: Could not find string at key " + key);
        }
      return result;
    }

    // Find end of array or object value.
    int numBounds = 1;
    for (int i = start + 1; i < jsonObject.length(); i++) {
        if (jsonObject.charAt(i) == bounds.charAt(0)) {
            numBounds++;
        }
        if (jsonObject.charAt(i) == bounds.charAt(1)) {
            numBounds--;
        }
        if (numBounds == 0) {
            return i;
        }
    }

    throw new RuntimeException("ParseJson: Could not find end of " +
        (bounds.charAt(0) == '[' ? "array" : "object") + " at key " + key);
  }

  private static String getNumber(String jsonObject, String key) {
    return getValue(jsonObject, key, "");
  }

  // Removes spaces between keys and values.
  private static String removeSpaces(String jsonString) {
    StringBuilder result = new StringBuilder(jsonString.length());
    boolean inString = false;
    for (int i = 0; i < jsonString.length(); i++) {
        if (jsonString.charAt(i) == '\"') {
            inString = !inString;
        }
        if (inString || jsonString.charAt(i) != ' ') {
            result.append(jsonString.charAt(i));
        }
    }
    return result.toString();
  }

  private static boolean checkNull(String jsonString, String key) {
    String json = removeSpaces(jsonString);

    int start = findStart(json, key, "");
    int end = findEnd(json, key, start, "");

    return json.substring(start, end).equalsIgnoreCase("null");
  }

  //endregion

}

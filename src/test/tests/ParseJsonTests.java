import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import Backend.Helper.ParseJson;
import java.util.Objects;
import org.junit.jupiter.api.*;

public class ParseJsonTests {
  private static final String jsonEmptyObject = "{ \"object\" : { } }";
  private static final String jsonEmptyArray = "{ \"array\" : [ ] }";
  private static final String jsonEmptyString = "{ \"string\" : \"\" }";

  private static final String jsonBool = "{ \"bool\" : true }";
  private static final String jsonInt = "{ \"int\" : 101 }";
  private static final String jsonDouble = "{ \"double\" : 101.01 }";
  private static final String jsonString = "{ \"string\" : \" s p a c e d o u t \" }";
  private static final String jsonArray = "{ \"array\" : [1, 3, 5, 3, 1] }";

  private static final String jsonKeys = "{ \"1\" : \"3:\", \"2\" : \"1:\", \"3\" : \"2:\" }";

  private static final String jsonNestedValue = "{ \"object\" : { \"array\" : [ \"nailed it\" ] } }";
  private static final String jsonNestedObject = "{ \"object\" : { \"array\" : [ { \"string\" : \"nailed it\" } ] } }";

  private static final String jsonNull = "{ \"null\" : null }";

  @Test
  public void testEmptyItems() {
    assertEquals("{}", ParseJson.getObject(jsonEmptyObject, "object"));
    assertEquals(0, (Objects.requireNonNull(ParseJson.getArray(jsonEmptyArray, "array"))).length);
    assertEquals("", ParseJson.getString(jsonEmptyString, "string"));
  }

  @Test
  public void testOneValue() {
    assertEquals(Boolean.TRUE, ParseJson.getBool(jsonBool, "bool"));
    assertEquals(101, ParseJson.getInt(jsonInt, "int"));
    assertEquals(101.01, ParseJson.getDouble(jsonDouble, "double"));
    assertEquals(" s p a c e d o u t ", ParseJson.getString(jsonString, "string"));
  }

  @Test
  public void testGetArray() {
    String[] array = ParseJson.getArray(jsonArray, "array");
    assertNotNull(array);
    assertEquals(5, array.length);
    assertEquals("1", array[0]);
    assertEquals("3", array[1]);
    assertEquals("5", array[2]);
    assertEquals("3", array[3]);
    assertEquals("1", array[4]);
  }

  @Test
  public void testKeyInString() {
    assertEquals("3:", ParseJson.getString(jsonKeys, "1"));
    assertEquals("1:", ParseJson.getString(jsonKeys, "2"));
    assertEquals("2:", ParseJson.getString(jsonKeys, "3"));
  }

  @Test
  public void testNestedValues() {
    String object = ParseJson.getObject(jsonNestedValue, "object");
    String[] array = ParseJson.getArray(object, "array");
    assertNotNull(array);
    assertEquals(1, array.length);
    assertEquals("\"nailed it\"", array[0]);

    object = ParseJson.getObject(jsonNestedObject, "object");
    array = ParseJson.getArray(object, "array");
    assertNotNull(array);
    assertEquals(1, array.length);
    assertEquals("nailed it", ParseJson.getString(array[0], "string"));
  }

  @Test
  public void testNullValues() {
    assertNull(ParseJson.getString(jsonNull, "null"));
    assertNull(ParseJson.getBool(jsonNull, "null"));
    assertNull(ParseJson.getInt(jsonNull, "null"));
    assertNull(ParseJson.getDouble(jsonNull, "null"));
    assertNull(ParseJson.getArray(jsonNull, "null"));
    assertNull(ParseJson.getObject(jsonNull, "null"));
  }
}

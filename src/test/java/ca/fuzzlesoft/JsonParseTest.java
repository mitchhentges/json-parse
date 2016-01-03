package ca.fuzzlesoft;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mitch
 * @since 30/12/15
 */
public class JsonParseTest {

    private JsonParse jsonParse;

    @Before
    public void setUp() {
        jsonParse = new JsonParse();
    }

    @Test
    public void shouldParseStrings() {
        String test = "{\"foo\":\"bar\"}";
        Map<String, Object> expected = MapBuilder.init()
                .add("foo", "bar")
                .build();
        Assert.assertEquals(expected, jsonParse.map(test));
    }

    @Test
    public void shouldParseNumbers() {
        String test = "{\"foo\":1234,\"bar\":-1.1e+1,\"baz\":2.2E-2}";
        Map<String, Object> expected = MapBuilder.init()
                .add("foo", 1234L)
                .add("bar", Double.valueOf("-1.1e+1"))
                .add("baz", Double.valueOf("2.2E-2"))
                .build();
        Assert.assertEquals(expected, jsonParse.map(test));
    }

    @Test
    public void shouldParseBooleans() {
        String test = "{\"foo\":true,\"bar\":false}";
        Map<String, Object> expected = MapBuilder.init()
                .add("foo", true)
                .add("bar", false)
                .build();
        Assert.assertEquals(expected, jsonParse.map(test));
    }

    @Test
    public void shouldParseNulls() {
        String test = "{\"foo\":null}";
        Map<String, Object> expected = MapBuilder.init()
                .add("foo", null)
                .build();
        Assert.assertEquals(expected, jsonParse.map(test));
    }

    @Test
    public void shouldAllowSpacesAroundColons() {
        String test = "{\"foo\" :  true}";
        Map<String, Object> expected = MapBuilder.init()
                .add("foo", true)
                .build();
        Assert.assertEquals(expected, jsonParse.map(test));
    }

    @Test
    public void shouldAllowSpacesAndCommasInPropertyNames() {
        String test = "{\"special, foo\": \"isspecial\"}";
        Map<String, Object> expected = MapBuilder.init()
                .add("special, foo", "isspecial")
                .build();
        Assert.assertEquals(expected, jsonParse.map(test));
    }

    @Test
    public void shouldAllowSpacesInStringPropertyValues() {
        String test = "{\"specialfoo\": \"is, special\"}";
        Map<String, Object> expected = MapBuilder.init()
                .add("specialfoo", "is, special")
                .build();
        Assert.assertEquals(expected, jsonParse.map(test));
    }

    @Test
    public void shouldIgnoreSurroundWhitespace() {
        String test = "    {\"foo\": true}   ";
        Map<String, Object> expected = MapBuilder.init()
                .add("foo", true)
                .build();
        Assert.assertEquals(expected, jsonParse.map(test));
    }

    @Test
    public void shouldAcceptABunchOfPropertiesSeperatedByWhitespace() {
        String test = "{\"string\":\"foo\"\n,\n\"number\"\t:123 ,\"true\":true,\"false\":false,\"null\":null}";
        Map<String, Object> expected = MapBuilder.init()
                .add("string", "foo")
                .add("number", 123L)
                .add("true", true)
                .add("false", false)
                .add("null", null)
                .build();
        Assert.assertEquals(expected, jsonParse.map(test));
    }

    @Test
    public void shouldParseNestedObject() {
        String test = "{\"obj\":{\"swag\":true}}";
        Map<String, Object> expected = MapBuilder.init()
                .add("obj", MapBuilder.init().add("swag", true).build())
                .build();
        Assert.assertEquals(expected, jsonParse.map(test));
    }

    @Test
    public void shouldAllowSpecialCharactersInStrings() {
        String test = "{\"foo\":\"{}[]'1234\"}";
        Map<String, Object> expected = MapBuilder.init()
                .add("foo", "{}[]'1234")
                .build();
        Assert.assertEquals(expected, jsonParse.map(test));
    }

    @Test
    public void shouldParseList() {
        String test = "[1, \"foo\", true]";
        List<Object> list = Arrays.<Object>asList(1L, "foo", true);
        Assert.assertEquals(jsonParse.list(test), list);
    }

    @Test
    public void shouldParseNestedNests() {
        String test = "{\"outer\":{\"array\":[\"inner1\",\"inner2\", [1,2,3]], \"ayy\":\"lmao\"}}";
        Map<String, Object> expected = MapBuilder.init()
                .add("outer", MapBuilder.init()
                        .add("array", Arrays.asList("inner1", "inner2", Arrays.asList(1L, 2L, 3L)))
                        .add("ayy", "lmao").build()
                )
                .build();
        Assert.assertEquals(expected, jsonParse.map(test));
    }

    @Test
    public void shouldThrowExceptionIfNoObject() {
        String noObject = "    ";
        String noEnd = " { ";
        String noStart = " } ";

        try {
            jsonParse.map(noObject);
            Assert.fail("Did not not throw exception when there was no containing object");
        } catch (Exception ignored) {}

        try {
            jsonParse.map(noEnd);
            Assert.fail("Did not throw exception when object didn't end");
        } catch (Exception ignored) {}

        try {
            jsonParse.map(noStart);
            Assert.fail("Did not throw exception when object didn't start");
        } catch (Exception ignored) {}
    }

    @Test
    public void shouldAllowTabsOrNewlines() {
        String test = "\t\n{\t}\n\t";
        Map<String, Object> expected = new HashMap<>();
        Assert.assertEquals(expected, jsonParse.map(test));
    }

    static class MapBuilder {
        private final Map<String, Object> map = new HashMap<>();
        private MapBuilder() {}

        public MapBuilder add(String key, Object value) {
            map.put(key, value);
            return this;
        }

        public Map<String, Object> build() {
            return map;
        }

        public static MapBuilder init() {
            return new MapBuilder();
        }
    }
}
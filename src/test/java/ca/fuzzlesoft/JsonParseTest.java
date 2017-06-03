package ca.fuzzlesoft;

import org.junit.Assert;
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

    @Test
    public void shouldParseStrings() {
        String test = "{\"foo\":\"bar\"}";
        Map<String, Object> expected = MapBuilder.init()
                .add("foo", "bar")
                .build();
        Assert.assertEquals(expected, JsonParse.map(test));
    }

    @Test
    public void shouldParseEmptyString() {
        String test = "\"\"";
        Assert.assertEquals("", JsonParse.string(test));
    }

    @Test
    public void shouldParseSingleCharacterStrings() {
        String test = "{\"a\":\"b\"}";
        Map<String, Object> expected = MapBuilder.init()
                .add("a", "b")
                .build();
        Assert.assertEquals(expected, JsonParse.map(test));
    }

    @Test
    public void shouldParseNumbers() {
        String test = "{\"foo\":1234,\"bar\":-1.1e+1,\"baz\":2.2E-2,\"zurb\":1E-4,\"boop\":6}";
        Map<String, Object> expected = MapBuilder.init()
                .add("foo", 1234L)
                .add("bar", Double.valueOf("-1.1e+1"))
                .add("baz", Double.valueOf("2.2E-2"))
                .add("zurb", Double.valueOf("1E-4"))
                .add("boop", 6L)
                .build();
        Assert.assertEquals(expected, JsonParse.map(test));
    }

    @Test
    public void shouldExcludeCharactersAfterNumbers() {
        String test = "{\"foo\":1 ,\"bar\":2\t,\"baz\":3\n}";
        Map<String, Object> expected = MapBuilder.init()
                .add("foo", 1L)
                .add("bar", 2L)
                .add("baz", 3L)
                .build();
        Assert.assertEquals(expected, JsonParse.map(test));
    }

    @Test
    public void shouldParseBooleans() {
        String test = "{\"foo\":true,\"bar\":false}";
        Map<String, Object> expected = MapBuilder.init()
                .add("foo", true)
                .add("bar", false)
                .build();
        Assert.assertEquals(expected, JsonParse.map(test));
    }

    @Test
    public void shouldParseNulls() {
        String test = "{\"foo\":null}";
        Map<String, Object> expected = MapBuilder.init()
                .add("foo", null)
                .build();
        Assert.assertEquals(expected, JsonParse.map(test));
    }

    @Test
    public void shouldAllowSpacesAroundColons() {
        String test = "{\"foo\" :  true}";
        Map<String, Object> expected = MapBuilder.init()
                .add("foo", true)
                .build();
        Assert.assertEquals(expected, JsonParse.map(test));
    }

    @Test
    public void shouldAllowSpacesAndCommasInPropertyNames() {
        String test = "{\"special, foo\": \"isspecial\"}";
        Map<String, Object> expected = MapBuilder.init()
                .add("special, foo", "isspecial")
                .build();
        Assert.assertEquals(expected, JsonParse.map(test));
    }

    @Test
    public void shouldAllowSpacesInStringPropertyValues() {
        String test = "{\"specialfoo\": \"is, special\"}";
        Map<String, Object> expected = MapBuilder.init()
                .add("specialfoo", "is, special")
                .build();
        Assert.assertEquals(expected, JsonParse.map(test));
    }

    @Test
    public void shouldIgnoreSurroundWhitespace() {
        String test = "    {\"foo\": true}   ";
        Map<String, Object> expected = MapBuilder.init()
                .add("foo", true)
                .build();
        Assert.assertEquals(expected, JsonParse.map(test));
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
        Assert.assertEquals(expected, JsonParse.map(test));
    }

    @Test
    public void shouldParseNestedObject() {
        String test = "{\"obj\":{\"swag\":true}}";
        Map<String, Object> expected = MapBuilder.init()
                .add("obj", MapBuilder.init().add("swag", true).build())
                .build();
        Assert.assertEquals(expected, JsonParse.map(test));
    }

    @Test
    public void shouldAllowSpecialCharactersInStrings() {
        String test = "{\"foo\":\"{}[]'1234\"}";
        Map<String, Object> expected = MapBuilder.init()
                .add("foo", "{}[]'1234")
                .build();
        Assert.assertEquals(expected, JsonParse.map(test));
    }

    @Test
    public void shouldParseList() {
        String test = "[1, \"foo\", true]";
        List<Object> list = Arrays.<Object>asList(1L, "foo", true);
        Assert.assertEquals(JsonParse.list(test), list);
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
        Assert.assertEquals(expected, JsonParse.map(test));
    }

    @Test
    public void shouldThrowExceptionIfNoObject() {
        String noObject = "    ";
        String noEnd = " { ";
        String noStart = " } ";

        try {
            JsonParse.map(noObject);
            Assert.fail("Did not not throw exception when there was no containing object");
        } catch (Exception ignored) {}

        try {
            JsonParse.map(noEnd);
            Assert.fail("Did not throw exception when object didn't end");
        } catch (Exception ignored) {}

        try {
            JsonParse.map(noStart);
            Assert.fail("Did not throw exception when object didn't start");
        } catch (Exception ignored) {}
    }

    @Test
    public void shouldAllowTabsOrNewlines() {
        String test = "\t\n{\t}\n\t";
        Map<String, Object> expected = new HashMap<>();
        Assert.assertEquals(expected, JsonParse.map(test));
    }

    @Test
    public void shouldTestIndividualStrings() {
        Assert.assertEquals("foo", JsonParse.string("\n\t\"foo\"  "));
        Assert.assertEquals(null, JsonParse.string("\n\tnull  "));
    }

    @Test
    public void shouldTestIndividualNumbers() {
        Assert.assertEquals(1234L, JsonParse.number("\n\t1234"));
        Assert.assertEquals(Double.valueOf("-13.3e+7"), JsonParse.number("\n\t-13.3e+7"));
    }

    @Test
    public void shouldTestIndividualBooleans() {
        Assert.assertEquals(true, JsonParse.bool("\n\ttrue  "));
        Assert.assertEquals(false, JsonParse.bool("\n\tfalse  "));
    }

    @Test
    public void shouldAllowBackslashAtEndOfString() {
        Assert.assertEquals("a\\", JsonParse.string("\"a\\\\\""));
    }

    @Test
    public void shouldConvertControlSequences() {
        Assert.assertEquals("\"", JsonParse.string("\"\\\"\""));
        Assert.assertEquals("\\", JsonParse.string("\"\\\\\""));
        Assert.assertEquals("/", JsonParse.string("\"\\/\""));
        Assert.assertEquals("\b", JsonParse.string("\"\\b\""));
        Assert.assertEquals("\f", JsonParse.string("\"\\f\""));
        Assert.assertEquals("\n", JsonParse.string("\"\\n\""));
        Assert.assertEquals("\r", JsonParse.string("\"\\r\""));
        Assert.assertEquals("\t", JsonParse.string("\"\\t\""));
        Assert.assertEquals("A", JsonParse.string("\"\\u0041\""));
    }

    @Test
    public void shouldUseRootForTopLevelException() {
        assertFormatting("bork", "<root>: \"bork\" is not a valid constant. Missing quotes?");
    }

    @Test
    public void shouldThrowErrorIfNoJsonToParse() {
        assertFormatting("", "Provided JSON string did not contain a value");
    }

    @Test
    public void shouldFormatExceptionsWithJsonStack() {
        assertFormatting("{\"a\":{\"b\":{\"c\": fasle}}}", "<root>.a.b.c: \"fasle\" is not a valid constant. Missing quotes?");
        assertFormatting("{\"a\":true \"b\":false}", "<root>.a: wasn't followed by a comma");
        assertFormatting("{\"a\" true}", "<root>.a: wasn't followed by a colon");
        assertFormatting("{\"a\"::true}", "<root>.a: was followed by too many colons");
        assertFormatting("{\"a\": true, v}", "<root>: unexpected character 'v' where a property name is expected. Missing quotes?");
        assertFormatting("{\"a\": v}", "<root>.a: \"v\" is not a valid constant. Missing quotes?");
    }

    @Test
    public void shouldUseSquareBracketsForFormattingErrorsInArrays() {
        assertFormatting("[true, false false]", "<root>.[2]: wasn't preceded by a comma");
        assertFormatting("[v]", "<root>.[0]: \"v\" is not a valid constant. Missing quotes?");
        assertFormatting("{\"a\": [{v}]}", "<root>.a.[0]: unexpected character 'v' where a property name is expected. Missing quotes?");
        assertFormatting("[{\"key\":\"value\"},{\"other\":bap}]", "<root>.[1].other: \"bap\" is not a valid constant. Missing quotes?");
        assertFormatting("[!]", "<root>.[0]: Unexpected character \"!\" instead of array value");
    }

    private void assertFormatting(String test, String expected) {
        try {
            JsonParse.parse(test);
        } catch (JsonParseException e) {
            Assert.assertEquals(expected, e.getMessage());
        }
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
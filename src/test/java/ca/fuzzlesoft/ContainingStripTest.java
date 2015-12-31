package ca.fuzzlesoft;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author mitch
 * @since 30/12/15
 */
public class ContainingStripTest {
    @Test
    public void shouldStripContainingObjectAndWhitespace() {
        String test = "     {\"a\":\"true\",\"b\":[{},{}]}  ";
        String expected = "\"a\":\"true\",\"b\":[{},{}]";
        Assert.assertEquals(expected, ContainingStrip.OBJECT.strip(test));
    }

    @Test
    public void shouldStripContainingArray() {
        String test = "[1,2]";
        String expected = "1,2";
        Assert.assertEquals(expected, ContainingStrip.ARRAY.strip(test));
    }

    @Test
    public void shouldThrowExceptionIfNoObject() {
        String noObject = "    ";
        String noEnd = " { ";
        String noStart = " } ";
        ContainingStrip strip = ContainingStrip.OBJECT;

        try {
            strip.strip(noObject);
            Assert.fail("Did not not throw exception when there was no containing object");
        } catch (Exception ignored) {}

        try {
            strip.strip(noEnd);
            Assert.fail("Did not throw exception when object didn't end");
        } catch (Exception ignored) {}

        try {
            strip.strip(noStart);
            Assert.fail("Did not throw exception when object didn't start");
        } catch (Exception ignored) {}
    }

    @Test
    public void shouldAllowTabsOrNewlinesAroundObject() {
        String test = "\t\n{\t}\n\t";
        String expected = "\t";
        Assert.assertEquals(expected, ContainingStrip.OBJECT.strip(test));
    }
}
# Json Parse

A tool to quickly parse JSON into Java maps and lists. Goes in a single pass, so should be `O(n)`.
However, FasterXML's `jackson` still kicks it around. If a heavyweight library needs to be avoided, then this
is a good solution

## Comparison to Jackson (on my machine)

Testing with Jackson's `ObjectMapper` showed a solid 200ms startup time, after which thousands of lines of json could be
read in under a dozen milliseconds.

Json Parse starts up in less that 5ms, but really stumbles on anything other than quick sprints. For example, a
nicely-formatted 2000 line file takes over 4000ms, as opposed to Jackson's 8ms.

## ... Why?

Currently, I've got a project that communicates to the server with JSON. It's an Android app, but I'd prefer to keep
as much of the code as Android-free as possible, so it can be run with pure-Java with but a few tweaks. Sadly, JSON
parsing was done with Android's `JSONObject`, forcing dependent code to transitively depend on Android.

I figured "hey, Jackson's cool and all, but it's so _big_. Why not make my own JSON parser"?
Needless to say, a-day-younger-me isn't that clever of a guy. Jackson's speed benefits are very much worth the
additional class files.
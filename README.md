# Json Parse

A tool to quickly parse JSON into Java maps and lists. Goes in a single pass, so should be `O(n)`.
Competes very well with FasterXML's Jackson.

## Comparison to Jackson (on my machine)

Testing with Jackson's `ObjectMapper` showed a solid 200ms startup time. Following that, every parse takes at
least 2ms. Fortunately, when dealing with large amounts of JSON (e.g. 4000 nicely formatted lines), Jackson only
takes a few dozen milliseconds.

Json Parse initializes in less that 5ms, then speedily works along. It can usually take under half the time as Jackson
on parsing any JSON under 1000 (nicely formatted) lines long. As the number of lines increases, Jackson makes back
lost time and eventually wins.

### Small, bite-sized json

I benchmarked Jackson vs. Json Parse with a few event-like pieces of JSON.

```
Number of characters in string: 132
json-parse: 3ms
jackson: 263ms

Number of characters in string: 6746
json-parse: 2ms
jackson: 3ms

Number of characters in string: 6746
json-parse: 1ms
jackson: 2ms

Number of characters in string: 254
json-parse: 0ms
jackson: 2ms

Number of characters in string: 153
json-parse: 0ms
jackson: 2ms

Number of characters in string: 270
json-parse: 0ms
jackson: 2ms

Number of characters in string: 238
json-parse: 0ms
jackson: 2ms

Number of characters in string: 180
json-parse: 1ms
jackson: 2ms

Number of characters in string: 172
json-parse: 0ms
jackson: 2ms
```

### Nice big configuration JSON

```
Number of characters in string: 8852
json-parse: 13ms
jackson: 236ms

Number of characters in string: 5615
json-parse: 2ms
jackson: 5ms

Number of characters in string: 13794
json-parse: 5ms
jackson: 6ms

Number of characters in string: 83458
json-parse: 22ms
jackson: 11ms

Number of characters in string: 129075
json-parse: 12ms
jackson: 7ms

Number of characters in string: 87255
json-parse: 13ms
jackson: 4ms
```

## ... Why?

Currently, I've got a project that communicates to the server with JSON. It's an Android app, but I'd prefer to keep
as much of the code as Android-free as possible, so it can be run with pure-Java with but a few tweaks. Sadly, JSON
parsing was done with Android's `JSONObject`, forcing dependent code to transitively depend on Android.

I figured "hey, Jackson's cool and all, but it's so _big_. Why not make my own JSON parser"? This is the culmination
of my efforts, and it's quite effective.

## ... Why is the code so messy?

I was going for 100% speed and 5% maintainability. It's small, so the maintainability hit won't be too bad, and once
the bugs are worked out, then (ideally) nobody will ever have to touch the code again #pipedreams.

## License
[MIT License (Expat)](http://www.opensource.org/licenses/mit-license.php)
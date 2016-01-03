# Json Parse [![Build Status](https://travis-ci.org/mitchhentges/json-parse.svg?branch=master)](https://travis-ci.org/mitchhentges/json-parse)

A tool to quickly parse JSON into Java maps and lists. Competes well with FasterXML's Jackson with an extremely
small memory footprint

## Usage

```
String mapString = "{\"fast\":true, \"super-neat\":true}";
String listString = "[1, 2, false]";

JsonParse parse = new JsonParse();
Map<String, Object> map = parse.map(mapString);
List<Object> list = parse.list(listString);
```

## Features

* Convert a JSON string to a Java `List<Object>`
* Convert a JSON string to a Java `Map<String, Object>`
* Thread safe
* Throw exceptions with helpful messages with trace to error, e.g. 'a.b.c: "fasle" is an invalid value'

## FAQ

* Can this convert from JSON directly to `SomeObject`?

Unfortunately not. JSON doesn't have any type information, so explicit knowledge is required before JSON can be
converted. This is the job of a "binding framework"

* Why do my JSON traces show "[]" for array elements, rather than their index?

Since efficiency is a primary goal of this project, array indices could not be added to JSON traces. It would
cost some performance, and that's not worth it ;)

## License
[MIT License (Expat)](http://www.opensource.org/licenses/mit-license.php)
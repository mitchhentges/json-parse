# 1.3.3

Bugfix release

* Change format of error-trace message
* Failing to parse root constant should throw JsonParseException, not NullPointerException
* Ignore all input after root element

# 1.3.2

Bugfix release

* Empty strings will no longer break parsing

# 1.3.1

Performance release

* Now will handle arrays faster, as it delimits their current "propertyName" as `null` instead of `"[]"`

# 1.3.0

Performance and array-index-trace release

* Array index is now shown in JSON traces. [0] is the first element, [1] is the second, and so on
* Can now parse strings, numbers and constants as the root element. It doesn't have to be an array or object anymore
* More internal consistency in the way root objects are handled
* Performance increased by at least 25% (only one stack is used for managing state)

# 1.2.0

* `list()` and `map()` are now static methods. Building an object is no longer necessary

# 1.1.0

Performance release

* Refactored object `NAME` and value types `STRING`, `NUMBER` and `CONSTANT` to use forward look-aheads (my term) to
increase speed

# 1.0.3

Performance release

* Detecting constants (whitespace, numbers) is now faster
* Improves avoidance of processing for end-of-block (e.g. object or array), when no end-of-block is occurring

# 1.0.2

* Don't include Junit in compile scope. Instead, it should only be available for testing

# 1.0.1

Performance release

* Don't check if non-string value is null/true/false up front, just attempt to parse it upon value termination
* Add `CHANGELOG.md` to see version changes

# 1.0.0

Initial release

* Convert json string to array/map.
* Show json stack trace on error
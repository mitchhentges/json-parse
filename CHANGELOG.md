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
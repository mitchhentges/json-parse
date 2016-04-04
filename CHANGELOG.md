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
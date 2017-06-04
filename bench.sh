#!/bin/bash

echo "Count   |FasterXML(ms)|JsonParse(ms)";

i="10"
while [ $i -le 5000000 ]
do
	echo "	$i"
	echo "Faster:"
	java -jar target/json-parse-1.3.4-jar-with-dependencies.jar $i fasterxml

	echo "JsonParse:"
	java -jar target/json-parse-1.3.4-jar-with-dependencies.jar $i fuzzlesoft
	let i*=4
	echo
done

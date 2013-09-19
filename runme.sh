#!/bin/bash

rm *-gc.log

echo "Running Simple..."
$JAVA_HOME/bin/java -Xloggc:simple-gc.log \
     -verbose:gc \
     -XX:+PrintGCDateStamps \
     -XX:+PrintGCApplicationStoppedTime \
     -cp bin:lib/disruptor-3.2.0.jar yow2013.immutable.SimplePerformanceTest
echo "Done"

grep 'stopped:' simple-gc.log | sed 's/.*stopped: \([0-9.]*\) seconds/\1/' | sort -n | awk '{ printf "%1.3f\n", $1 }' | (echo " Count Millis" ; uniq -c )
     
echo "Running Custom..."
$JAVA_HOME/bin/java -Xloggc:custom-gc.log \
     -verbose:gc \
     -XX:+PrintGCDateStamps \
     -XX:+PrintGCApplicationStoppedTime \
     -cp bin:lib/disruptor-3.2.0.jar yow2013.immutable.CustomPerformanceTest
echo "Done"

grep 'stopped:' custom-gc.log | sed 's/.*stopped: \([0-9.]*\) seconds/\1/' | sort -n | awk '{ printf "%1.3f\n", $1 }' | (echo " Count Millis" ; uniq -c )
     

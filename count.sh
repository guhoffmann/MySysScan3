#!/bin/sh
# count lines of code written for this project ;)

c=0
arr="*.cfg *.sh src/*.java res/layout/*.xml res/menu/*.xml res/values/*.xml"
for i in $arr;do
	c=$(expr  $c + $(wc -l $i|awk '{print $1}'))
done
echo "Lines of code written: "$c


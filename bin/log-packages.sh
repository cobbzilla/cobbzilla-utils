#!/bin/bash
grep class,load | awk '{print $2}' | awk -F '$' '{print $1}' | awk -F '.' 'BEGIN {OFS="."} {$(NF--)=""; print}' | sed -e 's/\.$//' | sort | uniq

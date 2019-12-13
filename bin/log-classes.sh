#!/bin/bash

grep class,load | awk '{print $2}' | awk -F '$' '{print $1}' | sort | uniq

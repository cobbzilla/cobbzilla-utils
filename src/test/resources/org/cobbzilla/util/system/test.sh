#!/bin/bash

# Simulates a long-running process for CommandShellTest
# Periodically prints out certain lines that match regexes for measuring command progress

i=1
limit=500
while [ ${i} -le ${limit} ] ; do
  rand=$(od -An -d /dev/urandom | head -n 1 | awk '{print $1}')
  num_rand=$(expr ${rand} % 3)
  for j in {0..${num_rand}} ; do
    echo "some random line: ${rand}"
  done
  if [ $(expr ${i} % 100) -eq 0 ] ; then
    pct=$(expr 100 \* ${i} / ${limit})
    echo -n "Process is ${pct}% complete: "
    case ${pct} in
      20) echo "marker::cat" ;;
      40) echo "marker::dog" ;;
      60) echo "marker::bird" ;;
      80) echo "marker::fish" ;;
      100) echo "marker::hedgehog" ;;
    esac
  fi
  i=$(expr ${i} + 1)
done
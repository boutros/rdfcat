#!/bin/sh

cat index.log | grep Insuficient | awk '{print $10}' > missing_info.txt
cat index.log | grep 'Error indexing' | awk '{print $9}' > failed.txt


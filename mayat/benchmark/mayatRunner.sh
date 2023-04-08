#!/bin/bash

ulimit -s hard

root=`dirname $1`
dir=`basename $1`
paths=$root/$dir/*/*

python3 -m mayat.frontends.Java $paths -o JSON | python3 -m mayat.benchmark.json_to_csv

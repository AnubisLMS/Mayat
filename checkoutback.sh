#!/bin/zsh

curr_dir=${pwd}

cd $1
list_dirs=($(ls -d */))
for entry in $list_dirs
do
    echo $entry
    cd $entry
    git checkout `git rev-list -n 1 --first-parent --before="$2" main` 1> /dev/null
    cd ..
done

cd $curr_dir
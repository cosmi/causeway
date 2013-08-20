#!/bin/bash


find . > files.txt

rm files1.txt

for EXT in "\\.clj" "\\.html" "\\.js" "\\.css" "\\.less" "\\.scss" "\\.eot" "\\.svg" "\\.ttf" "\\.woff" "\\.xml"
do
grep $EXT files.txt >> files1.txt
grep -v $EXT files.txt > files2.txt
mv files2.txt files.txt
done

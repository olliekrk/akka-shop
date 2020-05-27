#!/bin/bash

case $# in
 1) product=$1 ;;
 *) product=test_product ;;
esac

# test for product price
url=localhost:9090/price/${product}

for i in {1..50}
do
  curl ${url} --compressed
  echo ""
done

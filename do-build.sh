#!/bin/bash
PROJ="fabric-1.20.6
echo
echo
echo
free
echo
echo
echo
echo


for i in $PROJ; do
  echo
  echo
  echo '**********************************'
  echo "Will build $i"
  echo '**********************************'
  echo
  echo

  cd platforms/$i

  ../../gradlew --max-workers 1 -S --build-cache  build
  cd ../../
  killall -9 java
done
killall -9 java

  echo
  echo
  echo '**********************************'
  echo "End with full build"
  echo '**********************************'
  echo
  echo

./gradlew --max-workers 1 -S --build-cache build

exit 0

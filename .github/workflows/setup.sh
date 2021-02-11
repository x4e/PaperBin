#!/bin/sh
cd "${GITHUB_WORKSPACE}"
mkdir paperbin
wget -q https://papermc.io/api/v2/projects/paper/versions/1.12.2/builds/1618/downloads/paper-1.12.2-1618.jar -O paperbin/paper.jar
cd paperbin
java -jar paper.jar
cd ../

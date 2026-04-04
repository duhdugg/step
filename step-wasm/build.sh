#!/usr/bin/env bash

set -x

source ./env.sh
mvn clean
mvn native:compile -e -Dgraalvm.native.target=wasm32-wasi
# mvn native:compile -Dgraalvm.native.target=wasm32-wasi \
# "-Dnative.buildArgs=-H:+UnlockExperimentalVMOptions -H:+PrintAnalysisCallTree -H:AbortOnMethodReachable=java.lang.Thread.start"

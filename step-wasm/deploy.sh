#!/usr/bin/evn bash

set -euxo pipefail

cp -v ./_poc_cheerpj.html ../step-web/src/main/webapp/html/
cp -v ./wasm-worker.js ../step-web/src/main/webapp/js/
cp -v ./target/step-wasm-26.8.3.jar ../step-web/src/main/webapp/js/step-wasm.jar

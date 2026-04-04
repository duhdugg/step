#!/usr/bin/evn bash

set -euxo pipefail

cp -v target/step-engine.* ../step-web/src/main/webapp/html/

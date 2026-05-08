#!/usr/bin/env bash

set -euxo pipefail

docker buildx build --platform linux/arm64 -t step-debian-arm64 .
docker create --name step-debian-arm64 --platform linux/arm64 step-debian-arm64

mkdir -p ./cache
docker export step-debian-arm64 | tar -C ./cache -xf -
tar -C ./cache --hard-dereference -caf step-debian-arm64.tar.gz .

rm -rf ./cache
docker rm step-debian-arm64

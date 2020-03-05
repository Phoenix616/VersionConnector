#!/usr/bin/env sh

OUTPUT=`git rev-parse --show-toplevel`/src/main/java/de/themoep/versionconnector/ProtocolVersion.java

echo "Generating ProtocolVersion.java to ${OUTPUT}"
python3 ./protocols.py > "${OUTPUT}"

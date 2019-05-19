#!/bin/bash
set -e
cd ../../../server/ && sbt dist && cd -
cp ../../../server/target/universal/xsn-block-explorer-0.1.0-SNAPSHOT.zip app.zip

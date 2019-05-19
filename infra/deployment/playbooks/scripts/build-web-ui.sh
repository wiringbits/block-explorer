#!/bin/bash
set -e
cd ../../../web-ui/ && ng build --prod && zip -r web.zip dist/* && cd -
mv ../../../web-ui/web.zip web.zip

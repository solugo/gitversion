#!/bin/sh

BINARY=$(mktemp)
TARGET=${GITVERSION:+download/v$GITVERSION}
URL=https://github.com/solugo/gitversion/releases/${TARGET:-latest/download}/gitversion
curl $URL -Lso $BINARY && chmod a+x $BINARY && $BINARY

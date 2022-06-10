#!/bin/bash

PLATFORM="$(uname -s)"
ARCH="$(uname -m)"

if [[ "$PLATFORM" == "Linux" ]] || [[ "$PLATFORM" == "msys" ]] || [[ "$PLATFORM" == "cygwin" ]]; then
  PLATFORM="linux"
elif [[ "$OSTYPE" == "win32" ]] || [[ "$OSTYPE" == "Microsoft Windows" ]] || [[ "$OSTYPE" == "Windows" ]] || [[ "$OSTYPE" == "Microsoft" ]]; then
  echo "Platform $PLATFORM is not supported"
  exit 1
  #PLATFORM="windows"
else
  echo "Platform $PLATFORM is not supported"
  exit 1
fi

if [[ "$ARCH" == "x86_64" ]] || [[ "$ARCH" == "x64" ]]; then
  ARCH="x64"
else
  echo "Architecture $ARCH is not supported"
  exit 2
fi


if [[ $GITVERSION == v0.0.* ]]; then
  ARTIFACT="gitversion"
else
  ARTIFACT="gitversion-$PLATFORM-$ARCH"
fi

BINARY=$(mktemp)
VERSION=${GITVERSION:+download/$GITVERSION}
URL=https://github.com/solugo/gitversion/releases/${VERSION:-latest/download}/$ARTIFACT
curl $URL -Lso $BINARY && chmod a+x $BINARY && $BINARY $ARGS

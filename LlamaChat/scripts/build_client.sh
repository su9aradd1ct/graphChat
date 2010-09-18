#!/bin/bash
#
# This will compile and jar the server to LlamaChatServer.jar

if [ ! -d "build" ]
then
	mkdir build
fi

javac -d build -sourcepath src src/client/LlamaChat.java
jar -cf client/LlamaChat.jar -C build client -C build common
if [ "$?" -ne 0 ]
then
     echo ""
     echo "ERROR: Command must be run from directory containing class files"
     echo "ERROR:  run like this ./scripts/build_client.sh from the LlamaChat directory"
     exit -1
fi

echo "client was built and placed in client/LlamaChat.jar"
exit 0


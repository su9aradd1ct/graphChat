#!/bin/bash
#
# This will compile and jar the server to LlamaChatServer.jar

if [ ! -d "build" ]
then
	mkdir build
fi

javac -d build -sourcepath src src/server/LlamaChatServer.java
jar -cmf src/server/LlamaChatServer.manifest \
				server/LlamaChatServer.jar \
				-C build server -C build common
if [ "$?" -ne 0 ]
then
     echo ""
     echo "ERROR: Command must be run from directory containing class files"
     echo "ERROR:  run like this ./scripts/build_server.sh from the LlamaChat directory"
     exit -1
fi

echo "server was built and placed in server/LlamaChatSerer.jar"
exit 0


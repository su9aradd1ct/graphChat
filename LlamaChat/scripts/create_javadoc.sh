#!/bin/bash
# A simple utility to create javadoc info
if [ ! -e "javadoc" ]
then
     echo ""
     echo "ERROR: Command must be run from directory containing java files"
     echo "ERROR:  run like this ./scripts/create_javadoc.sh from the LlamaChat directory"
     exit -1
fi

javadoc -sourcepath src client server common common.sd -author -version -private -d javadoc
if [ "$?" -ne 0 ]
then
     echo ""
     echo "ERROR: Command must be run from directory containing java files"
     echo "ERROR:  run like this ./scripts/create_javadoc.sh from the LlamaChat directory"
     exit -1
fi

exit 0

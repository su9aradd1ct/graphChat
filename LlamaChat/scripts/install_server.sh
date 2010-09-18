#!/bin/bash

INSTALL_DIR="/usr/local/llamachat"
LLAMACHAT="/usr/bin/llamachat"
SERVER_PATH="server/"

if [ "$1" = "--help" ]
then
    echo "This command will install llamachat to $INSTALL_DIR"
    echo "You can specify an alternate directy  by running"
    echo "'./scripts/install_server.sh /path/to/llamachat'"
    echo "from the install directory"
    exit
fi

if [ -n "$1" ]
then
  INSTALL_DIR="$1"
fi

cd server

java -version
echo ""

if [ $? -ne 0 ]
then
   echo "java could not be found in ${PATH}"
   exit -1
fi

if [ -z `which java` ]
then
   echo "java could not be found in ${PATH}"
   exit -1
fi

if [ ! -f "${SERVERPATH}LlamaChatServer.jar" ]
then
   echo " Can't find ${SERVERPATH}LlamaChatServer.jar."
   echo "  You cannot install until you create the jar file and run"
   echo "  ./scripts/install_server.sh from the LlamaChat root directory"
   exit -1
fi

echo "#!/bin/bash" > $LLAMACHAT
echo "cd ${INSTALL_DIR}" >> $LLAMACHAT
echo "java -server -jar LlamaChatServer.jar" >> $LLAMACHAT

if [ $? -ne 0 ]
then
   echo "Error durring installation"
   exit -1
fi

chmod a+x $LLAMACHAT
if [ $? -ne 0 ]
then
   echo "Error durring installation"
   exit -1
fi


if [ -e "$INSTALL_DIR" ]
then
    echo "WARNING: Installing over previous installation"
else
    mkdir $INSTALL_DIR
fi

if [ $? -ne 0 ]
then
   echo "Error durring installation"
   exit -1
fi

cp ${SERVERPATH}LlamaChatServer.jar $INSTALL_DIR
if [ $? -ne 0 ]
then
   echo "Error durring installation"
   exit -1
fi

cp ${SERVERPATH}llamachatconf.xml.sample $INSTALL_DIR
if [ $? -ne 0 ]
then
   echo "Error durring installation"
   exit -1
fi

echo ""
echo " Successfully installed LlamaChatServer to ${INSTALL_DIR}"
echo ""
echo " A script has been created called ${LLAMACHAT} which"
echo " will start the server for you."
echo ""
echo " DONT FORGET TO EDIT ${INSTALL_DIR}/llamachatconf.xml.sample AND"
echo " SAVE AS ${INSTALL_DIR}/llamachatconf.xml"
echo ""
echo " ALSO, IF YOU PLAN ON LOGGING CHAT SESSIONS MAKE SURE THE PATH"
echo " YOU CHOSE EXISTS (THE DEFAULT IS chat_logs, in ${INSTALL_DIR}"
echo ""

exit

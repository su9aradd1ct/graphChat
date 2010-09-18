#!/bin/bash

# If you need to set your java path, use:
#  JAVAPATH=/opt/sun-jdk-1.4.1.01/bin/
# Otherwise leave alone
JAVAPATH=

cd /home/httpd/htdocs/LlamaChat/server/
exec ${JAVAPATH}java -server -jar LlamaChatServer.jar

#!/bin/bash
JAVA=java
LEIN=lein
SMTP_LIB=.lib/DevNullSmtp.jar
export SUBMITIT_SETUP_FILE=`pwd -P`/config

if [ ! -e "$SMTP_LIB" ]; then
  wget http://www.aboutmyip.com/AboutMyXApp/DevNullSmtp.jar -O $SMTP_LIB
fi
$JAVA -jar $SMTP_LIB -console -p 1025 &
SMTP_PID=$!

cleanup() {
  kill $SMTP_PID
}

control_c()
# run if user hits control-c
{
  echo -en "\n*** Exiting ***\n"
  cleanup
  exit $?
}
 
# trap keyboard interrupt (control-c)
trap control_c SIGINT

$LEIN run
#!/bin/bash
JAVA=java
LEIN=lein
SMTP_LIB=.lib/DevNullSmtp.jar
export SUBMITIT_SETUP_FILE=`pwd -P`/config

if [ ! -e "$SMTP_LIB" ]; then
  wget http://www.aboutmyip.com/AboutMyXApp/DevNullSmtp.jar -O $SMTP_LIB
fi
if [ -f ".smtp.pid" ]; then 
  SMTP_PID=$(cat .smtp.pid)
else 
  $JAVA -jar $SMTP_LIB -console -p 1025 &
  SMTP_PID=$!
  echo $SMTP_PID > .smtp.pid
fi

cleanup() {
if [ -f ".smtp.pid" ]; then 
  SMTP_PID=$(cat .smtp.pid)  
  kill $SMTP_PID
  rm .smtp.pid
fi
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

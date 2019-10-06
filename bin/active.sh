#!/bin/tcsh
#  run active name learner for JET
java -cp $JET_HOME/build/main:$JET_HOME/jet-all.jar -Xmx800m -DjetHome=$JET_HOME edu.nyu.jet.hmm.ActiveLearnerTool $1 $2 $3 

#!/bin/bash

#####################################################################################################################################################
#
# For this script to work correctly it mus be executed from the root directory of you msdlib.loopy repository (the directory it is located in)
# It is also possible to provide the (absolute or relative) Path to the Repository as an argument
# Also the correct settings script inside the Xilinx ise installation directory must have been sourced (e.g. settings64.sh)
#
####################################################################################################################################################

# obtain xilinx installation directory (stored in $XILINX), define the path, where the needed files are stored
SRC="$XILINX_EDK/sw/lib/sw_apps/lwip_echo/src/"

# check if xilinx installation directory is correct
if [[ ! -d "$SRC" ]]
then
	echo "Error: Xilinx installation directory not found, please check if you have source the correct settings*.*sh"
	exit 1
fi

#obtain repository directory

# read path from argument if argument exists
ARG=$1
if [ -n "$ARG" ]
then
	# put together absolute path
	if [[ "$ARG" = /* ]]
	then
	    REPO=$ARG
	else
    	REPO=$PWD/$ARG
	fi

	# check if given path exists
	if [[ ! -d "$REPO" ]]
	then
    	echo "Error: Invalid Path in argument $REPO"
    	exit 1
	fi
else
	REPO="$PWD"
fi

# put together destination path
DESTPATH="src/main/resources/deploy/board/generic/sdk/generic/app/src/"
DEST=$REPO/$DESTPATH

# determine if path for repo is correct, if not complain and exit with failure
if [[ ! -d $DEST ]]; then
	echo "Error: Given directory is not the repositories root directory"
	exit 1
fi

# define names of files needed
FILE1="platform.h"
FILE2="platform.c"
FILE3="platform_mb.c"
FILE4="platform_ppc.c"
FILE5="platform_zynq.c"

# do acutal copying
cp $SRC$FILE1 $DEST
cp $SRC$FILE2 $DEST
cp $SRC$FILE3 $DEST
cp $SRC$FILE4 $DEST
cp $SRC$FILE5 $DEST

exit 0

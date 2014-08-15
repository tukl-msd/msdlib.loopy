#!/bin/bash

#obtain xilinx directory (possibly provide xildir as argument)
xildir="/opt/Xilinx/"
version="14.7/"
relpath="ISE_DS/EDK/sw/lib/sw_apps/lwip_echo/src/"

src=$xildir$version$relpath

#obtain repository directory (possibly provide repopath as argument or retrieve automated)
repopath="$HOME/msdlib.loopy/"
#destpath=""
destpath="src/main/resources/deploy/board/generic/sdk/generic/app/src/"

dest=$repopath$destpath

# define names of files needed
file1="platform.h"
file2="platform.c"
file3="platform_mb.c"
file4="platform_ppc.c"
file5="platform_zynq.c"

# do acutal copying
cp $src$file1 $dest
cp $src$file2 $dest
cp $src$file3 $dest
cp $src$file4 $dest
cp $src$file5 $dest


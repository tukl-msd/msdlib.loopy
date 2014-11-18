#-----------------------------------------------------------------------------------------------------------
# tcl ip vivado hw platform tcl script
# author:CV
# project:loopy
# date: October 2014
# target platform: Zynq 7000 SoC
#
#
# usage:
# 		source by base.tcl in order to call IPI and package new IP 
#-----------------------------------------------------------------------------------------------------------



#-----------------------------------------------------------------------------------------------------------
# step#1: setup design sources and constraints

create_project axi4_wrapper ./axi4_wrapper

#-------------------------------------------------------------------------
# Set project properties

set_property PART xc7z020clg484-1 [current_project]

set obj [current_project]
set_property "default_lib" "xil_defaultlib" $obj
set_property "part" "xc7z020clg484-1" $obj
set_property "simulator_language" "Mixed" $obj
set_property "target_language" "VHDL" $obj

#-------------------------------------------------------------------------
# add sources for the new IP

#read_vhdl [ glob ./src/hdl/*.vhd ] 

#read_verilog -quiet [ glob ./src/hdl/*.v ] 

# this is the more general command, supports both Verilog/VHDL but it only works with a project:CV
add_files [ glob ./src/hdl/* ] 

#-----------------------------------------------------------------------------------------------------------
# step#2: package IP


ipx::package_project -generated_files true -root_dir {./} 

set_property library {user} [ipx::current_core]

#set_property ip_repo_paths  ./ [current_fileset]`
#update_ip_catalog

close_project

#-----------------------------------------------------------------------------------------------------------
#eof
#start_gui

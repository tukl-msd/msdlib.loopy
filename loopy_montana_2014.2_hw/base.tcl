#-----------------------------------------------------------------------------------------------------------
# tcl base vivado hw platform tcl script
# author:CV
# project:loopy
# date: September 2014
# target platform: Zynq 7000 SoC
#
#
# usage:
# 		vivado -mode batch -source base.tcl 
#-----------------------------------------------------------------------------------------------------------
set outputDir ./output
file mkdir $outputDir

#-----------------------------------------------------------------------------------------------------------
# step#1: setup design sources and constraints

create_project -in_memory loopy

#read_vhdl -library work [ glob ./src/hdl/*.vhd ] 

#~ read_vhdl -library work ./src/hdl/test.vhdl  -- glob = takes all files, -library defines vhdl library
#~ 
#~ read_verilog [ glob ./Sources/hdl/*.v ] -- -sv and .sv for system verilog files
#~ read_ip -- .xco || .xci -- vivado native or coregen ip's

#~ read_xdc ./src/*.xdc 
#-- used to import xdc constraint files

#-------------------------------------------------------------------------
# Set project properties

set_property PART xc7z020clg484-1 [current_project]

set obj [current_project]
set_property "default_lib" "xil_defaultlib" $obj
set_property "part" "xc7z020clg484-1" $obj
set_property "simulator_language" "Mixed" $obj
set_property "target_language" "VHDL" $obj


#-------------------------------------------------------------------------
# Package new IP for DUT sources
source loopy_ip.tcl

#-------------------------------------------------------------------------
#set ip repository path
set_property ip_repo_paths  ./ [current_fileset]
update_ip_catalog


#-------------------------------------------------------------------------
# create the BD-Design
source loopy_bd.tcl
generate_target all [get_files  .srcs/sources_1/bd/loopy_core/loopy_core.bd]
#read_vhdl -library work [ glob .srcs/sources_1/bd/loopy_core/hdl/loopy_core.vhd ]

#set_property top loopy_core_wrapper [current_fileset]
  



#-----------------------------------------------------------------------------------------------------------
# step#2: run synthesis

# requires name and part with speed grade
synth_design -top loopy_core -part xc7z020clg484-1 -flatten rebuilt    

# used to write checkpoints at any time = netlist + constraints + current status
#write_checkpoint -force $outputDir/post_synth

#report_utilization -file $outputDir/post_synth_util.rpt
#~ 
#report_timing -sort_by group -max_paths 5 -path_type summary -file $outputDir/post_synth_timing.rpt

#-----------------------------------------------------------------------------------------------------------
# step#3: run optimization and placement

#ADVANCED: this step represents the technology(platform) independent logic optimization process
opt_design 

#~ power_opt_design  -- clock gating

place_design

#~ phys_opt_design -- technology mapping -- platform dependent optimization

#write_checkpoint -force $outputDir/post_place

#report_clock_utilization -file $outputDir/clock_util.rpt
#~ 
#report_utilization -file $outputDir/post_place_util.rpt
#~ 
#report_timing -sort_by group -max_paths 5 -path_type summary -file $outputDir/post_place_timing.rpt

#-----------------------------------------------------------------------------------------------------------
# step#4: route the design
route_design

#write_checkpoint -force $outputDir/post_route

#report_timing_summary -file $outputDir/post_route_timing_summary.rpt
#~ 
#report_utilization -file $outputDir/post_route_util.rpt
#~ 
#report_power -file $outputDir/post_route_power.rpt
#~ 
#report_drc -file $outputDir/post_imp_drc.rpt

#write_verilog -force $outputDir/loopy_impl_netlist.v 
#--write HDL (VHDL/Verilog) model for simulation and verification
#~ 
#write_xdc -no_fixed_only -force $outputDir/loopy_impl.xdc  
#-- write final xdc constraints from routing

#-----------------------------------------------------------------------------------------------------------
# step#5: generate a bitstream

write_bitstream $outputDir/loopy_hw_platform.bit

#export_hardware [get_files  .srcs/sources_1/bd/loopy_core/loopy_core.bd] 
 


--!
--! Copyright (C) 2012 University of Kaiserslautern
--! Microelectronic Systems Design Research Group
--!
--! @file
--! @brief Axi4-Stream interface + Axi4-Stream FIFO queue
--! @author Vladimir Rybalkin

library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

entity AxiFifo is
  port (
    --! clock input, sensitive to positive clock edge    
    aclk          : in  std_logic;
    --! reset input, depends on G_RESET_ACTIVE if active high or active low    
    aresetn       : in  std_logic;    
	
	--! s_axis_tvalid input, unit read data if s_axis_tvalid = '1'  
    s_axis_tvalid : in  std_logic;
    --! s_axis_tdata input, payload data input   
    s_axis_tdata  : in  std_logic_vector(31 downto 0);
    --! s_axis_tlast input, unit read the last value of the stream if s_axis_tlast = '1'
    s_axis_tlast  : in  std_logic;
    --! s_axis_tready out, unit is able to read data if s_axis_tready = '1'
    s_axis_tready : out std_logic;
	 
    --! m_axis_tvalid output, unit write valid data when s_axis_tvalid = '1
    m_axis_tvalid : out std_logic;
    --! m_axis_tdata output, payload data output   
    m_axis_tdata  : out std_logic_vector(31 downto 0);
    --! m_axis_tlast output, unit write the last value of the stream if m_axis_tlast = '1'
    m_axis_tlast  : out std_logic;
    --! m_axis_tready input, unit write value if m_axis_tready = '1' 
    m_axis_tready : in  std_logic);
	
end AxiFifo;

architecture behavioral of AxiFifo is

	component Fifo is
		port (
		 s_aclk 		: in  std_logic;
		 s_aresetn 		: in  std_logic;
		 s_axis_tvalid 	: in  std_logic;
		 s_axis_tready 	: out std_logic;
		 s_axis_tdata 	: in  std_logic_vector(31 downto 0);
		 s_axis_tlast 	: in  std_logic;
		 m_axis_tvalid 	: out std_logic;
		 m_axis_tready 	: in  std_logic;
		 m_axis_tdata 	: out std_logic_vector(31 downto 0);
		 m_axis_tlast 	: out std_logic);
		end component;
 

begin  -- behavioral

	Fifo_1 : Fifo 
		port map (
		 s_aclk 				=> aclk,
		 s_aresetn 				=> aresetn,	
		 
		 s_axis_tvalid 			=> s_axis_tvalid,
		 s_axis_tready 			=> s_axis_tready,
		 s_axis_tdata 			=> s_axis_tdata,
		 s_axis_tlast 			=> s_axis_tlast,
		 
		 m_axis_tvalid 			=> m_axis_tvalid,
		 m_axis_tready 			=> m_axis_tready,
		 m_axis_tdata 			=> m_axis_tdata,
		 m_axis_tlast 			=> m_axis_tlast);  
		 
end behavioral;

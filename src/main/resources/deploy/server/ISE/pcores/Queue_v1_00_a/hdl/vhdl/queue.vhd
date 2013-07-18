-- ===================================================================
-- (C)opyright 2013
-- 
-- Microelectronic Systems Design Research Group
-- University of Kaiserslautern, Germany
-- 
-- ===================================================================
-- 
-- Author(s): Weithoffer
-- 
-- ===================================================================
-- 
-- Project:   
-- 
-- ===================================================================
-- 
-- Description:
-- * Simple Queue
-- * Can be configured as LIFO or FIFO
--
-- ===================================================================


-- ===================================================================
--! @date 12/07/2013
--! @brief Remarks and bugs fixing
--! @author Vladimir Rybalkin
--! @Supervisor: Christian de Schryver
-- ===================================================================

library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;


entity queue is
	generic
	(
		G_EN_ACTIVE  : std_logic := '1';                              --! Enable signal sensitivity
		G_RST_ACTIVE : std_logic := '1';                              --! Reset signal sensitivity
		G_BW : natural := 32;                                         --! Bitwidth of the Queue Elements
		G_DEPTH : natural := 128;                                     --! Queue Length
		G_MODE : integer := 1                                         --! 1: FIFO / 0: LIFO
	);
	port 
	(                                                                 
		clk : in std_logic;                                           --! clock signal
		rst : in std_logic;                                           --! synchrounous reset signal
		in_TReady : out std_logic;                                    --! input-AXIS-port TReady signal
		in_TValid : in std_logic;                                     --! input-AXIS-port TValid signal
		in_TData  : in std_logic_vector(G_BW-1 downto 0);             --! input-AXIS-port TData signal
		in_TLast : in std_logic;                                      --! input-AXIS-port TLast signal
		
		out_TReady : in std_logic;                                    --! output-AXIS-port TReady signal
		out_TValid : out std_logic;                                   --! output-AXIS-port TValid signal
		out_TData : out std_logic_vector(G_BW-1 downto 0);            --! output-AXIS-port TData signal
		out_TLast : out std_logic                                     --! output-AXIS-port TLast signal

	);                                                             
end queue;


architecture rtl of queue is


	----------------------------------------------------------------------------
	-- Types
	----------------------------------------------------------------------------
	
	type data_array_type is array (0 to G_DEPTH-1) of std_logic_vector(G_BW-1 downto 0);
	type stdl_array_type is array (0 to G_DEPTH-1) of std_logic;	

	----------------------------------------------------------------------------
	-- Signals
	----------------------------------------------------------------------------
	
	signal memory : data_array_type := (others => (others => '0'));
	signal num_data : natural range 0 to G_DEPTH-1 := 0;
	
	signal tlast_flag : stdl_array_type := (others => '0');
	
	-- read and write pointers for FIFO mode
	signal rd_ptr : natural range 0 to G_DEPTH-1 := 0;
	signal wr_ptr : natural range 0 to G_DEPTH-1 := 0;
	
	-- pointer for LIFO mode
	signal ptr : natural range 0 to G_DEPTH-1 := 0;
	
        signal en : std_logic;
begin
        en <= '1';

	----------------------------------------------------------------------------
	-- Generate FIFO
	----------------------------------------------------------------------------

	fifo : if G_MODE = 1 generate begin
		process(clk)
			variable v_num_data : natural; -- number of unread data in the FIFO
		begin
			if clk'event and clk='1' then
				if rst = G_RST_ACTIVE then
						rd_ptr <= 0;
						wr_ptr <= 0;
						num_data <= 0;
				else
					v_num_data := num_data;
				
					if en = G_EN_ACTIVE then
						
						-- writing to the FIFO
						if num_data < G_DEPTH then    			      		-- !!! [G_DEPTH-1] changed to [G_DEPTH]
							if in_TValid = '1' then
								memory(wr_ptr) <= in_TData;
								tlast_flag(wr_ptr) <= in_TLast;				-- !!! [tlast_flag(wr_ptr) <= in_TLast] is added
								
								-- write pointer wraps around
								if wr_ptr < G_DEPTH-1 then
									wr_ptr <= wr_ptr + 1;
								else
									wr_ptr <= 0;
								end if;
								
								v_num_data := v_num_data + 1;
							end if;
						end if;
						
						-- reading from the FIFO
						if out_TReady = '1' and num_data > 0 then    		-- !!! [and num_data > 0] is added
																				
							-- read pointer wraps around	
							if rd_ptr < G_DEPTH-1 then		
								rd_ptr <= rd_ptr + 1;		
							else
								rd_ptr <= 0;
							end if;
							
							v_num_data := v_num_data - 1;
						end if;						
						
					end if; -- en
				end if; -- rst
				
				num_data <= v_num_data;               
			end if; -- clk
		end process;
		
	
		-- Ready to receive data if the FIFO is not full
		in_TReady <= '1' when num_data < G_DEPTH else           			-- !!! [G_DEPTH-1] changed to [G_DEPTH]
		             '0';
		
		-- Ready to send data if the FIFO is not empty 
		out_TValid <= '1' when num_data > 0 else 							-- !!! [else '1' when num_data = 0 and in_TValid = '1'] is removed 
					  '0';                                      																					
	
		out_TData <= memory(rd_ptr) when num_data > 0 else		   
		             in_TData;											  
	
			
		out_Tlast <= tlast_flag(rd_ptr) when num_data > 0 else				-- !!! [tlast_flag(rd_ptr) when num_data > 0] is added
					 in_Tlast;		
					 
		             
	end generate fifo; 
	
end rtl;
	


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


library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;


entity queue is
	generic
	(
		G_EN_ACTIVE  : std_ulogic := '1';                              --! Enable signal sensitivity
		G_RST_ACTIVE : std_ulogic := '0';                              --! Reset signal sensitivity
		G_BW : natural := 32;                                          --! Bitwidth of the Queue Elements
		G_DEPTH : natural;                                             --! Queue Length
		G_MODE : integer := 1                                          --! 1: FIFO / 0: LIFO
	);
	port 
	(                                                                 
		clk : in std_ulogic;                                           --! clock signal
		rst : in std_ulogic;                                           --! synchrounous reset signal
		en : in std_ulogic;  -- enable signal (for clock gating)       --! enable signal (for clock gating)
		
		in_TReady : out std_ulogic;                                    --! input-AXIS-port TReady signal
		in_TValid : in std_ulogic;                                     --! input-AXIS-port TValid signal
		in_TData  : in std_ulogic_vector(G_BW-1 downto 0);             --! input-AXIS-port TData signal
		in_TLast : in std_ulogic;                                      --! input-AXIS-port TLast signal
		
		out_TReady : in std_ulogic;                                    --! output-AXIS-port TReady signal
		out_TValid : out std_ulogic;                                   --! output-AXIS-port TValid signal
		out_TData : out std_ulogic_vector(G_BW-1 downto 0);            --! output-AXIS-port TData signal
		out_TLast : out std_ulogic                                     --! output-AXIS-port TLast signal

	);                                                             
end queue;


architecture rtl of queue is


	----------------------------------------------------------------------------
	-- Types
	----------------------------------------------------------------------------
	
	type data_array_type is array (0 to G_DEPTH-1) of std_ulogic_vector(G_BW-1 downto 0);
	

	----------------------------------------------------------------------------
	-- Signals
	----------------------------------------------------------------------------
	
	signal memory : data_array_type := (others => (others => '0'));
	signal num_data : natural range 0 to G_DEPTH-1 := 0;
	
	-- read and write pointers for FIFO mode
	signal rd_ptr : natural range 0 to G_DEPTH-1 := 0;
	signal wr_ptr : natural range 0 to G_DEPTH-1 := 0;
	
	-- pointer for LIFO mode
	signal ptr : natural range 0 to G_DEPTH-1 := 0;
	

begin


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
						if num_data < G_DEPTH-1 then
							if in_TValid = '1' then
								memory(wr_ptr) <= in_TData;
								
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
						if out_TReady = '1' then

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
		in_TReady <= '1' when num_data < G_DEPTH-1 else
		             '0';
		
		-- Ready to send data if the FIFO is not empty or data is valid at the inputs
		out_TValid <= '1' when num_data > 0 else
		              '1' when num_data = 0 and in_TValid = '1' else
		              '0';
	
		out_TData <= memory(rd_ptr) when num_data > 0 else
		             in_TData;
	
		-- only one data item in the FIFO
		out_TLast <= '1' when num_data = 1 else
		             '0';
		             
	end generate fifo;
    

	----------------------------------------------------------------------------
	-- Generate LIFO
	----------------------------------------------------------------------------
	
	lifo : if G_MODE = 0 generate begin
		process(clk)
			variable v_num_data : natural := 0;
		begin
			if clk'event and clk='1' then
				if rst = G_RST_ACTIVE then
						ptr <= 0;
						num_data <= 0;
				else
					v_num_data := num_data;
				
					if en = G_EN_ACTIVE then
						
						-- writing to the LIFO
						if num_data < G_DEPTH then
							if in_TValid = '1' and out_TReady = '0' then
								if ptr < G_DEPTH-1 then
									if ptr = 0 then
										memory(0) <= in_TData;
									else
										memory(ptr+1) <= in_TData;
										ptr <= ptr + 1;
									end if;
									
									v_num_data := v_num_data + 1;
								end if;
							end if;
						end if;
						
						-- reading from the LIFO
						if num_data > 0 then
							if out_TReady = '1' and in_TValid = '0' then
								v_num_data := v_num_data - 1;
								 
								if ptr > 0 then
									ptr <= ptr - 1;
								end if;
							end if;
						end if;
						
					end if; -- en
				end if; -- rst
				
				num_data <= v_num_data;
			end if; -- clk
		end process;
	
	
			-- Ready to receive data if the LIFO is not full
		in_TReady <= '1' when num_data < G_DEPTH-1 else
		             '0';
		
		-- Ready to send data if the LIFO is not empty or data is valid at the inputs
		out_TValid <= '1' when num_data > 0 else
		              '1' when in_TValid = '1' else
		              '0';
	
		out_TData <= memory(ptr) when in_TValid = '0' else
		             in_TData;
	
		-- only one data item in the LIFO
		out_TLast <= '1' when num_data = 1 else
		             '0';
	
	end generate lifo;
	
end rtl;

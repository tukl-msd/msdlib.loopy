--!
--! Copyright (C) 2013 University of Kaiserslautern
--! Microelectronic Systems Design Research Group
--!
--! This file is part of the financial mathematics research project
--! de.uni-kl.eit.ems.finance
--! @file
--! @brief Axi4-Stream Downsizer ip core (Parallel-To-Serial) 
--! @author Luis Vega
--! @brief Supervisors: Philipp Schläfer and Christian de Schryver

library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

--! @details s_axis_tlast not used, can have any value "X"
--! @details m_axis_tlast not used, always m_axis_tlast = '0'

entity Downsizer is
  generic (G_RESET_ACTIVE : std_logic;  --! reset is active on level '1' (rst active high) or '0' (rst active low)
           WIDTH          : integer;    --! bit width
           NUM_REG        : integer);   --! number of registers
  port(
    clk           : in  std_logic;
    rst           : in  std_logic;
    s_axis_tarray : in  std_logic_vector(NUM_REG*WIDTH - 1 downto 0);
    s_axis_tlast  : in  std_logic;
    s_axis_tvalid : in  std_logic;
    s_axis_tready : out std_logic;
    m_axis_tdata  : out std_logic_vector(WIDTH - 1 downto 0);
    m_axis_tlast  : out std_logic;
    m_axis_tvalid : out std_logic;
    m_axis_tready : in  std_logic);

end Downsizer;

architecture Behavioral of Downsizer is

  type   states is (READY, SHIFT, LAST);
  signal state   : states;
  signal cnt     : integer;
  signal s_zeros : std_logic_vector(WIDTH - 1 downto 0);
  signal s_reg   : std_logic_vector(NUM_REG*WIDTH - 1 downto 0);

begin

  -- FSM 
  process(rst, clk)
  begin
    if (rst = G_RESET_ACTIVE) then
      state           <= READY;
      s_axis_tready   <= '1';
      m_axis_tvalid   <= '0';
      s_reg           <= (others => '0');
      s_zeros         <= (others => '0');
    elsif (clk'event and clk = '1') then
      case state is
        when READY =>
          cnt <= 0;
          if (s_axis_tvalid = '1') then
            state           <= SHIFT;
            s_reg           <= s_axis_tarray;
            s_axis_tready   <= '0';
		        m_axis_tvalid   <= '1';
          end if;
        when SHIFT =>
          if (cnt < NUM_REG - 1 and m_axis_tready = '1') then
            cnt   <= cnt + 1;
            s_reg <= s_reg((NUM_REG - 1)*WIDTH - 1 downto 0) & s_zeros;
          elsif (cnt < NUM_REG - 1 and m_axis_tready = '0') then
            state <= SHIFT;
          else
            state <= LAST;
	          m_axis_tvalid <= '0';
	        end if;	
	      when LAST =>
	        if (m_axis_tready = '1') then
            state         <= READY;
            s_axis_tready <= '1';	
	        end if;			
      end case;
    end if;
  end process;

  m_axis_tdata <= s_reg(NUM_REG*WIDTH - 1 downto (NUM_REG - 1)*WIDTH);
  m_axis_tlast <= '0';
  
end architecture behavioral;

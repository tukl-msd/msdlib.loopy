--!
--! Copyright (C) 2013 University of Kaiserslautern
--! Microelectronic Systems Design Research Group
--!
--! This file is part of the financial mathematics research project
--! de.uni-kl.eit.ems.finance
--! @file
--! @brief Axi4-Stream Upsizer ip core (Serial-To-Parallel)
--! @author Luis Vega
--! @brief Supervisors: Philipp Schläfer and Christian de Schryver

library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

--! @details s_axis_tlast not used, can have any value "X"
--! @details m_axis_tlast not used, always m_axis_tlast = '0'

entity Upsizer is
  generic (G_RESET_ACTIVE : std_logic := '1';  --! reset is active on level '1' (rst active high) or '0' (rst active low)
           WIDTH_IN       : integer :=  33;    --! bit width
           WIDTH_OUT      : integer := 128);   --! number of registers
  port(
    clk           : in  std_logic;
    rst           : in  std_logic;
    s_axis_tdata  : in  std_logic_vector(WIDTH_IN-1 downto 0);
    s_axis_tlast  : in  std_logic;
    s_axis_tvalid : in  std_logic;
    s_axis_tready : out std_logic;
    m_axis_tdata  : out std_logic_vector(WIDTH_OUT-1 downto 0);
    m_axis_tlast  : out std_logic;
    m_axis_tvalid : out std_logic;
    m_axis_tready : in  std_logic);
end Upsizer;

architecture Behavioral of Upsizer is

  type   states is (READY, LOAD, WRtdata);
  signal state   : states;
  signal s_reg   : std_logic_vector(WIDTH_OUT - 1 downto 0);
  signal cnt     : integer;

  constant NUM_REG : natural := WIDTH_OUT / WIDTH_IN;
  constant NUM_DUMMIES : natural := WIDTH_OUT - NUM_REG*WIDTH_IN;

  signal dummy : std_logic_vector(NUM_DUMMIES - 1 downto 0);

begin

  -- FSM
  process(rst, clk)
  begin
    if (rst = G_RESET_ACTIVE) then
      state           <= READY;
      cnt             <=  0;
      s_axis_tready   <= '1';
      m_axis_tvalid   <= '0';
      s_reg           <= (others => '0');
      dummy           <= (others => '0');
    elsif (clk'event and clk = '1') then
      case state is
        when READY =>
          cnt <=  0;
          if (s_axis_tvalid = '1') then
            s_reg(WIDTH_IN - 1 downto 0) <= s_axis_tdata;
            state         <= LOAD;
            cnt           <= cnt + 1;
          end if;
        when LOAD =>
          if (cnt < NUM_REG and s_axis_tvalid = '1') then
            s_reg <= dummy & s_reg((NUM_REG - 1)*WIDTH_IN - 1 downto 0) & s_axis_tdata;
            cnt   <= cnt + 1;
            state <= LOAD;
          elsif (cnt < NUM_REG and s_axis_tvalid = '0') then
            state <= LOAD;
          else
            state           <= WRtdata;
            s_axis_tready   <= '0';
            m_axis_tvalid <= '1';
          end if;
        when WRtdata =>
          if (m_axis_tready = '1') then
            state           <= READY;
            m_axis_tvalid <= '0';
	          s_axis_tready   <= '1';
          end if;
      end case;
    end if;
  end process;

m_axis_tdata <= s_reg;
m_axis_tlast <= '0';

end architecture behavioral;

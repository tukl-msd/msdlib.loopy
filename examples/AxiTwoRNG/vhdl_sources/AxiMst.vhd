--!
--! Copyright (C) 2012 University of Kaiserslautern
--! Microelectronic Systems Design Research Group
--!
--! This file is part of the financial mathematics research project
--! de.uni-kl.eit.ems.finance
--! @file
--! @brief Axi-Stream Master (Write) Interface (AxiMst --> Microblaze)
--! @author Luis Vega

library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

--! @details this unit serve as Write Interface for AXI-Stream protocol
--! @details basically, is a Parallel-to_Serial interface
--! @details Data is serialized taking as big endian representation
--! @details first m_axis_tdata is taken from the most significant WIDTH bits from reg_array
--! @details reg_array_ready is '1' when is ready to load data otherwise is
--busy serializing

entity AxiMst is
  generic (G_RESET_ACTIVE : std_logic;  --! reset is active on level '1' (rst active high) or '0' (rst active low)
           WIDTH          : integer;    --! bit width
           NUM_REG        : integer);   --! number of registers
  port(
    --! clock input, sensitive to positive clock edge
    clk             : in  std_logic;
    --! reset input, depends on G_RESET_ACTIVE if active high or active low
    rst             : in  std_logic;
    --! reg_array input, input for the array
    reg_array       : in  std_logic_vector(NUM_REG*WIDTH - 1 downto 0);
    --! reg_array_valid input, unit read reg_array if reg_array_valid = '1'
    reg_array_valid : in  std_logic;
    --! reg_array_ready output, unit is ready to receive data if reg_array_ready = '1'
    reg_array_ready : out std_logic;
    --! m_axis_tvalid output, unit is seriliazing valid data if m_axis_tvalid = '1'
    m_axis_tvalid   : out std_logic;
    --! m_axis_tdata output, data serialized
    m_axis_tdata    : out std_logic_vector(WIDTH - 1 downto 0);
    --! m_axis_tlast output, unit is sending the last value if last = '1'
    m_axis_tlast    : out std_logic;
    --! m_axis_tready input, unit is enable to send data
    m_axis_tready   : in  std_logic);

end AxiMst;

architecture Behavioral of AxiMst is

  type     states is (READY, SERIAL);
  signal   state    : states;
  signal   cnt      : integer;
  signal   s_zeros  : std_logic_vector(WIDTH - 1 downto 0);
  signal   s_reg    : std_logic_vector((NUM_REG + 1)*WIDTH - 1 downto 0);

begin

  -- FSM 
  process(rst, clk)
  begin
    if (rst = G_RESET_ACTIVE) then
      state   <= READY;
      reg_array_ready <= '1';
      m_axis_tvalid   <= '0';
      s_reg   <= (others => '0');
      s_zeros <= (others => '0');
    elsif (clk'event and clk = '1') then
      case state is
        when READY =>
          cnt             <= 0;
		  m_axis_tvalid   <= '0';
          if (reg_array_valid = '1') then
            state <= SERIAL;
            s_reg <= s_zeros & reg_array;           
			reg_array_ready <= '0';            			
          end if;
        when SERIAL =>
          if (cnt < NUM_REG and m_axis_tready = '1') then
            cnt   <= cnt + 1;
            s_reg <= s_reg(NUM_REG*WIDTH - 1 downto 0) & s_zeros;          
			m_axis_tvalid <= '1';			
          elsif (cnt < NUM_REG and m_axis_tready = '0') then
            state <= SERIAL;
          else
            state <= READY;
			--m_axis_tvalid <= '0';
			reg_array_ready <= '1';
          end if;
      end case;
    end if;
  end process;

  m_axis_tdata <= s_reg((NUM_REG + 1)*WIDTH - 1 downto NUM_REG*WIDTH);
  m_axis_tlast <= '0';
  
end architecture behavioral;

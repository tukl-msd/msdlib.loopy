--!
--! Copyright (C) 2012 University of Kaiserslautern
--! Microelectronic Systems Design Research Group
--!
--! This file is part of the financial mathematics research project
--! de.uni-kl.eit.ems.finance
--! @file
--! @brief Axi-Stream Slave Interface (Microblaze --> AxiSlv)
--! @author Luis Vega

library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

--! @details this unit serve as Read Interface for AXI-Stream protocol
--! @details basically, is a Serial-To-Parallel interface.
--! @details s_axis_tlast is not used inside the core.
--! @details Put it to low '0', if you want to simulate.
--! @details Data will be written (valid) in registers when
--! @details the number of input data has filled all the registers.
--! @details Data is written in big endian representation
--! @details First valid s_axis_tdata is placed in most significant WIDTH-bits of reg_array.
--! @details rst_output can be used as reset output controlled by software
--! @details rst_output depends on G_RESET_ACTIVE.
--! @details rst_output will check the first written register (cfg_reg) to generate the rst
--! @details rst_output is activated if the first s_axis_tdata is 0x00000001

entity AxiSlv is
  generic (G_RESET_ACTIVE : std_logic;  --! reset is active on level '1' (rst active high) or '0' (rst active low)
           NUM_RST_CYCLE  : integer;  --! number of reset cycles for rst_output
           WIDTH          : integer;    --! bit width
           NUM_REG        : integer);   --! number of registers
  port(
    --! clock input, sensitive to positive clock edge
    clk             : in  std_logic;
    --! reset input, depends on G_RESET_ACTIVE if active high or active low
    rst             : in  std_logic;
    --! s_axis_tvalid input, unit read data if s_axis_tvalid = '1'
    s_axis_tvalid   : in  std_logic;
    --! s_axis_tdata input, input s_axis_tdata stream
    s_axis_tdata    : in  std_logic_vector(WIDTH-1 downto 0);
    --! last input, unit read the last value of the stream if last = '1'
    s_axis_tlast    : in  std_logic;
    --! ready out, unit is able to read data if ready = '1'
    s_axis_tready   : out std_logic;
    --! reset output, generated by the cfg_reg and depends on G_RESET_ACTIVE
    rst_output      : out std_logic;
    --! reg_array, output signal from registers
    reg_array       : out std_logic_vector(WIDTH*NUM_REG-1 downto 0);
    --! reg_array_valid, unit is able to store the data stream if reg_array_valid = '1'
    reg_array_valid : out std_logic);

end AxiSlv;

architecture Behavioral of AxiSlv is

  type   states is (CFG, LOAD);
  signal state           : states;
  type   arr is array (NUM_REG - 1 downto 0) of std_logic_vector(WIDTH - 1 downto 0);
  signal tmp_array       : arr;
  signal cfg_reg         : std_logic_vector(WIDTH - 1 downto 0);
  signal stream_valid    : std_logic;
  signal stream_valid_d1 : std_logic;
  signal write_valid     : std_logic;
  signal cnt             : integer;
  signal rst_cnt         : integer;
  signal i               : integer;

begin

  -- Always ready to receive a value
  s_axis_tready <= '1';


  -- FSM 
  process(rst, clk)
  begin
    if (rst = G_RESET_ACTIVE) then
      state <= CFG;
    elsif (clk'event and clk = '1') then
      case state is
        when CFG =>
          cnt <= 0;
          if (s_axis_tvalid = '1') then
            cfg_reg <= s_axis_tdata;
            state   <= LOAD;
          end if;
        when LOAD =>
          if (cnt < NUM_REG and s_axis_tvalid = '1') then
            tmp_array(cnt) <= s_axis_tdata;
            cnt            <= cnt + 1;
            state          <= LOAD;
          elsif (cnt < NUM_REG and s_axis_tvalid = '0') then
            state <= LOAD;
          else
            state <= CFG;
          end if;
      end case;
    end if;
  end process;


  -- reg_array_valid generation
  process(rst, clk)
  begin
    if (rst = G_RESET_ACTIVE) then
      stream_valid <= '0';
      write_valid  <= '0';
    elsif (clk'event and clk = '1') then
      if (cnt = NUM_REG - 1 and s_axis_tvalid = '1') then
        stream_valid <= '1';
        write_valid  <= '1';
      else
        write_valid <= '0';
      end if;
      reg_array_valid <= stream_valid;
    end if;
  end process;


  -- Gnerating rst_output
  process(clk)
  begin
    if (clk'event and clk = '1') then
      if (state = LOAD) then
        rst_cnt <= 0;
        rst_output <= not(G_RESET_ACTIVE);
      elsif (cfg_reg(0) = '1' and rst_cnt < NUM_RST_CYCLE) then
        rst_cnt    <= rst_cnt + 1;
        rst_output <= G_RESET_ACTIVE;
      else  
        rst_output <= not(G_RESET_ACTIVE);
      end if;
    end if;   
  end process;

  
  -- Write registers (reg_array) 
  process(clk)
  begin
    if (clk'event and clk = '1') then
      if (write_valid = '1') then
        for i in 0 to NUM_REG - 1 loop
          reg_array(WIDTH*(NUM_REG - i) - 1 downto WIDTH*(NUM_REG - (i+1))) <= tmp_array(i);
        end loop;  -- i        
      end if;
    end if;
  end process;

end architecture behavioral;

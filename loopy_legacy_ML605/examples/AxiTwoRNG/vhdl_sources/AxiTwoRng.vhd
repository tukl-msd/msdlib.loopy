--!
--! Copyright (C) 2012 University of Kaiserslautern
--! Microelectronic Systems Design Research Group
--!
--! This file is part of the financial mathematics research project
--! de.uni-kl.eit.ems.finance
--! @file
--! @brief Axi4-Stream interface + 2*RngUniformTausworthe88
--! @author Luis Vega

library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

entity AxiTwoRng is
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
end AxiTwoRng;

architecture behavioral of AxiTwoRng is

  -- component generics
  constant G_RESET_ACTIVE : std_logic := '0';
  constant NUM_RST_CYCLE  : integer   := 5;
  constant WIDTH          : integer   := 32;
  constant NUM_REG_SLV    : integer   := 6;
  constant NUM_REG_MST    : integer   := 2;

  component AxiSlv
    generic (
      G_RESET_ACTIVE : std_logic;
      NUM_RST_CYCLE  : integer;
      WIDTH          : integer;
      NUM_REG        : integer);
    port (
      clk             : in  std_logic;
      rst             : in  std_logic;
      s_axis_tvalid   : in  std_logic;
      s_axis_tdata    : in  std_logic_vector(WIDTH-1 downto 0);
      s_axis_tlast    : in  std_logic;
      s_axis_tready   : out std_logic;
      rst_output      : out std_logic;
      reg_array       : out std_logic_vector(NUM_REG_SLV*WIDTH-1 downto 0);
      reg_array_valid : out std_logic);
  end component;

  component RngUniformTausworthe88
    generic (
      G_RESET_ACTIVE : std_logic);
    port (
      seed1             : in  std_logic_vector(31 downto 0);
      seed2             : in  std_logic_vector(31 downto 0);
      seed3             : in  std_logic_vector(31 downto 0);
      clk               : in  std_logic;
      rst               : in  std_logic;
      ready             : in  std_logic;
      valid             : out std_logic;
      random_number_out : out std_logic_vector(31 downto 0));
  end component;

  component AxiMst
    generic (
      G_RESET_ACTIVE : std_logic;
      WIDTH          : integer;
      NUM_REG        : integer);
    port (
      clk             : in  std_logic;
      rst             : in  std_logic;
      reg_array       : in  std_logic_vector(NUM_REG_MST*WIDTH - 1 downto 0);
      reg_array_valid : in  std_logic;
      reg_array_ready : out std_logic;
      m_axis_tvalid   : out std_logic;
      m_axis_tdata    : out std_logic_vector(WIDTH - 1 downto 0);
      m_axis_tlast    : out std_logic;
      m_axis_tready   : in  std_logic);
  end component;
  
  -- general signals
  signal s_seed            : std_logic_vector(NUM_REG_SLV*WIDTH - 1 downto 0);
  signal s_rng_output      : std_logic_vector(NUM_REG_MST*WIDTH - 1 downto 0);
  signal s_rst_output      : std_logic;
  signal s_reg_array_valid : std_logic;
  signal s_rst_rng         : std_logic;  
  signal s_rng_valid1      : std_logic;
  signal s_rng_valid2      : std_logic;
  signal s_reg_valid       : std_logic;
  signal s_reg_ready       : std_logic;

begin  -- behavioral

  AxiSlv_1 : AxiSlv
    generic map (
      G_RESET_ACTIVE => G_RESET_ACTIVE,
      NUM_RST_CYCLE  => NUM_RST_CYCLE,
      WIDTH          => WIDTH,
      NUM_REG        => NUM_REG_SLV)
    port map (
      clk             => aclk,
      rst             => aresetn,
      s_axis_tvalid   => s_axis_tvalid,
      s_axis_tdata    => s_axis_tdata,
      s_axis_tlast    => s_axis_tlast,
      s_axis_tready   => s_axis_tready,
      rst_output      => s_rst_output,
      reg_array       => s_seed,
      reg_array_valid => s_reg_array_valid);

  s_rst_rng <= (s_rst_output and s_reg_array_valid and aresetn);

  RngUniformTausworthe88_1 : RngUniformTausworthe88
    generic map (
      G_RESET_ACTIVE => G_RESET_ACTIVE)
    port map (
      seed1             => s_seed(NUM_REG_SLV*WIDTH - 1 downto (NUM_REG_SLV-1)*WIDTH),
      seed2             => s_seed((NUM_REG_SLV-1)*WIDTH - 1 downto (NUM_REG_SLV-2)*WIDTH),
      seed3             => s_seed((NUM_REG_SLV-2)*WIDTH - 1 downto (NUM_REG_SLV-3)*WIDTH),
      clk               => aclk,
      rst               => s_rst_rng,
      ready             => s_reg_ready,
      valid             => s_rng_valid1,
      random_number_out => s_rng_output(NUM_REG_MST*WIDTH - 1 downto (NUM_REG_MST - 1)*WIDTH));  

  RngUniformTausworthe88_2 : RngUniformTausworthe88
    generic map (
      G_RESET_ACTIVE => G_RESET_ACTIVE)
    port map (
      seed1             => s_seed((NUM_REG_SLV-3)*WIDTH - 1 downto (NUM_REG_SLV-4)*WIDTH),
      seed2             => s_seed((NUM_REG_SLV-4)*WIDTH - 1 downto (NUM_REG_SLV-5)*WIDTH),
      seed3             => s_seed((NUM_REG_SLV-5)*WIDTH - 1 downto 0),
      clk               => aclk,
      rst               => s_rst_rng,
      ready             => s_reg_ready,
      valid             => s_rng_valid2,
      random_number_out => s_rng_output((NUM_REG_MST - 1)*WIDTH - 1 downto 0));

  s_reg_valid <= (s_rng_valid1 and s_rng_valid2);
  
  AxiMst_1: AxiMst
    generic map (
      G_RESET_ACTIVE => G_RESET_ACTIVE,
      WIDTH          => WIDTH,
      NUM_REG        => NUM_REG_MST)
    port map (
      clk             => aclk,
      rst             => s_rst_rng,
      reg_array       => s_rng_output,
      reg_array_valid => s_reg_valid,
      reg_array_ready => s_reg_ready,
      m_axis_tvalid   => m_axis_tvalid,
      m_axis_tdata    => m_axis_tdata,
      m_axis_tlast    => m_axis_tlast,
      m_axis_tready   => m_axis_tready);
  
end behavioral;

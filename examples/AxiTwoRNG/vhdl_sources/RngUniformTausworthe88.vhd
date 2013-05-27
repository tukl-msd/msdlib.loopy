--
-- Copyright (C) 2011 University of Kaiserslautern
-- Microelectronic Systems Design Research Group
--
-- This file is part of the financial mathematics research project
-- de.uni-kl.eit.ems.finance
--! @file
--! @brief uniform Tausworthe88 random number generator
--! @author Daniel Schmidt, Dominik Goeke, Christian de Schryver
--! @date 2011-08-01
--

library ieee;
use ieee.std_logic_1164.all; 
use ieee.numeric_std.all;

--! @brief uniform Tausworthe88 random number generator

--! @detail This unit generates uniformly distributed random numbers.
--! @detail It outputs one random numer per clock cycle with the first random
--! @detail number available in the next clock cycle after the reset.
--! @detail If enable is '0', the unit is stalled and the output remains unchanged.
--! @detail The three seeds are read at the reset.

--! @test check if bit-true to de.uni-kl.eit.ems.common.rng.cpp.RngUniformTausworthe88


entity RngUniformTausworthe88 is
	generic (
		g_reset_active : std_logic --! reset is active on level '1' (rst active high) or '0' (rst active low)
	);
	port (
		--! seed 1 input, value is read at reset
		seed1 : in std_logic_vector(31 downto 0);
		--! seed 2 input, value is read at reset		
		seed2 : in std_logic_vector(31 downto 0);
		--! seed 3 input, value is read at reset
		seed3 : in std_logic_vector(31 downto 0);
		--! clock input, sensitive to positive clock edge
		clk : in std_logic;
		--! reset input, depends on g_reset_active if active high or active low
		rst : in std_logic;
		--! unit generates one new random number per clock cycle if ready='1'		
		ready : in std_logic;
		--! unit is able to ouput a valid random number if valid = '1'
		valid : out std_logic;
		--! random number out, MSB is at random_number_out(31)
		random_number_out: out std_logic_vector(31 downto 0) 
	);
end RngUniformTausworthe88;

architecture Behavioral of RngUniformTausworthe88 is

constant c1_n : integer := -2;
constant c2_n : integer := -8;
constant c3_n : integer := -16;

constant c1 : std_logic_vector(31 downto 0) := std_logic_vector(to_signed(c1_n, 32));
constant c2 : std_logic_vector(31 downto 0) := std_logic_vector(to_signed(c2_n, 32));
constant c3 : std_logic_vector(31 downto 0) := std_logic_vector(to_signed(c3_n, 32));

signal s1 : std_logic_vector(31 downto 0);
signal s2 : std_logic_vector(31 downto 0);
signal s3 : std_logic_vector(31 downto 0);

signal x1 : std_logic_vector(31 downto 0);
signal x2 : std_logic_vector(31 downto 0);
signal x3 : std_logic_vector(31 downto 0);

signal a1 : std_logic_vector(31 downto 0);
signal a2 : std_logic_vector(31 downto 0);
signal a3 : std_logic_vector(31 downto 0);

signal ns1 : std_logic_vector(31 downto 0);
signal ns2 : std_logic_vector(31 downto 0);
signal ns3 : std_logic_vector(31 downto 0);

type states is (START, GEN);
signal state : states;


begin

taus_generator: process(clk)
begin
	if (clk'event and clk = '1') then
		if (rst = g_reset_active) then
			s1 <= seed1; --! unit generates one random number per clock cycle if enable='1'
			s2 <= seed2;
			s3 <= seed3;
			random_number_out <= (others => '0');
			state <= START;
			valid <= '0';
		else
			case state is
				when START =>
					random_number_out <= s1 XOR s2 XOR s3;
					s1 <= ns1;
					s2 <= ns2;
					s3 <= ns3;
					valid <= '1';	
					state <= GEN;
				when GEN =>
					if(ready = '1') then
						random_number_out <= s1 XOR s2 XOR s3;
						s1 <= ns1;
						s2 <= ns2;
						s3 <= ns3;
					end if;	
			end case;	
		end if;
	end if;
end process;

x1 <= (s1(31-13 downto 0) & (12 downto 0 => '0')) XOR s1;
x2 <= (s2(31-2 downto 0) & (1 downto 0 => '0')) XOR s2;
x3 <= (s3(31-3 downto 0) & (2 downto 0 => '0')) XOR s3;

a1 <= s1 AND c1;
a2 <= s2 AND c2;
a3 <= s3 AND c3;

ns1 <= ((31 downto 31-18 => '0') & x1(31 downto 19)) XOR (a1(31-12 downto 0) & (11 downto 0 => '0'));
ns2 <= ((31 downto 31-24 => '0') & x2(31 downto 25)) XOR (a2(31-4 downto 0) & (3 downto 0 => '0'));
ns3 <= ((31 downto 31-10 => '0') & x3(31 downto 11)) XOR (a3(31-17 downto 0) & (16 downto 0 => '0'));

end Behavioral;


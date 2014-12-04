#include "api/setup.h"
#include "api/components.h"

#include <vector>
#include <bitset>

#include <stdio.h>

void randomTest() {
  // init test seeds
  int reset = 0x00000001;
  int seed1 = 0x32408702;
  int seed2 = 0x39480457;
  int seed3 = 0x37543452;

  // init seed vector
  std::vector<std::bitset<32> > seeds;
  seeds.push_back(reset);
  seeds.push_back(seed1);
  seeds.push_back(seed2);
  seeds.push_back(seed3);
  seeds.push_back(seed1);
  seeds.push_back(seed1);
  seeds.push_back(seed1);

  // write seed vector
  rng_a.s_axis.write(seeds);

  // read values
  std::vector<std::bitset<32> > vals(8);
  rng_a.m_axis.read(vals);

  // print values
  for(unsigned int i = 0; i < vals.size(); i++)
      std::cout << std::endl << "value: " << vals[i].to_ulong();
}

int main() {
  startup();

  check_checksum();

  randomTest();

  shutdown();
}


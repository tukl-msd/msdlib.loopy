#include "api/setup.h"
#include "api/components.h"

#include <vector>
#include <bitset>

#include <stdio.h>

void YourFunction() {

  // init test sequence  
  int value1 = 0x88cb47c9;
  int value2 = 0x8a9cdf65;
  int value3 = 0xcaf40ed9;

  // init vector
  std::vector<std::bitset<32> > values;
  
  //seeds.push_back(reset);
  values.push_back(value1);
  values.push_back(value2);
  values.push_back(value3);
  values.push_back(value1);
  values.push_back(value2);
  values.push_back(value3);
  values.push_back(value1);
  values.push_back(value2);
  values.push_back(value3);

  // write vector
  axififo_a.s_axis.write(values);

  // read values
  std::vector<std::bitset<32> > vals(9);
  axififo_a.m_axis.read(vals);

  // print values
  for(unsigned int i = 0; i < vals.size(); i++)
      std::cout << std::endl << "value: " << vals[i].to_ulong();
}

int main()
{
  startup("131.246.74.3");

  YourFunction();

  shutdown();
}

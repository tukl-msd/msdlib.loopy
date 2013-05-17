#include "api/setup.h"
#include "api/components.h"

#include <vector>
#include <bitset>

#include <unistd.h>
#include <stdarg.h>

using namespace std;

void randomTest() {
  // init test seeds
  int reset = 0x00000001;
  int seed1 = 0x32408702;
  int seed2 = 0x39480457;
  int seed3 = 0x37543452;

  // init seed vector
  vector<bitset<32> > vector;
  vector.push_back(reset);
  vector.push_back(seed1);
  vector.push_back(seed2);
  vector.push_back(seed3);
  vector.push_back(seed1);
  vector.push_back(seed1);
  vector.push_back(seed1);

  // write seed vector
  rng_a.s_axis.write(vector);

  // wait for values
  sleep(5);

  // read value
  bitset<32> rslt = rng_a.m_axis.read();

  // print value
  printf("\nvalue: %lu", rslt.to_ulong());
}

int main() {
  startup();

  randomTest();

  shutdown();
}






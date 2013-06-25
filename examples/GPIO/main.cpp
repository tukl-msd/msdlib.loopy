#include "api/setup.h"
#include "api/components.h"

#include <bitset>
#include <iostream>

using namespace std;

void led_test() {
  cout << "starting loopy gpio example led test" << endl;
  cout << "watch your board's leds carefully (;" << endl;

  // run gpo test
  gpio_leds.test();
  gpio_leds.test();
  gpio_leds.test();

  // reset led state to 0
  gpio_leds.writeState(bitset<8>(0));

  cout << "test has been completed" << endl;
}

void gpi_test() {
  cout << "starting loopy gpio example input test" << endl;
  cout << "press button 1 to increment the led state" << endl;
  cout << "press button 2 to reset the led state" << endl;
  cout << "press button 3 to print the count of these test executed so far" << endl;
  cout << "press button 4 to lock client until it is pressed again" << endl;
  cout << "press button 5 to terminate example application" << endl;
  cout << "press several buttons at once for a suprise" << endl;

  bool var = true;
  int ledState = 0;
  int count = 0;
  while(var) {
    gpio_buttons.waitForChange();

    bitset<5> state = gpio_buttons.readState();

    switch(state.to_ulong()) {
    case 0: break; // changing the state back to 0 also triggers an event
    case 1: count++;
      cout << "you pressed button 1!" << endl;
      ledState++;
      ledState = ledState % 256;
      gpio_leds.writeState(bitset<8>(ledState));
      break;
    case 2: count++;
      cout << "you pressed button 2!" << endl;
      gpio_leds.writeState(bitset<8>(0));
      break;
    case 4: count++;
      cout << "you pressed button 3!" << endl;
      cout << "in total, you've executed " << count << " button examples so far" << endl;
      break;
    case 8: count++;
      cout << "you pressed button 4!" << endl;
      do {
    	  gpio_buttons.waitForChange();
      } while(gpio_buttons.readState() != 8);
      cout << "you pressed button 4 again!" << endl;
      break;
    case 16: count++;
      cout << "you pressed button 5! terminating" << endl;
      var = false;
      break;
    default: count++;
      cout << "you pressed multiple buttons at once!" << endl;
      gpio_leds.writeState(bitset<8>("10101010"));
      sleep(1);
      gpio_leds.writeState(bitset<8>("01010101"));
      sleep(1);
      gpio_leds.writeState(bitset<8>("10101010"));
      sleep(1);
      gpio_leds.writeState(bitset<8>("01010101"));
      sleep(1);
      gpio_leds.writeState(bitset<8>(0));
      break;
    }
  }
}

int main() {
  startup();

  led_test();

  gpi_test();

  shutdown();
}


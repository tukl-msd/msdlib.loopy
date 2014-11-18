#include "api/setup.h"
#include "api/components.h"

#include <bitset>
#include <iostream>

void led_test() {
  std::cout << "starting loopy gpio example led test" << std::endl;
  std::cout << "watch your board's leds carefully (;" << std::endl;

  // run gpo test
  gpio_leds.test();
  gpio_leds.test();
  gpio_leds.test();

  // reset led state to 0
  gpio_leds.writeState(std::bitset<8>(0));

  std::cout << "test has been completed" << std::endl;
}

void gpi_test() {
  std::cout << "starting loopy gpio example input test" << std::endl;
  std::cout << "press button 1 to increment the led state" << std::endl;
  std::cout << "press button 2 to reset the led state" << std::endl;
  std::cout << "press button 3 to print the count of these test executed so far" << std::endl;
  std::cout << "press button 4 to lock client until it is pressed again" << std::endl;
  std::cout << "press button 5 to terminate example application" << std::endl;
  std::cout << "press several buttons at once for a suprise" << std::endl;

  bool var = true;
  int ledState = 0;
  int count = 0;
  while(var) {
    gpio_buttons.waitForChange();

    std::bitset<5> state = gpio_buttons.readState();

    switch(state.to_ulong()) {
    case 0: break; // changing the state back to 0 also triggers an event
    case 1: count++;
    std::cout << "you pressed button 1!" << std::endl;
      ledState++;
      ledState = ledState % 256;
      gpio_leds.writeState(std::bitset<8>(ledState));
      break;
    case 2: count++;
    std::cout << "you pressed button 2!" << std::endl;
      gpio_leds.writeState(std::bitset<8>(0));
      break;
    case 4: count++;
    std::cout << "you pressed button 3!" << std::endl;
    std::cout << "in total, you've executed " << count << " button examples so far" << std::endl;
      break;
    case 8: count++;
      std::cout << "you pressed button 4!" << std::endl;
      do {
    	  gpio_buttons.waitForChange();
      } while(gpio_buttons.readState() != 8);
      std::cout << "you pressed button 4 again!" << std::endl;
      break;
    case 16: count++;
    std::cout << "you pressed button 5! terminating" << std::endl;
      var = false;
      break;
    default: count++;
    std::cout << "you pressed multiple buttons at once!" << std::endl;
      gpio_leds.writeState(std::bitset<8>("10101010"));
      sleep(1);
      gpio_leds.writeState(std::bitset<8>("01010101"));
      sleep(1);
      gpio_leds.writeState(std::bitset<8>("10101010"));
      sleep(1);
      gpio_leds.writeState(std::bitset<8>("01010101"));
      sleep(1);
      gpio_leds.writeState(std::bitset<8>(0));
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


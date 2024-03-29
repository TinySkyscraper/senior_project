#!/usr/bin/python3

from gpiozero import Servo
from time import sleep

def unlock():           # Move lock to unlock door
                        #-------------------------

    servo = Servo(27,                                   # Create servo motor object
              max_pulse_width=(2.45 / 1000.0),      #--------------------------
              min_pulse_width=(0.95 / 1000.0))

    servo.min()         # Unlock
    sleep(2)            #-------

    servo.max()         # Lock
    sleep(2)            #-----

    servo.detach()      # Turn off motor
                        #---------------

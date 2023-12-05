#!/usr/bin/python3

import bluetooth
import secrets
import sqlite3
from Crypto.PublicKey import RSA
from Crypto.Signature import pss
from Crypto.Hash import SHA256
import requests
import base64

import hardware                 # Custom python file in same directory
                                # to control lock mechanism
                                #-------------------------------------

CENTRAL_HOST: str               # Door number and hostname of central
DOOR_NUMBER: str                # server read from config file
                                #------------------------------------


with open('./server_config', 'r') as f:         # Read values from config file
    data = f.read()                             #-----------------------------
    lines = data.split('\n')
    for i in range(0, len(lines)):
        lines[i] = lines[i].split('=')

    for env in lines:
        if env[0] == 'CENTRAL_HOST':
            CENTRAL_HOST = env[1]
        elif env[0] == 'DOOR_NUMBER':
            DOOR_NUMBER = env[1]
    



def verify(pubkey, signature, challenge_message):       # Verifies user's identity using pubkey
    publickey = RSA.import_key(pubkey)                  # from central server, challenge message
    sign_verifier = pss.new(publickey)                  # sent to user's mobile device, and the
    hasher = SHA256.new()                               # signature returned by the user's mobile
    hasher.update(challenge_message)                    # device
    verified = True                                     #----------------------------------------
    try:
        sign_verifier.verify(hasher, signature)
    except ValueError:
        verified = False

    return verified


def random_string():                        # Generates random 256 byte string for
    return secrets.token_bytes(256)         # challenge message
                                            #-------------------------------------


def start_server():                                             # Starts Bluetooth server
    server_sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)   #------------------------
    server_sock.bind(("", bluetooth.PORT_ANY))
    server_sock.listen(1)
    port = server_sock.getsockname()[1]
    uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ee"       # UUID to identify Bluetooth service
    username = ""                                       #-----------------------------------

    bluetooth.advertise_service(
        server_sock,
        "SampleServer",
        service_id=uuid,
        service_classes=[uuid, bluetooth.SERIAL_PORT_CLASS],
        profiles=[bluetooth.SERIAL_PORT_PROFILE],
    )

    while True:
        print("Waiting for connection on RFCOMM channel", port)
        client_sock, client_info = server_sock.accept()             # Wait for connection
        print("Accepted connection from", client_info)              #--------------------

        try:
            rand_bytes = random_string()
            client_sock.send(rand_bytes)            # Send challenge message
            while True:                             #-----------------------
                data = client_sock.recv(2500)
                if data:
                    username, signature = data.split(b"EoU", 1)     # Separate username from signature
                                                                    #---------------------------------

                    response = requests.post(                   # Request pubkey if username is allowed
                                                                # for that door
                                                                #--------------------------------------
                        f"https://{CENTRAL_HOST}:50002/auth",
                        data="{" + f'"username": "{username.decode()}", "door": "{DOOR_NUMBER}"' + "}"
                                            )
                    
                    if (response.content.decode() == "Not Authorized"):     # Close connection if Not Allowed
                                                                            #--------------------------------
                        client_sock.close()
                        continue
                    
                    pubkey = base64.b64decode(response.content)

                    if verify(pubkey, signature, rand_bytes):           # Verify identity
                                                                        #----------------

                        hardware.unlock()                   # Unlock door and notify mobile  /
                        client_sock.send(b"VERIFIED")       # device of verification        /
                                                            #------------------------------
                        client_sock.close()
                        continue
                    else:

                        client_sock.send(b"DENIED")         # Notify mobile device of denial
                                                            #-------------------------------
                        client_sock.close()
                        continue

        except Exception as e:
            print("Connection closed")


if __name__ == "__main__":

    start_server()                  # start Bluetooth server
                                    #-----------------------

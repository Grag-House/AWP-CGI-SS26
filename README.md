# temi_app

Temi all on one Application

## Getting Started

New home for temi app, using a valid name for dart.

Current supported functionality
- temi basic functionalities


To migrate / todo:
- MQTT connections
- MQTT temi link
- Video capture 
- README

## Setup Mock Environment 

For Testing the App is Recommended to Build a Mosquitto Broker in the Local Network. <br>

### Raspberry Pi Mosquitto Broker Setup:
The following guide shows how to install the Mosquitto Broker on a ```Raspberry Pi```: <br>
https://www.elektronik-kompendium.de/sites/raspberry-pi/2709041.htm
<br><br>
After installation the following local.conf should be setted up on <br>
```sudo nano /etc/mosquitto/conf.d/local.conf```
<br>
Copy-Paste the following text:
```
listener 1883
allow_anonymous true
listener 8080
protocol websockets
```
If config is put on restart Mosquitto Service with ```sudo systemctl restart mosquitto ```
<br><br>
To connect with the Mosquitto Broker with the App you need only the IP Adress from the
Raspberry Pi

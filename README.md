# temi_app

Temi All-in-One Application

## Getting Started

New home for the Temi app, using a valid name for Dart.

### Current supported functionality
- Basic Temi functionalities

### To migrate / To-do
- [ ] MQTT connections
- [ ] MQTT Temi link
- [ ] Video capture 
- [ ] README updates

---

## Setup Mock Environment 

For testing the app, it is recommended to set up a **Mosquitto Broker** in your local network.

### Raspberry Pi Mosquitto Broker Setup
The following guide shows how to install the Mosquitto Broker on a **Raspberry Pi**:
[https://www.elektronik-kompendium.de/sites/raspberry-pi/2709041.htm](https://www.elektronik-kompendium.de/sites/raspberry-pi/2709041.htm)

After installation, the `local.conf` should be configured at:
`sudo nano /etc/mosquitto/conf.d/local.conf`

**Copy and paste the following configuration:**
```conf
listener 1883
allow_anonymous true
listener 8080
protocol websockets
````

If config is put on restart Mosquitto Service with ```sudo systemctl restart mosquitto ```
<br><br>
To connect with the Mosquitto Broker with the App you need only the IP Adress from the
Raspberry Pi
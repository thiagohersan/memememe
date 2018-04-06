## memememe Raspberry Pi setup

1. `sudo raspi-config`: disable x, enable console auto-login, enable ssh, change hostname, change password, expand filesystem
2. `sudo apt-get update`
3. `sudo apt-get purge wolfram-engine libreoffice*`
4. `sudo apt-get install emacs hostapd isc-dhcp-server autoconf libtool python-dev libssl-dev libnl-3-dev libnl-genl-3-dev liblo-dev`
5. [Configure Serial](http://www.oppedijk.com/robotics/control-dynamixel-with-raspberrypi):
  - Stop ttyAMA0: `sudo systemctl stop serial-getty@ttyAMA0.service`
  - Disable ttyAMA0: `sudo systemctl disable serial-getty@ttyAMA0.service`
  - Check: `systemctl list-units | grep getty`
  - Add the following line to ~/.bashrc: `sudo stty -F /dev/ttyAMA0 1000000`
  - Add the following line to /boot/config.txt: `init_uart_clock=16000000`
  - Edit /boot/cmdline.txt and remove all options mentioning ttyAMA0.
6. Install Rpi.GPIO: `sudo pip install --upgrade Rpi.GPIO`
7. Install pyserial: `sudo pip install --upgrade pyserial`
8. Intsall cython: `sudo pip install --upgrade cython`
9. Install [pyliblo](http://das.nasophon.de/pyliblo/):
  - `wget http://das.nasophon.de/download/pyliblo-0.10.0.tar.gz`
  - `tar -xvf pyliblo-0.10.0.tar.gz`
  - `cd pyliblo-0.10.0/`
  - `sudo ./setup.py build; sudo ./setup.py install`
  - `cd ../; sudo rm -rf pyliblo*`
10. Install [python noise](https://github.com/caseman/noise):
  - `git clone https://github.com/caseman/noise.git`
  - `cd noise; sudo python setup.py install`
  - `cd ../; sudo rm -rf noise`
11. Install [hostapd-rtl8188](https://github.com/lostincynicism/hostapd-rtl8188):
  - `git clone https://github.com/lostincynicism/hostapd-rtl8188.git`
  - `cd hostapd-rtl8188/hostapd`
  - `rm hostapd hostapd_cli`
  - `sudo make; sudo make install`
  - `sudo cp hostapd /usr/sbin/; sudo cp hostapd_cli /usr/sbin/`
12. Set up [WiFi Access Point](https://learn.adafruit.com/setting-up-a-raspberry-pi-as-a-wifi-access-point/install-software) Ignore the section **Update hostapd (maybe)** the step 11 seems work better
  - the dhcpd.conf will be
    - be aware that an external dns server can be block in some networks so you might need to change the `option domain-name-server` ips.
  ```
subnet 10.10.0.0 netmask 255.255.255.0 {
        range 10.10.0.100 10.10.0.200;
        option broadcast-address 10.10.0.255;
        option routers 10.10.0.1;
        default-lease-time 7200;
        max-lease-time 7200;
        option domain-name "local";
        option domain-name-servers 8.8.8.8, 8.8.4.4;
        }
  ```
  - the networks/interfaces
  ```
iface wlan0 inet static
  address 10.10.0.1
  netmask 255.255.255.0
  ```

13. Dual Wifi interfaces, in case no Ethernet is preset, Setup the extra Wlan Interface to be a client and foward wlan1 to wlan0

Enable wlan1 interface to be DHCP with wpa_supplicant 

Edit `/etc/network/interfaces`

```
allow-hotplug wlan1
iface wlan1 inet dhcp
    wpa-conf /etc/wpa_supplicant/wpa_supplicant.conf
```

Add the SSID and Password on `/etc/wpa_supplicant/wpa_supplicant.conf`

```
network={
 ssid="SSID"
 psk="PASSWORD"
}
```

Update iptables

```
sudo iptables -t nat -A POSTROUTING -o wlan1 -j MASQUERADE
sudo iptables -A FORWARD -i wlan1 -o wlan0 -m state --state RELATED,ESTABLISHED -j ACCEPT
sudo iptables -A FORWARD -i wlan0 -o wlan1 -j ACCEPT
```

```
sudo sh -c "iptables-save > /etc/iptables/rules.v4"
```

### Follow Steps To SetUp Raspberry Pi

1. Go to `/lib/systemd/system/bluetooth.service` and change `ExecStart=/usr/lib/bluetooth/bluetoothd` to `ExecStart=/usr/lib/bluetooth/bluetoothd -C`
2. Run `sudo systemctl daemon-reload` and `sudo systemctl restart bluetooth` then `sudo sdptool add SP`
3. Create file `/etc/systemd/system/var-run-sdp.path` then paste `[Unit]\nDescrption=Monitor /var/run/sdp\n\n[Install]\nWantedBy=bluetooth.service\n\n[Path]\nPathExists=/var/run/sdp\nUnit=var-run-sdp.service` into it
4. Create file `/etc/systemd/system/var-run-sdp.service` then paste `[Unit]\nDescription=Set permission of /var/run/sdp\n\n[Install]\nRequiredBy=var-run-sdp.path\n\n[Service]\nType=simple\nExecStart=/bin/chgrp bluetooth /var/run/sdp` into it
5. Run in sequential order `sudo systemctl daemon-reload` `sudo systemctl enable var-run-sdp.path` `sudo systemctl enable var-run-sdp.service` `sudo systemctl start var-run-sdp.path`
6. Append `Authentication=false` to /etc/bluetooth/main.conf and change name in `/etc/hostname` to `CryptoLock`
7. Run `shutdown -r`
8. `pip install pycryptodome`
9. `pip install requests`
10. `pip install python-multipart`
11. `pip install "requests[security]"`
12. add `@reboot python3 ~/bt_server.py` and `@reboot sudo pigpiod` to crontab for user
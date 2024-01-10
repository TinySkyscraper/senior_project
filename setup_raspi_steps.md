### Follow Steps To SetUp Raspberry Pi

1. Go to `/lib/systemd/system/bluetooth.service` and change `ExecStart=/usr/lib/bluetooth/bluetoothd` to `ExecStart=/usr/lib/bluetooth/bluetoothd -C`
2. Run `sudo systemctl daemon-reload` and `sudo systemctl restart bluetooth` then `sudo sdptool add SP`
3. Create file `/etc/systemd/system/var-run-sdp.path` then paste into it<br>`[Unit]`<br>`Descrption=Monitor /var/run/sdp`<br><br>`[Install]`<br>`WantedBy=bluetooth.service`<br><br>`[Path]`<br>`PathExists=/var/run/sdp`<br>`Unit=var-run-sdp.service`
4. Create file `/etc/systemd/system/var-run-sdp.service` then paste into it <br>`[Unit]`<br>`Description=Set permission of /var/run/sdp`<br><br>`[Install]`<br>`RequiredBy=var-run-sdp.path`<br><br>`[Service]`<br>`Type=simple`<br>`ExecStart=/bin/chgrp bluetooth /var/run/sdp`
5. Run in sequential order <br>`sudo systemctl daemon-reload` <br>`sudo systemctl enable var-run-sdp.path` <br>`sudo systemctl enable var-run-sdp.service` <br>`sudo systemctl start var-run-sdp.path`
6. Append `Authentication=false` to `/etc/bluetooth/main.conf` and change name in `/etc/hostname` to `CryptoLock`
7. Run `shutdown -r`
8. `pip install pycryptodome`
9. `pip install requests`
10. `pip install python-multipart`
11. `pip install "requests[security]"`
12. add `@reboot python3 ~/bt_server.py` and `@reboot sudo pigpiod` to crontab for user
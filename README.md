# Intro

``` shell
alias ansible-playbook="bb build exec -- minipc ansible ansible-playbook"
ansible-playbook main.yml
```

# sudo
Ubuntu requires a password to become root by default.

``` shell
sudo -s
echo "ubuntu ALL=(ALL) NOPASSWD:ALL" | tee -a /etc/sudoers.d/ubuntu
```

# vscode uid
Ubuntu user has 1000 by default. GitHub Actions uses 1001. The default user is `vscode` for historical reasons and it uses uid 1001.

# eth0
Ubuntu 24.04 doesn't support YT6801 (Soyo).

``` shell
sudo -s
add-apt-repository ppa:slimbook/slimbook
apt update
apt install slimbook-yt6801-dkms
reboot
```

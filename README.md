# Intro
Create a dev enviroment using nix and big-config.

## hetzner
It works out of the box. I use the CX33 Helsinki with Ubuntu 24.04, only IPv4 and ssh key. The rebuild feature is used to test the playbook from scratch.

## sudo
Ubuntu requires a password to become root by default.

``` shell
sudo -s
echo "ubuntu ALL=(ALL) NOPASSWD:ALL" | tee -a /etc/sudoers.d/ubuntu

# or provide the password in the shell only for the first time
bb render exec -- minipc ansible ansible-playbook -K
```

## shell alias
``` shell
alias ansible-playbook="bb render exec -- minipc ansible ansible-playbook"
ansible-playbook main.yml
```

## ssh-config

```
Host hetzner
  HostName 95.217.164.175
  User ubuntu
  IdentityFile ~/.ssh/id_ed25519
  IdentitiesOnly yes
  ForwardAgent yes
Host hetzner-dev
  HostName 46.62.162.129
  User ubuntu
  IdentityFile ~/.ssh/id_ed25519
  IdentitiesOnly yes
  ForwardAgent yes
```

## vscode uid
Ubuntu user has 1000 by default. GitHub Actions uses 1001. The default user is `vscode` for historical reasons and it uses uid 1001.

## eth0
Ubuntu 24.04 doesn't support YT6801 (Soyo).

``` shell
sudo -s
add-apt-repository ppa:slimbook/slimbook
apt update
apt install slimbook-yt6801-dkms
reboot
```

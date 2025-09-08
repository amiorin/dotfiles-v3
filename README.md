# Intro

``` shell
bb build exec -- minipc ansible ansible-galaxy install -r requirements.yml
alias ansible-playbook="bb build exec -- minipc ansible ansible-playbook"
ansible-playbook main.yml
```

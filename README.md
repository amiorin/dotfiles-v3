##
WIP BigConfig template combining OpenTofu and Ansible

``` sh
# generic steps
bb ops create delete --node-name cesar-ford
# tofu steps
bb ops tofu --node-name cesar-ford tofu init
alias tofu="bb ops tofu --node-name cesar-ford tofu"
bb ops init plan --node-name cesar-ford [tofu extra-args]
## ansible steps
bb ops ansible --node-name cesar-ford ansible-playbook main.yml
alias ansible-playbook="bb ops ansible --node-name cesar-ford ansible-playbook"
bb ops playbook --node-name cesar-ford [ansible extra-args]
## rendering step
bb ops render --node-name cesar-ford

bb ssh cesar-ford
```


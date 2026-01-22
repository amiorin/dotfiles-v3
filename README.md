##
WIP BigConfig template combining OpenTofu and Ansible

``` sh
# generic steps
bb resource create --resource-name cesar-ford --resource-type big-iron

bb resource delete --resource-name cesar-ford --resource-type big-iron

# tofu steps
bb tofu render tofu:init lock tofu:apply:-auto-approve -- big-iron cesar-ford

# ansible steps
bb ansible render ansible-playbook:main.yml unlock-any -- big-iron cesar-ford

# ssh resource
bb ssh cesar-ford
```

# Tell terraform to use the provider and select a version.
terraform {
  required_providers {
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = "~> 1.45"
    }
  }
}


# Set the variable value in *.tfvars file
# or using the -var="hcloud_token=..." CLI option
variable "hcloud_token" {
  sensitive = true
}

# Configure the Hetzner Cloud Provider
provider "hcloud" {
  token = var.hcloud_token
}

resource "hcloud_server" "node1" {
  name  = "node1"
  image = "ubuntu-24.04"
  server_type = "cx23"
  location    = "hel1"
  ssh_keys = ["32617+amiorin@users.noreply.github.com"]
  public_net {
    ipv4_enabled = true
    ipv6_enabled = false
  }
}

output "ipv4_address" {
  value = hcloud_server.node1.ipv4_address
}

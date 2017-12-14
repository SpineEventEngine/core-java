#!/bin/bash

# Adds the first command line argument as a SSH private key.
#
# Starts an `ssh-agent` process and adds the given file name to the agent.
#

eval $(ssh-agent -s)

ssh-add $1

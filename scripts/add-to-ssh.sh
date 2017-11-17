#!/bin/bash

eval $(ssh-agent -s)

ssh-add $1

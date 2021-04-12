#! /usr/bin/bash

# Check number input arguments
argc=$#

if ((argc > 1 ))
then
	echo "Usage: $0 [<peer_id>]  | $0"
	exit 1
elif ((argc < 0))
then
	echo "Usage: $0 [<peer_id>] | $0"
	exit 1
elif ((argc == 0))
then
  find . -type d -name 'service-*' -exec rm -rf {} +
else
	peer_id=$1
  rm -rf service-${peer_id}
fi



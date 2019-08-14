#!/bin/bash
#set -e
echo "Init DB with genesis.json"
geth --datadir test init genesis.json
echo "Start geth"
geth --datadir test --networkid 15 --rpcapi personal,db,eth,net,web3 --rpc --rpcaddr 0.0.0.0 \
           --mine --minerthreads=1 --gasprice=1 --etherbase=0xcd2a3d9f938e13cd947ec05abc7fe734df8dd826 \
           --nodiscover --maxpeers 0
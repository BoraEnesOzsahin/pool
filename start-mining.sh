#!/bin/bash
# SRBMiner startup script for Monero mining via proxy

# Configuration
POOL="192.168.56.1:3333"
WALLET="yourAntpoolSubaccount.w-ubuntu-miner"
PASSWORD="x"
ALGORITHM="randomx"
CPU_THREADS="4"

# Run SRBMiner
./SRBMiner-MULTI \
  --algorithm $ALGORITHM \
  --pool $POOL \
  --wallet $WALLET \
  --password $PASSWORD \
  --cpu-threads $CPU_THREADS \
  --disable-gpu \
  --api-enable \
  --api-port 21555

# Note: Replace 'yourAntpoolSubaccount' with your actual AntPool subaccount
# Example: myaccount.w-ubuntu-miner

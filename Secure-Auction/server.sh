#!/bin/bash

echo "Compiling Java classes..."
javac *.java

echo "Starting RMI registry..."

if ! nc -z localhost 1099; then
    rmiregistry 1099 &
    echo "RMI registry started on port 1099."
else
    echo "RMI registry is already running on port 1099."
fi


sleep 1

echo "Starting Auction Server..."
java AuctionServer


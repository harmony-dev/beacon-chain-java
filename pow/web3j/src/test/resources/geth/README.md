Go-Ethereum instance with open RPC and non-persistent DB initialized with `genesis.json`.   

Building image:
```shell script
docker build .
```
Running container:
```shell script
docker run -d -p 8545:8545 -p 30303:30303 <image id>
```
Stopping and removing container
```shell script
docker  rm -f <container id>
```

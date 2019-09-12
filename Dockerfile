FROM gcr.io/whiteblock/base:ubuntu1804

RUN apt-get update
RUN apt-get install -y openjdk-8-jdk

# Harmony implementation depends on JVM libp2p implementation, which is not in a public repository yet.
# So, one should build it manually and push to a local maven repository, to be able to build the node command.
RUN git clone https://github.com/libp2p/jvm-libp2p --branch feature/cleanup
WORKDIR /jvm-libp2p
RUN ./gradlew build -x test
RUN ./gradlew publishToMavenLocal
WORKDIR /

# Cloning Harmony
RUN git clone https://github.com/harmony-dev/beacon-chain-java.git
WORKDIR /beacon-chain-java
# TODO: switch to develop when merged
RUN git checkout interop
WORKDIR /

# Building Harmony
WORKDIR /beacon-chain-java
RUN ./gradlew build -x test
WORKDIR start/node/build/distributions/
RUN tar -xf node*.tar
RUN ln -s /beacon-chain-java/start/node/build/distributions/node-*/bin/node /usr/bin/harmony
WORKDIR /

# Copying start script
RUN mkdir /launch
RUN cp /beacon-chain-java/scripts/whiteblock_start.sh /launch/start.sh
RUN chmod +x /launch/start.sh

EXPOSE 8545 8546 9000 30303 30303/udp

ENTRYPOINT ["/bin/bash"]

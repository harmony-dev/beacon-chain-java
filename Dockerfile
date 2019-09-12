FROM gcr.io/whiteblock/base:ubuntu1804

RUN apt-get update

RUN apt-get install -y openjdk-8-jre 
RUN git clone https://github.com/harmony-dev/beacon-chain-java.git
WORKDIR /beacon-chain-java
RUN git checkout interop

RUN ./gradlew build -x test

RUN mkdir /launch

RUN cp /beacon-chain-java/scripts/whiteblock_start.sh /launch/start.sh

ENTRYPOINT ["/bin/bash"]

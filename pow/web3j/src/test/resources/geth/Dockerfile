# Build Geth in a stock Go builder container
FROM golang:1.12-alpine as builder

RUN apk add --no-cache make gcc musl-dev linux-headers git

ARG branch=master
RUN git clone --depth 1 --branch $branch https://github.com/ethereum/go-ethereum.git

ADD . /go-ethereum
RUN cd /go/go-ethereum && make geth

# Pull Geth into a second stage deploy alpine container
FROM alpine:latest

RUN apk add --no-cache ca-certificates bash
COPY --from=builder /go/go-ethereum/build/bin/geth /usr/local/bin/

ADD genesis.json /genesis.json
ADD geth.sh /geth.sh
RUN chmod +x /geth.sh

EXPOSE 8545 8546 30303 30303/udp
ENTRYPOINT ["/geth.sh"]

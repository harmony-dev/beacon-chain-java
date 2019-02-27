# Beacon Chain Java
Ethereum 2.0 Serenity ~~client~~ emulator. Someday, definitely, it will be a ~~Shasper~~ Serenity client. We are working to get there. Currently there is no p2p and we don't support other clients.
 
## Ethereum 2.0?
Yes, Ethereum Foundation, community and other interested parties are developing successor of the current [Ethereum network](https://ethereum.org/) without cons :). 
It starts with [Phase 0](https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md), the simplest one. Phase 1 will be the next and so on.

## Develop
If you want to take part in Ethereum 2.0 development and use our code, we split everything into several modules, so anyone could easily take only the needed part. To dig into module goals, check [settings.gradle](settings.gradle). 

## Run
Install Java 8 or later. There are many guidelines in Internet, even the [official one](https://java.com/en/download/help/download_options.xml), from Oracle. Next, git clone our repo, build `client` module distribution and run emulator:
```bash
git clone https://github.com/harmony-dev/beacon-chain-java.git
cd beacon-chain-java
./gradlew :start:cli:distZip
``` 
Cool, we made distribution tied to our system, it's in the `start/cli/build/distributions/` folder and looks like `cli-0.1.0.zip`. Unzip it, enter the `bin` folder and run emulator:
```bash
cd start/cli/build/distributions/
unzip cli-0.1.0.zip
cd cli-0.1.0/bin
./emulator
``` 
and you will see help for `emulator` launcher. Preceding commands will work for OS X/Linux, in Windows you will need to  use `.bat` commands where its needed.

Let's run emulator with default settings and 4 peers:
```bash
./emulator run 4
```
#### Latest release
Release [0.1](#tag01) replicates Phase 0 specification of Ethereum 2.0 release [v0.4.0](https://github.com/ethereum/eth2.0-specs/releases/tag/0.4.0)

## Contribution guideline
Thank you for joining our efforts to drive Ethereum forward! 
We are not very strict on requirements but your code should help us to reach our goal, it should be easy to get it, understand the idea, and it should be feasible to review it. Also we are trying to match [Google code style](https://google.github.io/styleguide/javaguide.html) but we don't like it. Feel free to choose any [issue](https://github.com/harmony-dev/beacon-chain-java/issues) and ask how to do it better.  

## Links
[Ethereum 2.0 specs](https://github.com/ethereum/eth2.0-specs)

[Vitalik Buterin on DevCon4 about Ethereum 2.0](https://slideslive.com/38911602/latest-of-ethereum)
 

## Licensing
This project is licensed under Apache 2.0 license. You could use it for any commercial, private or open-source project.
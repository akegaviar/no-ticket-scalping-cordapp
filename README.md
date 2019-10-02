# No ticket scalping CorDapp

Ticket scalping for popular events has been a problem even in the pre-Internet era; however, with the digital technology becoming ubiquitous, scalping looked like a solved problem for a brief moment—it made sense too, the tickets became digital and available through websites, and all one had to do was enter their credit card details online and click `Buy`.

This brief period of fair distribution lasted mostly during the dawn of the Internet. What happened later and is still happening today is that the ticket scalping returned at a much grander scale—something that the pre-Internet speculators could only dream of.

Today ticket scalping is run through [automated scalping bots](https://en.wikipedia.org/wiki/Ticket_resale#Automated_scalping_bots), there are even [claims of insider scalping](https://www.rollingstone.com/music/music-news/ticketmaster-cheating-scalpers-726353/), and some well-known media like Pitchfork just putting it bluntly that [scalping won't go away](https://pitchfork.com/thepitch/why-ticket-scalping-wont-go-away/).

What this tutorial offers is a very basic framework on the [Corda blockchain platform](https://docs.dev.chainstack.com/blockchains/corda) to give an idea and a steer in the direction of how the scalping problem can be solved.

## Overview

This tutorial will guide you through building and deploying a [CorDapp](https://docs.dev.chainstack.com/blockchains/corda#cordapp) that immutably registers ticket distribution with ticket distributors.

The ticket registration is done by running noScalpDapp on [Corda nodes](https://docs.dev.chainstack.com/blockchains/corda#node).

What noScalpDapp does is it lets the nodes running it send each other mutually signed transactions with the event name and the number of tickets distributed. All verified by a [notary](https://docs.dev.chainstack.com/blockchains/corda#notary-service).

In this framework, one node equals one ticket distributor. A distributor can be anything you want and geographically located wherever. Since one distributor equals one node, it can be a node that only you own or it can be a community-owned node. The idea is that the initial ticket distributor—as there is always one originating source—must provably distribute the tickets to a number of nodes; these nodes can, in turn, redistribute the tickets to other nodes.

You can interact with noScalpDapp via the Corda shell that starts when you run `runnodes` or via a webserver client that you can build separately.

## Prerequisites

1. Clone this repository to your machine.
1. Set up your CorDapp development environment. See [Corda docs: Set-up instructions](https://docs.corda.net/getting-set-up.html#set-up-instructions).

Note that Corda requires at least Java 8u171 and does not support Java 9 or higher.

If you are on Ubuntu, the easiest way to install Java 8 is:

``` sh
sudo apt install openjdk-8-jdk
```

The link provided for Java 8 installation in the Corda documentation is outdated and no longer available.

## noScalpDapp

Each CorDapp has the following components:

* [Flow](https://docs.dev.chainstack.com/blockchains/corda#flows)
* [Contract](https://docs.dev.chainstack.com/blockchains/corda#contracts)
* [State](https://docs.dev.chainstack.com/blockchains/corda#states)

noScalpDapp is no exception and has the components written in Kotlin:

* [noScalpFlow.kt](https://github.com/akegaviar/no-ticket-scalping-cordapp/blob/master/workflows/src/main/kotlin/com/noScalpDapp/noScalpFlow.kt) — the CorDapp flow that starts sessions between the nodes and builds and verifies the ticket distribution transactions.
* [noScalpContract.kt](https://github.com/akegaviar/no-ticket-scalping-cordapp/blob/master/contracts/src/main/kotlin/com/noScalpDapp/noScalpContract.kt) — the CorDapp contract for the ticket distribution transaction.
* [noScalpState.kt](https://github.com/akegaviar/no-ticket-scalping-cordapp/blob/master/contracts/src/main/kotlin/com/noScalpDapp/noScalpState.kt) — the CorDapp state that creates an on-ledger fact that can be retrieved by the nodes participating in the transaction.

The code in `noScalpFlow`, `noScalpContract`, and `noScalpState` has comments explaining the what and how, so do check them.

## Build and deploy noScalpDapp

### Build to run with Chainstack

The `build.gradle` script in root comes with the `jar` instructions to build the CorDapp:

``` sh
./gradlew jar
```

This will build the CorDapp and place the JAR files in:

* Contract: `/contracts/build/libs/`
* Workflow: `/workflows/build/libs`

You can now load the two JAR files one by one on Chainstack nodes.

### Build to run with locally deployed nodes

The `build.gradle` script in root comes with the `deployNodes` instructions to deploy a network with the following nodes:

* A notary node located in Sydney, Australia.
* DistributorA node located in New York, USA.
* DistributorB located in Tokyo, Japan.
* DistributorC located in Singapore, Singapore.

All nodes are loaded with noScalpDapp.

You can deploy the network with however many nodes you want. To do this, add new node details under the `deployNodes` task in the `build.gradle` script.

To build the nodes with noScalpDapp, run:

``` sh
./gradlew deployNodes
```

To run the network, navigate to `/build/nodes` and run:

* Unix: `runnodes`
* Windows: `runnodes.bat`

Once the nodes start, you will be able to interact with the CorDapp on each of them through Corda shell.

To check if noScalpDapp has loaded successfully, run:

``` sh
flow list
```

The output should be:

``` sh
com.noScalpDapp.noScalpFlow
```

## Interact with noScalpDapp through shell

To do a ticket distribution, run:

``` sh
start noScalpFlow eventName: "NAME", ticketQuantity: QUANTITY, toDistributor: "DISTRIBUTOR_DETAILS"
```

where

* NAME — any event name that you are distributing the tickets to.
* QUANTITY — the number of tickets you are distributing.
* DISTRIBUTOR_DETAILS — the distributor name and location that you are distributing the tickets to.

The following example distributes 6000 tickets to the TOOL band show in Singapore to DistributorC in Singapore:

``` sh
start noScalpFlow eventName: "TOOL Singapore show", ticketQuantity: 6000, toDistributor: "O=DistributorC,L=Singapore,C=SG"
```

You can now check the registered transaction on the node where you ran the transaction and on the node that received the transaction.

To check the transaction:

``` sh
run vaultQuery contractStateType: com.noScalpDapp.noScalpState
```

This will print the registered transaction details.

## Build and run the noScalpDapp webserver and client

Once you have your Corda network with noScalpDapp running, you can start the webserver to interact with the nodes.

The webserver is a Spring Boot implementation.

The webserver is in `clients` and has the following components:

* Backend:
  * [MainController.kt](https://github.com/akegaviar/no-ticket-scalping-cordapp/blob/master/clients/src/main/kotlin/com/noScalpDapp/server/MainController.kt) — the main component that does POST and GET mappings and calls [noScalpState.kt](https://github.com/akegaviar/no-ticket-scalping-cordapp/blob/master/noScalpDapp/src/main/kotlin/com/noScalpDapp/noScalpState.kt) for outputs.
  * [NodeRPCConnection.kt](https://github.com/akegaviar/no-ticket-scalping-cordapp/blob/master/clients/src/main/kotlin/com/noScalpDapp/server/NodeRPCConnection.kt) — the standard Corda RPC wrapper that uses the [CordaRPCClient](https://docs.corda.net/api/javadoc/net/corda/client/rpc/CordaRPCClient.html) and [CordaRPCConnection](https://docs.corda.net/api/javadoc/net/corda/client/rpc/CordaRPCConnection.html) classes.
  * [Server.kt](https://github.com/akegaviar/no-ticket-scalping-cordapp/blob/master/clients/src/main/kotlin/com/noScalpDapp/server/Server.kt) — a Spring Boot application with [JacksonSupport](https://docs.corda.net/api/kotlin/corda/net.corda.client.jackson/-jackson-support/index.html).

* Frontend:
  * [index.html](https://github.com/akegaviar/no-ticket-scalping-cordapp/blob/master/clients/src/main/resources/public/index.html) — calls [angular-module.js](https://github.com/akegaviar/no-ticket-scalping-cordapp/blob/master/clients/src/main/resources/public/js/angular-module.js).
  * [angular-module.js](https://github.com/akegaviar/no-ticket-scalping-cordapp/blob/master/clients/src/main/resources/public/js/angular-module.js) — calls [MainController.kt](https://github.com/akegaviar/no-ticket-scalping-cordapp/blob/master/clients/src/main/kotlin/com/noScalpDapp/server/MainController.kt) mappings.

### Configure the webserver

By default, the webserver is configured to run and connect to the Corda nodes deployed as specified in `build.gradle` under `task deployNodes`.

If you deploy the nodes with changed parameters or add new nodes, you can configure the webserver with the new connection details.

The webserver connection details are in `clients/build.gradle` under `task runDistributorX`:

`args '--server.port=SERVER_PORT', '--config.rpc.host=CORDA_RPC_HOST', '--config.rpc.port=CORDA_RPC_PORT', '--config.rpc.username=CORDA_RPC_USER', '--config.rpc.password=CORDA_RPC_PASSWORD'`

where

* SERVER_PORT — your Spring Boot server instance port.
* CORDA_RPC_HOST — hostname or IP address of your Corda node.
* CORDA_RPC_PORT — the RPC port of your Corda node.
* CORDA_RPC_USER — the RPC username of your Corda node.
* CORDA_RPC — the RPC password of your Corda node.

Example:

``` gradle
task runDistributorA(type: JavaExec, dependsOn: jar) {
    classpath = sourceSets.main.runtimeClasspath
    main = 'com.noScalpDapp.server.ServerKt'
    args '--server.port=50005', '--config.rpc.host=localhost', '--config.rpc.port=10006', '--config.rpc.username=user1', '--config.rpc.password=test'
}
```

This will start the webserver at `localhost:50005` and connect it to node DistributorA.

### Build and run the webserver

In the project root, run:

``` sh
./gradlew runDistributorA
```

### Interact with the node through webserver

Interact via modals or via API endpoints.

The GET requests are defined via `@GetMapping` in [MainController.kt](https://raw.githubusercontent.com/akegaviar/no-ticket-scalping-cordapp/master/clients/src/main/kotlin/com/noScalpDapp/server/MainController.kt).

The POST requests are defined via `@PostMapping` in [MainController.kt](https://raw.githubusercontent.com/akegaviar/no-ticket-scalping-cordapp/master/clients/src/main/kotlin/com/noScalpDapp/server/MainController.kt).

A GET request example to see the node identity:

``` sh
$ curl url http://localhots:50005/api/noScalpDapp/me
{
  "me" : "O=DistributorA, L=New York, C=US"
}
```

## Known issues

The transaction details output shows the confusing `totalStatesAvailable: -1`. This is a [known Corda issue](https://r3-cev.atlassian.net/browse/CORDA-2601).

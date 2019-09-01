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

## Pre-requisites

1. Clone this repository to your machine.
1. Set up your CorDapp development environment. See [Corda docs: Set-up instructions](https://docs.corda.net/getting-set-up.html#set-up-instructions).

## noScalpDapp

Each CorDapp has the following components:

* [Flow](https://docs.dev.chainstack.com/blockchains/corda#flows)
* [Contract](https://docs.dev.chainstack.com/blockchains/corda#contracts)
* [State](https://docs.dev.chainstack.com/blockchains/corda#states)

noScalpDapp is no exception and has the components written in Kotlin:

* noScalpFlow.kt — the CorDapp flow that starts sessions between the nodes and builds and verifies the ticket distribution transactions.
* noScalpContract.kt — the CorDapp contract for the ticket distribution transaction.
* noScalpState.kt — the CorDapp state that creates an on-ledger fact that can be retrieved by the nodes participating in the transaction.

The code in `noScalpFlow`, `noScalpContract`, and `noScalpState` has comments explaining the what and how, so do check them.

## Build and deploy noScalpDapp

By default, the `build.gradle` script in root comes with the `deployNodes` instructions to deploy a network with the following nodes:

* A [network map](https://docs.dev.chainstack.com/blockchains/corda#network-map-service) and notary node located in Sydney, Australia.
* DistributorA node located in New Yor, USA.
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

## Interact with noScalpDapp

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

## Known issues

The transaction details output shows the confusing `totalStatesAvailable: -1`. This is a [known Corda issue](https://r3-cev.atlassian.net/browse/CORDA-2601).
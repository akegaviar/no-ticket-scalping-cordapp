package com.noScalpDapp

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.SignedTransaction
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker


// *********
// * Flows *
// *********
/**
 * The NoScalpDapp flow to register ticket distribution with Distributor Parties
 */
@InitiatedBy(noScalpFlow::class)
class noScalpFlowResponder(val otherPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signTransactionFlow = object : SignTransactionFlow(otherPartySession, SignTransactionFlow.tracker()) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be a ticket distribution transaction." using (output is noScalpState)
                val noScalp = output as noScalpState
                "The event name must not be blank." using (noScalp.event.isNotBlank())
            }
        }

        subFlow(signTransactionFlow)
    }
}

@InitiatingFlow
@StartableByRPC
// Constructor parameters:
// - ticketQuantity is the number of tickets distributed in the transaction
// - toDistributor is the verified distributor that gets the tickets in the transaction;
// the distributor cannot distribute the tickets to themselves
class noScalpFlow(private val ticketQuantity: Int,
                  private val eventName: String,
                      private val toDistributor: Party) : FlowLogic<Unit>() {

    /** Just a progress tracker to show the flow progress */
    override val progressTracker = ProgressTracker()

    /** Requirements for the transaction */
    @Suspendable
    override fun call() {
        // A notary and a network map are required.
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        // Create a transaction builder.
        val txBuilder = TransactionBuilder(notary = notary)

        // Create transaction components.
        val outputState = noScalpState(ticketQuantity, eventName, ourIdentity, toDistributor)
        val outputContract = StateAndContract(outputState, NOSCALP_CONTRACT_ID)
        val cmd = Command(noScalpContract.Create(), listOf(ourIdentity.owningKey, toDistributor.owningKey))

        // Add the items to the builder.
        txBuilder.withItems(outputContract, cmd)

        // Verify the transaction.
        txBuilder.verify(serviceHub)

        // Sign the transaction.
        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        // Create a session with the toDistributor.
        val otherpartySession = initiateFlow(toDistributor)

        // Obtain the toDistributor's signature.
        val fullySignedTx = subFlow(CollectSignaturesFlow(signedTx, listOf(otherpartySession), CollectSignaturesFlow.tracker()))

        // Finalize the transaction.
        subFlow(FinalityFlow(fullySignedTx))
    }
}

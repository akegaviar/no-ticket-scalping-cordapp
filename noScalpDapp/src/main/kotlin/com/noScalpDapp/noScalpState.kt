package com.noScalpDapp

import net.corda.core.contracts.*
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction

// *********
// * State *
// *********
// Registers an on-ledger fact that the tickets have been distributed
// from one verified distributor to another.

data class noScalpState(val ticket: Int,
                            val event: String,
                            val fromDistributor: Party,
                            val toDistributor: Party) : ContractState {
    override val participants get() = listOf(fromDistributor, toDistributor)
}
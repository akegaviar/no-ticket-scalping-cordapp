package com.noScalpDapp.server

import com.noScalpDapp.flows.noScalpFlow.Initiator
import com.noScalpDapp.states.noScalpState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

val SERVICE_NAMES = listOf("Notary", "Network Map Service")

/**
 *  A Spring Boot Server API controller for interacting with the node via RPC.
 */

@RestController
@RequestMapping("/api/noScalpDapp/") // The paths for GET and POST requests are relative to this base path.
class MainController(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val myLegalName = rpc.proxy.nodeInfo().legalIdentities.first().name
    private val proxy = rpc.proxy

    /**
     * Returns the node's name.
     */
    @GetMapping(value = [ "me" ], produces = [ APPLICATION_JSON_VALUE ])
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the network map service. These names can be used to look up identities using
     * the identity service.
     */
    @GetMapping(value = [ "peers" ], produces = [ APPLICATION_JSON_VALUE ])
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = proxy.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }

    /**
     * Displays all distribution states that exist in the node's vault.
     */
    @GetMapping(value = [ "distributions" ], produces = [ APPLICATION_JSON_VALUE ])
    fun getDISTRIBUTIONs() : ResponseEntity<List<StateAndRef<noScalpState>>> {
        return ResponseEntity.ok(proxy.vaultQueryBy<noScalpState>().states)
    }

    /**
     * Initiates a flow to agree a distribution between two distributors.
     *
     * Once the flow finishes it will have written the distribution to ledger. Both distributors will be able to
     * see it when calling /spring/api/distributions on their respective nodes.
     *
     * This end-point takes a distributor name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */

    @PostMapping(value = [ "create-distribution" ], produces = [ TEXT_PLAIN_VALUE ], headers = [ "Content-Type=application/x-www-form-urlencoded" ])
    fun createDISTRIBUTION(request: HttpServletRequest): ResponseEntity<String> {
        val eventName = request.getParameter("eventName").toString()
        val ticketQuantity = request.getParameter("ticketQuantity").toInt()
        val distributorName = request.getParameter("distributorName")
        if(distributorName == null){
            return ResponseEntity.badRequest().body("Query parameter 'distributorName' must not be null.\n")
        }
        if (ticketQuantity <= 0 ) {
            return ResponseEntity.badRequest().body("Query parameter 'ticketQuantity' must be non-negative.\n")
        }
        if (eventName.isBlank()) {
            return ResponseEntity.badRequest().body("Query parameter 'eventName' must not be blank.\n")
        }
        val partyX500Name = CordaX500Name.parse(distributorName)
        val otherParty = proxy.wellKnownPartyFromX500Name(partyX500Name) ?: return ResponseEntity.badRequest().body("Distributor named $distributorName cannot be found.\n")

        return try {
            val signedTx = proxy.startTrackedFlow(::Initiator, ticketQuantity, eventName, otherParty).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("Distribution id ${signedTx.id} committed to ledger.\n")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }
}

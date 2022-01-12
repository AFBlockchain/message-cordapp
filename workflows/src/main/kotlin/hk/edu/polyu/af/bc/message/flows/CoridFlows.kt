package hk.edu.polyu.af.bc.message.flows

import co.paralleluniverse.fibers.Suspendable
import hk.edu.polyu.af.bc.account.flows.mapping.toAnonymousParty
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.transactions.SignedTransaction

@StartableByService
@StartableByRPC
class SendMessageUser(private val from: String,
                      private val to: String,
                      private val message: String): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val fromParty = toAnonymousParty(from)
        val toParty = toAnonymousParty(to)

        return subFlow(SendMessage(fromParty, toParty, message))
    }
}
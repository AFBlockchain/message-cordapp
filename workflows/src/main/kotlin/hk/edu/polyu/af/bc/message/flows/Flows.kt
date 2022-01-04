package hk.edu.polyu.af.bc.message.flows

import co.paralleluniverse.fibers.Suspendable
import hk.edu.polyu.af.bc.message.contracts.MessageContract
import hk.edu.polyu.af.bc.message.states.MessageState
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker


@InitiatingFlow
@StartableByRPC
class SendMessage(private val sender: AbstractParty,
                  private val receiver: AbstractParty,
                  private val msg: String) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        val output = MessageState(msg, sender, receiver)
        val builder = TransactionBuilder(notary)
                .addCommand(MessageContract.Commands.Create(), listOf(sender.owningKey, receiver.owningKey))
                .addOutputState(output)

        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder, sender.owningKey) // sign transaction with sender's key, assuming we have it

        // map the receiver to a well-known identity (i.e., the account's host)
        val receiverHost = wellKnowIdentity(receiver)
        val sessions = listOf(initiateFlow(receiverHost))
        val stx = subFlow(CollectSignaturesFlow(ptx, sessions, myOptionalKeys = listOf(sender.owningKey)))  // let the counter-party recognize us

        return subFlow(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(SendMessage::class)
class ReceiveMessage(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
               //Addition checks
            }
        }
        val txId = subFlow(signTransactionFlow).id
        return subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))
    }
}


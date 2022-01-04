package hk.edu.polyu.af.bc.message.contracts

import hk.edu.polyu.af.bc.message.states.MessageState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class MessageContract : Contract {
    companion object {
        val ID: String = MessageContract::class.java.canonicalName
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands.Create>()
        val messageState = tx.outputsOfType<MessageState>()[0]

        requireThat {
            "Sender needs to sign the transaction" using command.signers.contains(messageState.sender.owningKey)
            "Receiver needs to sign the transaction" using command.signers.contains(messageState.receiver.owningKey)
        }
    }

    interface Commands : CommandData {
        class Create : Commands
    }
}
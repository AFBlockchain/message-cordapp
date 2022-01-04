package hk.edu.polyu.af.bc.message.states

import hk.edu.polyu.af.bc.message.contracts.MessageContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty

@BelongsToContract(MessageContract::class)
data class MessageState(val msg: String,
                        val sender: AbstractParty,
                        val receiver: AbstractParty,
                        override val participants: List<AbstractParty> = listOf(sender,receiver)
) : ContractState

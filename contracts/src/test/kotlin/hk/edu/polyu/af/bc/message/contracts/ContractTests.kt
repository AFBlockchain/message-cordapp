package hk.edu.polyu.af.bc.message.contracts

import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import hk.edu.polyu.af.bc.message.states.MessageState

class ContractTests {
    private val ledgerServices: MockServices = MockServices(listOf("hk.edu.polyu.af.bc.message"))
    var alice = TestIdentity(CordaX500Name("Alice", "TestLand", "US"))
    var bob = TestIdentity(CordaX500Name("Bob", "TestLand", "US"))

    @Test
    fun test() {
        val state = MessageState("Hello-World", alice.party, bob.party)
        ledgerServices.ledger {
            transaction {
                output(MessageContract.ID, state)
                tweak {
                    command(alice.publicKey, MessageContract.Commands.Create())
                    fails()  // needs bob's signature
                }

                command(listOf(alice.publicKey, bob.publicKey), MessageContract.Commands.Create())
                verifies()
            }
        }
    }
}
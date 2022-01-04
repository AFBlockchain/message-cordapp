package hk.edu.polyu.af.bc.message

import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfo
import hk.edu.polyu.af.bc.message.flows.SendMessage
import hk.edu.polyu.af.bc.message.states.MessageState
import net.corda.core.identity.AnonymousParty
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class FlowTests {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(FlowTests::class.java)
    }

    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode

    private val parties: MutableMap<String, AnonymousParty> = mutableMapOf()

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("hk.edu.polyu.af.bc.message.contracts"),
                TestCordapp.findCordapp("com.r3.corda.lib.accounts.workflows.flows"),
                TestCordapp.findCordapp("com.r3.corda.lib.accounts.contracts"),
                TestCordapp.findCordapp("hk.edu.polyu.af.bc.message.flows")
        )))
        a = network.createPartyNode()
        b = network.createPartyNode()
        network.runNetwork()

        // make accounts and identities
        parties["a1"] = network.createAndShareAccount(a, b, "a1")
        parties["a2"] = network.createAndShareAccount(a, b, "a2")
        parties["b1"] = network.createAndShareAccount(b, a, "b1")
        parties["b2"] = network.createAndShareAccount(b, a, "b2")
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `send a message from a1 to b1`() {
        val flow = SendMessage(parties["a1"]!!, parties["b1"]!!, "Hello")
        val messageState = a.startFlow(flow).getOrThrow(network).output(MessageState::class.java)

        a.assertHaveState(messageState, messageStateComparator)
        b.assertHaveState(messageState, messageStateComparator)
    }

    private val messageStateComparator = {
        s1: MessageState, s2: MessageState -> s1.msg == s2.msg
    }
}
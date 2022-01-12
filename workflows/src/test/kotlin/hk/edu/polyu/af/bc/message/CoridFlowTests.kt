package hk.edu.polyu.af.bc.message

import hk.edu.polyu.af.bc.account.flows.plane.CreateNetworkIdentityPlane
import hk.edu.polyu.af.bc.account.flows.plane.SetCurrentNetworkIdentityPlaneByName
import hk.edu.polyu.af.bc.account.flows.user.CreateUser
import hk.edu.polyu.af.bc.account.flows.user.GetUserStates
import hk.edu.polyu.af.bc.message.flows.SendMessageUser
import hk.edu.polyu.af.bc.message.states.MessageState
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CoridFlowTests {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(CoridFlowTests::class.java)
    }

    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(
            MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("hk.edu.polyu.af.bc.message.contracts"),
                TestCordapp.findCordapp("hk.edu.polyu.af.bc.message.flows"),
                TestCordapp.findCordapp("hk.edu.polyu.af.bc.account.contracts"),
                TestCordapp.findCordapp("hk.edu.polyu.af.bc.account.flows"),
                TestCordapp.findCordapp("com.r3.corda.lib.accounts.workflows.flows"),
                TestCordapp.findCordapp("com.r3.corda.lib.accounts.contracts")
            )))

        a = network.createPartyNode()
        b = network.createPartyNode()
        network.runNetwork()

        // create and set planes
        a.startFlow(CreateNetworkIdentityPlane("message-plane", listOf(b.info.legalIdentities[0]))).getOrThrow(network)
        a.startFlow(SetCurrentNetworkIdentityPlaneByName("message-plane")).getOrThrow(network)
        b.startFlow(SetCurrentNetworkIdentityPlaneByName("message-plane")).getOrThrow(network)

        // create users
        a.startFlow(CreateUser("alice")).getOrThrow(network)
        a.startFlow(CreateUser("alice2")).getOrThrow(network)
        b.startFlow(CreateUser("bob")).getOrThrow(network)
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `send a message from alice to bob`() {
        a.startFlow(SendMessageUser("alice", "bob", "Hello bob")).getOrThrow(network)

        assert(a.startFlow(GetUserStates("alice", MessageState::class.java))
            .getOrThrow(network).any { it.state.data.msg == "Hello bob" })
        assert(b.startFlow(GetUserStates("bob", MessageState::class.java))
            .getOrThrow(network).any { it.state.data.msg == "Hello bob" })
    }

    @Test
    fun `send a message from alice to alice2`() {
        a.startFlow(SendMessageUser("alice", "alice2", "Hello alice2")).getOrThrow(network)

        assert(a.startFlow(GetUserStates("alice", MessageState::class.java))
            .getOrThrow(network).any { it.state.data.msg == "Hello alice2" })
        assert(a.startFlow(GetUserStates("alice2", MessageState::class.java))
            .getOrThrow(network).any { it.state.data.msg == "Hello alice2" })
    }

    private val messageStateComparator = {
            s1: MessageState, s2: MessageState -> s1.msg == s2.msg
    }
}
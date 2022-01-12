import com.github.manosbatsis.corda.testacles.nodedriver.NodeHandles
import com.github.manosbatsis.corda.testacles.nodedriver.config.NodeDriverNodesConfig
import com.github.manosbatsis.corda.testacles.nodedriver.jupiter.NodeDriverExtensionConfig
import com.github.manosbatsis.corda.testacles.nodedriver.jupiter.NodeDriverNetworkExtension
import hk.edu.polyu.af.bc.account.flows.plane.CreateNetworkIdentityPlane
import hk.edu.polyu.af.bc.account.flows.plane.GetCurrentNetworkIdentityPlane
import hk.edu.polyu.af.bc.account.flows.plane.SetCurrentNetworkIdentityPlaneByName
import hk.edu.polyu.af.bc.account.flows.user.CreateUser
import hk.edu.polyu.af.bc.account.flows.user.GetUserStates
import hk.edu.polyu.af.bc.account.flows.user.IsUserExists
import hk.edu.polyu.af.bc.message.*
import hk.edu.polyu.af.bc.message.flows.SendMessageUser
import hk.edu.polyu.af.bc.message.states.MessageState
import net.corda.core.messaging.CordaRPCOps
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(NodeDriverNetworkExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DriverBasedTest {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(DriverBasedTest::class.java)

        @NodeDriverExtensionConfig
        @JvmStatic
        val nodeDriverConfig: NodeDriverNodesConfig = customNodeDriverConfig
    }

    lateinit var proxyA: CordaRPCOps
    lateinit var proxyB: CordaRPCOps

    @Test
    @Order(1)
    fun setUp(nodeHandles: NodeHandles) {
        proxyA = nodeHandles.getNode("partyA").rpc
        proxyB = nodeHandles.getNode("partyB").rpc
    }

    @Test
    @Order(2)
    fun createAndSetPlanes() {
        proxyA.startFlowDynamic(CreateNetworkIdentityPlane::class.java,"message-plane", listOf(proxyB.party())).returnValue.get()

        proxyA.startFlowDynamic(SetCurrentNetworkIdentityPlaneByName::class.java, "message-plane")
        proxyB.startFlowDynamic(SetCurrentNetworkIdentityPlaneByName::class.java, "message-plane")

        assert(proxyA.startFlowDynamic(GetCurrentNetworkIdentityPlane::class.java).returnValue.get()!!.name == "message-plane")
        assert(proxyB.startFlowDynamic(GetCurrentNetworkIdentityPlane::class.java).returnValue.get()!!.name == "message-plane")
    }

    @Test
    @Order(3)
    fun createUsers() {
        proxyA.startFlowDynamic(CreateUser::class.java, "alice1").returnValue.get()
        proxyA.startFlowDynamic(CreateUser::class.java, "alice2").returnValue.get()
        proxyB.startFlowDynamic(CreateUser::class.java, "bob1").returnValue.get()
        proxyB.startFlowDynamic(CreateUser::class.java, "bob2").returnValue.get()

        listOf(proxyA, proxyB).forEach {
            assertTrue(it.startFlowDynamic(IsUserExists::class.java, "alice1").returnValue.get())
            assertTrue(it.startFlowDynamic(IsUserExists::class.java, "alice2").returnValue.get())
            assertTrue(it.startFlowDynamic(IsUserExists::class.java, "bob1").returnValue.get())
            assertTrue(it.startFlowDynamic(IsUserExists::class.java, "bob2").returnValue.get())

            assertFalse(it.startFlowDynamic(IsUserExists::class.java, "no-such-user").returnValue.get())
        }
    }

    @Test
    @Order(4)
    fun sendMessageOnSameNode() {
        val tx = proxyA.startFlowDynamic(SendMessageUser::class.java, "alice1", "alice2", "message1").returnValue.get()
        val messageState = tx.output(MessageState::class.java)

        proxyA.assertHaveState(messageState, messageComparator)
    }

    @Test
    @Order(4)
    fun sendMessageOnDifferentNodes() {
        val tx = proxyA.startFlowDynamic(SendMessageUser::class.java, "alice1" ,"bob1", "message2").returnValue.get()
        val messageState = tx.output(MessageState::class.java)

        proxyA.assertHaveState(messageState, messageComparator)
        proxyB.assertHaveState(messageState, messageComparator)
    }

    @Test
    @Order(5)
    fun userVaultQueries() {
        // "alice1" should have message1 & message2
        // "alice2" should have message1
        // "bob1" should have message2

        proxyA.startFlowDynamic(GetUserStates::class.java, "alice1", MessageState::class.java).returnValue.get().map { it.state.data as MessageState }.any { it.msg == "message1" }
        proxyA.startFlowDynamic(GetUserStates::class.java, "alice1", MessageState::class.java).returnValue.get().map { it.state.data as MessageState }.any { it.msg == "message2" }
        proxyA.startFlowDynamic(GetUserStates::class.java, "alice2", MessageState::class.java).returnValue.get().map { it.state.data as MessageState }.any { it.msg == "message1" }
        proxyB.startFlowDynamic(GetUserStates::class.java, "bob1", MessageState::class.java).returnValue.get().map { it.state.data as MessageState }.any { it.msg == "message2" }
    }
}

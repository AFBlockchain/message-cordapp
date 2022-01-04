package hk.edu.polyu.af.bc.message.states

import org.junit.Test
import kotlin.test.assertEquals

class StateTests {
    @Test
    fun hasFieldOfCorrectType() {
        // Does the field exist?
        MessageState::class.java.getDeclaredField("msg")
        // Is the field of the correct type?
        assertEquals(MessageState::class.java.getDeclaredField("msg").type, String()::class.java)
    }
}
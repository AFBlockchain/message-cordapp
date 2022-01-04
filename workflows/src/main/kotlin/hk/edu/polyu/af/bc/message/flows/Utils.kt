package hk.edu.polyu.af.bc.message.flows

import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party

/**
 * Resolve an [AbstractParty] to [Party]. If it is a [Party], return immediately. Else, the party is anonymous. Exception
 * is thrown when the given [AnonymousParty] cannot be identified. This step is typically carried out before initiating
 * a flow session with the resolved [Party].
 */
fun FlowLogic<*>.wellKnowIdentity(abstractParty: AbstractParty): Party {
    if (abstractParty is Party) return abstractParty

    val anonymousParty: AnonymousParty = abstractParty as AnonymousParty
    return serviceHub.identityService.wellKnownPartyFromAnonymous(anonymousParty) ?: throw FlowException("Cannot identify anonymousParty: $anonymousParty")
}

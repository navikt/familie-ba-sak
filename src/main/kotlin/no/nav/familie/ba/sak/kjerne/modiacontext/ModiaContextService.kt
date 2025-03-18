package no.nav.familie.ba.sak.kjerne.modiacontext

import no.nav.familie.ba.sak.ekstern.restDomene.RestNyAktivBrukerIModiaContext
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import org.springframework.stereotype.Service

@Service
class ModiaContextService(
    private val integrasjonClient: IntegrasjonClient,
) {
    fun settNyAktivBruker(nyAktivBrukerDto: RestNyAktivBrukerIModiaContext): ModiaContext = integrasjonClient.settNyAktivBrukerIModiaContext(nyAktivBrukerDto)

    fun hentContext(): ModiaContext = integrasjonClient.hentModiaContext()
}

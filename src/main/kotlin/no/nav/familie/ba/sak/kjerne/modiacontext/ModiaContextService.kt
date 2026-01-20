package no.nav.familie.ba.sak.kjerne.modiacontext

import no.nav.familie.ba.sak.ekstern.restDomene.NyAktivBrukerIModiaContextDto
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import org.springframework.stereotype.Service

@Service
class ModiaContextService(
    private val integrasjonKlient: IntegrasjonKlient,
) {
    fun settNyAktivBruker(nyAktivBruker: NyAktivBrukerIModiaContextDto): ModiaContext = integrasjonKlient.settNyAktivBrukerIModiaContext(nyAktivBruker)

    fun hentContext(): ModiaContext = integrasjonKlient.hentModiaContext()
}

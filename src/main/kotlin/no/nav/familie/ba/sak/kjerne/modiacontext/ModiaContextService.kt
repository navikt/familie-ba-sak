package no.nav.familie.ba.sak.kjerne.modiacontext

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import org.springframework.stereotype.Service

@Service
class ModiaContextService(
    private val integrasjonClient: IntegrasjonClient,
) {
    fun settNyAktivBruker(nyAktivBrukerDto: ModiaContextNyAktivBrukerDto): ModiaContext = integrasjonClient.settNyAktivBruker(nyAktivBrukerDto)

    fun hentContext(): ModiaContext = integrasjonClient.hentModiaContext()
}

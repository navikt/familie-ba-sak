package no.nav.familie.ba.sak.kjerne.søknad

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.behandling.søknadreferanse.SøknadReferanseService
import org.springframework.stereotype.Service

@Service
class SøknadService(
    val søknadReferanseService: SøknadReferanseService,
    val integrasjonClient: IntegrasjonClient,
    val søknadMapperLookup: SøknadMapper.Lookup,
) {
    fun hentSøknad(behandlingId: Long): Søknad? {
        val søknadReferanse = søknadReferanseService.hentSøknadReferanse(behandlingId) ?: return null
        val versjonertBarnetrygdSøknad = integrasjonClient.hentVersjonertBarnetrygdSøknad(søknadReferanse.journalpostId)
        val søknadMapper = søknadMapperLookup.hentMapperForSøknadVersjon(versjonertBarnetrygdSøknad.barnetrygdSøknad.kontraktVersjon)
        return søknadMapper.mapTilSøknad(versjonertBarnetrygdSøknad)
    }
}

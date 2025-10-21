package no.nav.familie.ba.sak.kjerne.søknad

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import org.springframework.stereotype.Service

@Service
class SøknadService(
    val behandlingSøknadsinfoService: BehandlingSøknadsinfoService,
    val integrasjonKlient: IntegrasjonKlient,
    val søknadMapperLookup: SøknadMapper.Lookup,
) {
    fun finnSøknad(behandlingId: Long): Søknad? {
        val journalpostId = behandlingSøknadsinfoService.hentJournalpostId(behandlingId) ?: return null
        val versjonertBarnetrygdSøknad = integrasjonKlient.hentVersjonertBarnetrygdSøknad(journalpostId)
        val søknadMapper = søknadMapperLookup.hentSøknadMapperForVersjon(versjonertBarnetrygdSøknad.barnetrygdSøknad.kontraktVersjon)
        return søknadMapper.mapTilSøknad(versjonertBarnetrygdSøknad)
    }
}

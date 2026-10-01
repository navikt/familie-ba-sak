package no.nav.familie.ba.sak.kjerne.barnetrygdhistorikk

import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import org.springframework.stereotype.Service

@Service
class BarnetrygdHistorikkService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val fagsakService: FagsakService,
    private val infotrygdService: InfotrygdService,
) {
    fun harSøkerHattInnvilgetBarnetrygd(fagsakId: Long): Boolean {
        val behandlingIder =
            behandlingHentOgPersisterService
                .hentFerdigstilteBehandlinger(fagsakId)
                .map { it.id }

        if (behandlingIder.isNotEmpty() &&
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(behandlingIder).isNotEmpty()
        ) {
            return true
        }

        val søkerIdent = fagsakService.hentPåFagsakId(fagsakId).aktør.aktivFødselsnummer()
        return infotrygdService.hentInfotrygdstønaderForSøker(ident = søkerIdent, historikk = true).bruker.isNotEmpty()
    }
}

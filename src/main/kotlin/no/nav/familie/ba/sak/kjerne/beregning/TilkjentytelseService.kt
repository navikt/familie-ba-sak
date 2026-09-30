package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import org.springframework.stereotype.Service

@Service
class TilkjentytelseService(
    private val behandlingRepository: BehandlingRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val fagsakService: FagsakService,
    private val infotrygdService: InfotrygdService,
) {
    fun søkerHarHattUtbetaling(fagsakId: Long): Boolean {
        val behandlingIder =
            behandlingRepository
                .finnBehandlinger(fagsakId)
                .filter { !it.erHenlagt() }
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

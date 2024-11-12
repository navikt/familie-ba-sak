package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import org.springframework.stereotype.Component
import java.time.YearMonth

@Component
class EndretMigreringsdatoUtleder(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingService: BehandlingService,
) {
    fun utled(
        fagsak: Fagsak,
        forrigeTilkjentYtelse: TilkjentYtelse?,
    ): YearMonth? {
        val forrigeTilstandFraDato = forrigeTilkjentYtelse?.andelerTilkjentYtelse?.minOfOrNull { it.stønadFom }

        if (forrigeTilstandFraDato == null) {
            return null
        }

        val erMigrertSak =
            behandlingHentOgPersisterService
                .hentBehandlinger(fagsak.id)
                .any { it.type == BehandlingType.MIGRERING_FRA_INFOTRYGD }

        if (!erMigrertSak) {
            return null
        }

        val migreringsdatoPåFagsak = behandlingService.hentMigreringsdatoPåFagsak(fagsakId = fagsak.id)

        if (migreringsdatoPåFagsak == null) {
            return null
        }

        val nyTilstandFraDato = migreringsdatoPåFagsak.toYearMonth().plusMonths(1)

        if (nyTilstandFraDato.isAfter(forrigeTilstandFraDato)) {
            throw IllegalStateException("Ny migreringsdato kan ikke være etter forrige migreringsdato")
        }

        return if (forrigeTilstandFraDato.isAfter(nyTilstandFraDato)) {
            nyTilstandFraDato
        } else {
            null
        }
    }
}

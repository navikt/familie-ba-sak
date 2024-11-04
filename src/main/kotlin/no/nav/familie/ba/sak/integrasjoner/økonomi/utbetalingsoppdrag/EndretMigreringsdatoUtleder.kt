package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import org.springframework.stereotype.Component
import java.time.YearMonth

@Component
class EndretMigreringsdatoUtleder(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingService: BehandlingService,
) {
    fun utled(
        behandling: Behandling,
        forrigeTilstandFraDato: YearMonth?,
    ): YearMonth? {
        if (forrigeTilstandFraDato == null) {
            return null
        }

        val erMigrertSak =
            behandlingHentOgPersisterService
                .hentBehandlinger(behandling.fagsak.id)
                .any { it.type == BehandlingType.MIGRERING_FRA_INFOTRYGD }

        if (!erMigrertSak) {
            return null
        }

        val nyttTilstandFraDato =
            behandlingService
                .hentMigreringsdatoPåFagsak(fagsakId = behandling.fagsak.id)
                ?.toYearMonth()
                ?.plusMonths(1)

        if (nyttTilstandFraDato == null) {
            return null
        }

        if (nyttTilstandFraDato.isAfter(forrigeTilstandFraDato)) {
            throw IllegalStateException("Ny migreringsdato kan ikke være etter forrige migreringsdato")
        }

        return if (forrigeTilstandFraDato.isAfter(nyttTilstandFraDato)) {
            nyttTilstandFraDato
        } else {
            null
        }
    }
}

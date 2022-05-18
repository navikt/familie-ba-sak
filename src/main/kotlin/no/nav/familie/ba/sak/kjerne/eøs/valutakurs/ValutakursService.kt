package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaService
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ValutakursService(
    repository: ValutakursRepository,
    tilbakestillBehandlingService: TilbakestillBehandlingService,
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService
) {
    val serviceDelegate = PeriodeOgBarnSkjemaService(
        repository,
        tilbakestillBehandlingService
    )

    fun hentValutakurser(behandlingId: Long) =
        serviceDelegate.hentMedBehandlingId(behandlingId)

    fun oppdaterValutakurs(behandlingId: Long, valutakurs: Valutakurs) =
        serviceDelegate.endreSkjemaer(behandlingId, valutakurs)

    fun slettValutakurs(valutakursId: Long) =
        serviceDelegate.slettSkjema(valutakursId)

    @Transactional
    fun tilpassUtenlandskPeriodebeløpTilKompetanser(behandlingId: Long) {

        val barnasUtenlandskePeriodebeløpTidslinjer = utenlandskPeriodebeløpService
            .hentUtenlandskePeriodebeløp(behandlingId)
            .tilTidslinjerForBarna()

        serviceDelegate.tilpassBarnasSkjemaerTilTidslinjer(
            behandlingId,
            barnasUtenlandskePeriodebeløpTidslinjer
        ) { aktør -> Valutakurs(null, null, setOf(aktør)) }
    }
}

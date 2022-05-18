package no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaService
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UtenlandskPeriodebeløpService(
    repository: UtenlandskPeriodebeløpRepository,
    tilbakestillBehandlingService: TilbakestillBehandlingService,
    private val kompetanseService: KompetanseService
) {
    val serviceDelegate = PeriodeOgBarnSkjemaService(
        repository,
        tilbakestillBehandlingService
    )

    fun hentUtenlandskePeriodebeløp(behandlingId: Long) =
        serviceDelegate.hentMedBehandlingId(behandlingId)

    fun oppdaterUtenlandskPeriodebeløp(behandlingId: Long, utenlandskPeriodebeløp: UtenlandskPeriodebeløp) =
        serviceDelegate.endreSkjemaer(behandlingId, utenlandskPeriodebeløp)

    fun slettUtenlandskPeriodebeløp(utenlandskPeriodebeløpId: Long) =
        serviceDelegate.slettSkjema(utenlandskPeriodebeløpId)

    @Transactional
    fun kopierOgErstattUtenlandskePeriodebeløp(fraBehandlingId: Long, tilBehandlingId: Long) =
        serviceDelegate.kopierOgErstattSkjemaer(fraBehandlingId, tilBehandlingId)

    @Transactional
    fun tilpassUtenlandskPeriodebeløpTilKompetanser(behandlingId: Long) {
        val barnasKompetanseTidslinjer = kompetanseService.hentKompetanser(behandlingId).tilTidslinjerForBarna()

        serviceDelegate.tilpassBarnasSkjemaerTilTidslinjer(
            behandlingId,
            barnasKompetanseTidslinjer
        ) { aktør -> UtenlandskPeriodebeløp(null, null, setOf(aktør)) }
    }
}

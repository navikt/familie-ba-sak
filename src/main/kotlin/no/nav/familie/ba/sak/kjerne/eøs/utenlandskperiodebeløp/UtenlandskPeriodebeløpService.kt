package no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEndringAbonnent
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class UtenlandskPeriodebeløpService(
    utenlandskPeriodebeløpRepository: PeriodeOgBarnSkjemaRepository<UtenlandskPeriodebeløp>,
    endringsabonnenter: Collection<PeriodeOgBarnSkjemaEndringAbonnent<UtenlandskPeriodebeløp>>,
) {
    val skjemaService =
        PeriodeOgBarnSkjemaService(
            utenlandskPeriodebeløpRepository,
            endringsabonnenter,
        )

    fun hentUtenlandskePeriodebeløp(behandlingId: BehandlingId) = skjemaService.hentMedBehandlingId(behandlingId)

    @Transactional
    fun oppdaterUtenlandskPeriodebeløp(
        behandlingId: BehandlingId,
        utenlandskPeriodebeløp: UtenlandskPeriodebeløp,
    ) {
        validerUtenlandskPeriodeBeløp(utenlandskPeriodebeløp)

        skjemaService.endreSkjemaer(behandlingId, utenlandskPeriodebeløp)
    }

    fun slettUtenlandskPeriodebeløp(
        behandlingId: BehandlingId,
        utenlandskPeriodebeløpId: Long,
    ) = skjemaService.slettSkjema(behandlingId, utenlandskPeriodebeløpId)

    @Transactional
    fun kopierOgErstattUtenlandskPeriodebeløp(
        fraBehandlingId: BehandlingId,
        tilBehandlingId: BehandlingId,
    ) = skjemaService.kopierOgErstattSkjemaer(fraBehandlingId, tilBehandlingId)

    private fun validerUtenlandskPeriodeBeløp(utenlandskPeriodebeløp: UtenlandskPeriodebeløp) {
        val fom = utenlandskPeriodebeløp.fom ?: throw FunksjonellFeil("Fra og med dato på utenlandskperiode beløp må være satt")
        val tom = utenlandskPeriodebeløp.tom ?: TIDENES_ENDE.toYearMonth()
        val januar2026 = YearMonth.of(2026, 1)

        if (utenlandskPeriodebeløp.valutakode == BULGARSK_LEV &&
            ((fom.isSameOrAfter(januar2026)) || (tom.isSameOrAfter(januar2026)))
        ) {
            throw FunksjonellFeil(
                "Bulgarske lev er ikke lenger gyldig valuta fra 01.01.26",
            )
        }
    }

    companion object {
        const val BULGARSK_LEV = "BGN"
    }
}

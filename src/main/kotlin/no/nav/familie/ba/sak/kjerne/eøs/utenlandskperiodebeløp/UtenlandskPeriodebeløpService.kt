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

    // Bulgarsk Lev (BGN) er ikke lenger gyldig valuta fra jan. 2026 da de har byttet til EUR
    // Er det UPB med BGN som løper etter denne tid, patcher vi disse ved å manuelt ved å sette tom til cutoff og oppdatere skjemaer
    // Da får vi en blank UPB som saksbehandler kan oppdatere til riktige verdier
    // Kan fjernes etter at behandlinger med løpende BGN er patchet
    @Transactional
    fun oppdaterBulgarskUtenlandskPeriodebeløpVedBehov(behandlingId: BehandlingId) {
        val nyesteUpb = hentUtenlandskePeriodebeløp(behandlingId).maxByOrNull { upb -> upb.tom ?: TIDENES_ENDE.toYearMonth() }
        if (nyesteUpb == null) return

        if (løperBulgarskLevEtterCutoff(nyesteUpb)) {
            skjemaService.endreSkjemaer(
                behandlingId, nyesteUpb.copy(
                    tom = YearMonth.of(2025, 12),
                )
            )
        }
    }

    fun løperBulgarskLevEtterCutoff(utenlandskPeriodebeløp: UtenlandskPeriodebeløp): Boolean {
        val fom = utenlandskPeriodebeløp.fom ?: throw FunksjonellFeil("Fra og med dato på utenlandskperiode beløp må være satt")
        val tom = utenlandskPeriodebeløp.tom ?: TIDENES_ENDE.toYearMonth()

        return utenlandskPeriodebeløp.valutakode == BULGARSK_LEV &&
                ((fom.isSameOrAfter(BULGARSK_LEV_CUTOFF)) || (tom.isSameOrAfter(BULGARSK_LEV_CUTOFF)))
    }

    private fun validerUtenlandskPeriodeBeløp(utenlandskPeriodebeløp: UtenlandskPeriodebeløp) {
        if (løperBulgarskLevEtterCutoff(utenlandskPeriodebeløp)) {
            throw FunksjonellFeil(
                "Bulgarske lev er ikke lenger gyldig valuta fra 01.01.26",
            )
        }
    }

    companion object {
        const val BULGARSK_LEV = "BGN"
        val BULGARSK_LEV_CUTOFF: YearMonth = YearMonth.of(2026, 1)
    }
}

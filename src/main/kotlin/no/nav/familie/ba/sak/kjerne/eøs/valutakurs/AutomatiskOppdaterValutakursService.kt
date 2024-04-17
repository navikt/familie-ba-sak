package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.common.LocalDateProvider
import no.nav.familie.ba.sak.common.rangeTo
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBService
import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.tilSisteVirkedag
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtfyltUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.tilIUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class AutomatiskOppdaterValutakursService(
    private val valutakursService: ValutakursService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val localDateProvider: LocalDateProvider,
    private val ecbService: ECBService,
    private val utenlandskPeriodebeløpRepository: PeriodeOgBarnSkjemaRepository<UtenlandskPeriodebeløp>,
) {
    @Transactional
    fun oppdaterValutakurserEtterEndringsmånedUtenomEndringIValutakurser(
        behandlingId: BehandlingId,
    ) {
        val endringsmånedUavhengigAvValutakurser = vedtaksperiodeService.finnEndringstidspunktForBehandlingFørValutakurser(behandlingId.id).toYearMonth()

        oppdaterValutakurserEtterEndringsmåned(
            behandlingId = behandlingId,
            utenlandskePeriodebeløp = utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId.id),
            endringsmåned = endringsmånedUavhengigAvValutakurser,
        )
    }

    @Transactional
    fun oppdaterValutakurserEtterEndringsmåned(
        behandlingId: BehandlingId,
        utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>? = null,
    ) = oppdaterValutakurserEtterEndringsmåned(
        behandlingId = behandlingId,
        utenlandskePeriodebeløp = utenlandskePeriodebeløp ?: utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId.id),
        endringsmåned = vedtaksperiodeService.finnEndringstidspunktForBehandling(behandlingId.id).toYearMonth(),
    )

    @Transactional
    private fun oppdaterValutakurserEtterEndringsmåned(
        behandlingId: BehandlingId,
        utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>,
        endringsmåned: YearMonth,
    ) {
        val automatiskGenererteValutakurser =
            utenlandskePeriodebeløp
                .map { it.tilIUtenlandskPeriodebeløp() }
                .filterIsInstance<UtfyltUtenlandskPeriodebeløp>()
                .flatMap { utenlandskPeriodebeløp ->
                    utenlandskPeriodebeløp.tilAutomatiskOppdaterteValutakurserEtter(endringsmåned)
                }

        valutakursService.oppdaterValutakurser(BehandlingId(behandlingId.id), automatiskGenererteValutakurser)
    }

    private fun UtfyltUtenlandskPeriodebeløp.tilAutomatiskOppdaterteValutakurserEtter(
        endringsmåned: YearMonth,
    ): List<Valutakurs> {
        val start = maxOf(endringsmåned, fom)
        val denneMåneden = localDateProvider.now().toYearMonth()
        val slutt = tom ?: denneMåneden

        return start.rangeTo(slutt).map { måned ->
            val sisteVirkedagForrigeMåned = måned.minusMonths(1).tilSisteVirkedag()

            Valutakurs(
                fom = måned,
                tom = if (måned == denneMåneden && tom == null) null else måned,
                barnAktører = barnAktører,
                valutakursdato = sisteVirkedagForrigeMåned,
                valutakode = valutakode,
                kurs = ecbService.hentValutakurs(valutakode, sisteVirkedagForrigeMåned),
                vurderingsform = Vurderingsform.AUTOMATISK,
            )
        }
    }
}

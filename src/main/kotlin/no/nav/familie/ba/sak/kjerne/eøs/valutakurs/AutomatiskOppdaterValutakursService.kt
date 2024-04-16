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
    fun oppdaterValutakurserEtterEndringstidspunktet(
        behandlingId: BehandlingId,
    ) = oppdaterValutakurserEtterEndringstidspunktet(
        behandlingId = behandlingId,
        utenlandskePeriodebeløp = utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId.id),
    )

    @Transactional
    fun oppdaterValutakurserEtterEndringstidspunktet(
        behandlingId: BehandlingId,
        utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>,
    ) {
        val endringsmåned = vedtaksperiodeService.finnEndringstidspunktForBehandling(behandlingId.id).toYearMonth()

        val automatiskGenererteValutakurser =
            utenlandskePeriodebeløp
                .map { it.tilIUtenlandskPeriodebeløp() }
                .filterIsInstance<UtfyltUtenlandskPeriodebeløp>()
                .flatMap { utenlandskPeriodebeløp ->
                    utenlandskPeriodebeløp.tilAutomatiskOppdaterteValutakurserEtter(endringsmåned)
                }

        automatiskGenererteValutakurser.forEach {
            valutakursService.oppdaterValutakurs(BehandlingId(behandlingId.id), it)
        }
    }

    private fun UtfyltUtenlandskPeriodebeløp.tilAutomatiskOppdaterteValutakurserEtter(
        endringsmåned: YearMonth,
    ): List<Valutakurs> {
        val start = maxOf(endringsmåned, fom)
        val slutt = tom ?: localDateProvider.now().toYearMonth()

        return start.rangeTo(slutt).map { måned ->
            val sisteVirkedagForrigeMåned = måned.minusMonths(1).tilSisteVirkedag()

            Valutakurs(
                fom = måned,
                tom = if (måned == slutt) null else måned,
                barnAktører = barnAktører,
                valutakursdato = sisteVirkedagForrigeMåned,
                valutakode = valutakode,
                kurs = ecbService.hentValutakurs(valutakode, sisteVirkedagForrigeMåned),
                vurderingsform = Vurderingsform.AUTOMATISK,
            )
        }
    }
}

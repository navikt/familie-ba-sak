package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.common.LocalDateProvider
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.rangeTo
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBService
import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.tilSisteVirkedag
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.TilpassValutakurserTilUtenlandskePeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtfyltUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.filtrerErUtfylt
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
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
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val tilpassValutakurserTilUtenlandskePeriodebeløpService: TilpassValutakurserTilUtenlandskePeriodebeløpService,
    private val simuleringService: SimuleringService,
) {
    @Transactional
    fun resettValutakurserOgLagValutakurserEtterEndringsmåned(
        behandlingId: BehandlingId,
    ) {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId.id)
        val forrigeBehandlingVedtatt = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)

        // Resetter valutaen til slik den var i forrige behandling
        valutakursService.kopierOgErstattValutakurser(
            fraBehandlingId = BehandlingId(forrigeBehandlingVedtatt!!.id),
            tilBehandlingId = behandlingId,
        )

        // Tilpasser valutaen til potensielle endringer i utenlandske periodebeløp fra denne behandlingen
        tilpassValutakurserTilUtenlandskePeriodebeløpService.tilpassValutakursTilUtenlandskPeriodebeløp(behandlingId)

        val endringsmånedFørValutakurser = vedtaksperiodeService.finnEndringstidspunktForBehandlingFørValutakurser(behandlingId.id).toYearMonth()

        oppdaterValutakurserEtterEndringsmåned(
            behandling = behandling,
            utenlandskePeriodebeløp = utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId.id),
            endringsmåned = endringsmånedFørValutakurser,
        )
    }

    @Transactional
    fun oppdaterValutakurserEtterEndringsmåned(
        behandlingId: BehandlingId,
        utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>? = null,
    ) = oppdaterValutakurserEtterEndringsmåned(
        behandling = behandlingHentOgPersisterService.hent(behandlingId.id),
        utenlandskePeriodebeløp = utenlandskePeriodebeløp ?: utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId.id),
        endringsmåned = vedtaksperiodeService.finnEndringstidspunktForBehandling(behandlingId.id).toYearMonth(),
    )

    @Transactional
    private fun oppdaterValutakurserEtterEndringsmåned(
        behandling: Behandling,
        utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>,
        endringsmåned: YearMonth,
    ) {
        val simuleringMottakere = simuleringService.oppdaterSimuleringPåBehandlingVedBehov(behandlingId = behandling.id)
        val datoSisteManuellePostering = simuleringMottakere.finnDatoSisteManuellePostering() ?: TIDENES_MORGEN
        val månedEtterSisteManuellePostering = datoSisteManuellePostering.toYearMonth().plusMonths(1)

        val automatiskGenererteValutakurser =
            utenlandskePeriodebeløp
                .filtrerErUtfylt()
                .flatMap { utenlandskPeriodebeløp ->
                    utenlandskPeriodebeløp.tilAutomatiskOppdaterteValutakurserEtter(maxOf(månedEtterSisteManuellePostering, endringsmåned))
                }

        valutakursService.oppdaterValutakurser(BehandlingId(behandling.id), automatiskGenererteValutakurser)
    }

    private fun UtfyltUtenlandskPeriodebeløp.tilAutomatiskOppdaterteValutakurserEtter(
        endringsmåned: YearMonth,
    ): List<Valutakurs> {
        val start = maxOf(endringsmåned, fom)
        val denneMåneden = localDateProvider.now().toYearMonth()
        val slutt = tom ?: denneMåneden

        if (endringsmåned.isAfter(slutt)) return emptyList()

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

private fun List<ØkonomiSimuleringMottaker>.finnDatoSisteManuellePostering() =
    this.flatMap { it.økonomiSimuleringPostering }.filter { it.erManuellPostering }.maxOfOrNull { it.tom }

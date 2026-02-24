package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.rangeTo
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBService
import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.tilSisteVirkedag
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.TilpassDifferanseberegningEtterValutakursService
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.TilpassValutakurserTilUtenlandskePeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.filtrerErUtfylt
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.logger
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

val DATO_FOR_PRAKSISENDRING_AUTOMATISK_VALUTAJUSTERING = YearMonth.of(2024, 6)

@Service
class AutomatiskOppdaterValutakursService(
    private val valutakursService: ValutakursService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val clockProvider: ClockProvider,
    private val ecbService: ECBService,
    private val utenlandskPeriodebeløpRepository: PeriodeOgBarnSkjemaRepository<UtenlandskPeriodebeløp>,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val tilpassValutakurserTilUtenlandskePeriodebeløpService: TilpassValutakurserTilUtenlandskePeriodebeløpService,
    private val simuleringService: SimuleringService,
    private val vurderingsstrategiForValutakurserRepository: VurderingsstrategiForValutakurserRepository,
    private val featureToggleService: FeatureToggleService,
    private val tilpassDifferanseberegningEtterValutakursService: TilpassDifferanseberegningEtterValutakursService,
) {
    @Transactional
    fun oppdaterAndelerMedValutakurser(behandlingId: BehandlingId) {
        val valutakurser = valutakursService.hentValutakurser(behandlingId)
        tilpassDifferanseberegningEtterValutakursService.skjemaerEndret(behandlingId, valutakurser)
    }

    @Transactional
    fun resettValutakurserOgLagValutakurserEtterEndringstidspunkt(
        behandlingId: BehandlingId,
    ) {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId.id)
        val forrigeBehandlingVedtatt = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)

        if (forrigeBehandlingVedtatt != null) {
            // Resetter valutaen til slik den var i forrige behandling
            valutakursService.kopierOgErstattValutakurser(
                fraBehandlingId = BehandlingId(forrigeBehandlingVedtatt.id),
                tilBehandlingId = behandlingId,
            )
        }

        // Tilpasser valutaen til potensielle endringer i utenlandske periodebeløp fra denne behandlingen
        tilpassValutakurserTilUtenlandskePeriodebeløpService.tilpassValutakursTilUtenlandskPeriodebeløp(behandlingId)

        val endringstidspunktUtenValutakursendringer = finnEndringstidspunktForOppdateringAvValutakurs(behandling)

        oppdaterValutakurserEtterEndringstidspunkt(
            behandling = behandling,
            utenlandskePeriodebeløp = utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId.id),
            endringstidspunkt = endringstidspunktUtenValutakursendringer,
        )
    }

    @Transactional
    fun oppdaterValutakurserEtterEndringstidspunkt(
        behandlingId: BehandlingId,
        utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>? = null,
    ) {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId.id)
        oppdaterValutakurserEtterEndringstidspunkt(
            behandling = behandling,
            utenlandskePeriodebeløp = utenlandskePeriodebeløp ?: utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId.id),
            endringstidspunkt = finnEndringstidspunktForOppdateringAvValutakurs(behandling),
        )
    }

    @Transactional
    fun oppdaterValutakurserEtterEndringstidspunkt(
        behandling: Behandling,
        utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>? = null,
    ) = oppdaterValutakurserEtterEndringstidspunkt(
        behandling = behandling,
        utenlandskePeriodebeløp = utenlandskePeriodebeløp ?: utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandling.id),
        endringstidspunkt = finnEndringstidspunktForOppdateringAvValutakurs(behandling),
    )

    private fun oppdaterValutakurserEtterEndringstidspunkt(
        behandling: Behandling,
        utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>,
        endringstidspunkt: YearMonth,
    ) {
        val datoForPraksisEndringAutomatiskValutajustering = DATO_FOR_PRAKSISENDRING_AUTOMATISK_VALUTAJUSTERING

        if (behandling.erMigrering() || behandling.opprettetÅrsak.erManuellMigreringsårsak()) return

        if (behandling.skalBehandlesAutomatisk) return

        val vurderingsstrategiForValutakurser = vurderingsstrategiForValutakurserRepository.findByBehandlingId(behandling.id)
        if (vurderingsstrategiForValutakurser?.vurderingsstrategiForValutakurser == VurderingsstrategiForValutakurser.MANUELL) return

        val simuleringMottakere = simuleringService.oppdaterSimuleringPåBehandlingVedBehov(behandlingId = behandling.id)
        val datoSisteManuellePostering = simuleringMottakere.finnDatoSisteManuellePostering() ?: TIDENES_MORGEN
        val månedEtterSisteManuellePostering = datoSisteManuellePostering.toYearMonth().plusMonths(1)

        val månedForTidligsteTillatteAutomatiskeValutakurs =
            if (behandling.type == BehandlingType.FØRSTEGANGSBEHANDLING) {
                logger.info("Førstegangsbehandling: Setter tidligste måned for automatisk valutakurs det seneste av endringstidspunkt($endringstidspunkt) og måned etter siste manuelle postering($månedEtterSisteManuellePostering)")
                maxOf(endringstidspunkt, månedEtterSisteManuellePostering)
            } else {
                logger.info("Revurdering: Setter tidligste måned for automatisk valutakurs det seneste av endringstidspunkt($endringstidspunkt), måned etter siste manuelle postering($månedEtterSisteManuellePostering) og praksisendring($datoForPraksisEndringAutomatiskValutajustering)")
                maxOf(endringstidspunkt, månedEtterSisteManuellePostering, datoForPraksisEndringAutomatiskValutajustering)
            }

        val automatiskGenererteValutakurser =
            utenlandskePeriodebeløp.tilAutomatiskeValutakurserEtter(månedForTidligsteTillatteAutomatiskeValutakurs)

        valutakursService.oppdaterValutakurser(BehandlingId(behandling.id), automatiskGenererteValutakurser)
    }

    private fun finnEndringstidspunktForOppdateringAvValutakurs(
        behandling: Behandling,
    ): YearMonth {
        val endringstidspunkt = vedtaksperiodeService.finnEndringstidspunktForBehandling(behandling.id).toYearMonth()

        val valutakurserDenneBehandling = valutakursService.hentValutakurser(BehandlingId(behandling.id))
        val forrigeBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)
        val valutakurserForrigeBehandling = forrigeBehandling?.let { valutakursService.hentValutakurser(BehandlingId(forrigeBehandling.id)) } ?: emptyList()

        val førsteEndringIValutakurs = finnFørsteEndringIValutakurs(valutakurserDenneBehandling, valutakurserForrigeBehandling)

        logger.info("Finner minste av første endringstidspunkt for vedtaksperioder $endringstidspunkt og valutakurser $førsteEndringIValutakurs")

        return minOf(endringstidspunkt, førsteEndringIValutakurs)
    }

    private fun Collection<UtenlandskPeriodebeløp>.tilAutomatiskeValutakurserEtter(
        månedForTidligsteTillatteAutomatiskeValutakurs: YearMonth,
    ): List<Valutakurs> {
        logger.info(
            "Lager automatisk valutakurs for perioder etter $månedForTidligsteTillatteAutomatiskeValutakurs. + " +
                "Se securelogger for info om de utenlandske periodebeløpene",
        )
        secureLogger.info(
            "Lager automatisk valutakurs for perioder etter $månedForTidligsteTillatteAutomatiskeValutakurs. " +
                "Utenlandske periodebeløp: $this",
        )

        val valutakoder =
            filtrerErUtfylt().map { it.valutakode }.toSet()

        val automatiskGenererteValutakurser =
            valutakoder
                .map { valutakode ->
                    val upbGruppertPerBarnForValutakode = filtrerErUtfylt().filter { it.valutakode == valutakode }.groupBy { it.barnAktører }
                    val upbPerBarnTidslinjer = upbGruppertPerBarnForValutakode.values.map { upbForBarn -> upbForBarn.sortedBy { it.fom }.map { Periode(it, it.fom.førsteDagIInneværendeMåned(), it.tom?.sisteDagIInneværendeMåned()) }.tilTidslinje() }

                    val perioderAvBarnMedValutakode = upbPerBarnTidslinjer.kombiner { upberIPeriode -> upberIPeriode.flatMap { it.barnAktører }.toSet() }.tilPerioder()

                    perioderAvBarnMedValutakode
                        .mapNotNull { periode ->
                            periode.verdi?.let {
                                lagAutomatiskeValutakurserIPeriode(
                                    månedForTidligsteTillatteAutomatiskeValutakurs = månedForTidligsteTillatteAutomatiskeValutakurs,
                                    fom = periode.fom?.toYearMonth() ?: throw Feil("Fra og med-dato kan ikke være null"),
                                    tom = periode.tom?.toYearMonth(),
                                    barn = it,
                                    valutakode = valutakode,
                                )
                            }
                        }.flatten()
                }.flatten()
        return automatiskGenererteValutakurser
    }

    private fun lagAutomatiskeValutakurserIPeriode(
        månedForTidligsteTillatteAutomatiskeValutakurs: YearMonth,
        fom: YearMonth,
        tom: YearMonth?,
        barn: Set<Aktør>,
        valutakode: String,
    ): List<Valutakurs> {
        val start = maxOf(månedForTidligsteTillatteAutomatiskeValutakurs, fom)
        val denneMåneden = YearMonth.now(clockProvider.get())
        val slutt = tom ?: denneMåneden

        if (månedForTidligsteTillatteAutomatiskeValutakurs.isAfter(slutt)) return emptyList()

        return start.rangeTo(slutt).map { måned ->
            val sisteVirkedagForrigeMåned = måned.minusMonths(1).tilSisteVirkedag()

            Valutakurs(
                fom = måned,
                tom = if (måned == denneMåneden && tom == null) null else måned,
                barnAktører = barn,
                valutakursdato = sisteVirkedagForrigeMåned,
                valutakode = valutakode,
                kurs = ecbService.hentValutakurs(valutakode, sisteVirkedagForrigeMåned),
                vurderingsform = Vurderingsform.AUTOMATISK,
            )
        }
    }

    @Transactional
    fun endreVurderingsstrategiForValutakurser(
        behandlingId: BehandlingId,
        nyStrategi: VurderingsstrategiForValutakurser,
    ): VurderingsstrategiForValutakurserDB {
        if (!featureToggleService.isEnabled(FeatureToggle.TEKNISK_ENDRING)) {
            throw Feil("Relevante toggler for å overstyre vurderingsstrategi for valutakurser er ikke satt.")
        }

        val vurderingsstrategiForValutakurser = vurderingsstrategiForValutakurserRepository.findByBehandlingId(behandlingId.id)
        if (vurderingsstrategiForValutakurser != null) {
            vurderingsstrategiForValutakurserRepository.delete(vurderingsstrategiForValutakurser)
            vurderingsstrategiForValutakurserRepository.flush()
        }

        if (nyStrategi == VurderingsstrategiForValutakurser.AUTOMATISK) {
            resettValutakurserOgLagValutakurserEtterEndringstidspunkt(behandlingId)
        }

        return vurderingsstrategiForValutakurserRepository.save(
            VurderingsstrategiForValutakurserDB(
                behandlingId = behandlingId.id,
                vurderingsstrategiForValutakurser = nyStrategi,
            ),
        )
    }

    @Transactional
    fun oppdaterValutakurserOgSimulering(behandlingId: BehandlingId) {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId.id)
        oppdaterValutakurserEtterEndringstidspunkt(behandling)
        simuleringService.oppdaterSimuleringPåBehandling(behandling)
    }

    @Transactional
    fun oppdaterValutakurserOgSimulerVedBehov(behandlingId: Long) {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        val utenlandskePeriodebeløp = utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId)
        if (utenlandskePeriodebeløp.isEmpty()) return

        val valutakurser = valutakursService.hentValutakurser(BehandlingId(behandlingId))
        val inneværendeMåned = YearMonth.now(clockProvider.get())

        if (valutakurser.måValutakurserOppdateresForMåned(inneværendeMåned)) {
            val sisteValutakursdato = valutakurser.lastOrNull { it.valutakursdato != null }?.valutakursdato
            logger.info("Valutakurs for behandling: $behandlingId er utdatert, siste valutakursDato: $sisteValutakursdato. Oppdaterer valutakurser og simulering.")
            oppdaterValutakurserEtterEndringstidspunkt(behandling, utenlandskePeriodebeløp)
            simuleringService.oppdaterSimuleringPåBehandling(behandling)
        }
    }
}

private fun List<ØkonomiSimuleringMottaker>.finnDatoSisteManuellePostering() = this.flatMap { it.økonomiSimuleringPostering }.filter { it.erManuellPostering }.maxOfOrNull { it.tom }

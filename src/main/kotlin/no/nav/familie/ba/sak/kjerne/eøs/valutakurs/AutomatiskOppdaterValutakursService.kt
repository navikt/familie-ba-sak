﻿package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.LocalDateProvider
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.rangeTo
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.KAN_OPPRETTE_AUTOMATISKE_VALUTAKURSER_PÅ_MANUELLE_SAKER
import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.KAN_OVERSTYRE_AUTOMATISKE_VALUTAKURSER
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBService
import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.tilSisteVirkedag
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.TilpassValutakurserTilUtenlandskePeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.filtrerErUtfylt
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.månedPeriodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonthEllerNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

val DATO_FOR_PRAKSISENDRING_AUTOMATISK_VALUTAJUSTERING = YearMonth.of(2023, 1)

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
    private val vurderingsstrategiForValutakurserRepository: VurderingsstrategiForValutakurserRepository,
    private val unleashNextMedContextService: UnleashNextMedContextService,
) {
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

        val endringstidspunktUtenValutakursendringer = vedtaksperiodeService.finnEndringstidspunktForBehandlingUtenValutakursendringer(behandlingId.id).toYearMonth()

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
    ) = oppdaterValutakurserEtterEndringstidspunkt(
        behandling = behandlingHentOgPersisterService.hent(behandlingId.id),
        utenlandskePeriodebeløp = utenlandskePeriodebeløp ?: utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId.id),
        endringstidspunkt = vedtaksperiodeService.finnEndringstidspunktForBehandling(behandlingId.id).toYearMonth(),
    )

    private fun oppdaterValutakurserEtterEndringstidspunkt(
        behandling: Behandling,
        utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>,
        endringstidspunkt: YearMonth,
    ) {
        if (behandling.erMigrering() || behandling.opprettetÅrsak.erManuellMigreringsårsak()) return

        if (behandling.skalBehandlesAutomatisk) return

        val vurderingsstrategiForValutakurser = vurderingsstrategiForValutakurserRepository.findByBehandlingId(behandling.id)
        if (vurderingsstrategiForValutakurser?.vurderingsstrategiForValutakurser == VurderingsstrategiForValutakurser.MANUELL) return

        val simuleringMottakere = simuleringService.oppdaterSimuleringPåBehandlingVedBehov(behandlingId = behandling.id)
        val datoSisteManuellePostering = simuleringMottakere.finnDatoSisteManuellePostering() ?: TIDENES_MORGEN
        val månedEtterSisteManuellePostering = datoSisteManuellePostering.toYearMonth().plusMonths(1)

        val månedForTidligsteTillatteAutomatiskeValutakurs =
            if (behandling.type == BehandlingType.FØRSTEGANGSBEHANDLING) {
                maxOf(endringstidspunkt, månedEtterSisteManuellePostering)
            } else {
                maxOf(endringstidspunkt, månedEtterSisteManuellePostering, DATO_FOR_PRAKSISENDRING_AUTOMATISK_VALUTAJUSTERING)
            }

        val automatiskGenererteValutakurser =
            utenlandskePeriodebeløp.tilAutomatiskeValutakurserEtter(månedForTidligsteTillatteAutomatiskeValutakurs)

        valutakursService.oppdaterValutakurser(BehandlingId(behandling.id), automatiskGenererteValutakurser)
    }

    private fun Collection<UtenlandskPeriodebeløp>.tilAutomatiskeValutakurserEtter(
        månedForTidligsteTillatteAutomatiskeValutakurs: YearMonth,
    ): List<Valutakurs> {
        val valutakoder =
            filtrerErUtfylt().map { it.valutakode }.toSet()

        val automatiskGenererteValutakurser =
            valutakoder.map { valutakode ->
                val upbGruppertPerBarnForValutakode = filtrerErUtfylt().filter { it.valutakode == valutakode }.groupBy { it.barnAktører }
                val upbPerBarnTidslinjer = upbGruppertPerBarnForValutakode.values.map { upbForBarn -> upbForBarn.sortedBy { it.fom }.map { månedPeriodeAv(it.fom, it.tom, it) }.tilTidslinje() }

                val perioderAvBarnMedValutakode = upbPerBarnTidslinjer.kombiner { upberIPeriode -> upberIPeriode.flatMap { it.barnAktører }.toSet() }.perioder()

                perioderAvBarnMedValutakode.mapNotNull { periode ->
                    periode.innhold?.let {
                        lagAutomatiskeValutakurserIPeriode(
                            månedForTidligsteTillatteAutomatiskeValutakurs = månedForTidligsteTillatteAutomatiskeValutakurs,
                            fom = periode.fraOgMed.tilYearMonth(),
                            tom = periode.tilOgMed.tilYearMonthEllerNull(),
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
        val denneMåneden = localDateProvider.now().toYearMonth()
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
        if (!unleashNextMedContextService.isEnabled(KAN_OPPRETTE_AUTOMATISKE_VALUTAKURSER_PÅ_MANUELLE_SAKER) ||
            !unleashNextMedContextService.isEnabled(KAN_OVERSTYRE_AUTOMATISKE_VALUTAKURSER)
        ) {
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
}

private fun List<ØkonomiSimuleringMottaker>.finnDatoSisteManuellePostering() =
    this.flatMap { it.økonomiSimuleringPostering }.filter { it.erManuellPostering }.maxOfOrNull { it.tom }

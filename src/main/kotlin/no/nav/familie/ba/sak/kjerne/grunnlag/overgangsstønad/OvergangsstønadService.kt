package no.nav.familie.ba.sak.kjerne.grunnlag.overgangsstønad

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.ef.EfSakRestKlient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.SmåbarnstilleggGenerator
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.slåSammenTidligerePerioder
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilInternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.kontrakter.felles.ef.EksternPeriode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class OvergangsstønadService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val efSakRestKlient: EfSakRestKlient,
    private val periodeOvergangsstønadGrunnlagRepository: PeriodeOvergangsstønadGrunnlagRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val persongrunnlagService: PersongrunnlagService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val clockProvider: ClockProvider,
) {
    @Transactional
    fun hentOgLagrePerioderMedOvergangsstønadForBehandling(
        søkerAktør: Aktør,
        behandling: Behandling,
    ) {
        if (behandling.erSatsendringMånedligValutajusteringFinnmarkstilleggEllerSvalbardtillegg()) {
            kopierPerioderMedOvergangsstønadFraForrigeBehandling(behandling)
        } else {
            hentOgLagrePerioderMedFullOvergangsstønadFraEf(
                søkerAktør = søkerAktør,
                behandlingId = behandling.id,
            )
        }
    }

    private fun hentOgLagrePerioderMedFullOvergangsstønadFraEf(
        søkerAktør: Aktør,
        behandlingId: Long,
    ) {
        val periodeOvergangsstønad = hentPerioderMedFullOvergangsstønad(aktør = søkerAktør)

        periodeOvergangsstønadGrunnlagRepository.deleteByBehandlingId(behandlingId = behandlingId)

        periodeOvergangsstønadGrunnlagRepository.saveAll(
            periodeOvergangsstønad.map {
                it.tilPeriodeOvergangsstønadGrunnlag(
                    behandlingId = behandlingId,
                    aktør = søkerAktør,
                )
            },
        )
    }

    private fun kopierPerioderMedOvergangsstønadFraForrigeBehandling(
        inneværendeBehandling: Behandling,
    ) {
        val perioderFraForrigeBehandling =
            hentPerioderMedOvergangsstønadFraForrigeVedtatteBehandling(behandling = inneværendeBehandling)

        periodeOvergangsstønadGrunnlagRepository.deleteByBehandlingId(behandlingId = inneværendeBehandling.id)

        periodeOvergangsstønadGrunnlagRepository.saveAll(
            perioderFraForrigeBehandling.map {
                PeriodeOvergangsstønadGrunnlag(
                    behandlingId = inneværendeBehandling.id,
                    aktør = it.aktør,
                    fom = it.fom,
                    tom = it.tom,
                    datakilde = it.datakilde,
                )
            },
        )
    }

    fun hentPerioderMedFullOvergangsstønad(
        behandling: Behandling,
    ): List<InternPeriodeOvergangsstønad> {
        val dagensDato = LocalDate.now(clockProvider.get())

        val perioderOvergangsstønad = periodeOvergangsstønadGrunnlagRepository.findByBehandlingId(behandlingId = behandling.id).map { it.tilInternPeriodeOvergangsstønad() }
        val overgangsstønadPerioderFraForrigeBehandling =
            hentPerioderMedOvergangsstønadFraForrigeVedtatteBehandling(behandling).map { it.tilInternPeriodeOvergangsstønad() }.slåSammenTidligerePerioder(dagensDato)

        return perioderOvergangsstønad.splittOgSlåSammen(overgangsstønadPerioderFraForrigeBehandling, dagensDato)
    }

    private fun hentPerioderMedOvergangsstønadFraForrigeVedtatteBehandling(behandling: Behandling): List<PeriodeOvergangsstønadGrunnlag> {
        val forrigeVedtatteBehandling =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling = behandling)

        return if (forrigeVedtatteBehandling != null) {
            periodeOvergangsstønadGrunnlagRepository.findByBehandlingId(
                behandlingId = forrigeVedtatteBehandling.id,
            )
        } else {
            emptyList()
        }
    }

    fun vedtakOmOvergangsstønadPåvirkerFagsak(fagsak: Fagsak): Boolean {
        val sistIverksatteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = fagsak.id)
                ?: return false

        val tilkjentYtelseFraSistIverksatteBehandling =
            tilkjentYtelseRepository.findByBehandling(behandlingId = sistIverksatteBehandling.id)

        val persongrunnlagFraSistIverksatteBehandling =
            persongrunnlagService.hentAktivThrows(behandlingId = sistIverksatteBehandling.id)

        val dagensDato = LocalDate.now(clockProvider.get())

        val nyePerioderMedFullOvergangsstønad =
            hentPerioderMedFullOvergangsstønad(aktør = fagsak.aktør)
                .map { it.tilInternPeriodeOvergangsstønad() }
                .slåSammenTidligerePerioder(dagensDato)

        val andelerMedEndringerFraSistIverksatteBehandling =
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(sistIverksatteBehandling.id)

        secureLogger.info("Perioder med overgangsstønad fra EF: ${nyePerioderMedFullOvergangsstønad.map { "Periode(fom=${it.fomDato}, tom=${it.tomDato})" }}")

        return vedtakOmOvergangsstønadPåvirkerFagsak(
            småbarnstilleggGenerator =
                SmåbarnstilleggGenerator(
                    tilkjentYtelse = tilkjentYtelseFraSistIverksatteBehandling,
                ),
            nyePerioderMedFullOvergangsstønad = nyePerioderMedFullOvergangsstønad,
            forrigeAndelerTilkjentYtelse = andelerMedEndringerFraSistIverksatteBehandling,
            barnasAktørerOgFødselsdatoer =
                persongrunnlagFraSistIverksatteBehandling.barna.map {
                    Pair(
                        it.aktør,
                        it.fødselsdato,
                    )
                },
        )
    }

    fun hentPerioderMedFullOvergangsstønad(aktør: Aktør): List<EksternPeriode> =
        efSakRestKlient
            .hentPerioderMedFullOvergangsstønad(
                aktør.aktivFødselsnummer(),
            ).perioder
}

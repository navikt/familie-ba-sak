package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.integrasjoner.ef.EfSakRestClient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.slåSammenTidligerePerioder
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilInternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.tilPeriodeOvergangsstønadGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.kontrakter.felles.ef.EksternPeriode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SmåbarnstilleggService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val efSakRestClient: EfSakRestClient,
    private val periodeOvergangsstønadGrunnlagRepository: PeriodeOvergangsstønadGrunnlagRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val persongrunnlagService: PersongrunnlagService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
) {

    fun hentOgLagrePerioderMedFullOvergangsstønadFraEf(
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

    fun kopierPerioderMedOvergangsstønadFraForrigeBehandling(
        inneværendeBehandlingId: Long,
    ) {
        val perioderFraForrigeBehandling =
            hentPerioderMedOvergangsstønadFraForrigeIverksatteBehandling(behandlingId = inneværendeBehandlingId)

        periodeOvergangsstønadGrunnlagRepository.deleteByBehandlingId(behandlingId = inneværendeBehandlingId)

        periodeOvergangsstønadGrunnlagRepository.saveAll(
            perioderFraForrigeBehandling.map {
                PeriodeOvergangsstønadGrunnlag(
                    behandlingId = inneværendeBehandlingId,
                    aktør = it.aktør,
                    fom = it.fom,
                    tom = it.tom,
                    datakilde = it.datakilde,
                )
            },
        )
    }

    fun hentPerioderMedFullOvergangsstønad(
        behandlingId: Long,
    ): List<InternPeriodeOvergangsstønad> {
        val perioderOvergangsstønad = periodeOvergangsstønadGrunnlagRepository.findByBehandlingId(behandlingId)
        val overgangsstønadPerioderFraForrigeBehandling =
            hentPerioderMedOvergangsstønadFraForrigeIverksatteBehandling(behandlingId).map { it.tilInternPeriodeOvergangsstønad() }

        return perioderOvergangsstønad.splittOgSlåSammen(overgangsstønadPerioderFraForrigeBehandling)
    }

    private fun hentPerioderMedOvergangsstønadFraForrigeIverksatteBehandling(behandlingId: Long): List<PeriodeOvergangsstønadGrunnlag> {
        val forrigeIverksatteBehandling =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksattFraBehandlingsId(behandlingId = behandlingId)

        return if (forrigeIverksatteBehandling != null) {
            periodeOvergangsstønadGrunnlagRepository.findByBehandlingId(
                behandlingId = forrigeIverksatteBehandling.id,
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

        val nyePerioderMedFullOvergangsstønad =
            hentPerioderMedFullOvergangsstønad(aktør = fagsak.aktør).map { it.tilInternPeriodeOvergangsstønad() }
                .slåSammenTidligerePerioder()

        val andelerMedEndringerFraSistIverksatteBehandling = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(sistIverksatteBehandling.id)

        secureLogger.info("Perioder med overgangsstønad fra EF: ${nyePerioderMedFullOvergangsstønad.map { "Periode(fom=${it.fomDato}, tom=${it.tomDato})" }}")

        return vedtakOmOvergangsstønadPåvirkerFagsak(
            småbarnstilleggBarnetrygdGenerator = SmåbarnstilleggBarnetrygdGenerator(
                behandlingId = sistIverksatteBehandling.id,
                tilkjentYtelse = tilkjentYtelseFraSistIverksatteBehandling,
            ),
            nyePerioderMedFullOvergangsstønad = nyePerioderMedFullOvergangsstønad,
            forrigeAndelerTilkjentYtelse = andelerMedEndringerFraSistIverksatteBehandling,
            barnasAktørerOgFødselsdatoer = persongrunnlagFraSistIverksatteBehandling.barna.map {
                Pair(
                    it.aktør,
                    it.fødselsdato,
                )
            },
        )
    }

    private fun hentPerioderMedFullOvergangsstønad(aktør: Aktør): List<EksternPeriode> {
        return efSakRestClient.hentPerioderMedFullOvergangsstønad(
            aktør.aktivFødselsnummer(),
        ).perioder
    }

    companion object {
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}

package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.integrasjoner.`ef-sak`.EfSakRestClient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.slåSammenTidligerePerioder
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilInternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.tilPeriodeOvergangsstønadGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import org.springframework.stereotype.Service

@Service
class SmåbarnstilleggService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val efSakRestClient: EfSakRestClient,
    private val periodeOvergangsstønadGrunnlagRepository: PeriodeOvergangsstønadGrunnlagRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val persongrunnlagService: PersongrunnlagService,
) {

    fun hentOgLagrePerioderMedFullOvergangsstønad(
        aktør: Aktør,
        behandlingId: Long
    ): List<InternPeriodeOvergangsstønad> {
        val periodeOvergangsstønad = hentPerioderMedFullOvergangsstønad(aktør)

        periodeOvergangsstønadGrunnlagRepository.deleteByBehandlingId(behandlingId)
        periodeOvergangsstønadGrunnlagRepository.saveAll(
            periodeOvergangsstønad.map {
                it.tilPeriodeOvergangsstønadGrunnlag(
                    behandlingId, aktør
                )
            }
        )

        return periodeOvergangsstønad.map { it.tilInternPeriodeOvergangsstønad() }.slåSammenTidligerePerioder()
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

        return vedtakOmOvergangsstønadPåvirkerFagsak(
            småbarnstilleggBarnetrygdGenerator = SmåbarnstilleggBarnetrygdGenerator(
                behandlingId = sistIverksatteBehandling.id,
                tilkjentYtelse = tilkjentYtelseFraSistIverksatteBehandling
            ),
            nyePerioderMedFullOvergangsstønad = nyePerioderMedFullOvergangsstønad,
            forrigeAndelerTilkjentYtelse = tilkjentYtelseFraSistIverksatteBehandling.andelerTilkjentYtelse.toList(),
            barnasAktørerOgFødselsdatoer = persongrunnlagFraSistIverksatteBehandling.barna.map {
                Pair(
                    it.aktør,
                    it.fødselsdato
                )
            },
        )
    }

    private fun hentPerioderMedFullOvergangsstønad(aktør: Aktør): List<PeriodeOvergangsstønad> {
        return efSakRestClient.hentPerioderMedFullOvergangsstønad(
            aktør.aktivFødselsnummer()
        ).perioder
    }
}

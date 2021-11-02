package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.dokument.BrevKlient
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerIngenOverlappendeEndring
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerPeriodeInnenforTilkjentytelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.erStartPåUtvidetSammeMåned
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.fraRestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.hentGyldigEndretBegrunnelser
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EndretUtbetalingAndelService(
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val beregningService: BeregningService,
    private val persongrunnlagService: PersongrunnlagService,
    private val brevKlient: BrevKlient,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) {

    @Transactional
    fun oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        endretUtbetalingAndelId: Long,
        restEndretUtbetalingAndel: RestEndretUtbetalingAndel
    ) {
        val endretUtbetalingAndel = endretUtbetalingAndelRepository.getById(endretUtbetalingAndelId)
        val person =
            persongrunnlagService.hentPersonerPåBehandling(listOf(restEndretUtbetalingAndel.personIdent!!), behandling)
                .first()

        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
                ?: throw Feil("Fant ikke personopplysninggrunnlag på behandling ${behandling.id}")

        val andelTilkjentYtelser = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)
        // Todo hva gjør vi med dette dersom den utvidede perioden endres til 0?
        val erStartPåUtvidetSammeMåned =
            erStartPåUtvidetSammeMåned(
                personopplysningGrunnlag,
                andelTilkjentYtelser,
                restEndretUtbetalingAndel.fom
            )

        endretUtbetalingAndel.fraRestEndretUtbetalingAndel(restEndretUtbetalingAndel, person).also {
            it.vedtakBegrunnelseSpesifikasjoner =
                it.hentGyldigEndretBegrunnelser(brevKlient.hentSanityBegrunnelser(), erStartPåUtvidetSammeMåned)
        }

        validerIngenOverlappendeEndring(
            endretUtbetalingAndel,
            endretUtbetalingAndelRepository.findByBehandlingId(behandling.id)
                .filter { it.id != endretUtbetalingAndelId }
        )

        validerPeriodeInnenforTilkjentytelse(endretUtbetalingAndel, andelTilkjentYtelser)

        beregningService.oppdaterBehandlingMedBeregning(
            behandling, personopplysningGrunnlag, endretUtbetalingAndel
        )
    }

    @Transactional
    fun fjernEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        endretUtbetalingAndelId: Long,
    ) {
        val endretUtbetalingAndel = endretUtbetalingAndelRepository.getById(endretUtbetalingAndelId)
        endretUtbetalingAndel.andelTilkjentYtelser.forEach { it.endretUtbetalingAndeler.clear() }
        endretUtbetalingAndelRepository.delete(endretUtbetalingAndel)

        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
                ?: throw Feil("Fant ikke personopplysninggrunnlag på behandling ${behandling.id}")

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
    }

    @Transactional
    fun opprettTomEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling
    ) =
        endretUtbetalingAndelRepository.save(
            EndretUtbetalingAndel(
                behandlingId = behandling.id,
            )
        )

    @Transactional
    fun kopierEndretUtbetalingAndelFraForrigeBehandling(behandling: Behandling, forrigeBehandling: Behandling) {
        hentForBehandling(forrigeBehandling.id).forEach {
            endretUtbetalingAndelRepository.save(
                it.copy(
                    id = 0,
                    behandlingId = behandling.id,
                    andelTilkjentYtelser = mutableListOf()
                )
            )
        }
    }

    fun hentForBehandling(behandlingId: Long) = endretUtbetalingAndelRepository.findByBehandlingId(behandlingId)

    @Transactional
    fun fjernKnytningTilAndelTilkjentYtelse(behandlingId: Long) {
        hentForBehandling(behandlingId).filter { it.andelTilkjentYtelser.isNotEmpty() }.forEach {
            it.andelTilkjentYtelser.clear()
        }
    }
}

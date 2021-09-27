package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EndretUtbetalingAndelService(
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val beregningService: BeregningService,
) {

    @Transactional
    fun oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        endretUtbetalingAndelId: Long,
        restEndretUtbetalingAndel: RestEndretUtbetalingAndel
    ) {
        val endretUtbetalingAndel = endretUtbetalingAndelRepository.getById(endretUtbetalingAndelId)

        endretUtbetalingAndel.fom = restEndretUtbetalingAndel.fom
        endretUtbetalingAndel.tom = restEndretUtbetalingAndel.tom
        endretUtbetalingAndel.prosent = restEndretUtbetalingAndel.prosent
        endretUtbetalingAndel.årsak = restEndretUtbetalingAndel.årsak

        endretUtbetalingAndelRepository.save(endretUtbetalingAndel)

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
            ?: throw Feil("Fant ikke personopplysninggrunnlag på behandling ${behandling.id}")

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
    }

    @Transactional
    fun fjernEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        endretUtbetalingAndelId: Long,
    ) {
        endretUtbetalingAndelRepository.deleteById(endretUtbetalingAndelId)

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
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
}
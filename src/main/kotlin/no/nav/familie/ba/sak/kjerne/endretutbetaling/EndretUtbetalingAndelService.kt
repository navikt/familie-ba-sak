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
       val endretUtbetalingAndel = endretUtbetalingAndelRepository.findById(endretUtbetalingAndelId)

        endretUtbetalingAndelRepository.save(endretUtbetalingAndel.get().copy(
            fom = restEndretUtbetalingAndel.fom,
            tom = restEndretUtbetalingAndel.tom,
            prosent = restEndretUtbetalingAndel.prosent,
            årsak = restEndretUtbetalingAndel.årsak,
        ))

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
    fun opprettEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        restEndretUtbetalingAndel: RestEndretUtbetalingAndel
    ) {
        val personOpplysninger = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
            ?: error("Finner ikke persongrunnlag")

        val barn =
            personOpplysninger.barna.single { b -> b.personIdent.ident == restEndretUtbetalingAndel.personIdent }

        val endretUtbetalingAndel = EndretUtbetalingAndel(
            behandlingId = behandling.id,
            person = barn,
            prosent = restEndretUtbetalingAndel.prosent,
            fom = restEndretUtbetalingAndel.fom,
            tom = restEndretUtbetalingAndel.tom,
            årsak = restEndretUtbetalingAndel.årsak,
            begrunnelse = restEndretUtbetalingAndel.begrunnelse,
        )

        endretUtbetalingAndelRepository.save(endretUtbetalingAndel)
        beregningService.oppdaterBehandlingMedBeregning(behandling, personOpplysninger)
    }
}
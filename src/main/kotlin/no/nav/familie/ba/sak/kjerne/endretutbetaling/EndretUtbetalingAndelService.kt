package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import org.springframework.stereotype.Service

@Service
class EndretUtbetalingAndelService(
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val persongrunnlagService: PersongrunnlagService,
    private val beregningService: BeregningService,
    ) {

    fun opprettEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        restEndretUtbetalingAndel: RestEndretUtbetalingAndel
    ) {
        val personOpplysninger = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
            ?: error("Finner ikke persongrunnlag")

        val barn =
            personOpplysninger.barna.single { b -> b.personIdent.ident == restEndretUtbetalingAndel.personIdent }

        val overstyrtUtbetaling = EndretUtbetalingAndel(
            behandlingId = behandling.id,
            person = barn,
            prosent = restEndretUtbetalingAndel.prosent,
            fom = restEndretUtbetalingAndel.fom,
            tom = restEndretUtbetalingAndel.tom,
            årsak = restEndretUtbetalingAndel.årsak,
            begrunnelse = restEndretUtbetalingAndel.begrunnelse,
        )

        endretUtbetalingAndelRepository.save(overstyrtUtbetaling)

        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandling.id)
            ?: throw Feil("Fant ikke personopplysninggrunnlag på behandling ${behandling.id}")

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
    }
}
package no.nav.familie.ba.sak.kjerne.overstyring

import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.overstyring.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.overstyring.domene.EndretUtbetalingAndelRepository
import org.springframework.stereotype.Service

@Service
class EndretUtbetalingAndelService(
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository
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

        //regenerer andeler utbetalt ytelse
        // oppdater andeler utbetalt ytelse med overstyrt periode.

    }
}
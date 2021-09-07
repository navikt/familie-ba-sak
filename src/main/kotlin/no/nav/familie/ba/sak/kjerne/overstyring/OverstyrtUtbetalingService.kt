package no.nav.familie.ba.sak.kjerne.overstyring

import no.nav.familie.ba.sak.ekstern.restDomene.RestOverstyrtUtbetaling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.overstyring.domene.OverstyrtUtbetaling
import no.nav.familie.ba.sak.kjerne.overstyring.domene.OverstyrtUtbetalingRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import org.springframework.stereotype.Service

@Service
class OverstyrtUtbetalingService(
    private val overstyrtUtbetalingRepository: OverstyrtUtbetalingRepository,
    private val vedtakService: VedtakService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository
) {

    fun lagreOverstyrtUtbetalingOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        restOverstyrtUtbetaling: RestOverstyrtUtbetaling
    ) {
        overstyrtUtbetalingRepository.save(restOverstyrtUtbetaling.tilOverstyrtUtbetaling())
    }

    fun opprettOverstyrtUtbetalingOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        restOverstyrtUtbetaling: RestOverstyrtUtbetaling
    ) {
        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
            ?: error("Fant ikke aktivt vedtak på behandling ${behandling.id}")

        val personOpplysninger = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
            ?: error("Finner ikke persongrunnlag")

        val barn =
            personOpplysninger.barna.single { b -> b.personIdent.ident == restOverstyrtUtbetaling.personIdent }

        val overstyrtUtbetaling = OverstyrtUtbetaling(
            vedtakId = vedtak.id,
            person = barn,
            prosent = restOverstyrtUtbetaling.prosent,
            fom = restOverstyrtUtbetaling.fom,
            tom = restOverstyrtUtbetaling.tom,
            årsak = restOverstyrtUtbetaling.årsak,
            begrunnelse = restOverstyrtUtbetaling.begrunnelse,
        )

        overstyrtUtbetalingRepository.save(overstyrtUtbetaling)
    }
}
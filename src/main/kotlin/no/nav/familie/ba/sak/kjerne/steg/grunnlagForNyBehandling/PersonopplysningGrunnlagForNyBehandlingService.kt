package no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.skalTaMedBarnFraForrigeBehandling
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import org.springframework.stereotype.Service

@Service
class PersonopplysningGrunnlagForNyBehandlingService(
    private val personidentService: PersonidentService,
    private val beregningService: BeregningService,
    private val persongrunnlagService: PersongrunnlagService
) {

    fun opprettPersonopplysningGrunnlag(
        behandling: Behandling,
        forrigeBehandlingSomErVedtatt: Behandling?,
        søkerIdent: String,
        barnasIdenter: List<String>
    ) {
        val aktør = personidentService.hentOgLagreAktør(søkerIdent, true)
        val barnaAktør = personidentService.hentOgLagreAktørIder(barnasIdenter, true)

        val målform = forrigeBehandlingSomErVedtatt
            ?.let { persongrunnlagService.hentSøkersMålform(behandlingId = it.id) }
            ?: Målform.NB

        val barnMedTilkjentYtelseIForrigeBehandling =
            if (skalTaMedBarnFraForrigeBehandling(behandling) && forrigeBehandlingSomErVedtatt != null) {
                beregningService.finnBarnFraBehandlingMedTilkjentYtelse(behandlingId = forrigeBehandlingSomErVedtatt.behandlingId)
            } else {
                emptyList()
            }

        persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
            aktør = aktør,
            barnFraInneværendeBehandling = barnaAktør,
            barnFraForrigeBehandling = barnMedTilkjentYtelseIForrigeBehandling,
            behandling = behandling,
            målform = målform
        )
    }
}

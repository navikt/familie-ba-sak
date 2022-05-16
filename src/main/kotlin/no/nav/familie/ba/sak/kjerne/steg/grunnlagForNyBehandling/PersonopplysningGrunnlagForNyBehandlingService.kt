package no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
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
        val søker = personidentService.hentOgLagreAktør(søkerIdent, true)
        val barna = personidentService.hentOgLagreAktørIder(barnasIdenter, true)

        if (behandling.type == BehandlingType.REVURDERING && forrigeBehandlingSomErVedtatt != null) {
            val forrigePersongrunnlagBarna =
                beregningService.finnBarnFraBehandlingMedTilkjentYtsele(forrigeBehandlingSomErVedtatt.id)
            val forrigeMålform =
                persongrunnlagService.hentSøkersMålform(forrigeBehandlingSomErVedtatt.id)

            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                aktør = søker,
                barnFraInneværendeBehandling = barna,
                barnFraForrigeBehandling = forrigePersongrunnlagBarna,
                behandling = behandling,
                målform = forrigeMålform
            )
        } else {
            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                aktør = søker,
                barnFraInneværendeBehandling = barna,
                behandling = behandling,
                målform = Målform.NB
            )
        }
    }
}

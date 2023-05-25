package no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling

import no.nav.familie.ba.sak.common.Feil
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
    private val persongrunnlagService: PersongrunnlagService,
) {

    fun opprettKopiEllerNyttPersonopplysningGrunnlag(
        behandling: Behandling,
        forrigeBehandlingSomErVedtatt: Behandling?,
        søkerIdent: String,
        barnasIdenter: List<String>,
    ) {
        if (behandling.erSatsendring()) {
            if (forrigeBehandlingSomErVedtatt == null) {
                throw Feil("Vi kan ikke kjøre satsendring dersom det ikke finnes en tidligere behandling. Behandling: ${behandling.id}")
            }
            opprettKopiAvPersonopplysningGrunnlag(behandling, forrigeBehandlingSomErVedtatt)
        } else {
            opprettPersonopplysningGrunnlag(behandling, forrigeBehandlingSomErVedtatt, søkerIdent, barnasIdenter)
        }
    }

    private fun opprettKopiAvPersonopplysningGrunnlag(
        behandling: Behandling,
        forrigeBehandlingSomErVedtatt: Behandling,
    ) {
        val personopplysningGrunnlag =
            persongrunnlagService.hentAktivThrows(forrigeBehandlingSomErVedtatt.id).tilKopiForNyBehandling(behandling)
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)
    }

    private fun opprettPersonopplysningGrunnlag(
        behandling: Behandling,
        forrigeBehandlingSomErVedtatt: Behandling?,
        søkerIdent: String,
        barnasIdenter: List<String>,
    ) {
        val aktør = personidentService.hentOgLagreAktør(søkerIdent, true)
        val barnaAktør = personidentService.hentOgLagreAktørIder(barnasIdenter, true)

        val målform = forrigeBehandlingSomErVedtatt
            ?.let { persongrunnlagService.hentSøkersMålform(behandlingId = it.id) }
            ?: Målform.NB

        val barnMedTilkjentYtelseIForrigeBehandling =
            if (skalTaMedBarnFraForrigeBehandling(behandling) && forrigeBehandlingSomErVedtatt != null) {
                beregningService.finnBarnFraBehandlingMedTilkjentYtelse(behandlingId = forrigeBehandlingSomErVedtatt.id)
            } else {
                emptyList()
            }

        persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
            aktør = aktør,
            barnFraInneværendeBehandling = barnaAktør,
            barnFraForrigeBehandling = barnMedTilkjentYtelseIForrigeBehandling,
            behandling = behandling,
            målform = målform,
        )
    }
}

package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerInstitusjonOgVerge
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.institusjon.InstitusjonService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.verge.VergeService
import org.springframework.stereotype.Service

// TODO: Kan vi registre beger verge og institusjon på en fagsak? Kan vi endre verge og institusjon sinere?
@Service
class RegistrerInstitusjonOgVerge(
    val institusjonService: InstitusjonService,
    val vergeService: VergeService,
    val loggService: LoggService,
    val behandlingHentOgPersisterService: BehandlingHentOgPersisterService
) : BehandlingSteg<RestRegistrerInstitusjonOgVerge> {

    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: RestRegistrerInstitusjonOgVerge
    ): StegType {
        val fagsakId = behandling.fagsak.id
        var verge = data.tilVerge(behandling)
        var institusjon = data.tilInstitusjon()
        if (verge != null) {
            vergeService.RegistrerVergeForBehandling(behandling, verge)
            loggService.opprettRegistrerVergeLogg(
                behandling
            )
        }
        if (institusjon != null) {
            institusjonService.registrerInstitusjonForFagsak(fagsakId, institusjon)
            loggService.opprettRegistrerInstitusjonLogg(
                behandling
            )
        }

        if (verge == null && institusjon == null) {
            throw Feil("Ugyldig DTO for registrer verge")
        }

        return hentNesteStegForNormalFlyt(behandling = behandlingHentOgPersisterService.hent(behandlingId = behandling.id))
    }

    override fun stegType(): StegType {
        return StegType.REGISTRERE_INSTITUSJON_OG_VERGE
    }
}

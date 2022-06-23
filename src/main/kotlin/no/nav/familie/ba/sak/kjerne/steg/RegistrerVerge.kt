package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerVerge
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.institusjon.InstitusjonService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.logg.RegistrerVergeLoggType
import org.springframework.stereotype.Service

//TODO: Kan vi registre beger verge og institusjon på en fagsak? Kan vi endre verge og institusjon sinere?
@Service
class RegistrerVerge(
    val institusjonService: InstitusjonService,
    val loggService: LoggService,
    val behandlingHentOgPersisterService: BehandlingHentOgPersisterService
) : BehandlingSteg<RestRegistrerVerge> {

    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: RestRegistrerVerge
    ): StegType {
        val fagsakId = behandling.fagsak.id
        var verge = data.tilVerge()
        var institusjon = data.tilInstitusjon()
        if (verge != null) {
            institusjonService.RegistrerVergeForFagsak(fagsakId, verge)
        }
        if (institusjon != null) {
            institusjonService.RegistrerInstitusjonForFagsak(fagsakId, institusjon)
        }

        if (verge == null && institusjon == null) {
            throw Feil("Ugyldig DTO for registrer verge")
        }

        loggService.opprettRegistrerVergeLogg(
            behandling,
            if (verge != null) RegistrerVergeLoggType.VERGE_REGISTRERT else RegistrerVergeLoggType.INSTITUSJON_REGISTRERT
        )

        return hentNesteStegForNormalFlyt(behandling = behandlingHentOgPersisterService.hent(behandlingId = behandling.id))
    }

    override fun stegType(): StegType {
        return StegType.REGISTRERE_VERGE
    }
}

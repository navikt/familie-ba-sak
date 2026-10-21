package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.institusjon.InstitusjonService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.stereotype.Service

@Service
class RegistrerInstitusjon(
    val institusjonService: InstitusjonService,
    val loggService: LoggService,
    val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) : BehandlingSteg<String> {
    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: String,
    ): StegType {
        if (behandling.fagsak.institusjon == null) {
            throw Feil("Fagsak ${behandling.fagsak.id} mangler institusjon. Institusjon må være registrert på fagsaken før steget REGISTRERE_INSTITUSJON kan utføres.")
        }

        loggService.opprettRegistrerInstitusjonLogg(
            behandling,
        )

        if (!behandling.status.erLåstForVidereRedigering()) {
            institusjonService.lagreInstitusjonsinfo(behandling.id)
        }

        return hentNesteStegForNormalFlyt(behandling = behandlingHentOgPersisterService.hent(behandlingId = behandling.id))
    }

    override fun stegType(): StegType = StegType.REGISTRERE_INSTITUSJON
}

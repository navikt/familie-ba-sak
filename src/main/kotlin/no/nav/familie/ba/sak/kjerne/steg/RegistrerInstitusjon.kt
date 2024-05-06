package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.institusjon.Institusjon
import no.nav.familie.ba.sak.kjerne.institusjon.InstitusjonService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.stereotype.Service

@Service
class RegistrerInstitusjon(
    val institusjonService: InstitusjonService,
    val loggService: LoggService,
    val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    val fagsakService: FagsakService,
) : BehandlingSteg<Institusjon> {
    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        institusjon: Institusjon,
    ): StegType {
        institusjonService.hentEllerOpprettInstitusjon(
            orgNummer = institusjon.orgNummer,
            tssEksternId = institusjon.tssEksternId,
        ).apply {
            val fagsak = behandling.fagsak
            fagsak.institusjon = this
            fagsakService.lagre(fagsak)
        }
        loggService.opprettRegistrerInstitusjonLogg(
            behandling,
        )

        return hentNesteStegForNormalFlyt(behandling = behandlingHentOgPersisterService.hent(behandlingId = behandling.id))
    }

    override fun stegType(): StegType {
        return StegType.REGISTRERE_INSTITUSJON
    }
}

package no.nav.familie.ba.sak.cucumber.mock.komponentMocks

import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.cucumber.mock.CucumberMock
import no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg.AutovedtakFinnmarkstilleggService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus

fun mockAutovedtakFinnmarkstilleggService(
    dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition,
    fagsakId: Long,
    nyBehanldingId: Long,
): AutovedtakFinnmarkstilleggService {
    val forrigeBehandling =
        dataFraCucumber.behandlinger.values
            .filter { it.fagsak.id == fagsakId && it.status == BehandlingStatus.AVSLUTTET }
            .maxByOrNull { it.id }

    dataFraCucumber.behandlingTilForrigeBehandling.put(nyBehanldingId, forrigeBehandling?.id)

    val cucumberMock =
        CucumberMock(
            dataFraCucumber = dataFraCucumber,
            nyBehandlingId = nyBehanldingId,
            forrigeBehandling = forrigeBehandling,
        )

    return AutovedtakFinnmarkstilleggService(
        behandlingHentOgPersisterService = cucumberMock.behandlingHentOgPersisterService,
        autovedtakService = cucumberMock.autovedtakService,
        behandlingService = cucumberMock.behandlingService,
        fagsakService = cucumberMock.fagsakService,
        taskService = cucumberMock.taskService,
        persongrunnlagService = cucumberMock.persongrunnlagService,
        beregningService = cucumberMock.beregningService,
        simuleringService = cucumberMock.simuleringService,
        pdlRestKlient = cucumberMock.systemOnlyPdlRestKlient,
        autovedtakFinnmarkstilleggBegrunnelseService = cucumberMock.autovedtakFinnmarkstilleggBegrunnelseService,
        oppgaveService = cucumberMock.oppgaveService,
    )
}

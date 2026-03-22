package no.nav.familie.ba.sak.cucumber.mock.komponentMocks

import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.cucumber.mock.CucumberMock
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.AutovedtakSatsendringRollbackService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus

fun mockAutovedtakSatsendringRollbackService(
    dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition,
    fagsakId: Long,
    nyBehandlingId: Long,
): AutovedtakSatsendringRollbackService {
    val forrigeBehandling =
        dataFraCucumber.behandlinger.values
            .filter { it.fagsak.id == fagsakId && it.status == BehandlingStatus.AVSLUTTET }
            .maxByOrNull { it.id }

    dataFraCucumber.behandlingTilForrigeBehandling.put(nyBehandlingId, forrigeBehandling?.id)

    val cucumberMock =
        CucumberMock(
            dataFraCucumber = dataFraCucumber,
            nyBehandlingId = nyBehandlingId,
            forrigeBehandling = forrigeBehandling,
        )

    return AutovedtakSatsendringRollbackService(
        taskRepository = cucumberMock.taskRepository,
        autovedtakService = cucumberMock.autovedtakService,
        behandlingService = cucumberMock.behandlingService,
        snikeIKøenService = cucumberMock.snikeIKøenService,
        behandlingHentOgPersisterService = cucumberMock.behandlingHentOgPersisterService,
    )
}

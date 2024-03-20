package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import no.nav.familie.ba.sak.cucumber.BegrunnelseTeksterStepDefinition
import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.AutovedtakMånedligValutajusteringService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak

fun mockAutovedtakMånedligValutajusteringService(
    dataFraCucumber: BegrunnelseTeksterStepDefinition,
    fagsak: Fagsak,
    nyBehanldingId: Long,
): AutovedtakMånedligValutajusteringService {
    val forrigeBehandling = dataFraCucumber.behandlinger.values.filter { it.fagsak.id == fagsak.id && it.status == BehandlingStatus.AVSLUTTET }.maxByOrNull { it.id }
    dataFraCucumber.behandlingTilForrigeBehandling.put(nyBehanldingId, forrigeBehandling?.id)

    val cucumberMock =
        CucumberMock(
            dataFraCucumber = dataFraCucumber,
            nyBehanldingId = nyBehanldingId,
            forrigeBehandling = forrigeBehandling,
        )

    every { cucumberMock.snikeIKøenService.kanSnikeForbi(any()) } returns true

    return AutovedtakMånedligValutajusteringService(
        behandlingHentOgPersisterService = cucumberMock.behandlingHentOgPersisterService,
        autovedtakService = cucumberMock.autovedtakService,
        taskRepository = cucumberMock.taskRepository,
        behandlingService = cucumberMock.behandlingService,
        snikeIKøenService = cucumberMock.snikeIKøenService,
    )
}

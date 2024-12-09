package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.kjerne.autovedtak.oppdaterutvidetklassekode.AutovedtakOppdaterUtvidetKlassekodeService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak

fun mockAutovedtakOppdaterUtvidetKlassekodeService(
    dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition,
    fagsak: Fagsak,
): AutovedtakOppdaterUtvidetKlassekodeService {
    val forrigeBehandling =
        dataFraCucumber.behandlinger.values
            .filter { it.fagsak.id == fagsak.id && it.status == BehandlingStatus.AVSLUTTET }
            .maxByOrNull { it.id } ?: error("Fant ingen forrige behandling for fagsak=${fagsak.id}")
    val nyBehandlingId = forrigeBehandling.id.plus(1)
    dataFraCucumber.behandlingTilForrigeBehandling[nyBehandlingId] = forrigeBehandling.id

    val cucumberMock =
        CucumberMock(
            dataFraCucumber = dataFraCucumber,
            nyBehandlingId = nyBehandlingId,
            forrigeBehandling = forrigeBehandling,
        )

    every { cucumberMock.snikeIKøenService.kanSnikeForbi(any()) } returns true

    return AutovedtakOppdaterUtvidetKlassekodeService(
        behandlingHentOgPersisterService = cucumberMock.behandlingHentOgPersisterService,
        autovedtakService = cucumberMock.autovedtakService,
        taskRepository = cucumberMock.taskRepository,
        snikeIKøenService = cucumberMock.snikeIKøenService,
        oppdaterUtvidetKlassekodeKjøringRepository = cucumberMock.oppdaterUtvidetKlassekodeKjøringRepository,
        tilkjentYtelseRepository = cucumberMock.tilkjentYtelseRepository,
    )
}

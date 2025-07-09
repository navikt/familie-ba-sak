package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBService
import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.AutovedtakMånedligValutajusteringService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import java.math.BigDecimal
import java.time.LocalDate

fun mockAutovedtakMånedligValutajusteringService(
    dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition,
    fagsak: Fagsak,
    nyBehanldingId: Long,
    svarFraEcbMock: Map<Pair<String, LocalDate>, BigDecimal>,
): AutovedtakMånedligValutajusteringService {
    val forrigeBehandling =
        dataFraCucumber.behandlinger.values
            .filter { it.fagsak.id == fagsak.id && it.status == BehandlingStatus.AVSLUTTET }
            .maxByOrNull { it.id }
    dataFraCucumber.behandlingTilForrigeBehandling.put(nyBehanldingId, forrigeBehandling?.id)

    val ecbService = mockk<ECBService>()
    every { ecbService.hentValutakurs(any(), any()) } answers {
        val valutakode = firstArg<String>()
        val dato = secondArg<LocalDate>()

        svarFraEcbMock[Pair(valutakode, dato)]
            ?: throw Feil("Fant ikke valutakurs for valutakode=$valutakode og dato=$dato i ECB mocken")
    }

    val cucumberMock =
        CucumberMock(
            dataFraCucumber = dataFraCucumber,
            nyBehandlingId = nyBehanldingId,
            forrigeBehandling = forrigeBehandling,
            ecbService = ecbService,
        )

    every { cucumberMock.snikeIKøenService.kanSnikeForbi(any()) } returns true

    return AutovedtakMånedligValutajusteringService(
        behandlingHentOgPersisterService = cucumberMock.behandlingHentOgPersisterService,
        autovedtakService = cucumberMock.autovedtakService,
        taskRepository = cucumberMock.taskRepository,
        behandlingService = cucumberMock.behandlingService,
        snikeIKøenService = cucumberMock.snikeIKøenService,
        clockProvider = cucumberMock.clockProvider,
        valutakursService = cucumberMock.valutakursService,
        simuleringService = cucumberMock.simuleringService,
        startSatsendring = cucumberMock.startSatsendring,
    )
}

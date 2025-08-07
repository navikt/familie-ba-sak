import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.cucumber.mock.CucumberMock
import no.nav.familie.ba.sak.integrasjoner.ef.EfSakRestClient
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.kontrakter.felles.ef.Datakilde
import no.nav.familie.kontrakter.felles.ef.EksternPeriode
import no.nav.familie.kontrakter.felles.ef.EksternePerioderResponse

fun mockAutovedtakSmåbarnstilleggService(
    dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition,
    fagsak: Fagsak,
    internPeriodeOvergangsstønadNyBehandling: List<InternPeriodeOvergangsstønad>,
    småbarnstilleggBehandlingId: Long,
): AutovedtakStegService {
    val forrigeBehandling =
        dataFraCucumber.behandlinger.values
            .filter { it.fagsak.id == fagsak.id && it.status == BehandlingStatus.AVSLUTTET }
            .maxByOrNull { it.id }
    dataFraCucumber.behandlingTilForrigeBehandling.put(småbarnstilleggBehandlingId, forrigeBehandling?.id)

    val efSakRestClient = mockEfSakRestClient(internPeriodeOvergangsstønadNyBehandling)

    val cucumberMock =
        CucumberMock(
            dataFraCucumber = dataFraCucumber,
            nyBehandlingId = småbarnstilleggBehandlingId,
            forrigeBehandling = forrigeBehandling,
            efSakRestClientMock = efSakRestClient,
        )

    return AutovedtakStegService(
        fagsakService = cucumberMock.fagsakService,
        behandlingHentOgPersisterService = cucumberMock.behandlingHentOgPersisterService,
        oppgaveService = cucumberMock.oppgaveService,
        autovedtakFødselshendelseService = mockk(),
        autovedtakBrevService = mockk(),
        autovedtakSmåbarnstilleggService = cucumberMock.autovedtakSmåbarnstilleggService,
        autovedtakFinnmarkstilleggService = mockk(),
        snikeIKøenService = mockk(),
    )
}

private fun mockEfSakRestClient(internPeriodeOvergangsstønadNyBehandling: List<InternPeriodeOvergangsstønad>): EfSakRestClient {
    val efSakRestClient = mockk<EfSakRestClient>()
    every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } answers {
        EksternePerioderResponse(
            internPeriodeOvergangsstønadNyBehandling.map {
                EksternPeriode(
                    personIdent = it.personIdent,
                    fomDato = it.fomDato,
                    tomDato = it.tomDato,
                    datakilde = Datakilde.EF,
                )
            },
        )
    }
    return efSakRestClient
}

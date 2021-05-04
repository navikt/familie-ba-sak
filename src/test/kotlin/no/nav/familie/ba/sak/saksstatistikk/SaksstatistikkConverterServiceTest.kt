package no.nav.familie.ba.sak.saksstatistikk

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRepository
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.saksstatistikk.domene.SaksstatistikkMellomlagring
import no.nav.familie.ba.sak.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.saksstatistikk.domene.SaksstatistikkMellomlagringType
import no.nav.familie.ba.sak.vedtak.producer.KafkaProducer
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.springframework.core.io.ClassPathResource


@TestInstance(Lifecycle.PER_CLASS)
class SaksstatistikkConverterServiceTest {

    lateinit var saksstatistikkConverterService: SaksstatistikkConverterService
    lateinit var behandling: Behandling

    @BeforeAll
    fun init() {

        val mockSaksstatistikkConverter = mockk<SaksstatistikkConverter>()
        val mockSaksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository = mockk()
        val mockBehandlingService: BehandlingService = mockk()
        val mockFeatureToggleService: FeatureToggleService = mockk(relaxed = true)
        val mockFagsakRepository: FagsakRepository = mockk()

        every {
            mockSaksstatistikkMellomlagringRepository.finnAlleSomIkkeErResendt(SaksstatistikkMellomlagringType.SAK)
        } returns listOf(
            SaksstatistikkMellomlagring(
                funksjonellId = "funksjonellID",
                kontraktVersjon = "test",
                json = "{\"sakId\": \"100\"}",
                type = SaksstatistikkMellomlagringType.SAK
            ),
            SaksstatistikkMellomlagring(
                funksjonellId = "funksjonellID",
                kontraktVersjon = "test",
                json = "{\"sakId\": \"100\"}",
                type = SaksstatistikkMellomlagringType.SAK
            ),
            SaksstatistikkMellomlagring(
                funksjonellId = "funksjonellID",
                kontraktVersjon = "test2",
                json = "{\"sakId\": \"100\"}",
                type = SaksstatistikkMellomlagringType.SAK
            ),
            SaksstatistikkMellomlagring(
                funksjonellId = "funksjonellID",
                kontraktVersjon = "test",
                json = "{\"sakId\": \"10\"}",
                type = SaksstatistikkMellomlagringType.SAK
            ),
            SaksstatistikkMellomlagring(
                funksjonellId = "funksjonellID",
                kontraktVersjon = "test",
                json = "{\"sakId\": \"100\"}",
                type = SaksstatistikkMellomlagringType.SAK
            ),
        )

        every {
            mockSaksstatistikkMellomlagringRepository.finnAlleSomIkkeErResendt(SaksstatistikkMellomlagringType.BEHANDLING)
        } returns listOf(
            SaksstatistikkMellomlagring(
                funksjonellId = "funksjonellID",
                kontraktVersjon = "test",
                json = "{\"behandlingId\": \"100\"}",
                type = SaksstatistikkMellomlagringType.BEHANDLING
            ),
            SaksstatistikkMellomlagring(
                funksjonellId = "funksjonellID",
                kontraktVersjon = "test",
                json = "{\"behandlingId\": \"100\"}",
                type = SaksstatistikkMellomlagringType.BEHANDLING
            ),
            SaksstatistikkMellomlagring(
                funksjonellId = "funksjonellID",
                kontraktVersjon = "test2",
                json = "{\"behandlingId\": \"100\"}",
                type = SaksstatistikkMellomlagringType.BEHANDLING
            ),
            SaksstatistikkMellomlagring(
                funksjonellId = "funksjonellID",
                kontraktVersjon = "test",
                json = "{\"behandlingId\": \"10\"}",
                type = SaksstatistikkMellomlagringType.BEHANDLING
            ),
            SaksstatistikkMellomlagring(
                funksjonellId = "funksjonellID",
                kontraktVersjon = "test",
                json = "{\"behandlingId\": \"100\"}",
                type = SaksstatistikkMellomlagringType.BEHANDLING
            ),
        )


        every { mockSaksstatistikkMellomlagringRepository.save(any()) } returns SaksstatistikkMellomlagring(
            funksjonellId = "funksjonellID",
            kontraktVersjon = "test",
            json = "{\"sakId\": \"100\"}",
            type = SaksstatistikkMellomlagringType.SAK
        )

        behandling = lagBehandling().copy(id = 100, skalBehandlesAutomatisk = true)

        every { mockFagsakRepository.finnFagsak(100) } returns behandling.fagsak
        every { mockFagsakRepository.finnFagsak(10) } returns null
        every { mockBehandlingService.hent(100)} returns behandling
        every { mockFeatureToggleService.isEnabled("familie-ba-sak.skal-konvertere-saksstatistikk", false) } returns true

        every { mockSaksstatistikkConverter.konverterSakTilSisteKontraktVersjon(any())} returns
                sakstatistikkObjectMapper.readValue(lesFil("sak/sakstatistikk-sak-2.0_20210427132344_d9066f5.json"), SakDVH::class.java)
        every { mockSaksstatistikkConverter.konverterBehandlingTilSisteKontraktversjon(any(), behandling)} returns
                sakstatistikkObjectMapper.readValue(lesFil("behandling/2.0_20210427132344_d9066f5.json"), BehandlingDVH::class.java)


        saksstatistikkConverterService = SaksstatistikkConverterService(
            mockSaksstatistikkMellomlagringRepository,
            mockBehandlingService,
            mockFeatureToggleService,
            mockFagsakRepository,
            mockSaksstatistikkConverter
        )
    }


    @Test
    fun konverterAlleSaker() {
        val response = saksstatistikkConverterService.konverterSakerTilSisteKontrakt()
        assertThat(response.antallKlarTilKonvertering).isEqualTo(5)
        assertThat(response.antallIkkeSendt).isEqualTo(1)
        assertThat(response.antallAvHverVersjonKonvertert["test"]).isEqualTo(3)
        assertThat(response.antallAvHverVersjonKonvertert["test2"]).isEqualTo(1)
        assertThat(response.antallSendtTilKafka).isEqualTo(4)
    }


    @Test
    fun konverterAlleBehandlinger() {
        val response = saksstatistikkConverterService.konverterBehandlingTilSisteKontrakt()
        assertThat(response.antallKlarTilKonvertering).isEqualTo(5)
        assertThat(response.antallIkkeSendt).isEqualTo(1)
        assertThat(response.antallAvHverVersjonKonvertert["test"]).isEqualTo(3)
        assertThat(response.antallAvHverVersjonKonvertert["test2"]).isEqualTo(1)
        assertThat(response.antallSendtTilKafka).isEqualTo(4)
    }

    private fun lesFil(filnavn: String): String {
        return ClassPathResource("dvh/$filnavn").url.readText()
    }
}
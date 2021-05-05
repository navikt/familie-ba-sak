package no.nav.familie.ba.sak.saksstatistikk

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.lagVedtakBegrunnesle
import no.nav.familie.ba.sak.saksstatistikk.domene.SaksstatistikkMellomlagring
import no.nav.familie.ba.sak.saksstatistikk.domene.SaksstatistikkMellomlagringType
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.springframework.core.io.ClassPathResource
import java.time.LocalDate


@TestInstance(Lifecycle.PER_CLASS)
class SaksstatistikkConverterTest {

    lateinit var saksstatistikkConverter: SaksstatistikkConverter
    lateinit var behandling: Behandling

    @BeforeAll
    fun init() {
        val mockkVedtakService = mockk<VedtakService>()
        val mockTotrinnskontrollService = mockk<TotrinnskontrollService>()

        behandling = lagBehandling().copy(id = 1000, skalBehandlesAutomatisk = true)
        every { mockTotrinnskontrollService.hentAktivForBehandling(any()) } returns Totrinnskontroll(
            0,
            behandling,
            true,
            "Sak Behandler",
            "sak@behandler",
            "Be Slutter",
            "be@slutter",
            false
        )

        val vedtakBegrunnelse = lagVedtakBegrunnesle(
            vedtakBegrunnelse = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOR_HOS_SØKER,
            fom = LocalDate.of(2020, 10, 1),
            tom = LocalDate.of(2030, 11, 1,
            )
        )
        val vedtak = lagVedtak(behandling, mutableSetOf(vedtakBegrunnelse))

        every { mockkVedtakService.hentAktivForBehandling(any()) } returns vedtak
        saksstatistikkConverter = SaksstatistikkConverter(mockkVedtakService, mockTotrinnskontrollService)
    }


    @Test
    fun konverterSakerMedKontraktversjonerIProd() {
        val sakerSomSkalKonverteresTilNyesteKontrakt = ClassPathResource("dvh/sak").file.listFiles()
        assertThat(sakerSomSkalKonverteresTilNyesteKontrakt).hasSize(4)

        sakerSomSkalKonverteresTilNyesteKontrakt.forEach {
            val sakDVH = saksstatistikkConverter.konverterSakTilSisteKontraktVersjon(it.readText())
            assertThat(
                sakstatistikkObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sakDVH)
            ).isEqualToIgnoringWhitespace(lesFil("sak/sakstatistikk-sak-2.0_20210427132344_d9066f5.json"))
        }
    }

    @Test
    fun `konverter behandling 2_0_20201217113247_876a253 til 2_0_20210427132344_d9066f5`() {

        val sm = SaksstatistikkMellomlagring(
            funksjonellId = "funksjonellId",
            type = SaksstatistikkMellomlagringType.BEHANDLING,
            json = lesFil("behandling/2.0_20201217113247_876a253.json"),
            kontraktVersjon = "2.0_20201217113247_876a253"
        )

        val behandlingDVH = saksstatistikkConverter.konverterBehandlingTilSisteKontraktversjon(sm, behandling)
        println(sakstatistikkObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(behandlingDVH))

        assertThat(behandlingDVH.utenlandstilsnitt).isEqualTo("NASJONAL")
        assertThat(behandlingDVH.behandlingKategori).isEqualTo("ORDINÆR")
        assertThat(behandlingDVH.behandlingUnderkategori).isNullOrEmpty()
        assertThat(behandlingDVH.behandlingAarsak).isEqualTo("SØKNAD")
        assertThat(behandlingDVH.automatiskBehandlet).isTrue

        assertThat(behandlingDVH.resultatBegrunnelser).hasSize(1)
        assertThat(behandlingDVH.resultatBegrunnelser.first().fom).isEqualTo(LocalDate.of(2020, 10, 1))
        assertThat(behandlingDVH.resultatBegrunnelser.first().tom).isEqualTo(LocalDate.of(2030, 11, 1))
        assertThat(behandlingDVH.resultatBegrunnelser.first().type).isEqualTo("INNVILGELSE")
        assertThat(behandlingDVH.resultatBegrunnelser.first().vedtakBegrunnelse).isEqualTo("INNVILGET_BOR_HOS_SØKER")
        assertThat(
            sakstatistikkObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(behandlingDVH)
        ).isEqualToIgnoringWhitespace(lesFil("behandling/2.0_20210427132344_d9066f5.json"))
    }

    @Test
    fun `konverter behandling 2_0_20201217113247_876a253 til 2_0_20210427132344_d9066f5 hvor dato er null`() {

        val sm = SaksstatistikkMellomlagring(
            funksjonellId = "funksjonellId",
            type = SaksstatistikkMellomlagringType.BEHANDLING,
            json = lesFil("behandling/2.0_20201217113247_876a253_vedtaksDato_null.json"),
            kontraktVersjon = "2.0_20201217113247_876a253"
        )

        val behandlingDVH = saksstatistikkConverter.konverterBehandlingTilSisteKontraktversjon(sm, behandling)
        println(sakstatistikkObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(behandlingDVH))

        assertThat(behandlingDVH.vedtaksDato).isNull()
        assertThat(behandlingDVH.resultatBegrunnelser).hasSize(1)
        assertThat(behandlingDVH.resultatBegrunnelser.first().fom).isEqualTo(LocalDate.of(2020, 10, 1))
    }


    @Test
    fun `konverter behandling 2_0_20210128104331_9bd0bd2 til 2_0_20210427132344_d9066f5`() {

        val sm = SaksstatistikkMellomlagring(
            funksjonellId = "funksjonellId",
            type = SaksstatistikkMellomlagringType.BEHANDLING,
            json = lesFil("behandling/2.0_20210128104331_9bd0bd2.json"),
            kontraktVersjon = "2.0_20210128104331_9bd0bd2"
        )

        val behandlingDVH = saksstatistikkConverter.konverterBehandlingTilSisteKontraktversjon(sm, behandling)
        println(sakstatistikkObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(behandlingDVH))

        assertThat(behandlingDVH.resultatBegrunnelser).hasSize(1)
        assertThat(behandlingDVH.resultatBegrunnelser.first().fom).isEqualTo(LocalDate.of(2020, 10, 1))
        assertThat(behandlingDVH.resultatBegrunnelser.first().tom).isEqualTo(LocalDate.of(2030, 11, 1))
        assertThat(behandlingDVH.resultatBegrunnelser.first().type).isEqualTo("INNVILGELSE")
        assertThat(behandlingDVH.resultatBegrunnelser.first().vedtakBegrunnelse).isEqualTo("INNVILGET_BOR_HOS_SØKER")
        assertThat(
            sakstatistikkObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(behandlingDVH)
        ).isEqualToIgnoringWhitespace(lesFil("behandling/2.0_20210427132344_d9066f5.json"))
    }

    @Test
    fun `konverter behandling 2_0_20210211132003_f81111d til 2_0_20210427132344_d9066f5`() {

        val sm = SaksstatistikkMellomlagring(
            funksjonellId = "funksjonellId",
            type = SaksstatistikkMellomlagringType.BEHANDLING,
            json = lesFil("behandling/2.0_20210211132003_f81111d.json"),
            kontraktVersjon = "2.0_20210211132003_f81111d"
        )

        val behandlingDVH = saksstatistikkConverter.konverterBehandlingTilSisteKontraktversjon(sm, behandling)
        println(sakstatistikkObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(behandlingDVH))

        assertThat(
            sakstatistikkObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(behandlingDVH)
        ).isEqualToIgnoringWhitespace(lesFil("behandling/2.0_20210427132344_d9066f5.json"))

    }

    @Test
    fun `konverter behandling 2_0_20210427132344_d9066f5 til 2_0_20210427132344_d9066f5`() {

        val sm = SaksstatistikkMellomlagring(
            funksjonellId = "funksjonellId",
            type = SaksstatistikkMellomlagringType.BEHANDLING,
            json = lesFil("behandling/2.0_20210427132344_d9066f5.json"),
            kontraktVersjon = "2.0_20210427132344_d9066f5"
        )

        val behandlingDVH = saksstatistikkConverter.konverterBehandlingTilSisteKontraktversjon(sm, behandling)
        println(sakstatistikkObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(behandlingDVH))

        assertThat(
            sakstatistikkObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(behandlingDVH)
        ).isEqualToIgnoringWhitespace(lesFil("behandling/2.0_20210427132344_d9066f5.json"))

    }



    private fun lesFil(filnavn: String): String {
        return ClassPathResource("dvh/$filnavn").url.readText()
    }
}
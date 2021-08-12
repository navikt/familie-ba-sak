package no.nav.familie.ba.sak.statistikk.saksstatistikk

import com.fasterxml.jackson.databind.JsonNode
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.lagVedtakBegrunnesle
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkConverterService.Companion.nyesteKontraktversjon
import no.nav.familie.eksterne.kontrakter.saksstatistikk.AktørDVH
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.springframework.core.io.ClassPathResource
import java.time.LocalDate


@TestInstance(Lifecycle.PER_CLASS)
class SaksstatistikkConverterServiceTest {

    lateinit var saksstatistikkConverterService: SaksstatistikkConverterService
    lateinit var behandling: Behandling

    @BeforeAll
    fun init() {
        val personopplysningerService: PersonopplysningerService = mockk()
        every { personopplysningerService.hentPersoninfoEnkel(any()) } returns PersonInfo(
            fødselsdato = LocalDate.of(
                2017,
                3,
                1
            ),
            bostedsadresser = mutableListOf(
                Bostedsadresse(
                    vegadresse = Vegadresse(
                        matrikkelId = 1111,
                        husnummer = null,
                        husbokstav = null,
                        bruksenhetsnummer = null,
                        adressenavn = null,
                        kommunenummer = null,
                        tilleggsnavn = null,
                        postnummer = "2222"
                    )
                )
            )
        )

        val mockBehandlingService = mockk<BehandlingService>()
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
            tom = LocalDate.of(
                2030, 11, 1,
            )
        )
        val vedtak = lagVedtak(behandling, mutableSetOf(vedtakBegrunnelse))

        every { mockkVedtakService.hentAktivForBehandling(any()) } returns vedtak
        every { mockBehandlingService.hent(1000) } returns behandling

        saksstatistikkConverterService = SaksstatistikkConverterService(
            personopplysningerService,
            mockBehandlingService,
            mockkVedtakService,
            mockTotrinnskontrollService
        )
    }


    @Test
    fun konverterSakUtenFunksjonellId() {
        val sakDVH =
            saksstatistikkConverterService.konverterSakTilSisteKontraktVersjon(lesFil("sak/saksstatistikk-sak-2.0_20201012132018_dc05978.json"))
        println(sakstatistikkObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sakDVH))

        assertThat(sakDVH.sakId).isEqualTo("10000")
        assertThat(sakDVH.aktorId).isEqualTo(1000510666000)
        assertThat(sakDVH.funksjonellTid.toString()).isEqualTo("2020-11-04T12:00:40.847304+01:00")
        assertThat(sakDVH.tekniskTid.toString()).isEqualTo("2020-11-04T12:00:40.847311+01:00")
        assertThat(sakDVH.opprettetDato.toString()).isEqualTo("2020-11-04")
        assertThat(sakDVH.funksjonellId).isNotEmpty
        assertThat(sakDVH.sakStatus).isEqualTo("OPPRETTET")
        assertThat(sakDVH.ytelseType).isEqualTo("BARNETRYGD")
        assertThat(sakDVH.versjon).isEqualTo(nyesteKontraktversjon())
        assertThat(sakDVH.aktorer).hasSize(1).contains(AktørDVH(1000510666000, "SØKER"))
        assertThat(sakDVH.bostedsland).isEqualTo("NO")
    }

    @Test
    fun konverterSakMedFunksjonellId() {
        val sakDVH =
            saksstatistikkConverterService.konverterSakTilSisteKontraktVersjon(lesFil("sak/saksstatistikk-sak-2.0_20201110145948_2c83b39.json"))
        println(sakstatistikkObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sakDVH))

        assertThat(sakDVH.sakId).isEqualTo("10000")
        assertThat(sakDVH.aktorId).isEqualTo(1000510666000)
        assertThat(sakDVH.funksjonellTid.toString()).isEqualTo("2020-11-04T12:00:40.847304+01:00")
        assertThat(sakDVH.tekniskTid.toString()).isEqualTo("2020-11-04T12:00:40.847311+01:00")
        assertThat(sakDVH.opprettetDato.toString()).isEqualTo("2020-11-04")
        assertThat(sakDVH.funksjonellId).isEqualTo("funksjonellId")
        assertThat(sakDVH.sakStatus).isEqualTo("OPPRETTET")
        assertThat(sakDVH.ytelseType).isEqualTo("BARNETRYGD")
        assertThat(sakDVH.versjon).isEqualTo(nyesteKontraktversjon())
        assertThat(sakDVH.aktorer).hasSize(1).contains(AktørDVH(1000510666000, "SØKER"))
        assertThat(sakDVH.bostedsland).isEqualTo("SE")
    }


    @Test
    fun `konverter behandling 2_0_20201012132018_dc05978 til 2_0_20210427132344_d9066f5`() {
        val behandlingDVH =
            saksstatistikkConverterService.konverterBehandlingTilSisteKontraktVersjon(lesFil("behandling/2.0_20201012132018_dc05978.json"))


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
        ).isEqualToIgnoringWhitespace(filTilString("behandling/2.0_20210427132344_d9066f5.json"))
    }

    @Test
    fun `konverter behandling 2_0_20201110145948_2c83b39 til 2_0_20210427132344_d9066f5`() {
        val behandlingDVH =
            saksstatistikkConverterService.konverterBehandlingTilSisteKontraktVersjon(lesFil("behandling/2.0_20201110145948_2c83b39.json"))


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
        ).isEqualToIgnoringWhitespace(filTilString("behandling/2.0_20210427132344_d9066f5.json"))
    }


    @Test
    fun `konverter behandling 2_0_20201217113247_876a253 til 2_0_20210427132344_d9066f5`() {

        val behandlingDVH = saksstatistikkConverterService.konverterBehandlingTilSisteKontraktVersjon(lesFil("behandling/2.0_20201217113247_876a253.json"))
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
        ).isEqualToIgnoringWhitespace(filTilString("behandling/2.0_20210427132344_d9066f5.json"))
    }

    @Test
    fun `konverter behandling 2_0_20201217113247_876a253 til 2_0_20210427132344_d9066f5 hvor dato er null`() {

        val behandlingDVH = saksstatistikkConverterService.konverterBehandlingTilSisteKontraktVersjon(lesFil("behandling/2.0_20201217113247_876a253_vedtaksDato_null.json"))
        println(sakstatistikkObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(behandlingDVH))

        assertThat(behandlingDVH.vedtaksDato).isNull()
        assertThat(behandlingDVH.resultatBegrunnelser).hasSize(1)
        assertThat(behandlingDVH.resultatBegrunnelser.first().fom).isEqualTo(LocalDate.of(2020, 10, 1))
    }

    @Test
    fun `konverter behandling med versjon som ikke støttes skal gi feil`() {
        Assertions.assertThrows(IllegalStateException::class.java) {
            saksstatistikkConverterService.konverterBehandlingTilSisteKontraktVersjon(lesFil("behandling/2.0_20210427132344_d9066f5.json"))
        }

    }

    private fun lesFil(filnavn: String): JsonNode {
        return sakstatistikkObjectMapper.readTree(ClassPathResource("dvh/$filnavn").url.readText())
    }

    private fun filTilString(filnavn: String): String {
        return ClassPathResource("dvh/$filnavn").url.readText()
    }
}
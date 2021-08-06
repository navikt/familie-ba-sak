package no.nav.familie.ba.sak.statistikk.saksstatistikk

import com.fasterxml.jackson.databind.JsonNode
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkConverterService.Companion.nyesteKontraktversjon
import no.nav.familie.eksterne.kontrakter.saksstatistikk.AktørDVH
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.springframework.core.io.ClassPathResource
import java.time.LocalDate


@TestInstance(Lifecycle.PER_CLASS)
class SaksstatistikkConverterServiceTest {

    lateinit var saksstatistikkConverterService: SaksstatistikkConverterService

    @BeforeAll
    fun init() {
        val personopplysningerService: PersonopplysningerService = mockk()
        every { personopplysningerService.hentPersoninfo(any()) } returns PersonInfo(
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

        saksstatistikkConverterService = SaksstatistikkConverterService(personopplysningerService)
    }


    @Test
    fun konverterSakUtenFunksjonellId() {
        val sakDVH = saksstatistikkConverterService.konverterSakTilSisteKontraktVersjon(lesFil("sak/saksstatistikk-sak-2.0_20201012132018_dc05978.json"))
        println(sakstatistikkObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sakDVH))

        assertThat(sakDVH.sakId).isEqualTo("10000")
        assertThat(sakDVH.aktorId).isEqualTo(1000510666000)
        assertThat(sakDVH.funksjonellTid.toString()).isEqualTo("2020-11-04T12:00:40.847304+01:00")
        assertThat(sakDVH.tekniskTid.toString()).isEqualTo("2020-11-04T12:00:40.847311+01:00")
        assertThat(sakDVH.opprettetDato.toString()).isEqualTo("2020-11-04")
        assertThat(sakDVH.funksjonellId).isNotEmpty()
        assertThat(sakDVH.sakStatus).isEqualTo("OPPRETTET")
        assertThat(sakDVH.ytelseType).isEqualTo("BARNETRYGD")
        assertThat(sakDVH.versjon).isEqualTo(nyesteKontraktversjon())
        assertThat(sakDVH.aktorer).hasSize(1).contains(AktørDVH(1000510666000, "SØKER"))
        assertThat(sakDVH.bostedsland).isEqualTo("NO")
    }

    @Test
    fun konverterSakMedFunksjonellId() {
        val sakDVH = saksstatistikkConverterService.konverterSakTilSisteKontraktVersjon(lesFil("sak/saksstatistikk-sak-2.0_20201110145948_2c83b39.json"))
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




    private fun lesFil(filnavn: String): JsonNode {
        return sakstatistikkObjectMapper.readTree(ClassPathResource("dvh/$filnavn").url.readText())
    }
}
package no.nav.familie.ba.sak.kjerne.institusjon

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.samhandlereInfoMock
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.KodeverkService
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.integrasjoner.samhandler.SamhandlerKlient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.kontrakter.ba.tss.SamhandlerAdresse
import no.nav.familie.kontrakter.ba.tss.SøkSamhandlerInfo
import no.nav.familie.kontrakter.felles.organisasjon.Gyldighetsperiode
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import no.nav.familie.kontrakter.felles.organisasjon.OrganisasjonAdresse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDate

internal class SamhandlerControllerTest {
    lateinit var samhandlerController: SamhandlerController
    private val samhandlerKlientMock: SamhandlerKlient = mockk()
    private val institusjonRepository: InstitusjonRepository = mockk()
    private val institusjonsinfoRepository: InstitusjonsinfoRepository = mockk()
    private val organisasjonService: OrganisasjonService = mockk()
    private val kodeverkService: KodeverkService = mockk()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private lateinit var institusjonService: InstitusjonService

    @BeforeEach
    fun setUp() {
        institusjonService = InstitusjonService(mockk(), samhandlerKlientMock, institusjonRepository, institusjonsinfoRepository, organisasjonService, kodeverkService, behandlingHentOgPersisterService)
        samhandlerController = SamhandlerController(institusjonService = institusjonService)
        clearMocks(samhandlerKlientMock)
    }

    @Nested
    inner class HentSamhandlerDataForOrganisasjonFraTssOgEreg {
        @Test
        fun `Skal hente samhandlerinformasjon med adresse fra TSS, hvis det ikke finnes adresse i ereg `() {
            every { samhandlerKlientMock.hentSamhandler(any()) } returns samhandlereInfoMock.first()
            every { organisasjonService.hentOrganisasjon(any()) } returns Organisasjon(samhandlereInfoMock.first().orgNummer!!, "Testinstitusjon")

            val samhandlerInfo = samhandlerController.hentSamhandlerDataForOrganisasjonFraTssOgEreg(samhandlereInfoMock.first().orgNummer!!)
            assertThat(samhandlerInfo.data).isNotNull()
            assertThat(samhandlerInfo.data!!.tssEksternId).isEqualTo("80000999999")
        }

        @Test
        fun `Skal hente tss ekstern id fra samhandlerinformasjon til TSS, men bruke navn og adresse fra ereg `() {
            every { samhandlerKlientMock.hentSamhandler(any()) } returns samhandlereInfoMock.first()
            every { kodeverkService.hentPoststed(any()) } returns "Oslo"
            every { organisasjonService.hentOrganisasjon(any()) } returns
                Organisasjon(
                    organisasjonsnummer = samhandlereInfoMock.first().orgNummer!!,
                    navn = "Et annet navn",
                    adresse =
                        OrganisasjonAdresse(
                            type = "Forretningsaddresse",
                            adresselinje1 = "Fyrstikkalleen 1",
                            adresselinje2 = null,
                            adresselinje3 = "7C",
                            postnummer = "0661",
                            kommunenummer = "0301",
                            gyldighetsperiode = Gyldighetsperiode(fom = LocalDate.now(), tom = null),
                        ),
                )

            val samhandlerInfo = samhandlerController.hentSamhandlerDataForOrganisasjonFraTssOgEreg(samhandlereInfoMock.first().orgNummer!!)
            assertThat(samhandlerInfo.data).isNotNull()
            assertThat(samhandlerInfo.data!!.tssEksternId).isEqualTo("80000999999")
            assertThat(samhandlerInfo.data!!.navn).isEqualTo("Et annet navn")
            assertThat(samhandlerInfo.data!!.adresser).hasSize(1).contains(
                SamhandlerAdresse(
                    adresselinjer = listOf("Fyrstikkalleen 1", "7C"),
                    postNr = "0661",
                    postSted = "Oslo",
                    adresseType = "Forretningsaddresse",
                    kommunenummer = "0301",
                    gyldighetsperiode =
                        no.nav.familie.kontrakter.ba.tss
                            .Gyldighetsperiode(LocalDate.now(), null),
                ),
            )
        }

        @Test
        fun `Kaster feilmelding hvis det ikke fins organisasjon med gitt orgnr`() {
            every { samhandlerKlientMock.hentSamhandler(any()) } throws HttpClientErrorException(HttpStatus.NOT_FOUND)
            val feil =
                assertThrows<FunksjonellFeil> {
                    samhandlerController.hentSamhandlerDataForOrganisasjonFraTssOgEreg("123456789")
                }
            assertThat(feil.message).isEqualTo("Finner ikke institusjon. Kontakt NØS for å opprette TSS-ident.")
        }
    }

    @Nested
    inner class SøkSamhandlerinfoFraNavn {
        @Test
        fun `Søk etter samhandlere skal returnere samhandlere på navn og ikke hente flere hvis det ikke finnes flere samhandlere`() {
            every { samhandlerKlientMock.søkSamhandlere("BUFETAT", null, null, 0) } returns
                SøkSamhandlerInfo(
                    false,
                    samhandlereInfoMock,
                )

            val samhandlerInfo =
                samhandlerController.søkSamhandlerinfoFraNavn(SøkSamhandlerInfoRequest("Bufetat", null, null))
            assertThat(samhandlerInfo.data).isNotNull()
            assertThat(samhandlerInfo.data).hasSize(2)
            assertThat(samhandlerInfo.data?.get(0)?.tssEksternId).isEqualTo("80000999999")
            assertThat(samhandlerInfo.data?.get(1)?.tssEksternId).isEqualTo("80000888888")
            verify(exactly = 1) { samhandlerKlientMock.søkSamhandlere(any(), any(), any(), any()) }
        }

        @Test
        fun `Søk etter samhandlere skal returnere samhandlere på navn og slå sammen resultatene fra alle sidene ved mer enn 1 side`() {
            every { samhandlerKlientMock.søkSamhandlere("BUFETAT", null, null, 0) } returns
                SøkSamhandlerInfo(
                    true,
                    listOf(samhandlereInfoMock.get(0)),
                )

            every { samhandlerKlientMock.søkSamhandlere("BUFETAT", null, null, 1) } returns
                SøkSamhandlerInfo(
                    false,
                    listOf(samhandlereInfoMock.get(1)),
                )

            val samhandlerInfo =
                samhandlerController.søkSamhandlerinfoFraNavn(SøkSamhandlerInfoRequest("Bufetat", null, null))
            assertThat(samhandlerInfo.data).isNotNull()
            assertThat(samhandlerInfo.data).hasSize(2)
            assertThat(samhandlerInfo.data?.get(0)?.tssEksternId).isEqualTo("80000999999")
            assertThat(samhandlerInfo.data?.get(1)?.tssEksternId).isEqualTo("80000888888")
            verify(exactly = 2) { samhandlerKlientMock.søkSamhandlere(any(), any(), any(), any()) }
        }
    }

    @Nested
    inner class HentSamhandlerDataForBehandling {
        @Test
        fun `skal returnere samhandlerdata fra institusjonsinfo for behandling`() {
            every { institusjonsinfoRepository.findByBehandlingId(any()) } returns
                Institusjonsinfo(
                    id = 42,
                    institusjon = Institusjon(orgNummer = "123456789", tssEksternId = "tssId"),
                    behandlingId = 42L,
                    type = "Forretningsadresse",
                    navn = "Institusjonsnavn",
                    adresselinje1 = "adresselinje1",
                    adresselinje2 = null,
                    adresselinje3 = "adresselinje3",
                    postnummer = "1732",
                    poststed = "Høtten",
                    kommunenummer = "0301",
                    gyldighetsperiode = DatoIntervallEntitet(LocalDate.now(), null),
                )
            val samhandlerInfo = samhandlerController.hentSamhandlerDataForBehandling(42L)
            assertThat(samhandlerInfo.data).isNotNull()
            assertThat(samhandlerInfo.data?.orgNummer).isEqualTo("123456789")
            assertThat(samhandlerInfo.data?.tssEksternId).isEqualTo("tssId")
            assertThat(samhandlerInfo.data?.navn).isEqualTo("Institusjonsnavn")
            assertThat(samhandlerInfo.data?.adresser).hasSize(1)
            val adresse = samhandlerInfo.data?.adresser?.first()!!
            assertThat(adresse.kommunenummer).isEqualTo("0301")
            assertThat(adresse.postNr).isEqualTo("1732")
            assertThat(adresse.postSted).isEqualTo("Høtten")
            assertThat(adresse.adresseType).isEqualTo("Forretningsadresse")
            assertThat(adresse.adresselinjer).hasSize(2).containsExactly("adresselinje1", "adresselinje3")
        }

        @Test
        fun `skal returnere samhandlerdata fra tss opg ereg hvis mangler for behandling`() {
            every { institusjonsinfoRepository.findByBehandlingId(any()) } returns null
            every { behandlingHentOgPersisterService.hent(42) } returns
                lagBehandling(lagFagsak(institusjon = Institusjon(orgNummer = "123456789", tssEksternId = "tssIdFraFagsak")))
            every { samhandlerKlientMock.hentSamhandler(any()) } returns samhandlereInfoMock.first()
            every { organisasjonService.hentOrganisasjon(any()) } returns Organisasjon(samhandlereInfoMock.first().orgNummer!!, "Testinstitusjon")

            val samhandlerInfo = samhandlerController.hentSamhandlerDataForBehandling(42L)
        }

        @Test
        fun `skal kaste feil hvis ingen eksisterende institusjonsinfo og ingen institusjon fra fagsak`() {
            every { institusjonsinfoRepository.findByBehandlingId(any()) } returns null
            every { behandlingHentOgPersisterService.hent(42) } returns
                lagBehandling(lagFagsak())

            assertThrows<FunksjonellFeil> { samhandlerController.hentSamhandlerDataForBehandling(42L) }
        }
    }
}

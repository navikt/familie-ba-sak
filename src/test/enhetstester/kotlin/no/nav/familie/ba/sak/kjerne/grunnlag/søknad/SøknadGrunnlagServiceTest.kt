package no.nav.familie.ba.sak.kjerne.grunnlag.søknad

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.BehandlingUnderkategoriDTO
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Test
import org.hamcrest.CoreMatchers.`is` as Is

internal class SøknadGrunnlagServiceTest {
    private val søknadGrunnlagRepository = mockk<SøknadGrunnlagRepository>()
    private val personidentService = mockk<PersonidentService>()
    private val persongrunnlagService = mockk<PersongrunnlagService>()

    private val søknadGrunnlagService =
        SøknadGrunnlagService(
            søknadGrunnlagRepository = søknadGrunnlagRepository,
            personidentService = personidentService,
            persongrunnlagService = persongrunnlagService,
        )

    @Test
    fun `finnPersonerFremstiltKravFor skal returnere tom liste dersom behandlingen ikke er søknad, fødselshendelse eller manuell migrering`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.DØDSFALL_BRUKER)
        every { søknadGrunnlagService.hentAktivSøknadDto(behandling.id) } returns null

        val personerFramstiltForKrav =
            søknadGrunnlagService.finnPersonerFremstiltKravFor(
                behandling = behandling,
                forrigeBehandling = null,
            )

        assertThat(personerFramstiltForKrav, Is(emptyList()))
    }

    @Test
    fun `finnPersonerFremstiltKravFor skal returnere aktør som person framstilt krav for dersom det er søkt for utvidet barnetrygd`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)

        val barnSomIkkeErKryssetAvFor =
            BarnMedOpplysninger(
                ident = randomFnr(),
                navn = "barn1",
                inkludertISøknaden = false,
                erFolkeregistrert = true,
            )

        val søknadDto =
            SøknadDTO(
                underkategori = BehandlingUnderkategoriDTO.UTVIDET,
                barnaMedOpplysninger = listOf(barnSomIkkeErKryssetAvFor),
                søkerMedOpplysninger = mockk(),
                endringAvOpplysningerBegrunnelse = "",
            )

        every { søknadGrunnlagService.hentAktivSøknadDto(behandling.id) } returns søknadDto

        val personerFramstiltForKrav =
            søknadGrunnlagService.finnPersonerFremstiltKravFor(
                behandling = behandling,
                forrigeBehandling = null,
            )

        assertThat(personerFramstiltForKrav.single(), Is(behandling.fagsak.aktør))
    }

    @Test
    fun `finnPersonerFremstiltKravFor skal returnere aktør som person framstilt krav for dersom det er søkt for utvidet barnetrygd og barn som er krysset for`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
        val barn = lagPerson(type = PersonType.BARN)

        val barnSomErKryssetAvFor =
            BarnMedOpplysninger(
                ident = barn.aktør.aktivFødselsnummer(),
                navn = "barn1",
                inkludertISøknaden = true,
                erFolkeregistrert = true,
            )

        val søknadDto =
            SøknadDTO(
                underkategori = BehandlingUnderkategoriDTO.UTVIDET,
                barnaMedOpplysninger = listOf(barnSomErKryssetAvFor),
                søkerMedOpplysninger = mockk(),
                endringAvOpplysningerBegrunnelse = "",
            )

        every { personidentService.hentAktør(barn.aktør.aktivFødselsnummer()) } returns barn.aktør
        every { søknadGrunnlagService.hentAktivSøknadDto(behandling.id) } returns søknadDto

        val personerFramstiltForKrav =
            søknadGrunnlagService.finnPersonerFremstiltKravFor(
                behandling = behandling,
                forrigeBehandling = null,
            )

        assertThat(personerFramstiltForKrav.size, Is(2))
        assertThat(personerFramstiltForKrav, containsInAnyOrder(behandling.fagsak.aktør, barn.aktør))
    }

    @Test
    fun `finnPersonerFremstiltKravFor skal bare returnere barn som er folkeregistret og krysset av på søknad`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
        val barn1Fnr = randomFnr()
        val mocketAktør = mockk<Aktør>()

        val barnSomErKryssetAvFor =
            BarnMedOpplysninger(
                ident = barn1Fnr,
                navn = "barn1",
                inkludertISøknaden = true,
                erFolkeregistrert = true,
            )

        val barnSomIkkeErKryssetAvFor =
            BarnMedOpplysninger(
                ident = randomFnr(),
                navn = "barn2",
                inkludertISøknaden = false,
                erFolkeregistrert = true,
            )

        val barnSomErKryssetAvForMenIkkeFolkeregistrert =
            BarnMedOpplysninger(
                ident = randomFnr(),
                navn = "barn3",
                inkludertISøknaden = true,
                erFolkeregistrert = false,
            )

        val søknadDto =
            SøknadDTO(
                underkategori = BehandlingUnderkategoriDTO.ORDINÆR,
                barnaMedOpplysninger =
                    listOf(
                        barnSomErKryssetAvFor,
                        barnSomIkkeErKryssetAvFor,
                        barnSomErKryssetAvForMenIkkeFolkeregistrert,
                    ),
                søkerMedOpplysninger = mockk(),
                endringAvOpplysningerBegrunnelse = "",
            )

        every { personidentService.hentAktør(barn1Fnr) } returns mocketAktør
        every { søknadGrunnlagService.hentAktivSøknadDto(behandling.id) } returns søknadDto

        val personerFramstiltForKrav =
            søknadGrunnlagService.finnPersonerFremstiltKravFor(
                behandling = behandling,
                forrigeBehandling = null,
            )

        assertThat(personerFramstiltForKrav.single(), Is(mocketAktør))

        verify(exactly = 1) { personidentService.hentAktør(barn1Fnr) }
    }

    @Test
    fun `finnPersonerFremstiltKravFor skal returnere nye barn dersom behandlingen har fødselshendelse som årsak`() {
        val forrigeBehandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
        val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE)
        val nyttBarn = lagPerson()

        every { persongrunnlagService.finnNyeBarn(behandling, forrigeBehandling) } returns listOf(nyttBarn)
        every { søknadGrunnlagService.hentAktivSøknadDto(behandling.id) } returns null

        val personerFramstiltForKrav =
            søknadGrunnlagService.finnPersonerFremstiltKravFor(
                behandling = behandling,
                forrigeBehandling = forrigeBehandling,
            )

        assertThat(personerFramstiltForKrav.single(), Is(nyttBarn.aktør))

        verify(exactly = 1) { persongrunnlagService.finnNyeBarn(behandling, forrigeBehandling) }
    }

    @Test
    fun `finnPersonerFremstiltKravFor skal returnere eksisterende personer fra persongrunnlaget dersom behandlingen er en manuell migrering`() {
        val behandling =
            lagBehandling(
                årsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            )
        val eksisterendeBarn = lagPerson()
        val eksisterendePersonpplysningGrunnlag =
            PersonopplysningGrunnlag(behandlingId = behandling.id, personer = mutableSetOf(eksisterendeBarn))

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns eksisterendePersonpplysningGrunnlag
        every { søknadGrunnlagService.hentAktivSøknadDto(behandling.id) } returns null

        val personerFramstiltForKrav =
            søknadGrunnlagService.finnPersonerFremstiltKravFor(
                behandling = behandling,
                forrigeBehandling = null,
            )

        assertThat(personerFramstiltForKrav.single(), Is(eksisterendeBarn.aktør))

        verify(exactly = 1) { persongrunnlagService.hentAktivThrows(behandling.id) }
    }

    @Test
    fun `finnPersonerFremstiltKravFor skal ikke returnere duplikater av personer`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
        val barn = lagPerson(type = PersonType.BARN)

        val barnSomErKryssetAvFor =
            BarnMedOpplysninger(
                ident = barn.aktør.aktivFødselsnummer(),
                navn = "barn1",
                inkludertISøknaden = true,
                erFolkeregistrert = true,
            )

        val duplikatBarnSomErKryssetAvFor =
            BarnMedOpplysninger(
                ident = barn.aktør.aktivFødselsnummer(),
                navn = "barn1",
                inkludertISøknaden = true,
                erFolkeregistrert = true,
            )

        val søknadDto =
            SøknadDTO(
                underkategori = BehandlingUnderkategoriDTO.ORDINÆR,
                barnaMedOpplysninger = listOf(barnSomErKryssetAvFor, duplikatBarnSomErKryssetAvFor),
                søkerMedOpplysninger = mockk(),
                endringAvOpplysningerBegrunnelse = "",
            )

        every { personidentService.hentAktør(barn.aktør.aktivFødselsnummer()) } returns barn.aktør
        every { søknadGrunnlagService.hentAktivSøknadDto(behandling.id) } returns søknadDto

        val personerFramstiltForKrav =
            søknadGrunnlagService.finnPersonerFremstiltKravFor(
                behandling = behandling,
                forrigeBehandling = null,
            )

        assertThat(personerFramstiltForKrav.size, Is(1))
        assertThat(personerFramstiltForKrav.single(), Is(barn.aktør))
    }
}

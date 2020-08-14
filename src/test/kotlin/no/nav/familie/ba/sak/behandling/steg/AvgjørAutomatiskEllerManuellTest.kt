package no.nav.familie.ba.sak.behandling.steg

import io.mockk.*
import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.*
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

class AvgjørAutomatiskEllerManuellTest {

    private lateinit var avgjørAutomatiskEllerManuell: AvgjørAutomatiskEllerManuellBehandlingForFødselshendelser
    private lateinit var personopplysningerService: PersonopplysningerService
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    val behandling = lagBehandling().copy(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            opprinnelse = BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE)

    @BeforeEach
    fun setUp() {
        personopplysningerService = mockk()
        personopplysningGrunnlagRepository = mockk()

        avgjørAutomatiskEllerManuell = AvgjørAutomatiskEllerManuellBehandlingForFødselshendelser(
                personopplysningerService,
                personopplysningGrunnlagRepository)
    }

    @Test
    fun `utførStegOgAngiNeste skal kjøre uten feil`() {
        spesifiserMocks(20)

        assertDoesNotThrow {
            avgjørAutomatiskEllerManuell.utførStegOgAngiNeste(behandling, "12345678911")
        }
    }

    private fun spesifiserMocks(morsAlder: Long) {
        val søker = tilfeldigPerson(LocalDate.now().minusYears(morsAlder), PersonType.SØKER).copy(personIdent = PersonIdent("12345678910"))
        val barn = tilfeldigPerson().copy(personIdent = PersonIdent("12345678911"))

        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) } returns lagTestPersonopplysningGrunnlag(1, søker, barn)
        every { personopplysningerService.hentPersoninfoFor(søker.personIdent.ident) } returns PersonInfo(
                LocalDate.now(),
                familierelasjoner = setOf(
                        Familierelasjon(Personident("12345678912"), FAMILIERELASJONSROLLE.FAR),
                        Familierelasjon(Personident("12345678911"), FAMILIERELASJONSROLLE.BARN),
                        Familierelasjon(Personident("12345678913"), FAMILIERELASJONSROLLE.BARN)))
        every { personopplysningerService.hentPersoninfoFor(barn.personIdent.ident) } returns PersonInfo(LocalDate.now())
        every { personopplysningerService.hentPersoninfoFor("12345678913") } returns PersonInfo(LocalDate.now().minusYears(4))
        every { personopplysningerService.hentDødsfall(any()) } returns DødsfallData(false, null)
        every { personopplysningerService.hentVergeData(any()) } returns VergeData(false)
    }
}
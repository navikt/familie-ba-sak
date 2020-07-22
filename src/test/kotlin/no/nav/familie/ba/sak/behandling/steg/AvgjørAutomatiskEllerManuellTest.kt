package no.nav.familie.ba.sak.behandling.steg

import io.mockk.*
import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.*
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalStateException
import java.time.LocalDate

class AvgjørAutomatiskEllerManuellTest {

    private lateinit var avgjørAutomatiskEllerManuell: AvgjørAutomatiskEllerManuellBehandlingForFødselshendelser
    private lateinit var integrasjonClient: IntegrasjonClient
    private lateinit var personopplysningerService: PersonopplysningerService
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var featureToggleService: FeatureToggleService

    val behandling = lagBehandling().copy(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            opprinnelse = BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE)

    @BeforeEach
    fun setUp() {
        integrasjonClient = mockk()
        taskRepository = mockk()
        personopplysningGrunnlagRepository = mockk()
        featureToggleService = mockk()

        avgjørAutomatiskEllerManuell = AvgjørAutomatiskEllerManuellBehandlingForFødselshendelser(
                integrasjonClient,
                personopplysningerService,
                personopplysningGrunnlagRepository,
                taskRepository,
                featureToggleService)
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
        every { featureToggleService.isEnabled(any()) } returns false
    }
}
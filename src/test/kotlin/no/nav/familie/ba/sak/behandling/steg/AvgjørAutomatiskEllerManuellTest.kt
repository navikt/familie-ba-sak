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
import no.nav.familie.ba.sak.integrasjoner.domene.*
import no.nav.familie.ba.sak.logg.LoggService
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
                personopplysningGrunnlagRepository,
                taskRepository,
                featureToggleService)
    }

    @Test
    fun `utførStegOgAngiNeste skal kjøre uten feil`() {
        spesifiserMocks(20)

        assertDoesNotThrow {
            avgjørAutomatiskEllerManuell.utførStegOgAngiNeste(behandling, "data")
        }
    }

    private fun spesifiserMocks(morsAlder: Long) {
        val søker = tilfeldigPerson(LocalDate.now().minusYears(morsAlder), PersonType.SØKER).copy(personIdent = PersonIdent("12345678910"))
        val barn = tilfeldigPerson().copy(personIdent = PersonIdent("12345678911"))

        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) } returns lagTestPersonopplysningGrunnlag(1, søker, barn)
        every { integrasjonClient.hentPersoninfoFor(søker.personIdent.ident) } returns Personinfo(
                LocalDate.now(),
                familierelasjoner = setOf(
                        Familierelasjoner(Personident("12345678912"), FAMILIERELASJONSROLLE.FAR),
                        Familierelasjoner(Personident("12345678911"), FAMILIERELASJONSROLLE.BARN),
                        Familierelasjoner(Personident("12345678913"), FAMILIERELASJONSROLLE.BARN)))
        every { integrasjonClient.hentPersoninfoFor(barn.personIdent.ident) } returns Personinfo(LocalDate.now())
        every { integrasjonClient.hentPersoninfoFor("12345678913") } returns Personinfo(LocalDate.now().minusYears(4))
        every { integrasjonClient.hentDødsfall(any()) } returns DødsfallData(false, null)
        every { integrasjonClient.hentVergeData(any()) } returns VergeData(false)
        every { featureToggleService.isEnabled(any()) } returns false
    }
}
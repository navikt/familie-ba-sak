package no.nav.familie.ba.sak.kjerne.personident

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PersonIdentServiceIntegrasjonsTest(
    @Autowired
    private val aktørIdRepository: AktørIdRepository,
    @Autowired
    private val behandlingRepository: BehandlingRepository,
    @Autowired
    private val fagsakService: FagsakService,
    @Autowired
    private val personidentRepository: PersonidentRepository,
    @Autowired
    private val taskRepository: TaskRepositoryWrapper,
    @Autowired
    private val fagsakRepository: FagsakRepository,
) : AbstractSpringIntegrationTest() {
    private val pdlIdentRestClient: PdlIdentRestClient = mockk()

    val personIdentService =
        PersonidentService(
            personidentRepository = personidentRepository,
            aktørIdRepository = aktørIdRepository,
            pdlIdentRestClient = pdlIdentRestClient,
            taskRepository = taskRepository,
            fagsakRepository = fagsakRepository,
        )

    @Test
    fun `hentFagsakerUtenBehandlingMedUtdatertIdent henter kun ut fagsaker som har utdatert ident`() {
        // Arrange
        val utdatertAktør = tilAktør(randomFnr())
        val oppdatertAktør = tilAktør(randomFnr())
        val aktørPåFagsakMedBehandling = tilAktør(randomFnr())
        aktørIdRepository.saveAll(listOf(utdatertAktør, oppdatertAktør, aktørPåFagsakMedBehandling))

        val identInformasjonFraPdl =
            listOf(
                IdentInformasjon(oppdatertAktør.aktørId, false, "AKTORID"),
                IdentInformasjon(oppdatertAktør.aktivFødselsnummer(), false, "FOLKEREGISTERIDENT"),
                IdentInformasjon(utdatertAktør.aktørId, true, "AKTORID"),
                IdentInformasjon(utdatertAktør.aktivFødselsnummer(), true, "FOLKEREGISTERIDENT"),
            )

        val fagsakSomHarUtdatertAktør = fagsakService.lagre(lagFagsakUtenId(aktør = utdatertAktør))
        val fagsakSomHarOppdatertAktør = fagsakService.lagre(lagFagsakUtenId(aktør = oppdatertAktør))
        val fagsakSomHarBehandling = fagsakService.lagre(lagFagsakUtenId(aktør = aktørPåFagsakMedBehandling))

        behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsakSomHarBehandling))

        every { pdlIdentRestClient.hentIdenter(utdatertAktør.aktivFødselsnummer(), historikk = true) } returns identInformasjonFraPdl
        every { pdlIdentRestClient.hentIdenter(oppdatertAktør.aktivFødselsnummer(), historikk = true) } returns identInformasjonFraPdl

        // Act
        val faktiskeFagsakIder = personIdentService.finnFagsakerUtenBehandlingMedUtdatertIdent()

        // Assert
        assertThat(faktiskeFagsakIder).isEqualTo(setOf(fagsakSomHarUtdatertAktør.id))
    }
}

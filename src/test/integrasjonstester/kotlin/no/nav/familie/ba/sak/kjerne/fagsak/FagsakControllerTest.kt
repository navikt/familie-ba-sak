package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.InstitusjonDto
import no.nav.familie.ba.sak.ekstern.restDomene.SkjermetBarnSøkerDto
import no.nav.familie.ba.sak.integrasjoner.skyggesak.SkyggesakRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.kjerne.personident.Personident
import no.nav.familie.ba.sak.kjerne.personident.PersonidentRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.util.BrukerContextUtil
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.log.mdc.MDCConstants
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable

class FagsakControllerTest(
    @Autowired
    private val fagsakService: FagsakService,
    @Autowired
    private val fagsakController: FagsakController,
    @Autowired
    private val behandlingService: BehandlingService,
    @Autowired
    private val persongrunnlagService: PersongrunnlagService,
    @Autowired
    private val mockPersonidentService: PersonidentService,
    @Autowired
    private val aktørIdRepository: AktørIdRepository,
    @Autowired
    private val personidentRepository: PersonidentRepository,
    @Autowired
    private val skyggesakRepository: SkyggesakRepository,
) : AbstractSpringIntegrationTest() {
    @BeforeEach
    fun init() {
        MDC.put(MDCConstants.MDC_CALL_ID, "00001111")
        BrukerContextUtil.mockBrukerContext(SikkerhetContext.SYSTEM_FORKORTELSE)
    }

    @AfterEach
    fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    @Tag("integration")
    fun `Skal opprette fagsak av type NORMAL`() {
        val fnr = randomFnr()

        fagsakController.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr))
        val fagsak = fagsakService.hentNormalFagsak(lagAktør(fnr))
        assertEquals(fnr, fagsak?.aktør?.aktivFødselsnummer())
        assertEquals(FagsakType.NORMAL, fagsak?.type)
        assertNull(fagsak?.institusjon)
        assertNull(fagsak?.skjermetBarnSøker)
    }

    @Test
    @Tag("integration")
    fun `Skal opprette fagsak av type SKJERMET_BARN`() {
        val fnr = randomFnr()
        val søkersIdent = randomFnr()
        val skjermetBarnSøker = SkjermetBarnSøkerDto(søkersIdent = søkersIdent)

        fagsakController.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr, fagsakType = FagsakType.SKJERMET_BARN, skjermetBarnSøker = skjermetBarnSøker))

        val fagsak = fagsakService.hentFagsakPåPerson(lagAktør(fnr), FagsakType.SKJERMET_BARN)

        assertThat(fnr).isEqualTo(fagsak?.aktør?.aktivFødselsnummer())
        assertThat(fagsak?.type).isEqualTo(FagsakType.SKJERMET_BARN)
        assertThat(fagsak?.skjermetBarnSøker).isNotNull
        assertThat(fagsak?.institusjon).isNull()
    }

    @Test
    @Tag("integration")
    fun `Skal opprette skyggesak i Sak`() {
        val fnr = randomFnr()

        val fagsak = fagsakController.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr)).body?.data

        val skyggesak = skyggesakRepository.finnSkyggesakerKlareForSending(Pageable.unpaged())
        assertEquals(1, skyggesak.filter { it.fagsakId == fagsak?.id }.size)
    }

    @Test
    @Tag("integration")
    fun `Skal returnere eksisterende fagsak på person som allerede finnes`() {
        val fnr = randomFnr()

        val nyMinimalFagsakDto = fagsakController.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr))
        assertEquals(Ressurs.Status.SUKSESS, nyMinimalFagsakDto.body?.status)
        assertEquals(fnr, fagsakService.hentNormalFagsak(lagAktør(fnr))?.aktør?.aktivFødselsnummer())

        val eksisterendeMinimalFagsakDto =
            fagsakController.hentEllerOpprettFagsak(
                FagsakRequest(
                    personIdent = fnr,
                ),
            )
        assertEquals(Ressurs.Status.SUKSESS, eksisterendeMinimalFagsakDto.body?.status)
        assertEquals(eksisterendeMinimalFagsakDto.body!!.data!!.id, nyMinimalFagsakDto.body!!.data!!.id)
    }

    @Test
    @Tag("integration")
    fun `Skal returnere eksisterende fagsak på person som allerede finnes med gammel ident`() {
        val fnr = randomFnr()
        val nyttFnr = randomFnr()
        val aktørId = randomAktør().aktørId

        // Får ikke mockPersonopplysningerService til å virke riktig derfor oppdateres db direkte.
        val aktør = aktørIdRepository.save(Aktør(aktørId))
        personidentRepository.save(Personident(fødselsnummer = fnr, aktør = aktør, aktiv = true))

        val nyFagsakDto = fagsakController.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr))
        assertEquals(Ressurs.Status.SUKSESS, nyFagsakDto.body?.status)
        assertEquals(fnr, fagsakService.hentNormalFagsak(aktør)?.aktør?.aktivFødselsnummer())

        personidentRepository.save(
            personidentRepository.getReferenceById(fnr).also { it.aktiv = false },
        )
        personidentRepository.save(Personident(fødselsnummer = nyttFnr, aktør = aktør, aktiv = true))

        val eksisterendeFagsakDto =
            fagsakController.hentEllerOpprettFagsak(
                FagsakRequest(
                    personIdent = nyttFnr,
                ),
            )
        assertEquals(Ressurs.Status.SUKSESS, eksisterendeFagsakDto.body?.status)
        assertEquals(eksisterendeFagsakDto.body!!.data!!.id, nyFagsakDto.body!!.data!!.id)
        assertEquals(nyttFnr, eksisterendeFagsakDto.body!!.data?.søkerFødselsnummer)
    }

    @Test
    @Tag("integration")
    fun `Skal returnere eksisterende fagsak på person som allerede finnes basert på aktørid`() {
        val aktørId = randomAktør()
        val fagsakRequest = FagsakRequest(personIdent = aktørId.aktivFødselsnummer())
        val fagsakDto =
            fagsakController.hentEllerOpprettFagsak(
                fagsakRequest,
            )
        assertEquals(Ressurs.Status.SUKSESS, fagsakDto.body?.status)

        val eksisterendeFagsakDto = fagsakController.hentEllerOpprettFagsak(fagsakRequest)
        assertEquals(Ressurs.Status.SUKSESS, eksisterendeFagsakDto.body?.status)
        assertEquals(eksisterendeFagsakDto.body!!.data!!.id, fagsakDto.body!!.data!!.id)
    }

    @Test
    @Tag("integration")
    fun `Skal få valideringsfeil ved oppretting av fagsak av type INSTITUSJON uten FagsakInstitusjon satt`() {
        val fnr = randomFnr()

        val exception =
            assertThrows<FunksjonellFeil> {
                fagsakController.hentEllerOpprettFagsak(
                    FagsakRequest(
                        personIdent = fnr,
                        fagsakType = FagsakType.INSTITUSJON,
                    ),
                )
            }
        val fagsaker = fagsakService.hentMinimalFagsakerForPerson(lagAktør(fnr))
        assertThat(fagsaker.data!!).isEmpty()
        assertThat(exception.message).isEqualTo("Institusjon mangler for fagsaktype institusjon.")
    }

    @Test
    @Tag("integration")
    fun `Skal opprette fagsak av type INSTITUSJON hvor FagsakInstitusjon er satt`() {
        val fnr = randomFnr()
        val orgNrNav = "889640782"

        fagsakController.hentEllerOpprettFagsak(
            FagsakRequest(
                personIdent = fnr,
                fagsakType = FagsakType.INSTITUSJON,
                institusjon = InstitusjonDto(orgNrNav, "tss-id"),
            ),
        )
        val fagsakerRessurs = fagsakService.hentMinimalFagsakerForPerson(lagAktør(fnr))
        assert(fagsakerRessurs.status == Ressurs.Status.SUKSESS)
        val fagsaker = fagsakerRessurs.data!!
        assert(fagsaker.isNotEmpty()) { "Fagsak skulle ha blitt opprettet" }
        assertEquals(fnr, fagsaker[0].søkerFødselsnummer)
        assertEquals(FagsakType.INSTITUSJON, fagsaker[0].fagsakType)
        assertNotNull(fagsaker[0].institusjon)
        assertEquals(orgNrNav, fagsaker[0].institusjon?.orgNummer)
        assertEquals("tss-id", fagsaker[0].institusjon?.tssEksternId)
    }
}

package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.nyOrdinærBehandling
import no.nav.familie.ba.sak.datagenerator.randomBarnFnr
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.FagsakDeltagerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.SøkParamDto
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.util.BrukerContextUtil
import no.nav.familie.log.mdc.MDCConstants
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired

class FagsakDeltagerControllerTest(
    @Autowired
    private val fagsakService: FagsakService,
    @Autowired
    private val fagsakDeltagerController: FagsakDeltagerController,
    @Autowired
    private val behandlingService: BehandlingService,
    @Autowired
    private val persongrunnlagService: PersongrunnlagService,
    @Autowired
    private val mockPersonidentService: PersonidentService,
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
    fun `Skal oppgi person med fagsak som fagsakdeltaker`() {
        val personAktør = mockPersonidentService.hentAktør(randomFnr())

        fagsakService
            .hentEllerOpprettFagsak(personAktør.aktivFødselsnummer())
            .also { fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE) }

        fagsakDeltagerController.oppgiFagsakdeltagere(SøkParamDto(personAktør.aktivFødselsnummer(), emptyList())).apply {
            assertEquals(personAktør.aktivFødselsnummer(), body!!.data!!.first().ident)
            assertEquals(FagsakDeltagerRolle.FORELDER, body!!.data!!.first().rolle)
        }
    }

    @Test
    fun `Skal oppgi det første barnet i listen som fagsakdeltaker`() {
        val personAktør = mockPersonidentService.hentOgLagreAktør(randomFnr(), true)
        val søkerFnr = randomFnr()
        val barnaFnr = listOf(randomBarnFnr())
        val søkerAktør = mockPersonidentService.hentOgLagreAktør(søkerFnr, true)
        val barnaAktør = mockPersonidentService.hentOgLagreAktørIder(barnaFnr, true)

        val fagsak =
            fagsakService.hentEllerOpprettFagsak(
                søkerAktør.aktivFødselsnummer(),
            )

        val behandling =
            behandlingService.opprettBehandling(
                nyOrdinærBehandling(
                    søkersIdent = søkerFnr,
                    fagsakId = fagsak.id,
                ),
            )
        persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
            personAktør,
            barnaAktør,
            behandling,
            Målform.NB,
        )

        fagsakDeltagerController
            .oppgiFagsakdeltagere(
                SøkParamDto(
                    personAktør.aktivFødselsnummer(),
                    barnaFnr + randomFnr(),
                ),
            ).apply {
                assertEquals(barnaFnr, body!!.data!!.map { it.ident })
                assertEquals(listOf(FagsakDeltagerRolle.BARN), body!!.data!!.map { it.rolle })
            }
    }
}

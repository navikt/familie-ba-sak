package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.config.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonInfoQuery
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.DødsfallData
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.EvaluerFiltreringsregler
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRequest
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest(properties = ["FAMILIE_FAMILIE_TILBAKE_API_URL=http://localhost:28085/api"])
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles(
    "postgres",
    "mock-brev-klient",
    "mock-oauth",
    "mock-pdl",
    "mock-arbeidsfordeling",
    "mock-familie-tilbake",
    "mock-infotrygd-feed",
    "mock-økonomi",
    "mock-tilbakekreving-klient",
    "mock-infotrygd-barnetrygd",
    "mock-task-repository"
)
@Tag("integration")
@AutoConfigureWireMock(port = 28085)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestFiltrering(
    @Autowired
    private val evaluerFiltreringsregler: EvaluerFiltreringsregler,

    @Autowired
    private val stegService: StegService,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,

    @Autowired
    private val personopplysningerService: PersonopplysningerService,
) {
    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    private val mockPdlRestClient: PdlRestClient = mockk(relaxed = true)

    @Test
    fun `Sende saker gjennom automatisk filter`() {
        val morIdent = "04082026621"
        val barnIdent = "21111777001"

        every { mockPdlRestClient.hentPerson("21111777001", any()) } returns mockBarnAutomatiskBehandling
        every { mockPdlRestClient.hentPerson("04082026621", any()) } returns mockSøkerAutomatiskBehandling

        val søker = mockPdlRestClient.hentPerson(morIdent, PersonInfoQuery.MED_RELASJONER)
        val barn = mockPdlRestClient.hentPerson(barnIdent, PersonInfoQuery.MED_RELASJONER)

        every { personopplysningerService.hentPersoninfo(morIdent) } returns søker
        every { personopplysningerService.hentPersoninfo(barnIdent) } returns barn
        every { personopplysningerService.hentDødsfall(Ident(morIdent)) } returns DødsfallData(false, null)
        every { personopplysningerService.hentDødsfall(Ident(barnIdent)) } returns DødsfallData(false, null)

        val fagsak = fagsakService.hentEllerOpprettFagsak(
            FagsakRequest(
                morIdent
            )
        )
        val behandling = stegService.håndterNyBehandling(
            NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                morIdent,
                BehandlingType.FØRSTEGANGSBEHANDLING
            )
        )
        val (evaluering, begrunnelse) = evaluerFiltreringsregler.automatiskBehandlingEvaluering(
            morIdent,
            setOf(barnIdent),
            behandling
        )
        //evaluerFiltreringsregler.lagFiltrering("04086226621", setOf("21111777001"), behandling)
        assert(evaluering) { begrunnelse }
    }
}
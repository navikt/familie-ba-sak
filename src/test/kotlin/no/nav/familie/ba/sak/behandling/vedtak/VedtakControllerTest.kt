package no.nav.familie.ba.sak.behandling.vedtak

<<<<<<<< HEAD:src/test/kotlin/no/nav/familie/ba/sak/behandling/vedtak/VedtakControllerTest.kt
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.domene.vilkår.UtfallType
import no.nav.familie.ba.sak.behandling.domene.vilkår.VilkårType
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
========
import no.nav.familie.ba.sak.behandling.fagsak.FagsakController
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.fagsak.NyFagsak
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
>>>>>>>> behandling:src/test/kotlin/no/nav/familie/ba/sak/FagsakControllerTest.kt
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.util.randomFnr
<<<<<<<< HEAD:src/test/kotlin/no/nav/familie/ba/sak/behandling/vedtak/VedtakControllerTest.kt
========
import no.nav.familie.kontrakter.felles.Ressurs
>>>>>>>> behandling:src/test/kotlin/no/nav/familie/ba/sak/FagsakControllerTest.kt
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
<<<<<<<< HEAD:src/test/kotlin/no/nav/familie/ba/sak/behandling/vedtak/VedtakControllerTest.kt
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
========
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
>>>>>>>> behandling:src/test/kotlin/no/nav/familie/ba/sak/FagsakControllerTest.kt

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev", "mock-dokgen")
@Tag("integration")
class VedtakControllerTest(
        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
<<<<<<<< HEAD:src/test/kotlin/no/nav/familie/ba/sak/behandling/vedtak/VedtakControllerTest.kt
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,

        @Autowired
        private val vedtakController: VedtakController,

        @Autowired
        private val vedtakRepository: VedtakRepository,

        @Autowired
        private val behandlingRepository: BehandlingRepository
) {

    @Test
    @Tag("integration")
    fun `Test opphør vedtak`() {
        val mockBehandlingLager: BehandlingService = mockk()
        val mockVedtakService: VedtakService = mockk()

        val fagsak = Fagsak(1, AktørId("1"), PersonIdent("1"))
        val behandling =
                lagOrdinærIverksattBehandling(fagsak, BehandlingType.MIGRERING_FRA_INFOTRYGD)
        val vedtak = lagInnvilgetVedtak(behandling)

        every { mockBehandlingLager.hentAktiv(any()) } returns behandling
        every { mockVedtakService.hentAktiv(any()) } returns vedtak

        val response = vedtakController.opphørMigrertVedtak(1)
        assert(response.statusCode == HttpStatus.OK)
========
        private val fagsakController: FagsakController
) {

    @MockBean
    lateinit var integrasjonTjeneste: IntegrasjonTjeneste

    @BeforeEach
    fun setup() {
        Mockito.`when`(integrasjonTjeneste.hentAktørId(ArgumentMatchers.anyString())).thenReturn(AktørId("1"))
>>>>>>>> behandling:src/test/kotlin/no/nav/familie/ba/sak/FagsakControllerTest.kt
    }

    @Test
    @Tag("integration")
<<<<<<<< HEAD:src/test/kotlin/no/nav/familie/ba/sak/behandling/vedtak/VedtakControllerTest.kt
    fun `Test opphør vedtak v2`() {
        val mockBehandlingLager: BehandlingService = mockk()
        val mockVedtakService: VedtakService = mockk()

        val fagsak = Fagsak(1, AktørId("1"), PersonIdent("1"))
        val behandling =
                lagOrdinærIverksattBehandling(fagsak, BehandlingType.MIGRERING_FRA_INFOTRYGD)
        val vedtak = lagInnvilgetVedtak(behandling)

        every { mockBehandlingLager.hentAktiv(any()) } returns behandling
        every { mockVedtakService.hentAktiv(any()) } returns vedtak

        val response = vedtakController.opphørMigrertVedtak(1,
                                                            Opphørsvedtak(
                                                                    LocalDate.now()))
        assert(response.statusCode == HttpStatus.OK)
========
    fun `Skal opprette fagsak`() {
        val fnr = randomFnr()

        fagsakController.nyFagsak(NyFagsak(personIdent = fnr))
        Assertions.assertEquals(fnr, fagsakService.hentFagsakForPersonident(PersonIdent(fnr))?.personIdent?.ident)
>>>>>>>> behandling:src/test/kotlin/no/nav/familie/ba/sak/FagsakControllerTest.kt
    }

    @Test
    @Tag("integration")
<<<<<<<< HEAD:src/test/kotlin/no/nav/familie/ba/sak/behandling/vedtak/VedtakControllerTest.kt
    fun `Test opprett avslag vedtak`() {
        val fagsakId = 1L
        val behandlingId = 1L
        val aktørId = randomFnr()
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = Fagsak(fagsakId, AktørId(aktørId), PersonIdent(søkerFnr))
        fagsakService.lagreFagsak(fagsak)
        val behandling =
                Behandling(behandlingId,
                           fagsak,
                           null,
                           BehandlingType.MIGRERING_FRA_INFOTRYGD,
                           status = BehandlingStatus.IVERKSATT,
                           kategori = BehandlingKategori.NASJONAL,
                           underkategori = BehandlingUnderkategori.ORDINÆR)
        behandlingRepository.save(behandling)
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
                behandling.id, søkerFnr, barnFnr)
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val response = vedtakController.nyttVedtak(1, NyttVedtak(
                resultat = VedtakResultat.AVSLÅTT,
                samletVilkårResultat = listOf(
                        RestVilkårResultat(
                                personIdent = søkerFnr,
                                vilkårType = VilkårType.BOSATT_I_RIKET,
                                utfallType = UtfallType.IKKE_OPPFYLT
                        ),
                        RestVilkårResultat(
                                personIdent = søkerFnr,
                                vilkårType = VilkårType.STØNADSPERIODE,
                                utfallType = UtfallType.IKKE_OPPFYLT
                        ),
                        RestVilkårResultat(
                                personIdent = barnFnr,
                                vilkårType = VilkårType.BOSATT_I_RIKET,
                                utfallType = UtfallType.IKKE_OPPFYLT
                        ),
                        RestVilkårResultat(
                                personIdent = barnFnr,
                                vilkårType = VilkårType.STØNADSPERIODE,
                                utfallType = UtfallType.IKKE_OPPFYLT
                        ),
                        RestVilkårResultat(
                                personIdent = barnFnr,
                                vilkårType = VilkårType.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                                utfallType = UtfallType.IKKE_OPPFYLT
                        )
                ),
                begrunnelse = "mock begrunnelse"
        ))

        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandling.id)
========
    fun `Skal kaste feil ved opprettelse av fagsak på person som allerede finnes`() {
        val fnr = randomFnr()

        val restFagsak = fagsakController.nyFagsak(NyFagsak(personIdent = fnr))
        Assertions.assertEquals(Ressurs.Status.SUKSESS, restFagsak.body?.status)
        Assertions.assertEquals(fnr, fagsakService.hentFagsakForPersonident(PersonIdent(fnr))?.personIdent?.ident)

        val feilendeRestFagsak = fagsakController.nyFagsak(NyFagsak(
                personIdent = fnr))
        Assertions.assertEquals(Ressurs.Status.FEILET, feilendeRestFagsak.body?.status)
>>>>>>>> behandling:src/test/kotlin/no/nav/familie/ba/sak/FagsakControllerTest.kt
        Assertions.assertEquals(
                "Kan ikke opprette fagsak på person som allerede finnes. Gå til fagsak ${restFagsak.body?.data?.id} for å se på saken",
                feilendeRestFagsak.body?.melding)
    }
}
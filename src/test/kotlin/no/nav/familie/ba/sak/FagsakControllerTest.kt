package no.nav.familie.ba.sak

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.DokGenService
import no.nav.familie.ba.sak.behandling.FagsakController
import no.nav.familie.ba.sak.behandling.FagsakService
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.domene.vedtak.*
import no.nav.familie.ba.sak.behandling.domene.vilkår.UtfallType
import no.nav.familie.ba.sak.behandling.domene.vilkår.VilkårService
import no.nav.familie.ba.sak.behandling.domene.vilkår.VilkårType
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.util.lagRandomSaksnummer
import no.nav.familie.ba.sak.util.lagTestPersonopplysningGrunnlag
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.sikkerhet.OIDCUtil
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.util.*

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev", "mock-dokgen")
@Tag("integration")
class FagsakControllerTest(
        @Autowired
        private val oidcUtil: OIDCUtil,

        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,

        @Autowired
        private val taskRepository: TaskRepository,

        @Autowired
        private val vedtakRepository: VedtakRepository,

        @Autowired
        private val behandlingRepository: BehandlingRepository,

        @Autowired
        private val dokGenService: DokGenService,

        @Autowired
        private val vilkårService: VilkårService,

        @Autowired
        private val featureToggleService: FeatureToggleService
) {

    @Test
    @Tag("integration")
    fun `Test opphør vedtak`() {
        val mockBehandlingLager: BehandlingService = mockk()

        val fagsak = Fagsak(1, AktørId("1"), PersonIdent("1"))
        val behandling =
                lagOrdinærIverksattBehandling(fagsak, BehandlingType.MIGRERING_FRA_INFOTRYGD)
        val vedtak = lagInnvilgetVedtak(behandling)

        every { mockBehandlingLager.hentBehandlingHvisEksisterer(any()) } returns behandling
        every { mockBehandlingLager.hentVedtakHvisEksisterer(any()) } returns vedtak
        val fagsakController =
                FagsakController(oidcUtil, fagsakService, mockBehandlingLager, personopplysningGrunnlagRepository, taskRepository)

        val response = fagsakController.opphørMigrertVedtak(1)
        assert(response.statusCode == HttpStatus.OK)
    }


    @Test
    @Tag("integration")
    fun `Test opphør vedtak v2`() {
        val mockBehandlingLager: BehandlingService = mockk()

        val fagsak = Fagsak(1, AktørId("1"), PersonIdent("1"))
        val behandling =
                lagOrdinærIverksattBehandling(fagsak, BehandlingType.MIGRERING_FRA_INFOTRYGD)
        val vedtak = lagInnvilgetVedtak(behandling)

        every { mockBehandlingLager.hentBehandlingHvisEksisterer(any()) } returns behandling
        every { mockBehandlingLager.hentVedtakHvisEksisterer(any()) } returns vedtak
        val fagsakController =
                FagsakController(oidcUtil, fagsakService, mockBehandlingLager, personopplysningGrunnlagRepository, taskRepository)

        val response = fagsakController.opphørMigrertVedtak(1, Opphørsvedtak(LocalDate.now()))
        assert(response.statusCode == HttpStatus.OK)
    }

    private fun randomPin() = UUID.randomUUID().toString()

    @Test
    @Tag("integration")
    fun `Test opprett avslag vedtak`() {
        val fagsakId = 1L
        val behandlingId = 1L
        val saksNummer = lagRandomSaksnummer()
        val aktørId = randomPin()
        val søkerFnr = randomPin()
        val barnFnr = randomPin()

        val behandlingService = BehandlingService(
                behandlingRepository = behandlingRepository,
                vedtakRepository = vedtakRepository,
                vedtakPersonRepository = mockk(),
                personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
                personRepository = mockk(),
                dokGenService = dokGenService,
                fagsakService = fagsakService,
                vilkårService = vilkårService,
                integrasjonTjeneste = mockk(),
                featureToggleService = featureToggleService,
                taskRepository = taskRepository)

        val fagsak = Fagsak(fagsakId, AktørId(aktørId), PersonIdent(søkerFnr))
        fagsakService.lagreFagsak(fagsak)
        val behandling =
                Behandling(behandlingId,
                           fagsak,
                           null,
                           BehandlingType.MIGRERING_FRA_INFOTRYGD,
                           saksNummer,
                           status = BehandlingStatus.IVERKSATT,
                           kategori = BehandlingKategori.NASJONAL,
                           underkategori = BehandlingUnderkategori.ORDINÆR)
        behandlingRepository.save(behandling)
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
                behandling.id!!, søkerFnr, barnFnr)
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val fagsakController =
                FagsakController(oidcUtil, fagsakService, behandlingService,
                                 personopplysningGrunnlagRepository, taskRepository)

        val response = fagsakController.nyttVedtak(1, NyttVedtak(
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
        Assertions.assertEquals(
                "mock begrunnelse",
                vedtak?.begrunnelse
        )
    }

    private fun lagInnvilgetVedtak(behandling: Behandling) =
            Vedtak(1, behandling, "sb", LocalDate.now(), "", VedtakResultat.INNVILGET,
                   begrunnelse = "")

    private fun lagOrdinærIverksattBehandling(fagsak: Fagsak,
                                              migreringFraInfotrygd: BehandlingType): Behandling {
        val behandling =
                Behandling(1,
                           fagsak,
                           null,
                           migreringFraInfotrygd,
                           "1",
                           status = BehandlingStatus.IVERKSATT,
                           kategori = BehandlingKategori.NASJONAL,
                           underkategori = BehandlingUnderkategori.ORDINÆR)
        return behandling
    }

}

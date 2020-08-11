package no.nav.familie.ba.sak.behandling.vedtak

import com.github.tomakehurst.wiremock.client.WireMock.*
import io.mockk.MockKAnnotations
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.fagsak.FagsakPersonRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@SpringBootTest(properties = ["FAMILIE_INTEGRASJONER_API_URL=http://localhost:28085/api"])
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-dokgen", "mock-oauth")
@Tag("integration")
@AutoConfigureWireMock(port = 28085)
class VedtakServiceTest(
        @Autowired
        val behandlingRepository: BehandlingRepository,

        @Autowired
        val behandlingResultatService: BehandlingResultatService,

        @Autowired
        val vedtakService: VedtakService,

        @Autowired
        val persongrunnlagService: PersongrunnlagService,

        @Autowired
        val beregningService: BeregningService,

        @Autowired
        val fagsakService: FagsakService,

        @Autowired
        val fagsakPersonRepository: FagsakPersonRepository,

        @Autowired
        val totrinnskontrollService: TotrinnskontrollService,

        @Autowired
        val loggService: LoggService
) {

    lateinit var behandlingService: BehandlingService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        behandlingService = BehandlingService(
                behandlingRepository,
                fagsakPersonRepository,
                persongrunnlagService,
                beregningService,
                fagsakService,
                loggService)

        stubFor(get(urlEqualTo("/api/aktoer/v1"))
                        .willReturn(aResponse()
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(objectMapper.writeValueAsString(Ressurs.success(mapOf("aktørId" to "1"))))))

        stubFor(get(urlEqualTo("/api/personopplysning/v1/info/BAR"))
                        .willReturn(aResponse()
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(objectMapper.writeValueAsString(Ressurs.success(PersonInfo(
                                                    LocalDate.of(2019,
                                                                 1,
                                                                 1)))))))
    }

    @Test
    @Tag("integration")
    fun `Opprett innvilget behandling med vedtak`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val behandlingResultat = lagBehandlingResultat(fnr, behandling, Resultat.JA)

        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat = behandlingResultat, loggHendelse = true)

        val behandlingResultatType =
                behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandlingId = behandling.id)
        Assertions.assertNotNull(behandling.fagsak.id)
        Assertions.assertEquals(behandlingResultatType, BehandlingResultatType.INNVILGET)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag
        )

        totrinnskontrollService.opprettEllerHentTotrinnskontroll(behandling, "ansvarligSaksbehandler")
        totrinnskontrollService.besluttTotrinnskontroll(behandling, "ansvarligBeslutter", Beslutning.GODKJENT)

        val hentetVedtak = vedtakService.hentAktivForBehandling(behandling.id)
        Assertions.assertNotNull(hentetVedtak)
        Assertions.assertNull(hentetVedtak!!.vedtaksdato)
        Assertions.assertEquals(null, hentetVedtak?.stønadBrevPdF)

        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandlingId = behandling.id)
        Assertions.assertNotNull(totrinnskontroll)
        Assertions.assertEquals("ansvarligSaksbehandler", totrinnskontroll!!.saksbehandler)
    }

    @Test
    @Tag("integration")
    fun `Opprett opphørt behandling med vedtak`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val behandlingResultat = lagBehandlingResultat(fnr, behandling, Resultat.NEI)

        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat = behandlingResultat, loggHendelse = true)

        val behandlingResultatType =
                behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandlingId = behandling.id)
        Assertions.assertNotNull(behandling.fagsak.id)
        Assertions.assertEquals(behandlingResultatType, BehandlingResultatType.AVSLÅTT)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag
        )

        val hentetVedtak = vedtakService.hentAktivForBehandling(behandling.id)
        Assertions.assertNotNull(hentetVedtak)
    }

    @Test
    fun `Skal hente forrige behandling`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                personopplysningGrunnlag = personopplysningGrunnlag,
                behandling = behandling
        )

        val revurderingInnvilgetBehandling =
                behandlingService.lagreNyOgDeaktiverGammelBehandling(Behandling(fagsak = fagsak,
                                                                                type = BehandlingType.REVURDERING,
                                                                                kategori = BehandlingKategori.NASJONAL,
                                                                                underkategori = BehandlingUnderkategori.ORDINÆR,
                                                                                opprinnelse = BehandlingOpprinnelse.MANUELL))


        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                personopplysningGrunnlag = personopplysningGrunnlag,
                behandling = revurderingInnvilgetBehandling
        )


        val revurderingOpphørBehandling =
                behandlingService.lagreNyOgDeaktiverGammelBehandling(Behandling(fagsak = fagsak,
                                                                                type = BehandlingType.REVURDERING,
                                                                                kategori = BehandlingKategori.NASJONAL,
                                                                                underkategori = BehandlingUnderkategori.ORDINÆR,
                                                                                opprinnelse = BehandlingOpprinnelse.MANUELL))

        val forrigeVedtak = vedtakService.hentForrigeVedtak(revurderingOpphørBehandling)
        Assertions.assertNotNull(forrigeVedtak)
        Assertions.assertEquals(revurderingInnvilgetBehandling.id, forrigeVedtak?.behandling?.id)
    }

    @Test
    @Tag("integration")
    fun `Opprett 2 vedtak og se at det siste vedtaket får aktiv satt til true`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        opprettNyttInvilgetVedtak(behandling, ansvarligEnhet = "ansvarligEnhet1")
        opprettNyttInvilgetVedtak(behandling, ansvarligEnhet = "ansvarligEnhet2")

        val hentetVedtak = vedtakService.hentAktivForBehandling(behandling.id)
        Assertions.assertNotNull(hentetVedtak)
        Assertions.assertEquals("ansvarligEnhet2", hentetVedtak?.ansvarligEnhet)
    }

    @Test
    @Tag("integration")
    fun `Opprett vedtak og sett begrunnelser til stønadsbrevet`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val vedtak = opprettNyttInvilgetVedtak(behandling, ansvarligEnhet = "ansvarligEnhet1")

        Assertions.assertEquals(0, vedtak.stønadBrevBegrunnelser.size)
        val periode = Periode(LocalDate.of(2018, 1, 1), TIDENES_ENDE)
        val begrunnelse = "Mock begrunnelse"
        vedtak.settStønadBrevBegrunnelse(periode, begrunnelse)

        val endretVedtak = vedtakService.lagreEllerOppdater(vedtak)
        Assertions.assertEquals(mapOf(periode.hash to begrunnelse), endretVedtak.stønadBrevBegrunnelser)
        Assertions.assertEquals(1, endretVedtak.stønadBrevBegrunnelser.size)
    }

    private fun opprettNyttInvilgetVedtak(behandling: Behandling, ansvarligEnhet: String = "ansvarligEnhet"): Vedtak {
        vedtakService.lagreOgDeaktiverGammel(Vedtak(behandling = behandling,
                                                    ansvarligEnhet = ansvarligEnhet,
                                                    vedtaksdato = LocalDate.now())
        )

        return vedtakService.hentAktivForBehandling(behandling.id)!!
    }
}
package no.nav.familie.ba.sak.økonomi

import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.Vilkårsvurdering
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest(classes = [ApplicationConfig::class],
                properties = ["FAMILIE_OPPDRAG_API_URL=http://localhost:28085/api",
                    "FAMILIE_BA_DOKGEN_API_URL=http://localhost:28085/api",
                    "FAMILIE_INTEGRASJONER_API_URL=http://localhost:28085/api"])
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-dokgen", "mock-oauth", "mock-pdl", "mock-arbeidsfordeling")
@TestInstance(Lifecycle.PER_CLASS)
@AutoConfigureWireMock(port = 28085)
class ØkonomiIntegrasjonTest {

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var vilkårsvurderingService: VilkårsvurderingService

    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    lateinit var økonomiService: ØkonomiService

    @Autowired
    lateinit var beregningService: BeregningService

    @Autowired
    lateinit var stegService: StegService

    @Autowired
    private lateinit var vedtakService: VedtakService

    @Test
    @Tag("integration")
    fun `Iverksett vedtak på aktiv behandling`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val stønadFom = LocalDate.now()
        val stønadTom = stønadFom.plusYears(17)

        val responseBody = Ressurs.Companion.success("ok")
        stubFor(get(urlEqualTo("/api/aktoer/v1"))
                        .willReturn(aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(objectMapper.writeValueAsString(Ressurs.Companion.success(mapOf("aktørId" to 1L))))))
        stubFor(post(anyUrl())
                        .willReturn(aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(objectMapper.writeValueAsString(responseBody))))


        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val vilkårsvurdering = lagBehandlingResultat(behandling, fnr, barnFnr, stønadFom, stønadTom)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
        Assertions.assertNotNull(behandling.fagsak.id)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag
        )

        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
        Assertions.assertNotNull(vedtak)
        vedtak!!.vedtaksdato = LocalDateTime.now()
        vedtakService.lagreEllerOppdater(vedtak)

        val oppdatertFagsak = beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        Assertions.assertEquals(Ressurs.Status.SUKSESS, oppdatertFagsak.status)

        assertDoesNotThrow {
            økonomiService.oppdaterTilkjentYtelseOgIverksettVedtak(vedtak, "ansvarligSaksbehandler")
        }
    }

    @Test
    @Tag("integration")
    fun `Hent behandlinger for løpende fagsaker til konsistensavstemming mot økonomi`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val stønadFom = LocalDate.now()
        val stønadTom = stønadFom.plusYears(17)

        stubFor(post(anyUrl())
                        .willReturn(aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(objectMapper.writeValueAsString(Ressurs.Companion.success("ok")))))

        //Lag fagsak med behandling og personopplysningsgrunnlag og Iverksett.
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val vedtak = Vedtak(behandling = behandling,
                            vedtaksdato = LocalDateTime.of(2020, 1, 1, 4, 35))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)
        vedtakService.lagreOgDeaktiverGammel(vedtak)

        val vilkårsvurdering = lagBehandlingResultat(behandling, fnr, barnFnr, stønadFom, stønadTom)
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        økonomiService.oppdaterTilkjentYtelseOgIverksettVedtak(vedtak, "ansvarligSaksbehandler")
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.AVSLUTTET)

        fagsak.status = FagsakStatus.LØPENDE
        fagsakService.lagre(fagsak)

        val søkerOgBehandlingListe = behandlingService.hentOppdragIderTilKonsistensavstemming()

        Assertions.assertTrue(søkerOgBehandlingListe.contains(OppdragIdForFagsystem(fnr, behandling.id)))
    }

    private fun lagBehandlingResultat(behandling: Behandling,
                                      søkerFnr: String,
                                      barnFnr: String,
                                      stønadFom: LocalDate,
                                      stønadTom: LocalDate): Vilkårsvurdering {
        val vilkårsvurdering =
                Vilkårsvurdering(behandling = behandling)
        vilkårsvurdering.personResultater = setOf(
                lagPersonResultat(vilkårsvurdering = vilkårsvurdering,
                                  fnr = søkerFnr,
                                  resultat = Resultat.OPPFYLT,
                                  periodeFom = stønadFom,
                                  periodeTom = stønadTom,
                                  lagFullstendigVilkårResultat = true,
                                  personType = PersonType.SØKER
                ),
                lagPersonResultat(vilkårsvurdering = vilkårsvurdering,
                                  fnr = barnFnr,
                                  resultat = Resultat.OPPFYLT,
                                  periodeFom = stønadFom,
                                  periodeTom = stønadTom,
                                  lagFullstendigVilkårResultat = true,
                                  personType = PersonType.BARN
                )
        )
        return vilkårsvurdering
    }
}

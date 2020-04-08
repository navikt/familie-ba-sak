package no.nav.familie.ba.sak.økonomi

import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.NyBeregning
import no.nav.familie.ba.sak.beregning.PersonBeregning
import no.nav.familie.ba.sak.beregning.lagPeriodeResultat
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@SpringBootTest(classes = [ApplicationConfig::class],
                properties = ["FAMILIE_OPPDRAG_API_URL=http://localhost:28085/api",
                    "FAMILIE_BA_DOKGEN_API_URL=http://localhost:28085/api",
                    "FAMILIE_INTEGRASJONER_API_URL=http://localhost:28085/api"])
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-dokgen", "mock-oauth")
@TestInstance(Lifecycle.PER_CLASS)
@AutoConfigureWireMock(port = 28085)
class ØkonomiIntegrasjonTest {

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var behandlingResultatService: BehandlingResultatService

    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    lateinit var økonomiService: ØkonomiService

    @Autowired
    lateinit var beregningService: BeregningService

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
        var behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val behandlingResultat = lagBehandlingResultat(behandling, fnr, barnFnr, stønadFom, stønadTom)

        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat)
        Assertions.assertNotNull(behandling.fagsak.id)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )

        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
        Assertions.assertNotNull(vedtak)

        val nyBeregning = NyBeregning(
                listOf(PersonBeregning(ident = barnFnr,
                                       beløp = 1054,
                                       stønadFom = LocalDate.of(
                                               2020,
                                               1,
                                               1),
                                       ytelsetype = Ytelsetype.ORDINÆR_BARNETRYGD))
        )

        val oppdatertFagsak = beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag, nyBeregning)

        Assertions.assertEquals(Ressurs.Status.SUKSESS, oppdatertFagsak.status)

        økonomiService.oppdaterTilkjentYtelseOgIverksettVedtak(behandling.id, vedtak!!.id, "ansvarligSaksbehandler")

        val oppdatertBehandling = behandlingService.hent(behandling.id)
        Assertions.assertEquals(BehandlingStatus.SENDT_TIL_IVERKSETTING, oppdatertBehandling.status)
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
                            ansvarligSaksbehandler = "ansvarligSaksbehandler",
                            vedtaksdato = LocalDate.of(2020, 1, 1))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)
        vedtakService.lagreOgDeaktiverGammel(vedtak)

        val behandlingResultat = lagBehandlingResultat(behandling, fnr, barnFnr, stønadFom, stønadTom)
        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat)

        val nyBeregning = NyBeregning(
                listOf(PersonBeregning(ident = barnFnr,
                                       beløp = 1054,
                                       stønadFom = LocalDate.of(
                                               2020,
                                               1,
                                               1),
                                       ytelsetype = Ytelsetype.ORDINÆR_BARNETRYGD))
        )
        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag, nyBeregning)

        økonomiService.oppdaterTilkjentYtelseOgIverksettVedtak(behandling.id, vedtak.id, "ansvarligSaksbehandler")
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.IVERKSATT)
        behandlingService.oppdaterGjeldendeBehandlingForFremtidigUtbetaling(fagsak.id, LocalDate.now())

        fagsak.status = FagsakStatus.LØPENDE
        fagsakService.lagre(fagsak)

        val oppdragIdListe = behandlingService.hentGjeldendeBehandlingerForLøpendeFagsaker()

        Assertions.assertTrue(oppdragIdListe.contains(OppdragId(fnr, behandling.id)))
    }

    private fun lagBehandlingResultat(behandling: Behandling, søkerFnr: String, barnFnr: String, stønadFom: LocalDate, stønadTom: LocalDate): BehandlingResultat {
        val behandlingResultat = BehandlingResultat(behandling = behandling)
        behandlingResultat.periodeResultater = setOf(
                lagPeriodeResultat(
                        søkerFnr,
                        behandlingResultat = behandlingResultat,
                        resultat = Resultat.JA,
                        periodeFom = stønadFom,
                        periodeTom = stønadTom
                ),
                lagPeriodeResultat(
                        barnFnr,
                        behandlingResultat = behandlingResultat,
                        resultat = Resultat.JA,
                        periodeFom = stønadFom,
                        periodeTom = stønadTom
                )
        )
        return behandlingResultat
    }
}

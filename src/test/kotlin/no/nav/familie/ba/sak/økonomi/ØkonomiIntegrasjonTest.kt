package no.nav.familie.ba.sak.økonomi

import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.familie.ba.sak.beregning.PersonBeregning
import no.nav.familie.ba.sak.beregning.NyBeregning
import no.nav.familie.ba.sak.beregning.mapNyBeregningTilVedtakPerson
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@SpringBootTest(classes = [ApplicationConfig::class],
                properties = ["FAMILIE_OPPDRAG_API_URL=http://localhost:28085/api",
                    "FAMILIE_BA_DOKGEN_API_URL=http://localhost:28085/api",
                    "FAMILIE_INTEGRASJONER_API_URL=http://localhost:28085/api"])
@ActiveProfiles("dev", "mock-oauth")
@TestInstance(Lifecycle.PER_CLASS)
@AutoConfigureWireMock(port = 28085)
class ØkonomiIntegrasjonTest {

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    lateinit var økonomiService: ØkonomiService

    @Autowired
    private lateinit var vedtakService: VedtakService

    @Test
    @Tag("integration")
    fun `Iverksett vedtak på aktiv behandling`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

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
        behandling = behandlingService.settVilkårsvurdering(behandling, BehandlingResultat.INNVILGET, "")
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

        val vedtakPersoner = mapNyBeregningTilVedtakPerson(vedtak!!.id,nyBeregning,personopplysningGrunnlag)

        val oppdatertFagsak = vedtakService.oppdaterAktivtVedtakMedBeregning(vedtak, vedtakPersoner)

        Assertions.assertEquals(Ressurs.Status.SUKSESS, oppdatertFagsak.status)

        økonomiService.lagreBeregningsresultatOgIverksettVedtak(behandling.id, vedtak.id, "ansvarligSaksbehandler")

        val oppdatertBehandling = behandlingService.hent(behandling.id)
        Assertions.assertEquals(BehandlingStatus.SENDT_TIL_IVERKSETTING, oppdatertBehandling.status)
    }

    @Test
    @Tag("integration")
    fun `Hent behandlinger for løpende fagsaker til konsistensavstemming mot økonomi`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

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
        vedtakService.oppdaterAktivVedtakMedBeregning(
                vedtak = vedtak!!,
                personopplysningGrunnlag = personopplysningGrunnlag,
                nyBeregning = NyBeregning(
                        listOf(PersonBeregning(ident = barnFnr,
                                beløp = 1054,
                                stønadFom = LocalDate.of(
                                        2020,
                                        1,
                                        1),
                                ytelsetype = Ytelsetype.ORDINÆR_BARNETRYGD))
                )
        )


        økonomiService.lagreBeregningsresultatOgIverksettVedtak(behandling.id, vedtak.id, "ansvarligSaksbehandler")
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.IVERKSATT)
        behandlingService.oppdaterGjeldendeBehandlingForFremtidigUtbetaling(fagsak.id, LocalDate.now())

        fagsak.status = FagsakStatus.LØPENDE
        fagsakService.lagre(fagsak)

        val oppdragIdListe = behandlingService.hentGjeldendeBehandlingerForLøpendeFagsaker()

        Assertions.assertTrue(oppdragIdListe.contains(OppdragId(fnr, behandling.id)))
    }
}

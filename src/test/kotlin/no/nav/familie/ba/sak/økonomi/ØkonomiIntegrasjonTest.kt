package no.nav.familie.ba.sak.økonomi

import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.vedtak.*
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.ba.sak.util.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.vilkår.vilkårsvurderingKomplettForBarnOgSøker
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


        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent("1")
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       "sdf",
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       BehandlingKategori.NASJONAL,
                                                                       BehandlingUnderkategori.ORDINÆR)
        Assertions.assertNotNull(behandling.fagsak.id)

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, "1", "12345678910")
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        vedtakService.nyttVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                nyttVedtak = NyttVedtak(
                        resultat = VedtakResultat.INNVILGET,
                        samletVilkårResultat = vilkårsvurderingKomplettForBarnOgSøker("1", listOf("12345678910")),
                        begrunnelse = ""
                ),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )

        val vedtak = vedtakService.hentVedtakHvisEksisterer(behandlingId = behandling.id)
        Assertions.assertNotNull(vedtak)

        val oppdatertFagsak = vedtakService.oppdaterAktivVedtakMedBeregning(
                vedtak = vedtak!!,
                personopplysningGrunnlag = personopplysningGrunnlag,
                nyBeregning = NyBeregning(
                        arrayOf(BarnBeregning(ident = "12345678910",
                                              beløp = 1054,
                                              stønadFom = LocalDate.of(
                                                      2020,
                                                      1,
                                                      1),
                                              ytelsetype = Ytelsetype.ORDINÆR_BARNETRYGD))
                )
        )

        Assertions.assertEquals(Ressurs.Status.SUKSESS, oppdatertFagsak.status)

        økonomiService.iverksettVedtak(behandling.id, vedtak.id!!, "ansvarligSaksbehandler")

        val oppdatertBehandling = behandlingService.hentBehandling(behandling.id)
        Assertions.assertEquals(BehandlingStatus.SENDT_TIL_IVERKSETTING, oppdatertBehandling?.status)
    }
}

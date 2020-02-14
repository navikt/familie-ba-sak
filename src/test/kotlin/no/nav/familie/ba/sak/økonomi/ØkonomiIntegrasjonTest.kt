package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.HttpTestBase
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.domene.vedtak.*
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.ba.sak.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.vilkår.vilkårsvurderingKomplettForBarnOgSøker
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate


@SpringBootTest(classes = [ApplicationConfig::class],
                properties = ["FAMILIE_OPPDRAG_API_URL=http://localhost:18085/api",
                    "FAMILIE_BA_DOKGEN_API_URL=http://localhost:18085/api"])
@ActiveProfiles("dev", "mock-oauth")
@TestInstance(Lifecycle.PER_CLASS)
class ØkonomiIntegrasjonTest : HttpTestBase(
        18085
) {

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var vedtakRepository: VedtakRepository

    @Autowired
    lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    lateinit var økonomiService: ØkonomiService

    @Test
    @Tag("integration")
    fun `Iverksett vedtak på aktiv behandling`() {
        val responseBody = Ressurs.Companion.success("ok")
        val response: MockResponse = MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(responseBody))
        mockServer.enqueue(response)
        mockServer.enqueue(response)

        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent("1")
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       "sdf",
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       "randomSaksnummer",
                                                                       BehandlingKategori.NATIONAL,
                                                                       BehandlingUnderkategori.ORDINÆR)
        Assertions.assertNotNull(behandling.fagsak.id)

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id!!, "1", "12345678910")
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        behandlingService.nyttVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                nyttVedtak = NyttVedtak(
                        resultat = VedtakResultat.INNVILGET,
                        samletVilkårResultat = vilkårsvurderingKomplettForBarnOgSøker("1", listOf("12345678910"))
                ),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )

        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
        Assertions.assertNotNull(vedtak)

        val oppdatertFagsak = behandlingService.oppdaterAktivVedtakMedBeregning(
                vedtak = vedtak!!,
                personopplysningGrunnlag = personopplysningGrunnlag,
                nyBeregning = NyBeregning(
                        arrayOf(BarnBeregning(fødselsnummer = "12345678910",
                                              beløp = 1054,
                                              stønadFom = LocalDate.now()))
                )
        )

        Assertions.assertEquals(Ressurs.Status.SUKSESS, oppdatertFagsak.status)

        økonomiService.iverksettVedtak(behandling.id!!, vedtak.id!!, "ansvarligSaksbehandler")

        val oppdatertBehandling = behandlingService.hentBehandling(behandling.id)
        Assertions.assertEquals(BehandlingStatus.SENDT_TIL_IVERKSETTING, oppdatertBehandling?.status)
    }
}

package no.nav.familie.ba.sak.økonomi

import junit.framework.Assert.assertEquals
import no.nav.familie.ba.sak.BehandlingIntegrationTest
import no.nav.familie.ba.sak.HttpTestBase
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.FagsakController
import no.nav.familie.ba.sak.behandling.FagsakService
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.domene.vedtak.BarnBeregning
import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtakStatus
import no.nav.familie.ba.sak.behandling.domene.vedtak.NyttVedtak
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.sikkerhet.OIDCUtil
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDate


@SpringBootTest(classes = [ApplicationConfig::class], properties = ["FAMILIE_OPPDRAG_API_URL=http://localhost:18085/api", "FAMILIE_BA_DOKGEN_API_URL=http://localhost:18085/api"])
@ActiveProfiles("dev", "mock-oauth")
@TestInstance(Lifecycle.PER_CLASS)
class ØkonomiIntegrasjonTest: HttpTestBase(
        18085
) {
    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    lateinit var fagsakController: FagsakController

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

        val behandling = behandlingService.nyBehandling("0", arrayOf("123456789010"), BehandlingType.FØRSTEGANGSBEHANDLING, "sdf", "randomSaksnummer")
        Assertions.assertNotNull(behandling.fagsak.id)

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandling.id)

        val søker = Person(personIdent = PersonIdent("123456789010"), type = PersonType.SØKER, personopplysningGrunnlag = personopplysningGrunnlag, fødselsdato = LocalDate.now())
        personopplysningGrunnlag.leggTilPerson(søker)

        personopplysningGrunnlag.leggTilPerson(Person(personIdent = PersonIdent("123456789011"), type = PersonType.BARN, personopplysningGrunnlag = personopplysningGrunnlag, fødselsdato = LocalDate.now()))
        personopplysningGrunnlag.setAktiv(true)
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val oppdatertFagsak = behandlingService.nyttVedtakForAktivBehandling(
                fagsakId = behandling.fagsak.id ?: 1L,
                nyttVedtak = NyttVedtak("sakstype", arrayOf(BarnBeregning(fødselsnummer = "123456789011", beløp = 1054, stønadFom = LocalDate.now()))),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )
        Assertions.assertEquals(Ressurs.Status.SUKSESS, oppdatertFagsak.status)

        fagsakController.iverksettVedtak(behandling.fagsak.id!!)

        val oppdatertBehandlingVedtak = behandlingService.hentAktivVedtakForBehandling(behandling.id)
        Assertions.assertEquals(BehandlingVedtakStatus.SENDT_TIL_IVERKSETTING, oppdatertBehandlingVedtak?.status)
    }
}
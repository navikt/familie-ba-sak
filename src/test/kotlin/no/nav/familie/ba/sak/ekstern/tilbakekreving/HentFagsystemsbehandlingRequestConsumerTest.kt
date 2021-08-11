package no.nav.familie.ba.sak.ekstern.tilbakekreving

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.statistikk.producer.KafkaProducer
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRespons
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.kafka.support.Acknowledgment
import java.time.LocalDate


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HentFagsystemsbehandlingRequestConsumerTest {

    private lateinit var hentFagsystemsbehandlingRequestConsumer: HentFagsystemsbehandlingRequestConsumer
    private lateinit var fagsystemsbehandlingService: FagsystemsbehandlingService

    private lateinit var acknowledgment: Acknowledgment

    private val behandlingService: BehandlingService = mockk(relaxed = true)
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val arbeidsfordelingService: ArbeidsfordelingService = mockk()
    private val vedtakService: VedtakService = mockk()
    private val tilbakekrevingService: TilbakekrevingService = mockk()
    private val kafkaProducer: KafkaProducer = mockk()

    private val requestSlot = slot<HentFagsystemsbehandlingRequest>()
    private val responsSlot = slot<HentFagsystemsbehandlingRespons>()
    private val keySlot = slot<String>()


    val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE, automatiskOpprettelse = true).also {
        it.resultat = BehandlingResultat.INNVILGET
    }

    @BeforeAll
    fun init() {
        fagsystemsbehandlingService = spyk(FagsystemsbehandlingService(behandlingService,
                                                                       persongrunnlagService,
                                                                       arbeidsfordelingService,
                                                                       vedtakService,
                                                                       tilbakekrevingService,
                                                                       kafkaProducer))
        hentFagsystemsbehandlingRequestConsumer = HentFagsystemsbehandlingRequestConsumer(fagsystemsbehandlingService)

        acknowledgment = mockk()
        every { acknowledgment.acknowledge() } returns Unit

        every { behandlingService.hent(any()) } returns behandling
        every { persongrunnlagService.hentAktiv(any()) } returns lagTestPersonopplysningGrunnlag(
                behandling.id,
                tilfeldigPerson(personType = PersonType.BARN),
                tilfeldigPerson(personType = PersonType.SØKER)
        )
        every { arbeidsfordelingService.hentAbeidsfordelingPåBehandling(any()) } returns ArbeidsfordelingPåBehandling(
                behandlendeEnhetId = "4820",
                behandlendeEnhetNavn = "Nav",
                behandlingId = behandling.id
        )
        every { vedtakService.hentAktivForBehandlingThrows(any()) } returns lagVedtak(behandling)
        every { tilbakekrevingService.hentTilbakekrevingsvalg(any()) } returns Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING
        every { kafkaProducer.sendFagsystemsbehandlingResponsForTopicTilbakekreving(any(), any()) } returns Unit
    }

    @Test
    fun `skal lytte request og opprette hentFagsystemsbehandlingRespons`() {
        val consumerRecord = ConsumerRecord("testtopic", 1, 1, "1", lagRequest())
        hentFagsystemsbehandlingRequestConsumer.listen(consumerRecord, acknowledgment)

        verify { fagsystemsbehandlingService.hentFagsystemsbehandling(capture(requestSlot)) }

        val request = requestSlot.captured
        assertEquals(behandling.fagsak.id.toString(), request.eksternFagsakId)
        assertEquals(behandling.id.toString(), request.eksternId)
        assertEquals(Ytelsestype.BARNETRYGD, request.ytelsestype)

        verify { fagsystemsbehandlingService.sendFagsystemsbehandling(capture(responsSlot), capture(keySlot)) }

        val respons = responsSlot.captured
        assertEquals(behandling.fagsak.id.toString(), respons.eksternFagsakId)
        assertEquals(behandling.id.toString(), respons.eksternId)
        assertEquals(Ytelsestype.BARNETRYGD, respons.ytelsestype)
        assertEquals("4820", respons.enhetId)
        assertEquals("Nav", respons.enhetsnavn)
        assertEquals(Målform.NB.tilSpråkkode(), respons.språkkode)
        assertEquals(LocalDate.now(), respons.revurderingsvedtaksdato)
        assertEquals(behandling.resultat.displayName, respons.faktainfo.revurderingsresultat)
        assertEquals(behandling.opprettetÅrsak.visningsnavn, respons.faktainfo.revurderingsårsak)
        assertEquals(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, respons.faktainfo.tilbakekrevingsvalg)
        assertTrue(respons.faktainfo.konsekvensForYtelser.isEmpty())
        assertNull(respons.verge)
    }

    private fun lagRequest(): String {
        return objectMapper.writeValueAsString(HentFagsystemsbehandlingRequest(eksternFagsakId = behandling.fagsak.id.toString(),
                                                                               eksternId = behandling.id.toString(),
                                                                               ytelsestype = Ytelsestype.BARNETRYGD))
    }
}
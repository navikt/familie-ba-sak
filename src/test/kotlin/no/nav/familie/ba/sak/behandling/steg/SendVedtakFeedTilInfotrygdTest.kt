package no.nav.familie.ba.sak.behandling.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.infotrygd.InfotrygdFeedClient
import no.nav.familie.ba.sak.infotrygd.InfotrygdVedtakFeedDto
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SendVedtakFeedTilInfotrygdTest {

    private lateinit var sendVedtakFeedTilInfotrygd: SendVedtakFeedTilInfotrygd
    private lateinit var vedtakService: VedtakService
    private lateinit var infotrygdFeedClient: InfotrygdFeedClient
    private lateinit var vedtak: Vedtak
    private lateinit var behandling: Behandling

    private val fnrStonadsmottaker = "1234"
    private val vedtakId = 333L
    private val vedtaksDate = LocalDate.parse("2020-09-01")


    @BeforeEach
    fun setUp() {
        vedtakService = mockk()
        infotrygdFeedClient = mockk()
        val vedtak = mockk<Vedtak>()

        every { vedtakService.hent(vedtakId) } returns vedtak
        every { vedtak.vedtaksdato } returns vedtaksDate
        every { vedtak.behandling.fagsak.hentAktivIdent().ident } returns fnrStonadsmottaker
        every { infotrygdFeedClient.sendVetakFeedTilInfotrygd(any()) } returns Unit

        sendVedtakFeedTilInfotrygd = SendVedtakFeedTilInfotrygd(vedtakService, infotrygdFeedClient)
    }

    @Test
    fun `Skal sende vedtak til infotrygd å returnere neste steg`() {
        val behandling = lagBehandling().apply { status = BehandlingStatus.IVERKSATT }
                .apply { steg = StegType.SEND_VETAKS_FEED_TIL_INFOTRYGD }

        val nesteSteg = sendVedtakFeedTilInfotrygd.utførStegOgAngiNeste(behandling, SendVedtakFeedTilInfotrygdDTO(vedtakId))

        verify(exactly = 1) {
            infotrygdFeedClient.sendVetakFeedTilInfotrygd(InfotrygdVedtakFeedDto(fnrStonadsmottaker,
                                                                                 vedtaksDate))
        }
        Assertions.assertEquals(StegType.JOURNALFØR_VEDTAKSBREV, nesteSteg)
    }

    @Test
    fun `Skal kaste exception om status ikke er IVERKSATT eller FERDIGSTILT`() {
        val behandling = lagBehandling().apply { status = BehandlingStatus.OPPRETTET }
                .apply { steg = StegType.SEND_VETAKS_FEED_TIL_INFOTRYGD }

        Assertions.assertThrows(Exception::class.java) {
            sendVedtakFeedTilInfotrygd.utførStegOgAngiNeste(behandling, SendVedtakFeedTilInfotrygdDTO(vedtakId))
        }
    }
}
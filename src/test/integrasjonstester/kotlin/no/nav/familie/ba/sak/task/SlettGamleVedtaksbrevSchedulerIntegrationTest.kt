package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.leader.LeaderClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Transactional
class SlettGamleVedtaksbrevSchedulerIntegrationTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val vedtakRepository: VedtakRepository,
) : AbstractSpringIntegrationTest() {
    private val featureToggleService = mockk<FeatureToggleService>()
    private val scheduler = SlettGamleVedtaksbrevScheduler(vedtakRepository, featureToggleService)

    @BeforeEach
    fun setUp() {
        mockkStatic(LeaderClient::class)
        every { LeaderClient.isLeader() } returns true
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_SLETTE_GAMLE_VEDTAKSBREV_FRA_DB) } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(LeaderClient::class)
    }

    @Test
    fun `skal kun slette vedtaksbrev på gamle avsluttede behandlinger`() {
        // Arrange
        val gammelVedtaksdato = LocalDateTime.now().minusMonths(6)
        val nyVedtaksdato = LocalDateTime.now().minusDays(1)

        val skalSlettes =
            lagreVedtakMedStønadBrev(
                status = BehandlingStatus.AVSLUTTET,
                vedtaksdato = gammelVedtaksdato,
            )
        val avsluttetMenForNylig =
            lagreVedtakMedStønadBrev(
                status = BehandlingStatus.AVSLUTTET,
                vedtaksdato = nyVedtaksdato,
            )
        val ikkeAvsluttet =
            lagreVedtakMedStønadBrev(
                status = BehandlingStatus.UTREDES,
                vedtaksdato = gammelVedtaksdato,
            )

        // Act
        scheduler.slettGamleVedtaksbrev()

        // Assert
        assertThat(vedtakRepository.findById(skalSlettes.id).get().stønadBrevPdF).isNull()
        assertThat(vedtakRepository.findById(avsluttetMenForNylig.id).get().stønadBrevPdF).isNotNull()
        assertThat(vedtakRepository.findById(ikkeAvsluttet.id).get().stønadBrevPdF).isNotNull()
    }

    @Test
    fun `skal ikke slette noe når feature toggle er avslått`() {
        // Arrange
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_SLETTE_GAMLE_VEDTAKSBREV_FRA_DB) } returns false
        val vedtak =
            lagreVedtakMedStønadBrev(
                status = BehandlingStatus.AVSLUTTET,
                vedtaksdato = LocalDateTime.now().minusMonths(6),
            )

        // Act
        scheduler.slettGamleVedtaksbrev()

        // Assert
        assertThat(vedtakRepository.findById(vedtak.id).get().stønadBrevPdF).isNotNull()
    }

    private fun lagreVedtakMedStønadBrev(
        status: BehandlingStatus,
        vedtaksdato: LocalDateTime,
    ): Vedtak {
        val søker = aktørIdRepository.save(randomAktør())
        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
        val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak, status = status))
        return vedtakRepository.save(
            Vedtak(
                behandling = behandling,
                vedtaksdato = vedtaksdato,
                stønadBrevPdF = "et vedtaksbrev".toByteArray(),
            ),
        )
    }
}

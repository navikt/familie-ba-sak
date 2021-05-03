package no.nav.familie.ba.sak.behandling.steg

import io.mockk.every
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.simulering.SimuleringService
import no.nav.familie.ba.sak.tilbakekreving.TilbakekrevingService
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("dev", "mock-pdl", "mock-infotrygd-barnetrygd", "mock-økonomi")
@TestInstance(Lifecycle.PER_CLASS)
@DirtiesContext
class SimuleringStegTest(
        @Autowired
        private val stegService: StegService,

        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
        private val personopplysningerService: PersonopplysningerService,

        @Autowired
        private val databaseCleanupService: DatabaseCleanupService,

        @Autowired
        private val simuleringService: SimuleringService,

        @Autowired
        private val vedtakService: VedtakService,

        @Autowired
        private val persongrunnlagService: PersongrunnlagService,

        @Autowired
        private val vilkårsvurderingService: VilkårsvurderingService,

        @Autowired
        private val tilbakekrevingService: TilbakekrevingService,
) {

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    @Tag("integration")
    fun `Kjør simulerings steg og verifiser at simulering legges til vedtak`() {
        val behandling = kjørStegprosessForFGB(
                tilSteg = StegType.VILKÅRSVURDERING,
                søkerFnr = randomFnr(),
                barnasIdenter = listOf(ClientMocks.barnFnr[0]),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                tilbakekrevingService = tilbakekrevingService
        )

        val simueringForSimuleringSteg = simuleringService.hentSimuleringPåVedtak(behandling.id)

        stegService.håndterSimulering(behandling = behandling)

        val vedtakEtterSimulering = vedtakService.hentAktivForBehandling(behandling.id) ?: error ("finner ikke vedtak")

        val simueringEtterSimuleringSteg = simuleringService.hentSimuleringPåVedtak(vedtakEtterSimulering.id)

        Assertions.assertEquals(0, simueringForSimuleringSteg.size)
        Assertions.assertEquals(1, simueringEtterSimuleringSteg.size)
    }
}
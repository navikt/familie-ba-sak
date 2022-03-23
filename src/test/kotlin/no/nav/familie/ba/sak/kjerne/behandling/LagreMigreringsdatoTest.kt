package no.nav.familie.ba.sak.kjerne.behandling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingMigreringsinfoRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth

class LagreMigreringsdatoTest {
    val behandlingRepository = mockk<BehandlingRepository>()
    val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    val behandlingMetrikker = mockk<BehandlingMetrikker>()
    val fagsakRepository = mockk<FagsakRepository>()
    val vedtakRepository = mockk<VedtakRepository>()
    val loggService = mockk<LoggService>()
    val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    val saksstatistikkEventPublisher = mockk<SaksstatistikkEventPublisher>()
    val oppgaveService = mockk<OppgaveService>()
    val infotrygdService = mockk<InfotrygdService>()
    val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    val personidentService = mockk<PersonidentService>()
    val featureToggleService = mockk<FeatureToggleService>()
    val taskRepository = mockk<TaskRepositoryWrapper>()
    val behandlingMigreringsinfoRepository = mockk<BehandlingMigreringsinfoRepository>()
    val behandlingSøknadsinfoRepository = mockk<BehandlingSøknadsinfoRepository>()
    val vilkårsvurderingService = mockk<VilkårsvurderingService>()

    private val behandlingService = BehandlingService(
        behandlingRepository,
        personopplysningGrunnlagRepository,
        andelTilkjentYtelseRepository,
        behandlingMetrikker,
        fagsakRepository,
        vedtakRepository,
        loggService,
        arbeidsfordelingService,
        saksstatistikkEventPublisher,
        oppgaveService,
        infotrygdService,
        vedtaksperiodeService,
        personidentService,
        featureToggleService,
        taskRepository,
        behandlingMigreringsinfoRepository,
        behandlingSøknadsinfoRepository,
        vilkårsvurderingService,
    )

    @Test
    fun `Lagre første migreringstidspunkt skal ikke kaste feil`() {
        every { behandlingMigreringsinfoRepository.finnSisteMigreringsdatoPåFagsak(any()) } returns null
        every { behandlingRepository.finnBehandlinger(any()) } returns emptyList()
        every { behandlingMigreringsinfoRepository.save(any()) } returns mockk()

        assertDoesNotThrow {
            behandlingService.lagreNedMigreringsdato(
                migreringsdato = LocalDate.now(),
                behandling = lagBehandling()
            )
        }
    }

    @Test
    fun `Lagre likt migreringstidspunkt skal kaste feil`() {
        every { behandlingMigreringsinfoRepository.finnSisteMigreringsdatoPåFagsak(any()) } returns LocalDate.now()
        every { behandlingRepository.finnBehandlinger(any()) } returns emptyList()
        every { behandlingMigreringsinfoRepository.save(any()) } returns mockk()

        val feil = assertThrows<FunksjonellFeil> {
            behandlingService.lagreNedMigreringsdato(
                migreringsdato = LocalDate.now(),
                behandling = lagBehandling(
                    behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                    årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO
                )
            )
        }
        assertEquals(
            "Migreringsdatoen du har lagt inn er lik eller senere enn eksisterende migreringsdato. Du må velge en tidligere migreringsdato for å fortsette.",
            feil.melding
        )
    }

    @Test
    fun `Lagre tidligere migreringstidspunkt skal ikke kaste feil`() {
        every { behandlingMigreringsinfoRepository.finnSisteMigreringsdatoPåFagsak(any()) } returns LocalDate.now()
        every { behandlingRepository.finnBehandlinger(any()) } returns emptyList()
        every { behandlingMigreringsinfoRepository.save(any()) } returns mockk()

        assertDoesNotThrow {
            behandlingService.lagreNedMigreringsdato(
                migreringsdato = LocalDate.now().minusMonths(1),
                behandling = lagBehandling(
                    behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                    årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO
                )
            )
        }
    }

    @Test
    fun `Lagre tidligere migreringstidspunkt skal kaste feil dersom forrige behandling ikke har lagret migreringsdato`() {
        every { behandlingMigreringsinfoRepository.finnSisteMigreringsdatoPåFagsak(any()) } returns null
        every { behandlingRepository.finnBehandlinger(any()) } returns listOf(
            lagBehandling().also {
                it.status = BehandlingStatus.AVSLUTTET
                it.resultat = Behandlingsresultat.INNVILGET
            }
        )
        every { vilkårsvurderingService.hentTidligsteVilkårsvurderingKnyttetTilMigrering(any()) } returns YearMonth.now()

        every { behandlingMigreringsinfoRepository.save(any()) } returns mockk()

        val feil = assertThrows<FunksjonellFeil> {
            behandlingService.lagreNedMigreringsdato(
                migreringsdato = LocalDate.now(),
                behandling = lagBehandling(
                    behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                    årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO
                )
            )
        }
        assertEquals(
            "Migreringsdatoen du har lagt inn er lik eller senere enn eksisterende migreringsdato. Du må velge en tidligere migreringsdato for å fortsette.",
            feil.melding
        )
    }
}

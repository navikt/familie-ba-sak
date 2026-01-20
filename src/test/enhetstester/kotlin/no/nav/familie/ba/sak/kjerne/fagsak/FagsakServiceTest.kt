package no.nav.familie.ba.sak.kjerne.fagsak

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagVisningsbehandling
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.randomBarnFnr
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.SkjermetBarnSøkerDto
import no.nav.familie.ba.sak.ekstern.restDomene.tilDto
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.integrasjoner.skyggesak.SkyggesakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.kjerne.institusjon.InstitusjonService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.skjermetbarnsøker.SkjermetBarnSøkerRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class FagsakServiceTest {
    private val fagsakRepository = mockk<FagsakRepository>()
    private val personRepository = mockk<PersonRepository>()
    private val andelerTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val personidentService = mockk<PersonidentService>()
    private val utvidetBehandlingService = mockk<UtvidetBehandlingService>()
    private val behandlingService = mockk<BehandlingService>()
    private val saksstatistikkEventPublisher = mockk<SaksstatistikkEventPublisher>()
    private val skyggesakService = mockk<SkyggesakService>()
    private val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    private val institusjonService = mockk<InstitusjonService>()
    private val organisasjonService = mockk<OrganisasjonService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val skjermetBarnSøkerRepository = mockk<SkjermetBarnSøkerRepository>()
    private val fagsakService =
        FagsakService(
            fagsakRepository = fagsakRepository,
            personRepository = personRepository,
            andelerTilkjentYtelseRepository = andelerTilkjentYtelseRepository,
            personidentService = personidentService,
            utvidetBehandlingService = utvidetBehandlingService,
            behandlingService = behandlingService,
            saksstatistikkEventPublisher = saksstatistikkEventPublisher,
            skyggesakService = skyggesakService,
            vedtaksperiodeService = vedtaksperiodeService,
            institusjonService = institusjonService,
            organisasjonService = organisasjonService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            featureToggleService = featureToggleService,
            skjermetBarnSøkerRepository = skjermetBarnSøkerRepository,
        )

    @Nested
    inner class LagRestMinimalFagsak {
        @Test
        fun `skal lage rest minimal fagsak`() {
            // Arrange
            val fagsak = lagFagsak()

            val visningsbehandling1 =
                lagVisningsbehandling(
                    behandlingId = 1L,
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    opprettetÅrsak = BehandlingÅrsak.SØKNAD,
                    aktiv = false,
                )

            val visningsbehandling2 =
                lagVisningsbehandling(
                    behandlingId = 2L,
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                    aktiv = false,
                )

            val sisteBehandlingSomErVedtatt =
                lagBehandling(
                    fagsak = fagsak,
                    id = visningsbehandling2.behandlingId,
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                    behandlingType = BehandlingType.REVURDERING,
                    årsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                    aktiv = false,
                )

            val utbetalingsperiode =
                Utbetalingsperiode(
                    periodeFom = LocalDate.now().minusYears(1),
                    periodeTom = LocalDate.now().plusYears(1),
                    vedtaksperiodetype = Vedtaksperiodetype.UTBETALING,
                    utbetalingsperiodeDetaljer = emptyList(),
                    ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD),
                    antallBarn = 1,
                    utbetaltPerMnd = 3000,
                )

            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak
            every { behandlingHentOgPersisterService.finnAktivForFagsak(fagsak.id) } returns null
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns sisteBehandlingSomErVedtatt
            every { vedtaksperiodeService.hentUtbetalingsperioder(sisteBehandlingSomErVedtatt) } returns listOf(utbetalingsperiode)
            every { behandlingHentOgPersisterService.hentVisningsbehandlinger(fagsak.id) } returns listOf(visningsbehandling1, visningsbehandling2)
            every { behandlingService.hentMigreringsdatoPåFagsak(fagsak.id) } returns null

            // Act
            val restMinimalFagsak = fagsakService.lagMinimalFagsakDto(fagsak.id)

            // Assert
            assertThat(restMinimalFagsak.opprettetTidspunkt).isNotNull()
            assertThat(restMinimalFagsak.id).isEqualTo(fagsak.id)
            assertThat(restMinimalFagsak.fagsakeier).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            assertThat(restMinimalFagsak.søkerFødselsnummer).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            assertThat(restMinimalFagsak.status).isEqualTo(fagsak.status)
            assertThat(restMinimalFagsak.løpendeKategori).isEqualTo(sisteBehandlingSomErVedtatt.kategori)
            assertThat(restMinimalFagsak.løpendeUnderkategori).isEqualTo(sisteBehandlingSomErVedtatt.underkategori)
            assertThat(restMinimalFagsak.gjeldendeUtbetalingsperioder).hasSize(1)
            assertThat(restMinimalFagsak.gjeldendeUtbetalingsperioder).containsExactly(utbetalingsperiode)
            assertThat(restMinimalFagsak.underBehandling).isFalse()
            assertThat(restMinimalFagsak.migreringsdato).isNull()
            assertThat(restMinimalFagsak.fagsakType).isEqualTo(FagsakType.NORMAL)
            assertThat(restMinimalFagsak.institusjon).isNull()
            assertThat(restMinimalFagsak.behandlinger).hasSize(2)
            assertThat(restMinimalFagsak.behandlinger).anySatisfy {
                assertThat(it.behandlingId).isEqualTo(visningsbehandling1.behandlingId)
                assertThat(it.opprettetTidspunkt).isEqualTo(visningsbehandling1.opprettetTidspunkt)
                assertThat(it.aktivertTidspunkt).isEqualTo(visningsbehandling1.aktivertTidspunkt)
                assertThat(it.kategori).isEqualTo(visningsbehandling1.kategori)
                assertThat(it.underkategori).isEqualTo(visningsbehandling1.underkategori.tilDto())
                assertThat(it.aktiv).isEqualTo(visningsbehandling1.aktiv)
                assertThat(it.årsak).isEqualTo(visningsbehandling1.opprettetÅrsak)
                assertThat(it.type).isEqualTo(visningsbehandling1.type)
                assertThat(it.status).isEqualTo(visningsbehandling1.status)
                assertThat(it.resultat).isEqualTo(visningsbehandling1.resultat)
                assertThat(it.vedtaksdato).isEqualTo(visningsbehandling1.vedtaksdato)
            }
            assertThat(restMinimalFagsak.behandlinger).anySatisfy {
                assertThat(it.behandlingId).isEqualTo(visningsbehandling2.behandlingId)
                assertThat(it.opprettetTidspunkt).isEqualTo(visningsbehandling2.opprettetTidspunkt)
                assertThat(it.aktivertTidspunkt).isEqualTo(visningsbehandling2.aktivertTidspunkt)
                assertThat(it.kategori).isEqualTo(visningsbehandling2.kategori)
                assertThat(it.underkategori).isEqualTo(visningsbehandling2.underkategori.tilDto())
                assertThat(it.aktiv).isEqualTo(visningsbehandling2.aktiv)
                assertThat(it.årsak).isEqualTo(visningsbehandling2.opprettetÅrsak)
                assertThat(it.type).isEqualTo(visningsbehandling2.type)
                assertThat(it.status).isEqualTo(visningsbehandling2.status)
                assertThat(it.resultat).isEqualTo(visningsbehandling2.resultat)
                assertThat(it.vedtaksdato).isEqualTo(visningsbehandling2.vedtaksdato)
            }
        }
    }

    @Nested
    inner class HentEllerOpprettFagsakTest {
        @Test
        fun `Skal kaste funksjonell feil dersom man forsøker å lage en fagsak med type skjermet barn i automatiske løyper`() {
            // Arrange
            every { featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_FAGSAKTYPE_SKJERMET_BARN) } returns true

            val barnIdent = randomBarnFnr(alder = 5)
            val søkerIdent = randomFnr()
            val skjermetBarnSøkerDto = SkjermetBarnSøkerDto(søkerIdent)

            // Act && Assert
            val frontendFeilmelding =
                assertThrows<FunksjonellFeil> {
                    fagsakService.hentEllerOpprettFagsak(
                        personIdent = barnIdent,
                        skjermetBarnSøker = skjermetBarnSøkerDto,
                        fraAutomatiskBehandling = true,
                        type = FagsakType.SKJERMET_BARN,
                    )
                }.frontendFeilmelding

            assertThat(frontendFeilmelding).isEqualTo("Kan ikke opprette fagsak med fagsaktype SKJERMET_BARN automatisk")
        }

        @Test
        fun `Skal kaste funksjonell feil dersom man forsøker å lage en fagsak med type skjermet barn uten at toggle er på`() {
            // Arrange
            every { featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_FAGSAKTYPE_SKJERMET_BARN) } returns false

            val barnIdent = randomBarnFnr(alder = 5)
            val søkerIdent = randomFnr()
            val skjermetBarnSøkerDto = SkjermetBarnSøkerDto(søkerIdent)

            // Act && Assert
            val frontendFeilmelding =
                assertThrows<FunksjonellFeil> {
                    fagsakService.hentEllerOpprettFagsak(
                        personIdent = barnIdent,
                        skjermetBarnSøker = skjermetBarnSøkerDto,
                        fraAutomatiskBehandling = true,
                        type = FagsakType.SKJERMET_BARN,
                    )
                }.frontendFeilmelding

            assertThat(frontendFeilmelding).isEqualTo("Fagsaktype SKJERMET_BARN er ikke støttet i denne versjonen av tjenesten.")
        }

        @Test
        fun `Skal kaste funksjonell feil dersom man forsøker å lage en fagsak med type skjermet barn men ikke sender med søkers ident`() {
            // Arrange
            val barnIdent = randomBarnFnr(alder = 5)
            val barnAktør = randomAktør(barnIdent)

            every { featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_FAGSAKTYPE_SKJERMET_BARN) } returns true
            every { personidentService.hentOgLagreAktør(barnIdent, true) } returns barnAktør

            // Act && Assert
            val frontendFeilmelding =
                assertThrows<FunksjonellFeil> {
                    fagsakService.hentEllerOpprettFagsak(
                        personIdent = barnIdent,
                        skjermetBarnSøker = null,
                        fraAutomatiskBehandling = false,
                        type = FagsakType.SKJERMET_BARN,
                    )
                }.frontendFeilmelding

            assertThat(frontendFeilmelding).isEqualTo("Mangler påkrevd variabel søkersident for skjermet barn søker")
        }

        @Test
        fun `Skal kaste funksjonell feil dersom man forsøker å lage en fagsak med type skjermet barn men søker og barn har samme ident`() {
            // Arrange
            val ident = randomBarnFnr(alder = 5)
            val barnIdent = ident
            val søkerIdent = ident
            val barnAktør = randomAktør(barnIdent)

            val skjermetBarnSøkerDto = SkjermetBarnSøkerDto(søkerIdent)

            every { featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_FAGSAKTYPE_SKJERMET_BARN) } returns true
            every { personidentService.hentOgLagreAktør(barnIdent, true) } returns barnAktør

            // Act && Assert
            val frontendFeilmelding =
                assertThrows<FunksjonellFeil> {
                    fagsakService.hentEllerOpprettFagsak(
                        personIdent = barnIdent,
                        skjermetBarnSøker = skjermetBarnSøkerDto,
                        fraAutomatiskBehandling = false,
                        type = FagsakType.SKJERMET_BARN,
                    )
                }.frontendFeilmelding

            assertThat(frontendFeilmelding).isEqualTo("Søker og barn søkt for kan ikke være lik for fagsak type skjermet barn")
        }

        @Test
        fun `Skal returnere eksisterende fagsak dersom man forsøker å lage en fagsak med type skjermet barn men samme kombinasjon av barn og søker finnes allerede`() {
            // Arrange
            val barnIdent = randomBarnFnr(alder = 5)
            val søkerIdent = randomFnr()
            val barnAktør = randomAktør(barnIdent)
            val søkerAktør = randomAktør(søkerIdent)
            val fagsak = lagFagsak(1, type = FagsakType.SKJERMET_BARN)

            val skjermetBarnSøkerDto = SkjermetBarnSøkerDto(søkerIdent)

            every { featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_FAGSAKTYPE_SKJERMET_BARN) } returns true
            every { personidentService.hentOgLagreAktør(barnIdent, true) } returns barnAktør
            every { personidentService.hentOgLagreAktør(søkerIdent, true) } returns søkerAktør
            every { fagsakRepository.finnFagsakForSkjermetBarnSøker(barnAktør, søkerAktør) } returns fagsak

            // Act
            val returnertFagsak =
                fagsakService.hentEllerOpprettFagsak(
                    personIdent = barnIdent,
                    skjermetBarnSøker = skjermetBarnSøkerDto,
                    fraAutomatiskBehandling = false,
                    type = FagsakType.SKJERMET_BARN,
                )

            // Assert
            assertThat(returnertFagsak).isEqualTo(fagsak)
        }
    }
}

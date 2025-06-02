package no.nav.familie.ba.sak.kjerne.fagsak

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagVisningsbehandling
import no.nav.familie.ba.sak.ekstern.restDomene.tilDto
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
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
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FagsakServiceTest {
    private val fagsakRepository = mockk<FagsakRepository>()
    private val personRepository = mockk<PersonRepository>()
    private val andelerTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val personidentService = mockk<PersonidentService>()
    private val utvidetBehandlingService = mockk<UtvidetBehandlingService>()
    private val behandlingService = mockk<BehandlingService>()
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val familieIntegrasjonerTilgangskontrollService = mockk<FamilieIntegrasjonerTilgangskontrollService>()
    private val saksstatistikkEventPublisher = mockk<SaksstatistikkEventPublisher>()
    private val skyggesakService = mockk<SkyggesakService>()
    private val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    private val institusjonService = mockk<InstitusjonService>()
    private val organisasjonService = mockk<OrganisasjonService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val unleashService = mockk<UnleashNextMedContextService>()
    private val fagsakService =
        FagsakService(
            fagsakRepository = fagsakRepository,
            personRepository = personRepository,
            andelerTilkjentYtelseRepository = andelerTilkjentYtelseRepository,
            personidentService = personidentService,
            utvidetBehandlingService = utvidetBehandlingService,
            behandlingService = behandlingService,
            personopplysningerService = personopplysningerService,
            familieIntegrasjonerTilgangskontrollService = familieIntegrasjonerTilgangskontrollService,
            saksstatistikkEventPublisher = saksstatistikkEventPublisher,
            skyggesakService = skyggesakService,
            vedtaksperiodeService = vedtaksperiodeService,
            institusjonService = institusjonService,
            organisasjonService = organisasjonService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            unleashService = unleashService,
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
            val restMinimalFagsak = fagsakService.lagRestMinimalFagsak(fagsak.id)

            // Assert
            assertThat(restMinimalFagsak.opprettetTidspunkt).isNotNull()
            assertThat(restMinimalFagsak.id).isEqualTo(fagsak.id)
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
}

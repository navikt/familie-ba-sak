package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.common.lagPersonEnkel
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.integrasjoner.oppgave.TilpassArbeidsfordelingService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.simulering.lagBehandling
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.unleash.UnleashService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ArbeidsfordelingServiceTest {
    private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository = mockk()
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository = mockk()
    private val personidentService: PersonidentService = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val loggService: LoggService = mockk()
    private val integrasjonClient: IntegrasjonClient = mockk()
    private val personopplysningerService: PersonopplysningerService = mockk()
    private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher = mockk()
    private val tilpassArbeidsfordelingService: TilpassArbeidsfordelingService = mockk()
    private val unleashService: UnleashService = mockk()

    private val arbeidsfordelingService: ArbeidsfordelingService =
        ArbeidsfordelingService(
            arbeidsfordelingPåBehandlingRepository = arbeidsfordelingPåBehandlingRepository,
            personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
            personidentService = personidentService,
            oppgaveService = oppgaveService,
            loggService = loggService,
            integrasjonClient = integrasjonClient,
            personopplysningerService = personopplysningerService,
            saksstatistikkEventPublisher = saksstatistikkEventPublisher,
            tilpassArbeidsfordelingService = tilpassArbeidsfordelingService,
            unleashService = unleashService,
        )

    @Nested
    inner class FastsettBehandlendeEnhet {
        @Test
        fun `skal overstyre behandlende enhet fra NORG dersom enhet fra finnArbeidsfordelingForOppgave er en annen`() {
            // Arrange
            val behandling = lagBehandling()
            val søker = lagPersonEnkel(PersonType.SØKER, behandling.fagsak.aktør)
            val barn = lagPersonEnkel(PersonType.BARN)
            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    enhetNavn = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnavn,
                )

            val oppdatertArbeidsfordelingPåBehandling = mockk<ArbeidsfordelingPåBehandling>()

            every {
                arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(behandling.id)
            } returns null

            every {
                unleashService.isEnabled(FeatureToggleConfig.OPPRETT_SAK_PÅ_RIKTIG_ENHET_OG_SAKSBEHANDLER, false)
            } returns true

            every { personopplysningerService.hentPersoninfoEnkel(any()).adressebeskyttelseGradering } returns null

            every {
                personopplysningGrunnlagRepository
                    .finnSøkerOgBarnAktørerTilAktiv(behandling.id)
            } returns listOf(søker, barn)

            every { integrasjonClient.hentBehandlendeEnhet(søker.aktør.aktivFødselsnummer()) } returns
                listOf(
                    arbeidsfordelingsenhet,
                )

            every {
                tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                    arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                    navIdent = NavIdent(SikkerhetContext.hentSaksbehandler()),
                )
            } returns Arbeidsfordelingsenhet(enhetId = BarnetrygdEnhet.OSLO.enhetsnummer, enhetNavn = BarnetrygdEnhet.OSLO.enhetsnavn)

            val arbeidsfordelingPåBehandlingSlot = slot<ArbeidsfordelingPåBehandling>()

            every {
                arbeidsfordelingPåBehandlingRepository.save(capture(arbeidsfordelingPåBehandlingSlot))
            } returns oppdatertArbeidsfordelingPåBehandling

            every { oppdatertArbeidsfordelingPåBehandling.toSecureString() } returns ""

            // Act
            arbeidsfordelingService.fastsettBehandlendeEnhet(behandling, null)

            // Assert
            val arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandlingSlot.captured
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetId).isEqualTo(BarnetrygdEnhet.OSLO.enhetsnummer)
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetNavn).isEqualTo(BarnetrygdEnhet.OSLO.enhetsnavn)
        }
    }
}

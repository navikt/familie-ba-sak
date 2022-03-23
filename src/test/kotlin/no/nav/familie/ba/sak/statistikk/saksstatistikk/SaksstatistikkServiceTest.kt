package no.nav.familie.ba.sak.statistikk.saksstatistikk

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.unmockkAll
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.tilfeldigSøker
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.dataGenerator.SettPåVent.lagSettPåVent
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.SYSTEM_FORKORTELSE
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.SYSTEM_NAVN
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SaksstatistikkServiceTest(
    @MockK(relaxed = true)
    private val behandlingService: BehandlingService,

    @MockK
    private val arbeidsfordelingService: ArbeidsfordelingService,

    @MockK
    private val totrinnskontrollService: TotrinnskontrollService,

    @MockK
    private val fagsakService: FagsakService,

    @MockK
    private val personopplysningerService: PersonopplysningerService,

    @MockK
    private val personidentService: PersonidentService,

    @MockK
    private val persongrunnlagService: PersongrunnlagService,

    @MockK
    private val vedtakService: VedtakService,

    @MockK
    private val vedtaksperiodeService: VedtaksperiodeService,

    @MockK
    private val featureToggleService: FeatureToggleService,

    @MockK
    private val settPåVentService: SettPåVentService,
) {

    private val sakstatistikkService = SaksstatistikkService(
        behandlingService,
        arbeidsfordelingService,
        totrinnskontrollService,
        vedtakService,
        fagsakService,
        personopplysningerService,
        persongrunnlagService,
        vedtaksperiodeService,
        featureToggleService,
        settPåVentService,
    )

    @BeforeAll
    fun init() {
        MockKAnnotations.init()

        every { arbeidsfordelingService.hentAbeidsfordelingPåBehandling(any()) } returns ArbeidsfordelingPåBehandling(
            behandlendeEnhetId = "4820",
            behandlendeEnhetNavn = "Nav",
            behandlingId = 1
        )
        every { arbeidsfordelingService.hentArbeidsfordelingsenhet(any()) } returns Arbeidsfordelingsenhet(
            "4821",
            "NAV"
        )

        every { settPåVentService.finnAktivSettPåVentPåBehandling(any()) } returns lagSettPåVent()
    }

    @AfterAll
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Skal mappe henleggelsesårsak til behandlingDVH for henlagt behandling`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE).also {
            it.resultat = Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET
        }

        every { behandlingService.hent(any()) } returns behandling
        every { totrinnskontrollService.hentAktivForBehandling(any()) } returns null
        every { vedtakService.hentAktivForBehandling(any()) } returns null

        val behandlingDvh = sakstatistikkService.mapTilBehandlingDVH(2)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(behandlingDvh))

        assertThat(behandlingDvh?.resultat).isEqualTo("HENLAGT_FEILAKTIG_OPPRETTET")
        assertThat(behandlingDvh?.resultatBegrunnelser).hasSize(0)
    }

    @Test
    fun `Skal mappe til behandlingDVH for Automatisk rute`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE, skalBehandlesAutomatisk = true).also {
            it.resultat = Behandlingsresultat.INNVILGET
        }

        val vedtak = lagVedtak(behandling)
        val vedtaksperiodeMedBegrunnelser =
            lagVedtaksperiodeMedBegrunnelser()

        every { behandlingService.hent(any()) } returns behandling
        every { vedtakService.hentAktivForBehandling(any()) } returns vedtak
        every { vedtaksperiodeService.hentPersisterteVedtaksperioder(any()) } returns listOf(
            vedtaksperiodeMedBegrunnelser
        )
        every { totrinnskontrollService.hentAktivForBehandling(any()) } returns Totrinnskontroll(
            saksbehandler = SYSTEM_NAVN,
            saksbehandlerId = SYSTEM_FORKORTELSE,
            beslutter = SYSTEM_NAVN,
            beslutterId = SYSTEM_FORKORTELSE,
            godkjent = true,
            behandling = behandling
        )

        val behandlingDvh = sakstatistikkService.mapTilBehandlingDVH(2)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(behandlingDvh))

        assertThat(behandlingDvh?.funksjonellTid).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(behandlingDvh?.tekniskTid).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(behandlingDvh?.mottattDato).isEqualTo(
            ZonedDateTime.of(
                behandling.opprettetTidspunkt,
                SaksstatistikkService.TIMEZONE
            )
        )
        assertThat(behandlingDvh?.registrertDato).isEqualTo(
            ZonedDateTime.of(
                behandling.opprettetTidspunkt,
                SaksstatistikkService.TIMEZONE
            )
        )
        assertThat(behandlingDvh?.vedtaksDato).isEqualTo(vedtak.vedtaksdato?.toLocalDate())
        assertThat(behandlingDvh?.behandlingId).isEqualTo(behandling.id.toString())
        assertThat(behandlingDvh?.relatertBehandlingId).isNull()
        assertThat(behandlingDvh?.sakId).isEqualTo(behandling.fagsak.id.toString())
        assertThat(behandlingDvh?.vedtakId).isEqualTo(vedtak.id.toString())
        assertThat(behandlingDvh?.behandlingType).isEqualTo(behandling.type.name)
        assertThat(behandlingDvh?.utenlandstilsnitt).isEqualTo(behandling.kategori.name)
        assertThat(behandlingDvh?.behandlingKategori).isEqualTo(behandling.underkategori.name)
        assertThat(behandlingDvh?.behandlingUnderkategori).isNull()
        assertThat(behandlingDvh?.behandlingStatus).isEqualTo(behandling.status.name)
        assertThat(behandlingDvh?.totrinnsbehandling).isFalse
        assertThat(behandlingDvh?.saksbehandler).isEqualTo(SYSTEM_FORKORTELSE)
        assertThat(behandlingDvh?.beslutter).isEqualTo(SYSTEM_FORKORTELSE)
        assertThat(behandlingDvh?.avsender).isEqualTo("familie-ba-sak")
        assertThat(behandlingDvh?.versjon).isNotEmpty
        assertThat(behandlingDvh?.resultat).isEqualTo(behandling.resultat.name)
    }

    @Test
    fun `Skal mappe til behandlingDVH for manuell rute`() {
        val behandling =
            lagBehandling(årsak = BehandlingÅrsak.SØKNAD).also { it.resultat = Behandlingsresultat.AVSLÅTT }

        every { totrinnskontrollService.hentAktivForBehandling(any()) } returns Totrinnskontroll(
            saksbehandler = "Saksbehandler",
            saksbehandlerId = "saksbehandlerId",
            beslutter = "Beslutter",
            beslutterId = "beslutterId",
            godkjent = true,
            behandling = behandling
        )

        val vedtak = lagVedtak(behandling)

        val vedtaksperiodeFom = LocalDate.of(2021, 3, 11)
        val vedtaksperiodeTom = LocalDate.of(21, 4, 11)
        val vedtaksperiodeMedBegrunnelser =
            lagVedtaksperiodeMedBegrunnelser(vedtak = vedtak, fom = vedtaksperiodeFom, tom = vedtaksperiodeTom)

        every { behandlingService.hent(any()) } returns behandling
        every { persongrunnlagService.hentSøker(any()) } returns tilfeldigSøker()
        every { persongrunnlagService.hentBarna(any<Behandling>()) } returns listOf(
            tilfeldigPerson()
                .copy(aktør = randomAktørId("01010000001"))
        )

        every { vedtakService.hentAktivForBehandling(any()) } returns vedtak
        every { vedtaksperiodeService.hentPersisterteVedtaksperioder(any()) } returns listOf(
            vedtaksperiodeMedBegrunnelser
        )

        val mottattDato = LocalDateTime.now()
        every { behandlingService.hentSøknadMottattDato(any()) } returns mottattDato

        every { featureToggleService.isEnabled(any()) } returns true

        val tidSattPåVent = LocalDate.now()
        val frist = LocalDate.now().plusWeeks(3)
        every { settPåVentService.finnAktivSettPåVentPåBehandling(any()) } returns lagSettPåVent(
            tidSattPåVent = tidSattPåVent,
            årsak = SettPåVentÅrsak.AVVENTER_DOKUMENTASJON,
            frist = frist
        )

        val behandlingDvh = sakstatistikkService.mapTilBehandlingDVH(2)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(behandlingDvh))

        assertThat(behandlingDvh?.funksjonellTid).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(behandlingDvh?.tekniskTid).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(behandlingDvh?.mottattDato).isEqualTo(
            mottattDato.atZone(SaksstatistikkService.TIMEZONE)
        )
        assertThat(behandlingDvh?.registrertDato).isEqualTo(
            behandling.opprettetTidspunkt.atZone(SaksstatistikkService.TIMEZONE)
        )
        assertThat(behandlingDvh?.vedtaksDato).isEqualTo(vedtak.vedtaksdato?.toLocalDate())
        assertThat(behandlingDvh?.behandlingId).isEqualTo(behandling.id.toString())
        assertThat(behandlingDvh?.relatertBehandlingId).isNull()
        assertThat(behandlingDvh?.sakId).isEqualTo(behandling.fagsak.id.toString())
        assertThat(behandlingDvh?.vedtakId).isEqualTo(vedtak.id.toString())
        assertThat(behandlingDvh?.behandlingType).isEqualTo(behandling.type.name)
        assertThat(behandlingDvh?.behandlingStatus).isEqualTo(behandling.status.name)
        assertThat(behandlingDvh?.totrinnsbehandling).isTrue
        assertThat(behandlingDvh?.saksbehandler).isEqualTo("saksbehandlerId")
        assertThat(behandlingDvh?.beslutter).isEqualTo("beslutterId")
        assertThat(behandlingDvh?.avsender).isEqualTo("familie-ba-sak")
        assertThat(behandlingDvh?.versjon).isNotEmpty
        assertThat(behandlingDvh?.settPaaVent?.tidSattPaaVent)
            .isEqualTo(tidSattPåVent.atStartOfDay(SaksstatistikkService.TIMEZONE))
        assertThat(behandlingDvh?.settPaaVent?.aarsak).isEqualTo(SettPåVentÅrsak.AVVENTER_DOKUMENTASJON.name)
        assertThat(behandlingDvh?.settPaaVent?.frist)
            .isEqualTo(frist.atStartOfDay(SaksstatistikkService.TIMEZONE))
    }

    @Test
    fun `Skal mappe til sakDVH, ingen aktiv behandling, så kun aktør SØKER, bostedsadresse i Norge`() {
        every { fagsakService.hentPåFagsakId(any()) } answers {
            Fagsak(status = FagsakStatus.OPPRETTET, aktør = tilAktør("12345678910"))
        }

        every { personidentService.hentAktør("12345678910") } returns Aktør("1234567891000")
        every { personidentService.hentAktør("12345678911") } returns Aktør("1234567891100")
        every { personopplysningerService.hentPersoninfoEnkel(tilAktør("12345678910")) } returns PersonInfo(
            fødselsdato = LocalDate.of(
                2017,
                3,
                1
            ),
            bostedsadresser = mutableListOf(
                Bostedsadresse(
                    vegadresse = Vegadresse(
                        matrikkelId = 1111,
                        husnummer = null,
                        husbokstav = null,
                        bruksenhetsnummer = null,
                        adressenavn = null,
                        kommunenummer = null,
                        tilleggsnavn = null,
                        postnummer = "2222"
                    )
                )
            )
        )

        every { behandlingService.hentAktivForFagsak(any()) } returns null

        val sakDvh = sakstatistikkService.mapTilSakDvh(1)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sakDvh))

        assertThat(sakDvh?.aktorId).isEqualTo(1234567891000)
        assertThat(sakDvh?.aktorer).hasSize(1).extracting("rolle").contains("SØKER")
        assertThat(sakDvh?.sakStatus).isEqualTo(FagsakStatus.OPPRETTET.name)
        assertThat(sakDvh?.avsender).isEqualTo("familie-ba-sak")
        assertThat(sakDvh?.bostedsland).isEqualTo("NO")
    }

    @Test
    fun `Skal mappe til sakDVH, ingen aktiv behandling, så kun aktør SØKER, bostedsadresse i Utland`() {
        every { fagsakService.hentPåFagsakId(any()) } answers {
            Fagsak(status = FagsakStatus.OPPRETTET, aktør = tilAktør("12345678910"))
        }

        every { personidentService.hentAktør("12345678910") } returns Aktør("1234567891000")
        every { personidentService.hentAktør("12345678911") } returns Aktør("1234567891100")

        every { personopplysningerService.hentPersoninfoEnkel(tilAktør("12345678910")) } returns PersonInfo(
            fødselsdato = LocalDate.of(
                2017,
                3,
                1
            )
        )
        every { personopplysningerService.hentLandkodeUtenlandskBostedsadresse(tilAktør("12345678910")) } returns "SE"

        every { behandlingService.hentAktivForFagsak(any()) } returns null

        val sakDvh = sakstatistikkService.mapTilSakDvh(1)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sakDvh))

        assertThat(sakDvh?.aktorId).isEqualTo(1234567891000)
        assertThat(sakDvh?.aktorer).hasSize(1).extracting("rolle").contains("SØKER")
        assertThat(sakDvh?.sakStatus).isEqualTo(FagsakStatus.OPPRETTET.name)
        assertThat(sakDvh?.avsender).isEqualTo("familie-ba-sak")
        assertThat(sakDvh?.bostedsland).isEqualTo("SE")
    }

    @Test
    fun `Skal mappe til sakDVH, aktører har SØKER og BARN`() {
        val randomAktørId = randomAktørId()
        every { fagsakService.hentPåFagsakId(any()) } answers {
            Fagsak(status = FagsakStatus.OPPRETTET, aktør = randomAktørId)
        }
        every { personidentService.hentAktør(any()) } returns randomAktørId
        every { personopplysningerService.hentLandkodeUtenlandskBostedsadresse(any()) } returns "SE"

        every { persongrunnlagService.hentAktiv(any()) } returns lagTestPersonopplysningGrunnlag(
            1,
            tilfeldigPerson(personType = PersonType.BARN),
            tilfeldigPerson(personType = PersonType.SØKER)
        )

        every { behandlingService.hentAktivForFagsak(any()) } returns lagBehandling()

        val sakDvh = sakstatistikkService.mapTilSakDvh(1)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sakDvh))

        assertThat(sakDvh?.aktorId).isEqualTo(randomAktørId.aktørId.toLong())
        assertThat(sakDvh?.aktorer).hasSize(2).extracting("rolle").containsOnly("SØKER", "BARN")
        assertThat(sakDvh?.sakStatus).isEqualTo(FagsakStatus.OPPRETTET.name)
        assertThat(sakDvh?.avsender).isEqualTo("familie-ba-sak")
        assertThat(sakDvh?.bostedsland).isEqualTo("SE")
    }
}

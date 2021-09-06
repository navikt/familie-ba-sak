package no.nav.familie.ba.sak.statistikk.saksstatistikk

import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.tilfeldigSøker
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.journalføring.JournalføringService
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.DbJournalpost
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.DbJournalpostType
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.JournalføringRepository
import no.nav.familie.ba.sak.integrasjoner.lagTestJournalpost
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakPerson
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.SYSTEM_FORKORTELSE
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.SYSTEM_NAVN
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.journalpost.RelevantDato
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SaksstatistikkServiceTest {


    private val behandlingService: BehandlingService = mockk(relaxed = true)
    private val journalføringRepository: JournalføringRepository = mockk()
    private val journalføringService: JournalføringService = mockk()
    private val arbeidsfordelingService: ArbeidsfordelingService = mockk()
    private val totrinnskontrollService: TotrinnskontrollService = mockk()
    private val fagsakService: FagsakService = mockk()
    private val personopplysningerService: PersonopplysningerService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val vedtakService: VedtakService = mockk()
    private val envService: EnvService = mockk()
    private val vedtaksperiodeService: VedtaksperiodeService = mockk()

    private val sakstatistikkService = SaksstatistikkService(
        behandlingService,
        journalføringRepository,
        journalføringService,
        arbeidsfordelingService,
        totrinnskontrollService,
        vedtakService,
        fagsakService,
        personopplysningerService,
        persongrunnlagService,
        envService,
        vedtaksperiodeService,
    )


    @BeforeAll
    fun init() {
        every { arbeidsfordelingService.hentAbeidsfordelingPåBehandling(any()) } returns ArbeidsfordelingPåBehandling(
            behandlendeEnhetId = "4820",
            behandlendeEnhetNavn = "Nav",
            behandlingId = 1
        )
        every { arbeidsfordelingService.hentArbeidsfordelingsenhet(any()) } returns Arbeidsfordelingsenhet("4821", "NAV")
        every { envService.skalIverksetteBehandling() } returns true
    }

    @AfterAll
    fun tearDown() {
        unmockkAll()
    }


    @Test
    fun `Skal mappe henleggelsesårsak til behandlingDVH for henlagt behandling`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE).also {
            it.resultat = BehandlingResultat.HENLAGT_FEILAKTIG_OPPRETTET
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
        val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE, automatiskOpprettelse = true).also {
            it.resultat = BehandlingResultat.INNVILGET
        }

        val vedtakFom = LocalDate.of(2021, 2, 11)
        val vedtakTom = LocalDate.of(21, 3, 11)

        val vedtak = lagVedtak(behandling).also {
            it.vedtakBegrunnelser.add(
                VedtakBegrunnelse(
                    vedtak = it,
                    fom = vedtakFom,
                    tom = vedtakTom,
                    begrunnelse = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET
                )
            )
        }
        val vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(vedtak = vedtak)

        every { behandlingService.hent(any()) } returns behandling
        every { vedtakService.hentAktivForBehandling(any()) } returns vedtak
        every { vedtaksperiodeService.hentPersisterteVedtaksperioder(any()) } returns listOf(vedtaksperiodeMedBegrunnelser)
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
        assertThat(behandlingDvh?.resultatBegrunnelser).hasSize(2)
            .extracting("vedtakBegrunnelse")
            .containsOnly("FORTSATT_INNVILGET_SØKER_OG_BARN_BOSATT_I_RIKET", "INNVILGET_BOSATT_I_RIKTET")
        assertThat(behandlingDvh?.resultatBegrunnelser)
            .extracting("type")
            .containsOnly("FORTSATT_INNVILGET", "INNVILGELSE")

    }

    @Test
    fun `Skal mappe til behandlingDVH for manuell rute`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD).also { it.resultat = BehandlingResultat.AVSLÅTT }


        every { totrinnskontrollService.hentAktivForBehandling(any()) } returns Totrinnskontroll(
            saksbehandler = "Saksbehandler",
            saksbehandlerId = "saksbehandlerId",
            beslutter = "Beslutter",
            beslutterId = "beslutterId",
            godkjent = true,
            behandling = behandling
        )

        val vedtakFom = LocalDate.of(2021, 2, 11)
        val vedtakTom = LocalDate.of(21, 3, 11)

        val vedtak = lagVedtak(behandling).also {
            it.vedtakBegrunnelser.add(
                VedtakBegrunnelse(
                    vedtak = it,
                    fom = vedtakFom,
                    tom = vedtakTom,
                    begrunnelse = VedtakBegrunnelseSpesifikasjon.OPPHØR_SØKER_HAR_IKKE_FAST_OMSORG
                )
            )
        }

        val vedtaksperiodeFom = LocalDate.of(2021, 3, 11)
        val vedtaksperiodeTom = LocalDate.of(21, 4, 11)
        val vedtaksperiodeMedBegrunnelser =
            lagVedtaksperiodeMedBegrunnelser(vedtak = vedtak, fom = vedtaksperiodeFom, tom = vedtaksperiodeTom)

        every { behandlingService.hent(any()) } returns behandling
        every { persongrunnlagService.hentSøker(any()) } returns tilfeldigSøker()
        every { persongrunnlagService.hentBarna(any()) } returns listOf(
            tilfeldigPerson()
                .copy(personIdent = PersonIdent("01010000001"))
        )

        every { vedtakService.hentAktivForBehandling(any()) } returns vedtak
        every { vedtaksperiodeService.hentPersisterteVedtaksperioder(any()) } returns listOf(vedtaksperiodeMedBegrunnelser)
        every { journalføringRepository.findByBehandlingId(any()) } returns listOf(
            DbJournalpost(
                1,
                "foo",
                LocalDateTime.now(),
                behandling,
                "123",
                DbJournalpostType.I
            )
        )
        val mottattDato = LocalDateTime.of(2019, 12, 20, 10, 0, 0)
        val jp = lagTestJournalpost("123", "123").copy(relevanteDatoer = listOf(RelevantDato(mottattDato, "DATO_REGISTRERT")))
        every { journalføringService.hentJournalpost(any()) } returns Ressurs.Companion.success(jp)

        val behandlingDvh = sakstatistikkService.mapTilBehandlingDVH(2)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(behandlingDvh))


        assertThat(behandlingDvh?.funksjonellTid).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(behandlingDvh?.tekniskTid).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(behandlingDvh?.mottattDato).isEqualTo(mottattDato.atZone(SaksstatistikkService.TIMEZONE))
        assertThat(behandlingDvh?.registrertDato).isEqualTo(mottattDato.atZone(SaksstatistikkService.TIMEZONE))
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
        assertThat(behandlingDvh?.resultatBegrunnelser).hasSize(2)
            .extracting("fom")
            .containsOnly(vedtaksperiodeFom, vedtakFom)
        assertThat(behandlingDvh?.resultatBegrunnelser)
            .extracting("tom")
            .containsOnly(vedtaksperiodeTom, vedtakTom)
        assertThat(behandlingDvh?.resultatBegrunnelser)
            .extracting("type")
            .containsOnly("FORTSATT_INNVILGET", "OPPHØR")
        assertThat(behandlingDvh?.avsender).isEqualTo("familie-ba-sak")
        assertThat(behandlingDvh?.versjon).isNotEmpty

    }


    @Test
    fun `Skal mappe til sakDVH, ingen aktiv behandling, så kun aktør SØKER, bostedsadresse i Norge`() {
        every { fagsakService.hentPåFagsakId(any()) } answers {
            val fagsak = Fagsak(status = FagsakStatus.OPPRETTET)
            val fagsakPerson = FagsakPerson(personIdent = PersonIdent("12345678910"), fagsak = fagsak)
            fagsak.copy(søkerIdenter = setOf(fagsakPerson))
        }

        every { personopplysningerService.hentAktivAktørId(Ident("12345678910")) } returns AktørId("1234567891011")
        every { personopplysningerService.hentAktivAktørId(Ident("12345678911")) } returns AktørId("1234567891111")
        every { personopplysningerService.hentPersoninfoEnkel("12345678910") } returns PersonInfo(
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

        assertThat(sakDvh?.aktorId).isEqualTo(1234567891011)
        assertThat(sakDvh?.aktorer).hasSize(1).extracting("rolle").contains("SØKER")
        assertThat(sakDvh?.sakStatus).isEqualTo(FagsakStatus.OPPRETTET.name)
        assertThat(sakDvh?.avsender).isEqualTo("familie-ba-sak")
        assertThat(sakDvh?.bostedsland).isEqualTo("NO")
    }

    @Test
    fun `Skal mappe til sakDVH, ingen aktiv behandling, så kun aktør SØKER, bostedsadresse i Utland`() {
        every { fagsakService.hentPåFagsakId(any()) } answers {
            val fagsak = Fagsak(status = FagsakStatus.OPPRETTET)
            val fagsakPerson = FagsakPerson(personIdent = PersonIdent("12345678910"), fagsak = fagsak)
            fagsak.copy(søkerIdenter = setOf(fagsakPerson))
        }

        every { personopplysningerService.hentAktivAktørId(Ident("12345678910")) } returns AktørId("1234567891011")
        every { personopplysningerService.hentAktivAktørId(Ident("12345678911")) } returns AktørId("1234567891111")
        every { personopplysningerService.hentPersoninfoEnkel("12345678910") } returns PersonInfo(
            fødselsdato = LocalDate.of(
                2017,
                3,
                1
            )
        )
        every { personopplysningerService.hentLandkodeUtenlandskBostedsadresse("12345678910") } returns "SE"


        every { behandlingService.hentAktivForFagsak(any()) } returns null

        val sakDvh = sakstatistikkService.mapTilSakDvh(1)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sakDvh))

        assertThat(sakDvh?.aktorId).isEqualTo(1234567891011)
        assertThat(sakDvh?.aktorer).hasSize(1).extracting("rolle").contains("SØKER")
        assertThat(sakDvh?.sakStatus).isEqualTo(FagsakStatus.OPPRETTET.name)
        assertThat(sakDvh?.avsender).isEqualTo("familie-ba-sak")
        assertThat(sakDvh?.bostedsland).isEqualTo("SE")
    }

    @Test
    fun `Skal mappe til sakDVH, aktører har SØKER og BARN`() {
        every { fagsakService.hentPåFagsakId(any()) } answers {
            val fagsak = Fagsak(status = FagsakStatus.OPPRETTET)
            val fagsakPerson = FagsakPerson(personIdent = PersonIdent("12345678910"), fagsak = fagsak)
            fagsak.copy(søkerIdenter = setOf(fagsakPerson))
        }
        val randomAktørId = randomAktørId()
        every { personopplysningerService.hentAktivAktørId(any()) } returns randomAktørId
        every { personopplysningerService.hentLandkodeUtenlandskBostedsadresse(any()) } returns "SE"

        every { persongrunnlagService.hentAktiv(any()) } returns lagTestPersonopplysningGrunnlag(
            1,
            tilfeldigPerson(personType = PersonType.BARN),
            tilfeldigPerson(personType = PersonType.SØKER)
        )



        every { behandlingService.hentAktivForFagsak(any()) } returns lagBehandling()

        val sakDvh = sakstatistikkService.mapTilSakDvh(1)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sakDvh))

        assertThat(sakDvh?.aktorId).isEqualTo(randomAktørId.id.toLong())
        assertThat(sakDvh?.aktorer).hasSize(2).extracting("rolle").containsOnly("SØKER", "BARN")
        assertThat(sakDvh?.sakStatus).isEqualTo(FagsakStatus.OPPRETTET.name)
        assertThat(sakDvh?.avsender).isEqualTo("familie-ba-sak")
        assertThat(sakDvh?.bostedsland).isEqualTo("SE")

    }

}
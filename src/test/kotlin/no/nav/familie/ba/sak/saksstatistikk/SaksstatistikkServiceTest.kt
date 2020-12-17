package no.nav.familie.ba.sak.saksstatistikk

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.integrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.lagTestJournalpost
import no.nav.familie.ba.sak.journalføring.JournalføringService
import no.nav.familie.ba.sak.journalføring.domene.DbJournalpost
import no.nav.familie.ba.sak.journalføring.domene.JournalføringRepository
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.SYSTEM_NAVN
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.journalpost.RelevantDato
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SaksstatistikkServiceTest {


    private val behandlingService: BehandlingService = mockk()
    private val behandlingRestultatService: VilkårsvurderingService = mockk()
    private val journalføringRepository: JournalføringRepository = mockk()
    private val journalføringService: JournalføringService = mockk()
    private val arbeidsfordelingService: ArbeidsfordelingService = mockk()
    private val totrinnskontrollService: TotrinnskontrollService = mockk()
    private val fagsakService: FagsakService = mockk()
    private val personopplysningerService: PersonopplysningerService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val vedtakService: VedtakService = mockk()
    private val envService: EnvService = mockk()

    private val sakstatistikkService = SaksstatistikkService(
            behandlingService,
            behandlingRestultatService,
            journalføringRepository,
            journalføringService,
            arbeidsfordelingService,
            totrinnskontrollService,
            vedtakService,
            fagsakService,
            personopplysningerService,
            persongrunnlagService,
            envService)


    @BeforeAll
    fun init() {
        every { arbeidsfordelingService.hentAbeidsfordelingPåBehandling(any()) } returns ArbeidsfordelingPåBehandling(
                behandlendeEnhetId = "4820",
                behandlendeEnhetNavn = "Nav",
                behandlingId = 1)
        every { arbeidsfordelingService.hentArbeidsfordelingsenhet(any()) } returns Arbeidsfordelingsenhet("4821", "NAV")
        every { envService.skalIverksetteBehandling() } returns true
    }

    @Test
    fun `Skal mappe henleggelsesårsak til behandlingDVH for henlagt behandling`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE).also {
            it.resultat = BehandlingResultat.HENLAGT_FEILAKTIG_OPPRETTET
        }

        val vilkårsvurdering = lagVilkårsvurdering("01010000001",
                                                   behandling,
                                                   Resultat.IKKE_OPPFYLT)

        every { behandlingService.hent(any()) } returns behandling
        every { behandlingRestultatService.hentAktivForBehandling(any()) } returns vilkårsvurdering
        every { totrinnskontrollService.hentAktivForBehandling(any()) } returns null
        every { vedtakService.hentAktivForBehandling(any()) } returns null

        val behandlingDvh = sakstatistikkService.mapTilBehandlingDVH(2, 1)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(behandlingDvh))

        assertThat(behandlingDvh?.resultatBegrunnelser).hasSize(1)
                .extracting("resultatBegrunnelse")
                .contains("Henlagt feilaktig opprettet")
    }

    @Test
    fun `Skal mappe til behandlingDVH for Automatisk rute`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE, automatiskOpprettelse = true).also {
            it.resultat = BehandlingResultat.INNVILGET
        }

        val vilkårsvurdering = lagVilkårsvurdering(behandling.fagsak.hentAktivIdent().ident,
                                                   behandling,
                                                   Resultat.OPPFYLT)
        val vedtak = lagVedtak(behandling)
        every { behandlingService.hent(any()) } returns behandling
        every { behandlingRestultatService.hentAktivForBehandling(any()) } returns vilkårsvurdering
        every { vedtakService.hentAktivForBehandling(any()) } returns vedtak
        every { totrinnskontrollService.hentAktivForBehandling(any()) } returns Totrinnskontroll(
                saksbehandler = SYSTEM_NAVN,
                beslutter = SYSTEM_NAVN,
                godkjent = true,
                behandling = behandling
        )

        val behandlingDvh = sakstatistikkService.mapTilBehandlingDVH(2, 1)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(behandlingDvh))


        assertThat(behandlingDvh?.funksjonellTid).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(behandlingDvh?.tekniskTid).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(behandlingDvh?.mottattDato).isEqualTo(ZonedDateTime.of(behandling.opprettetTidspunkt,
                                                                          SaksstatistikkService.TIMEZONE))
        assertThat(behandlingDvh?.registrertDato).isEqualTo(ZonedDateTime.of(behandling.opprettetTidspunkt,
                                                                             SaksstatistikkService.TIMEZONE))
        assertThat(behandlingDvh?.vedtaksDato).isEqualTo(vedtak.vedtaksdato?.toLocalDate())
        assertThat(behandlingDvh?.behandlingId).isEqualTo(behandling.id.toString())
        assertThat(behandlingDvh?.relatertBehandlingId).isEqualTo("1")
        assertThat(behandlingDvh?.sakId).isEqualTo(behandling.fagsak.id.toString())
        assertThat(behandlingDvh?.vedtakId).isEqualTo(vedtak.id.toString())
        assertThat(behandlingDvh?.behandlingType).isEqualTo(behandling.type.name)
        assertThat(behandlingDvh?.behandlingKategori).isEqualTo(behandling.kategori.name)
        assertThat(behandlingDvh?.behandlingUnderkategori).isEqualTo(behandling.underkategori.name)
        assertThat(behandlingDvh?.behandlingStatus).isEqualTo(behandling.status.name)
        assertThat(behandlingDvh?.totrinnsbehandling).isFalse
        assertThat(behandlingDvh?.saksbehandler).isEqualTo(SYSTEM_NAVN)
        assertThat(behandlingDvh?.beslutter).isEqualTo(SYSTEM_NAVN)
        assertThat(behandlingDvh?.avsender).isEqualTo("familie-ba-sak")
        assertThat(behandlingDvh?.versjon).isNotEmpty
        assertThat(behandlingDvh?.resultat).isEqualTo(behandling.resultat.name)
        assertThat(behandlingDvh?.resultatBegrunnelser).hasSize(1)
                .extracting("resultatBegrunnelse")
                .containsOnly("Alle vilkår er oppfylt")
        assertThat(behandlingDvh?.resultatBegrunnelser)
                .extracting("resultatBegrunnelseBeskrivelse").toString()
                .endsWith("Vilkår vurdert for barn: [Er under 18 år, Bor med søker, Gift/partnerskap, Bosatt i riket]")

    }

    @Test
    fun `Skal mappe til behandlingDVH for manuell rute`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD).also { it.resultat = BehandlingResultat.AVSLÅTT }

        val vilkårsvurdering = lagVilkårsvurdering("01010000001",
                                                   behandling,
                                                   Resultat.IKKE_OPPFYLT)

        every { totrinnskontrollService.hentAktivForBehandling(any()) } returns Totrinnskontroll(
                saksbehandler = "Saksbehandler",
                beslutter = "Beslutter",
                godkjent = true,
                behandling = behandling
        )

        val vedtak = lagVedtak(behandling)

        every { behandlingService.hent(any()) } returns behandling
        every { behandlingRestultatService.hentAktivForBehandling(any()) } returns vilkårsvurdering
        every { persongrunnlagService.hentSøker(any()) } returns tilfeldigSøker()
        every { persongrunnlagService.hentBarna(any()) } returns listOf(tilfeldigPerson()
                                                                                .copy(personIdent = PersonIdent("01010000001")))

        every { vedtakService.hentAktivForBehandling(any()) } returns vedtak
        every { journalføringRepository.findByBehandlingId(any()) } returns listOf(DbJournalpost(1,
                                                                                                 "foo",
                                                                                                 LocalDateTime.now(),
                                                                                                 behandling,
                                                                                                 "123"))
        val mottattDato = LocalDateTime.of(2019, 12, 20, 10, 0, 0)
        val jp = lagTestJournalpost("123", "123").copy(relevanteDatoer = listOf(RelevantDato(mottattDato, "DATO_REGISTRERT")))
        every { journalføringService.hentJournalpost(any()) } returns Ressurs.Companion.success(jp)

        val behandlingDvh = sakstatistikkService.mapTilBehandlingDVH(2, 1)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(behandlingDvh))


        assertThat(behandlingDvh?.funksjonellTid).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(behandlingDvh?.tekniskTid).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(behandlingDvh?.mottattDato).isEqualTo(mottattDato.atZone(SaksstatistikkService.TIMEZONE))
        assertThat(behandlingDvh?.registrertDato).isEqualTo(mottattDato.atZone(SaksstatistikkService.TIMEZONE))
        assertThat(behandlingDvh?.vedtaksDato).isEqualTo(vedtak.vedtaksdato?.toLocalDate())
        assertThat(behandlingDvh?.behandlingId).isEqualTo(behandling.id.toString())
        assertThat(behandlingDvh?.relatertBehandlingId).isEqualTo("1")
        assertThat(behandlingDvh?.sakId).isEqualTo(behandling.fagsak.id.toString())
        assertThat(behandlingDvh?.vedtakId).isEqualTo(vedtak.id.toString())
        assertThat(behandlingDvh?.behandlingType).isEqualTo(behandling.type.name)
        assertThat(behandlingDvh?.behandlingStatus).isEqualTo(behandling.status.name)
        assertThat(behandlingDvh?.totrinnsbehandling).isTrue
        assertThat(behandlingDvh?.saksbehandler).isEqualTo("Saksbehandler")
        assertThat(behandlingDvh?.beslutter).isEqualTo("Beslutter")
        assertThat(behandlingDvh?.resultatBegrunnelser).hasSize(2)
                .extracting("resultatBegrunnelse")
                .containsOnly("BOSATT_I_RIKET ikke oppfylt for barn 01010000001",
                              "LOVLIG_OPPHOLD ikke oppfylt for barn 01010000001")
        assertThat(behandlingDvh?.avsender).isEqualTo("familie-ba-sak")
        assertThat(behandlingDvh?.versjon).isNotEmpty

    }


    @Test
    fun `Skal mappe til sakDVH, ingen aktiv behandling, så kun aktør SØKER`() {
        every { fagsakService.hentRestFagsak(any()) } returns Ressurs.success(RestFagsak(LocalDateTime.now(),
                                                                                         1, "12345678910",
                                                                                         FagsakStatus.OPPRETTET,
                                                                                         true,
                                                                                         emptyList()))

        every { personopplysningerService.hentAktivAktørId(Ident("12345678910")) } returns AktørId("1234567891011")
        every { personopplysningerService.hentAktivAktørId(Ident("12345678911")) } returns AktørId("1234567891111")

        every { behandlingService.hentAktivForFagsak(any()) } returns null

        val sakDvh = sakstatistikkService.mapTilSakDvh(1)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sakDvh))

        assertThat(sakDvh?.aktorId).isEqualTo(1234567891011)
        assertThat(sakDvh?.aktorer).hasSize(1).extracting("rolle").contains("SØKER")
        assertThat(sakDvh?.sakStatus).isEqualTo(FagsakStatus.OPPRETTET.name)
        assertThat(sakDvh?.avsender).isEqualTo("familie-ba-sak")

    }

    @Test
    fun `Skal mappe til sakDVH, aktører har SØKER og BARN`() {
        every { fagsakService.hentRestFagsak(any()) } returns Ressurs.success(RestFagsak(LocalDateTime.now(),
                                                                                         1, "12345678910",
                                                                                         FagsakStatus.OPPRETTET,
                                                                                         true,
                                                                                         emptyList()))
        val randomAktørId = randomAktørId()
        every { personopplysningerService.hentAktivAktørId(any()) } returns randomAktørId

        every { persongrunnlagService.hentAktiv(any()) } returns lagTestPersonopplysningGrunnlag(1,
                                                                                                 tilfeldigPerson(personType = PersonType.BARN),
                                                                                                 tilfeldigPerson(personType = PersonType.SØKER))



        every { behandlingService.hentAktivForFagsak(any()) } returns lagBehandling()

        val sakDvh = sakstatistikkService.mapTilSakDvh(1)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sakDvh))

        assertThat(sakDvh?.aktorId).isEqualTo(randomAktørId.id.toLong())
        assertThat(sakDvh?.aktorer).hasSize(2).extracting("rolle").containsOnly("SØKER", "BARN")
        assertThat(sakDvh?.sakStatus).isEqualTo(FagsakStatus.OPPRETTET.name)
        assertThat(sakDvh?.avsender).isEqualTo("familie-ba-sak")

    }


    @Test
    fun `Skal gi feil hvis det kommer en ny BehandlingÅrsak som det ikke er tatt høyde for mot statistikk - Ved feil diskuterer ønsket resultat med statistikk`() {
        assertThat(enumValues<BehandlingÅrsak>()).containsOnly(BehandlingÅrsak.SØKNAD,
                                                               BehandlingÅrsak.FØDSELSHENDELSE,
                                                               BehandlingÅrsak.TEKNISK_OPPHØR,
                                                               BehandlingÅrsak.DØDSFALL,
                                                               BehandlingÅrsak.ÅRLIG_KONTROLL,
                                                               BehandlingÅrsak.NYE_OPPLYSNINGER,
                                                               BehandlingÅrsak.OMREGNING)
    }

}
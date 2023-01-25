package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.AutovedtakSatsendringService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.Satskjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.SatsTidspunkt
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class BehandlingSatsendringTest(
    @Autowired private val mockLocalDateService: LocalDateService,
    @Autowired private val behandleFødselshendelseTask: BehandleFødselshendelseTask,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val personidentService: PersonidentService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val autovedtakSatsendringService: AutovedtakSatsendringService,
    @Autowired private val andelTilkjentYtelseMedEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    @Autowired private val satskjøringRepository: SatskjøringRepository
) : AbstractVerdikjedetest() {

    @Test
    fun `Skal kjøre satsendring på løpende fagsak hvor brukeren har barnetrygd under 6 år`() {
        mockkObject(SatsTidspunkt)
        // Grunnen til at denne mockes er egentlig at den indirekte påvirker hva SatsService.hentGyldigSatsFor
        // returnerer. Det vi ønsker er at den sist tillagte satsendringen ikke kommer med slik at selve
        // satsendringen som skal kjøres senere faktisk utgjør en endring (slik at behandlingsresultatet blir ENDRET).
        every { SatsTidspunkt.senesteSatsTidspunkt } returns LocalDate.of(2023, 2, 1)

        every { mockLocalDateService.now() } returns LocalDate.now().minusYears(6) andThen LocalDate.now()

        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1993-01-12", fornavn = "Mor", etternavn = "Søker").copy(
                    bostedsadresser = mutableListOf(
                        Bostedsadresse(
                            angittFlyttedato = LocalDate.now().minusYears(10),
                            gyldigTilOgMed = null,
                            matrikkeladresse = Matrikkeladresse(
                                matrikkelId = 123L,
                                bruksenhetsnummer = "H301",
                                tilleggsnavn = "navn",
                                postnummer = "0202",
                                kommunenummer = "2231"
                            )
                        )
                    )
                ),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = LocalDate.of(2023, 1, 1)
                            .toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen"
                    ).copy(
                        bostedsadresser = mutableListOf(
                            Bostedsadresse(
                                angittFlyttedato = LocalDate.now().minusYears(6),
                                gyldigTilOgMed = null,
                                matrikkeladresse = Matrikkeladresse(
                                    matrikkelId = 123L,
                                    bruksenhetsnummer = "H301",
                                    tilleggsnavn = "navn",
                                    postnummer = "0202",
                                    kommunenummer = "2231"
                                )
                            )
                        )
                    )
                )
            )
        )
        val behandling = behandleFødselshendelse(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = scenario.søker.ident!!,
                barnasIdenter = listOf(scenario.barna.first().ident!!)
            ),
            behandleFødselshendelseTask = behandleFødselshendelseTask,
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            vedtakService = vedtakService,
            stegService = stegService,
            personidentService = personidentService
        )!!
        satskjøringRepository.saveAndFlush(Satskjøring(fagsakId = behandling.fagsak.id))

        // Fjerner mocking slik at den siste satsendringen vi fjernet via mocking nå skal komme med.
        unmockkObject(SatsTidspunkt)

        val satsendringResultat = autovedtakSatsendringService.kjørBehandling(fagsakId = behandling.fagsak.id)

        assertEquals(satsendringResultat, "Satsendring kjørt OK")

        val satsendringBehandling = behandlingHentOgPersisterService.hentAktivForFagsak(fagsakId = behandling.fagsak.id)
        assertEquals(Behandlingsresultat.ENDRET_UTBETALING, satsendringBehandling?.resultat)
        assertEquals(StegType.IVERKSETT_MOT_OPPDRAG, satsendringBehandling?.steg)

        val satsendingsvedtak = vedtakService.hentAktivForBehandling(behandlingId = satsendringBehandling!!.id)
        assertNull(satsendingsvedtak!!.stønadBrevPdF)

        val aty = andelTilkjentYtelseMedEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
            satsendringBehandling.id
        )

        val atyMedSenesteTilleggOrbaSats =
            aty.first { it.type == YtelseType.ORDINÆR_BARNETRYGD && it.stønadFom == YearMonth.of(2023, 3) }
        val atyMedVanligOrbaSats =
            aty.first { it.type == YtelseType.ORDINÆR_BARNETRYGD && it.stønadFom == YearMonth.of(2029, 1) }
        assertThat(atyMedSenesteTilleggOrbaSats.sats).isEqualTo(SatsService.finnSisteSatsFor(SatsType.TILLEGG_ORBA).beløp)
        assertThat(atyMedVanligOrbaSats.sats).isEqualTo(SatsService.finnSisteSatsFor(SatsType.ORBA).beløp)

        val satskjøring = satskjøringRepository.findByFagsakId(behandling.fagsak.id)
        assertThat(satskjøring?.ferdigTidspunkt)
            .isCloseTo(LocalDateTime.now(), Assertions.within(30, ChronoUnit.SECONDS))
    }

    @Test
    fun `Skal tilbakestille åpen behandling ved kjøring av satsendring`() {
        mockkObject(SatsTidspunkt)
        // Grunnen til at denne mockes er egentlig at den indirekte påvirker hva SatsService.hentGyldigSatsFor
        // returnerer. Det vi ønsker er at den sist tillagte satsendringen ikke kommer med slik at selve
        // satsendringen som skal kjøres senere faktisk utgjør en endring (slik at behandlingsresultatet blir ENDRET).
        every { SatsTidspunkt.senesteSatsTidspunkt } returns LocalDate.of(2023, 2, 1)

        every { mockLocalDateService.now() } returns LocalDate.now().minusYears(6) andThen LocalDate.now()

        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1993-01-12", fornavn = "Mor", etternavn = "Søker").copy(
                    bostedsadresser = mutableListOf(
                        Bostedsadresse(
                            angittFlyttedato = LocalDate.now().minusYears(10),
                            gyldigTilOgMed = null,
                            matrikkeladresse = Matrikkeladresse(
                                matrikkelId = 123L,
                                bruksenhetsnummer = "H301",
                                tilleggsnavn = "navn",
                                postnummer = "0202",
                                kommunenummer = "2231"
                            )
                        )
                    )
                ),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = LocalDate.of(2023, 1, 1)
                            .toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen"
                    ).copy(
                        bostedsadresser = mutableListOf(
                            Bostedsadresse(
                                angittFlyttedato = LocalDate.now().minusYears(6),
                                gyldigTilOgMed = null,
                                matrikkeladresse = Matrikkeladresse(
                                    matrikkelId = 123L,
                                    bruksenhetsnummer = "H301",
                                    tilleggsnavn = "navn",
                                    postnummer = "0202",
                                    kommunenummer = "2231"
                                )
                            )
                        )
                    )
                )
            )
        )
        val behandling = behandleFødselshendelse(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = scenario.søker.ident!!,
                barnasIdenter = listOf(scenario.barna.first().ident!!)
            ),
            behandleFødselshendelseTask = behandleFødselshendelseTask,
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            vedtakService = vedtakService,
            stegService = stegService,
            personidentService = personidentService
        )!!
        satskjøringRepository.saveAndFlush(Satskjøring(fagsakId = behandling.fagsak.id))

        // Opprett revurdering som blir liggende igjen som åpen og på behandlingsresultatsteget
        val revurdering = familieBaSakKlient().opprettBehandling(
            søkersIdent = scenario.søker.ident,
            behandlingType = BehandlingType.REVURDERING,
            behandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
            fagsakId = behandling.fagsak.id
        )
        val revurderingEtterVilkårsvurdering =
            familieBaSakKlient().validerVilkårsvurdering(behandlingId = revurdering.data!!.behandlingId)
        assertEquals(StegType.BEHANDLINGSRESULTAT, revurderingEtterVilkårsvurdering.data!!.steg)

        // Fjerner mocking slik at den siste satsendringen vi fjernet via mocking nå skal komme med.
        unmockkObject(SatsTidspunkt)

        val satsendringResultat = autovedtakSatsendringService.kjørBehandling(fagsakId = behandling.fagsak.id)

        assertTrue(satsendringResultat.contains("Tilbakestiller behandling"))

        val åpenBehandling = behandlingHentOgPersisterService.hentAktivForFagsak(fagsakId = behandling.fagsak.id)
        assertEquals(revurdering.data!!.behandlingId, åpenBehandling!!.id)
        assertEquals(StegType.VILKÅRSVURDERING, åpenBehandling.steg)
    }

    @Test
    fun `Skal ignorere satsendring hvis siste sats alt er satt`() {
        every { mockLocalDateService.now() } returns LocalDate.now().minusYears(6) andThen LocalDate.now()

        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1993-01-12", fornavn = "Mor", etternavn = "Søker").copy(
                    bostedsadresser = mutableListOf(
                        Bostedsadresse(
                            angittFlyttedato = LocalDate.now().minusYears(10),
                            gyldigTilOgMed = null,
                            matrikkeladresse = Matrikkeladresse(
                                matrikkelId = 123L,
                                bruksenhetsnummer = "H301",
                                tilleggsnavn = "navn",
                                postnummer = "0202",
                                kommunenummer = "2231"
                            )
                        )
                    )
                ),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = LocalDate.of(2023, 1, 1)
                            .toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen"
                    ).copy(
                        bostedsadresser = mutableListOf(
                            Bostedsadresse(
                                angittFlyttedato = LocalDate.now().minusYears(6),
                                gyldigTilOgMed = null,
                                matrikkeladresse = Matrikkeladresse(
                                    matrikkelId = 123L,
                                    bruksenhetsnummer = "H301",
                                    tilleggsnavn = "navn",
                                    postnummer = "0202",
                                    kommunenummer = "2231"
                                )
                            )
                        )
                    )
                )
            )
        )
        val behandling = behandleFødselshendelse(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = scenario.søker.ident!!,
                barnasIdenter = listOf(scenario.barna.first().ident!!)
            ),
            behandleFødselshendelseTask = behandleFødselshendelseTask,
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            vedtakService = vedtakService,
            stegService = stegService,
            personidentService = personidentService
        )!!
        satskjøringRepository.saveAndFlush(Satskjøring(fagsakId = behandling.fagsak.id))

        // Fjerner mocking slik at den siste satsendringen vi fjernet via mocking nå skal komme med.
        unmockkObject(SatsTidspunkt)

        val satsendringResultat = autovedtakSatsendringService.kjørBehandling(fagsakId = behandling.fagsak.id)

        assertEquals(satsendringResultat, "Satsendring allerede utført fagsak=${behandling.fagsak.id}")

        val satsendringBehandling = behandlingHentOgPersisterService.hentAktivForFagsak(fagsakId = behandling.fagsak.id)
        assertEquals(Behandlingsresultat.ENDRET_UTBETALING, satsendringBehandling?.resultat)
        assertEquals(StegType.IVERKSETT_MOT_OPPDRAG, satsendringBehandling?.steg)

        val satsendingsvedtak = vedtakService.hentAktivForBehandling(behandlingId = satsendringBehandling!!.id)
        assertNull(satsendingsvedtak!!.stønadBrevPdF)

        val aty = andelTilkjentYtelseMedEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
            satsendringBehandling.id
        )

        val atyMedSenesteTilleggOrbaSats =
            aty.first { it.type == YtelseType.ORDINÆR_BARNETRYGD && it.stønadFom == YearMonth.of(2023, 3) }
        val atyMedVanligOrbaSats =
            aty.first { it.type == YtelseType.ORDINÆR_BARNETRYGD && it.stønadFom == YearMonth.of(2029, 1) }
        assertThat(atyMedSenesteTilleggOrbaSats.sats).isEqualTo(SatsService.finnSisteSatsFor(SatsType.TILLEGG_ORBA).beløp)
        assertThat(atyMedVanligOrbaSats.sats).isEqualTo(SatsService.finnSisteSatsFor(SatsType.ORBA).beløp)

        val satskjøring = satskjøringRepository.findByFagsakId(behandling.fagsak.id)
        assertThat(satskjøring?.ferdigTidspunkt)
            .isCloseTo(LocalDateTime.now(), Assertions.within(30, ChronoUnit.SECONDS))
    }
}

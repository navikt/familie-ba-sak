package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.AutovedtakSatsendringService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import java.time.LocalDate
import java.time.YearMonth

// Todo. Bruker every. Dette endrer funksjonalliteten for alle klasser.
@DirtiesContext
class BehandlingSatsendringTest(
    @Autowired private val mockLocalDateService: LocalDateService,
    @Autowired private val behandleFødselshendelseTask: BehandleFødselshendelseTask,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val personidentService: PersonidentService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val autovedtakSatsendringService: AutovedtakSatsendringService,
) : AbstractVerdikjedetest() {

    @Test
    fun `Skal innvilge fødselshendelse på mor med 1 barn med eksisterende utbetalinger`() {
        mockkObject(SatsService)
        // Grunnen til at denne mockes er egentlig at den indirekte påvirker hva SatsService.hentGyldigSatsFor
        // returnerer. Det vi ønsker er at den sist tillagte satsendringen ikke kommer med slik at selve
        // satsendringen som skal kjøres senere faktisk utgjør en endring (slik at behandlingsresultatet blir ENDRET).
        every { SatsService.tilleggEndringJanuar2022 } returns YearMonth.of(2020, 9)

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
                        fødselsdato = LocalDate.now()
                            .minusYears(6)
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
                    ),
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
            behandlingService = behandlingService,
            vedtakService = vedtakService,
            stegService = stegService,
            personidentService = personidentService,
        )!!

        // Fjerner mocking slik at den siste satsendringen vi fjernet via mocking nå skal komme med.
        unmockkObject(SatsService)
        autovedtakSatsendringService.kjørBehandling(sistIverksatteBehandlingId = behandling.id)

        val satsendingBehandling = behandlingService.hentAktivForFagsak(fagsakId = behandling.fagsak.id)
        assertEquals(BehandlingResultat.ENDRET, satsendingBehandling?.resultat)
        assertEquals(StegType.IVERKSETT_MOT_OPPDRAG, satsendingBehandling?.steg)

        val satsendingsvedtak = vedtakService.hentAktivForBehandling(behandlingId = satsendingBehandling!!.id)
        assertNull(satsendingsvedtak!!.stønadBrevPdF)
    }

    @Test
    fun `Skal tilbakestille åpen behandling ved kjøring av satsendring`() {
        mockkObject(SatsService)
        // Grunnen til at denne mockes er egentlig at den indirekte påvirker hva SatsService.hentGyldigSatsFor
        // returnerer. Det vi ønsker er at den sist tillagte satsendringen ikke kommer med slik at selve
        // satsendringen som skal kjøres senere faktisk utgjør en endring (slik at behandlingsresultatet blir ENDRET).
        every { SatsService.tilleggEndringJanuar2022 } returns YearMonth.of(2020, 9)

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
                        fødselsdato = LocalDate.now()
                            .minusYears(6)
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
                    ),
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
            behandlingService = behandlingService,
            vedtakService = vedtakService,
            stegService = stegService,
            personidentService = personidentService,
        )!!

        // Opprett revurdering som blir liggende igjen som åpen og på behandlingsresultatsteget
        val revurdering = familieBaSakKlient().opprettBehandling(
            søkersIdent = scenario.søker.ident,
            behandlingType = BehandlingType.REVURDERING,
            behandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER
        )
        val revurderingEtterVilkårsvurdering =
            familieBaSakKlient().validerVilkårsvurdering(behandlingId = revurdering.data!!.behandlingId)
        assertEquals(StegType.BEHANDLINGSRESULTAT, revurderingEtterVilkårsvurdering.data!!.steg)

        // Fjerner mocking slik at den siste satsendringen vi fjernet via mocking nå skal komme med.
        unmockkObject(SatsService)
        autovedtakSatsendringService.kjørBehandling(sistIverksatteBehandlingId = behandling.id)

        val åpenBehandling = behandlingService.hentAktivForFagsak(fagsakId = behandling.fagsak.id)
        assertEquals(revurdering.data!!.behandlingId, åpenBehandling!!.id)
        assertEquals(StegType.VILKÅRSVURDERING, åpenBehandling.steg)
    }

    @Test
    fun `Skal finne alle behandlinger som påvirkes av satsendring`() {
        mockkObject(SatsService)
        // Grunnen til at denne mockes er egentlig at den indirekte påvirker hva SatsService.hentGyldigSatsFor
        // returnerer. Det vi ønsker er at den sist tillagte satsendringen ikke kommer med slik at selve
        // satsendringen som skal kjøres senere faktisk utgjør en endring (slik at behandlingsresultatet blir ENDRET).
        every { SatsService.tilleggEndringJanuar2022 } returns YearMonth.of(2020, 9)

        every { mockLocalDateService.now() } returns LocalDate.now().minusYears(6) andThen LocalDate.now()

        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1992-01-12", fornavn = "Mor", etternavn = "Søker").copy(
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
                        fødselsdato = LocalDate.now()
                            .minusYears(6)
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
                    ),
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
            behandlingService = behandlingService,
            vedtakService = vedtakService,
            stegService = stegService,
            personidentService = personidentService,
        )!!

        // Fjerner mocking slik at den siste satsendringen vi fjernet via mocking nå skal komme med.
        unmockkObject(SatsService)
        autovedtakSatsendringService.finnBehandlingerForSatsendring(1054, YearMonth.of(2022, 1))
            .filter { it == behandling.id } // kjør kun satsendring for behandlingen vi tester i dette testcaset
            .forEach { autovedtakSatsendringService.kjørBehandling(sistIverksatteBehandlingId = it) }

        val satsendingBehandling = behandlingService.hentAktivForFagsak(fagsakId = behandling.fagsak.id)
        assertEquals(BehandlingResultat.ENDRET, satsendingBehandling?.resultat)
        assertEquals(StegType.IVERKSETT_MOT_OPPDRAG, satsendingBehandling?.steg)

        val satsendingsvedtak = vedtakService.hentAktivForBehandling(behandlingId = satsendingBehandling!!.id)
        assertNull(satsendingsvedtak!!.stønadBrevPdF)
    }
}

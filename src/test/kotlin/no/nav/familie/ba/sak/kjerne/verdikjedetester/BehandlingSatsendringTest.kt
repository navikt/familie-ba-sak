package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.kjerne.autobrev.Autobrev6og18ÅrService
import no.nav.familie.ba.sak.kjerne.autobrev.FinnAlleBarn6og18ÅrTask
import no.nav.familie.ba.sak.kjerne.autorevurdering.SatsendringService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth

class BehandlingSatsendringTest(
    @Autowired private val mockLocalDateService: LocalDateService,
    @Autowired private val behandleFødselshendelseTask: BehandleFødselshendelseTask,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val satsendringService: SatsendringService
) : AbstractVerdikjedetest() {

    @Test
    fun `Skal innvilge fødselshendelse på mor med 1 barn med eksisterende utbetalinger`() {
        mockkObject(SatsService)
        // Grunnen til at denne mockes er egentlig at den indirekte påvirker hva SatsService.hentGyldigSatsFor
        // returnerer. Det vi ønsker er at den sist tillagte satsendringen ikke kommer med slik at selve
        // satsendringen som skal kjøres senere faktisk utgjør en endring (slik at behandlingsresultatet blir ENDRET).
        every { SatsService.tilleggEndringSeptember2021 } returns YearMonth.of(2020, 9)

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
            stegService = stegService
        )!!

        // Fjerner mocking slik at den siste satsendringen vi fjernet via mocking nå skal komme med.
        unmockkObject(SatsService)
        satsendringService.utførSatsendring(behandling.id)

        val satsendingBehandling = behandlingService.hentAktivForFagsak(fagsakId = behandling.fagsak.id)
        assertEquals(BehandlingResultat.ENDRET, satsendingBehandling?.resultat)
        assertEquals(StegType.IVERKSETT_MOT_OPPDRAG, satsendingBehandling?.steg)

        val satsendingsvedtak = vedtakService.hentAktivForBehandling(behandlingId = satsendingBehandling!!.id)
        assertNull(satsendingsvedtak!!.stønadBrevPdF)
    }
}

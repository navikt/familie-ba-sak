package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.verify
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.SatsService.sisteUtvidetSatsTilTester
import no.nav.familie.ba.sak.kjerne.beregning.SatsService.tilleggOrdinærSatsNesteMånedTilTester
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.defaultBostedsadresseHistorikk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDate.now

class FødselshendelseHenleggelseTest(
    @Autowired private val opprettTaskService: OpprettTaskService,
    @Autowired private val behandleFødselshendelseTask: BehandleFødselshendelseTask,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val personidentService: PersonidentService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val persongrunnlagService: PersongrunnlagService,
    @Autowired private val vilkårsvurderingService: VilkårsvurderingService,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired private val utvidetBehandlingService: UtvidetBehandlingService
) : AbstractVerdikjedetest() {

    @Test
    fun `Skal ikke starte behandling i ba-sak fordi det finnes saker i infotrygd (velg fagsystem)`() {
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(
                    fødselsdato = "1982-01-12",
                    fornavn = "Mor",
                    etternavn = "Søker",
                    infotrygdSaker = InfotrygdSøkResponse(
                        bruker = listOf(
                            lagInfotrygdSak(
                                SatsService.sisteTilleggOrdinærSats.beløp.toDouble(),
                                "1234",
                                "OR",
                                "OS"
                            )
                        ),
                        barn = emptyList()
                    )
                ),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = now().minusMonths(2).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
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
            behandlingService = behandlingService,
            vedtakService = vedtakService,
            stegService = stegService,
            personidentService = personidentService,
        )
        assertNull(behandling)

        verify(exactly = 1) {
            opprettTaskService.opprettSendFeedTilInfotrygdTask(scenario.barna.map { it.ident!! })
        }
    }

    @Test
    fun `Skal henlegge fødselshendelse på grunn av at søker er under 18 (filtreringsregel)`() {
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(
                    fødselsdato = now().minusYears(16).toString(),
                    fornavn = "Mor",
                    etternavn = "Søker"
                ),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = now().minusMonths(2).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
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
            behandlingService = behandlingService,
            personidentService = personidentService,
            vedtakService = vedtakService,
            stegService = stegService
        )

        assertEquals(BehandlingResultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE, behandling?.resultat)
        assertEquals(StegType.BEHANDLING_AVSLUTTET, behandling?.steg)

        verify(exactly = 1) {
            opprettTaskService.opprettOppgaveTask(
                behandlingId = behandling!!.id,
                oppgavetype = Oppgavetype.VurderLivshendelse,
                beskrivelse = "Fødselshendelse: Mor er under 18 år."
            )
        }

        val fagsak =
            familieBaSakKlient().hentFagsak(fagsakId = behandling!!.fagsak.id).data

        val automatiskVurdertBehandling = fagsak?.behandlinger?.first { it.skalBehandlesAutomatisk }!!
        assertEquals(0, automatiskVurdertBehandling.personResultater.size)
    }

    @Test
    fun `Skal henlegge fødselshendelse på grunn av at søker har flere adresser uten fom-dato (vilkårsvurdering)`() {
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(
                    fødselsdato = "1993-01-12",
                    fornavn = "Mor",
                    etternavn = "Søker",
                    bostedsadresser = defaultBostedsadresseHistorikk + listOf(
                        Bostedsadresse(
                            angittFlyttedato = null,
                            gyldigTilOgMed = null,
                            matrikkeladresse = Matrikkeladresse(
                                matrikkelId = 123L,
                                bruksenhetsnummer = "H301",
                                tilleggsnavn = "navn",
                                postnummer = "0202",
                                kommunenummer = "2231"
                            )
                        ),
                        Bostedsadresse(
                            angittFlyttedato = null,
                            gyldigTilOgMed = null,
                            matrikkeladresse = Matrikkeladresse(
                                matrikkelId = 123L,
                                bruksenhetsnummer = "H301",
                                tilleggsnavn = "navn",
                                postnummer = "0202",
                                kommunenummer = "2231"
                            )
                        ),
                        Bostedsadresse(
                            angittFlyttedato = now(),
                            gyldigTilOgMed = null,
                            matrikkeladresse = Matrikkeladresse(
                                matrikkelId = 123L,
                                bruksenhetsnummer = "H301",
                                tilleggsnavn = "navn",
                                postnummer = "0202",
                                kommunenummer = "2231"
                            )
                        ),
                    )
                ),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = now().toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
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
            behandlingService = behandlingService,
            personidentService = personidentService,
            vedtakService = vedtakService,
            stegService = stegService
        )

        assertEquals(BehandlingResultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE, behandling?.resultat)
        assertEquals(StegType.BEHANDLING_AVSLUTTET, behandling?.steg)

        verify(exactly = 1) {
            opprettTaskService.opprettOppgaveTask(
                behandlingId = behandling!!.id,
                oppgavetype = Oppgavetype.VurderLivshendelse,
                beskrivelse = "Fødselshendelse: Mor har flere bostedsadresser uten fra- og med dato"
            )
        }
    }

    @Test
    fun `Skal henlegge fødselshendelse på grunn av at barn ikke er bosatt i riket og bor ikke med mor (vilkårsvurdering)`() {
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1993-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = now().toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                        bostedsadresser = emptyList()
                    )
                )
            )
        )

        val barnIdent = scenario.barna.first().ident!!
        val behandling = behandleFødselshendelse(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = scenario.søker.ident!!,
                barnasIdenter = listOf(scenario.barna.first().ident!!)
            ),
            behandleFødselshendelseTask = behandleFødselshendelseTask,
            fagsakService = fagsakService,
            behandlingService = behandlingService,
            personidentService = personidentService,
            vedtakService = vedtakService,
            stegService = stegService
        )

        assertEquals(BehandlingResultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE, behandling?.resultat)
        assertEquals(StegType.BEHANDLING_AVSLUTTET, behandling?.steg)

        verify(exactly = 1) {
            opprettTaskService.opprettOppgaveTask(
                behandlingId = behandling!!.id,
                oppgavetype = Oppgavetype.VurderLivshendelse,
                beskrivelse = "Fødselshendelse: Barnet (fødselsdato: ${
                LocalDate.parse(scenario.barna.first().fødselsdato)
                    .tilKortString()
                }) er ikke bosatt med mor."
            )
        }

        val fagsak =
            familieBaSakKlient().hentFagsak(fagsakId = behandling!!.fagsak.id).data

        val automatiskVurdertBehandling = fagsak?.behandlinger?.first { it.skalBehandlesAutomatisk }!!
        val borMedSøkerVikårForbarn =
            automatiskVurdertBehandling.personResultater.firstOrNull { it.personIdent == barnIdent }?.vilkårResultater?.firstOrNull { it.vilkårType == Vilkår.BOR_MED_SØKER }
        val bosattIRiketVikårForbarn =
            automatiskVurdertBehandling.personResultater.firstOrNull { it.personIdent == barnIdent }?.vilkårResultater?.firstOrNull { it.vilkårType == Vilkår.BOSATT_I_RIKET }

        assertEquals(Resultat.IKKE_OPPFYLT, borMedSøkerVikårForbarn?.resultat)
        assertEquals(Resultat.IKKE_OPPFYLT, bosattIRiketVikårForbarn?.resultat)
    }

    @Test
    fun `Skal henlegge fødselshendelse på grunn av at mor mottar utvidet barnetrygd (filtreringsregel)`() {
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(
                    fødselsdato = now().minusYears(26).toString(),
                    fornavn = "Mor",
                    etternavn = "Søker"
                ),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = now().minusMonths(2).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                    ),
                    RestScenarioPerson(
                        fødselsdato = now().minusYears(2).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                    )
                )
            )
        )

        val behandling = kjørStegprosessForFGB(
            tilSteg = StegType.BEHANDLING_AVSLUTTET,
            søkerFnr = scenario.søker.ident!!,
            barnasIdenter = listOf(scenario.barna.last().ident!!),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            behandlingUnderkategori = BehandlingUnderkategori.UTVIDET
        )

        assertEquals(BehandlingUnderkategori.UTVIDET, behandling.underkategori)
        assertEquals(
            tilleggOrdinærSatsNesteMånedTilTester.beløp + sisteUtvidetSatsTilTester.beløp,
            hentNåværendeEllerNesteMånedsUtbetaling(
                behandling = utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandling.id)
            )
        )

        val revurdering = behandleFødselshendelse(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = scenario.søker.ident,
                barnasIdenter = listOf(scenario.barna.first().ident!!)
            ),
            behandleFødselshendelseTask = behandleFødselshendelseTask,
            fagsakService = fagsakService,
            behandlingService = behandlingService,
            personidentService = personidentService,
            vedtakService = vedtakService,
            stegService = stegService
        )

        assertEquals(BehandlingUnderkategori.UTVIDET, revurdering?.underkategori)
        assertEquals(BehandlingResultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE, revurdering?.resultat)
        assertEquals(StegType.BEHANDLING_AVSLUTTET, revurdering?.steg)

        verify(exactly = 1) {
            opprettTaskService.opprettOppgaveTask(
                behandlingId = revurdering!!.id,
                oppgavetype = Oppgavetype.VurderLivshendelse,
                beskrivelse = "Fødselshendelse: Mor mottar utvidet barnetrygd."
            )
        }
    }

    @Test
    fun `Skal henlegge fødselshendelse på grunn av at mor er EØS borger (vilkårsregel)`() {
        val søkerFødselsdato = now().minusYears(26)
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(
                    fødselsdato = søkerFødselsdato.toString(),
                    fornavn = "Mor",
                    etternavn = "Søker",
                    statsborgerskap = listOf(
                        Statsborgerskap(
                            land = "POL",
                            gyldigFraOgMed = søkerFødselsdato,
                            bekreftelsesdato = søkerFødselsdato,
                            gyldigTilOgMed = null
                        )
                    )
                ),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = now().minusMonths(2).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                    )
                )
            )
        )

        val behandling = behandleFødselshendelse(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = scenario.søker.ident!!,
                barnasIdenter = listOf(scenario.barna.single().ident!!)
            ),
            behandleFødselshendelseTask = behandleFødselshendelseTask,
            fagsakService = fagsakService,
            behandlingService = behandlingService,
            personidentService = personidentService,
            vedtakService = vedtakService,
            stegService = stegService
        )

        assertEquals(BehandlingResultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE, behandling?.resultat)
        assertEquals(StegType.BEHANDLING_AVSLUTTET, behandling?.steg)

        verify(exactly = 1) {
            opprettTaskService.opprettOppgaveTask(
                behandlingId = behandling!!.id,
                oppgavetype = Oppgavetype.VurderLivshendelse,
                beskrivelse = "Fødselshendelse: Mor har ikke lovlig opphold - EØS borgere kan ikke automatisk vurderes."
            )
        }
    }
}

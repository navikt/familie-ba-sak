package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.nyOrdinærBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPersonResultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class RevurderingMedEndredeUtbetalingandelerTest(
    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val persongrunnlagService: PersongrunnlagService,

    @Autowired
    private val vilkårService: VilkårService,

    @Autowired
    private val stegService: StegService,

    @Autowired
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,

) : AbstractVerdikjedetest() {
    @Test
    fun `Endrede utbetalingsandeler fra forrige behandling kopieres riktig`() {

        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1993-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusYears(4).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen"
                    )
                )
            )
        )
        val fnr = scenario.søker.ident!!
        val barnFnr = scenario.barna[0].ident!!

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(fnr))

        persongrunnlagService.lagreOgDeaktiverGammel(
            lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        )

        val vilkårsvurdering = vilkårService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = true,
            forrigeBehandling = null
        )

        vilkårsvurdering.personResultater.map { personResultat ->
            personResultat.tilRestPersonResultat().vilkårResultater.map {
                vilkårService.endreVilkår(
                    behandlingId = behandling.id, vilkårId = it.id,
                    restPersonResultat =
                    RestPersonResultat(
                        personIdent = personResultat.personIdent,
                        vilkårResultater = listOf(
                            it.copy(
                                resultat = Resultat.OPPFYLT,
                                periodeFom = LocalDate.of(2019, 5, 8),
                                erDeltBosted = it.vilkårType == Vilkår.BOR_MED_SØKER
                            )
                        )
                    )
                )
            }
        }

        behandling.behandlingStegTilstand.add(
            BehandlingStegTilstand(behandling = behandling, behandlingSteg = StegType.VILKÅRSVURDERING)
        )
        stegService.håndterVilkårsvurdering(behandling)

        val endretUtbetalingAndel =
            endretUtbetalingAndelService.opprettTomEndretUtbetalingAndelOgOppdaterTilkjentYtelse(behandling)

        val restEndretUtbetalingAndel = RestEndretUtbetalingAndel(
            id = endretUtbetalingAndel.id,
            fom = YearMonth.of(2019, 6),
            tom = YearMonth.of(2020, 10),
            avtaletidspunktDeltBosted = LocalDate.of(2019, 5, 8),
            søknadstidspunkt = LocalDate.of(2019, 5, 8),
            begrunnelse = "begrunnelse",
            personIdent = barnFnr,
            årsak = Årsak.DELT_BOSTED,
            prosent = BigDecimal(50),
            erTilknyttetAndeler = false
        )

        endretUtbetalingAndelService.oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
            behandling,
            endretUtbetalingAndel.id,
            restEndretUtbetalingAndel
        )

        behandling.behandlingStegTilstand.add(
            BehandlingStegTilstand(behandling = behandling, behandlingSteg = StegType.BEHANDLINGSRESULTAT)
        )
        val behandlingEtterHåndterBehandlingsresultat = stegService.håndterBehandlingsresultat(behandling)

        behandlingEtterHåndterBehandlingsresultat.behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandling = behandling, behandlingSteg = StegType.BEHANDLING_AVSLUTTET
            )
        )
        val iverksattBehandling = behandlingService.lagreEllerOppdater(behandlingEtterHåndterBehandlingsresultat)

        val behandlingRevurdering = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        persongrunnlagService.lagreOgDeaktiverGammel(
            lagTestPersonopplysningGrunnlag(behandlingRevurdering.id, fnr, listOf(barnFnr))
        )

        vilkårService.initierVilkårsvurderingForBehandling(
            behandling = behandlingRevurdering,
            bekreftEndringerViaFrontend = true,
            forrigeBehandling = iverksattBehandling
        )

        val kopierteEndredeUtbetalingAndeler = endretUtbetalingAndelService.hentForBehandling(behandlingRevurdering.id)
        assertEquals(1, kopierteEndredeUtbetalingAndeler.size)
    }
}

package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.vedtak.RestVilkårsvurdering
import no.nav.familie.ba.sak.behandling.vilkår.vilkårsvurderingKomplettForBarnOgSøker
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.mockHentPersoninfoForMedIdenter
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles


@SpringBootTest
@ActiveProfiles("dev")
class StegServiceTest(
        @Autowired
        private val stegService: StegService,

        @Autowired
        private val behandlingService: BehandlingService,

        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
        private val mockIntegrasjonClient: IntegrasjonClient
) {

    @Test
    fun `Skal håndtere steg for ordinær behandling`() {
        val søkerFnr = randomFnr()
        val annenPartIdent = randomFnr()
        val barnFnr = randomFnr()

        mockHentPersoninfoForMedIdenter(mockIntegrasjonClient, søkerFnr, barnFnr)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        Assertions.assertEquals(initSteg(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING), behandling.steg)

        stegService.håndterSøknad(behandling,
                                  lagSøknadDTO(annenPartIdent = annenPartIdent,
                                               søkerIdent = søkerFnr,
                                               barnasIdenter = listOf(barnFnr)))
        val behandlingEtterSøknadGrunnlagSteg = behandlingService.hent(behandlingId = behandling.id)
        Assertions.assertEquals(StegType.REGISTRERE_PERSONGRUNNLAG, behandlingEtterSøknadGrunnlagSteg.steg)

        stegService.håndterPersongrunnlag(behandlingEtterSøknadGrunnlagSteg, RegistrerPersongrunnlagDTO(
                ident = søkerFnr,
                barnasIdenter = listOf(barnFnr)
        ))

        val behandlingEtterPersongrunnlagSteg = behandlingService.hent(behandlingId = behandling.id)
        Assertions.assertEquals(StegType.VILKÅRSVURDERING, behandlingEtterPersongrunnlagSteg.steg)

        stegService.håndterVilkårsvurdering(behandlingEtterPersongrunnlagSteg, RestVilkårsvurdering(
                resultat = BehandlingResultat.INNVILGET,
                samletVilkårResultat = vilkårsvurderingKomplettForBarnOgSøker(søkerFnr, listOf(barnFnr)),
                begrunnelse = ""
        ))

        val behandlingEtterVilkårsvurderingSteg = behandlingService.hent(behandlingId = behandling.id)
        Assertions.assertEquals(StegType.SEND_TIL_BESLUTTER, behandlingEtterVilkårsvurderingSteg.steg)
    }

    @Test
    fun `Skal feile når man prøver å håndtere feil steg`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        Assertions.assertEquals(initSteg(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING), behandling.steg)

        assertThrows<IllegalStateException> {
            stegService.håndterVilkårsvurdering(behandling, RestVilkårsvurdering(
                    resultat = BehandlingResultat.INNVILGET,
                    samletVilkårResultat = vilkårsvurderingKomplettForBarnOgSøker(søkerFnr, listOf(barnFnr)),
                    begrunnelse = ""
            ))
        }
    }
}
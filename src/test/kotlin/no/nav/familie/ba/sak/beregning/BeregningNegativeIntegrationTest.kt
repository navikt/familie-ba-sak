package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.familie.ba.sak.behandling.vilkår.vilkårsvurderingKomplettForBarnOgSøker
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate


@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-dokgen-negative")
@Tag("integration")
class BeregningNegativeIntegrationTest {

    @Autowired
    private lateinit var beregningController: BeregningController

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    private lateinit var vedtakService: VedtakService


    @Test
    @Tag("integration")
    fun `Oppdater avslag vedtak med beregning`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        behandlingService.settVilkårsvurdering(behandling, BehandlingResultat.AVSLÅTT, "")
        Assertions.assertNotNull(behandling.fagsak.id)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                restSamletVilkårResultat = vilkårsvurderingKomplettForBarnOgSøker(
                        fnr,
                        listOf(barnFnr)),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )
        val vedtak = vedtakService.hentAktivForBehandling(behandling.id)
        Assertions.assertNotNull(vedtak)

        val fagsakRes = beregningController.oppdaterVedtakMedBeregning(vedtak!!.id,
                                                                       NyBeregning(
                                                                               listOf(
                                                                                       PersonBeregning(
                                                                                               ident = barnFnr,
                                                                                               beløp = 1054,
                                                                                               stønadFom = LocalDate.of(2020,
                                                                                                                        1,
                                                                                                                        1),
                                                                                               ytelsetype = Ytelsetype.ORDINÆR_BARNETRYGD
                                                                                       ))
                                                                       ))

        Assertions.assertEquals(Ressurs.Status.FEILET, fagsakRes.body?.status)
        Assertions.assertEquals("Kan ikke lage beregning på et vedtak som ikke er innvilget", fagsakRes.body?.melding)
    }
}

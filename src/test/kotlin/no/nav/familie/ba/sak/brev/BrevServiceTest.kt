package no.nav.familie.ba.sak.brev

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.brev.domene.maler.Førstegangsvedtak
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Utils.formaterBeløp
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.config.ClientMocks
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@SpringBootTest(properties = ["FAMILIE_INTEGRASJONER_API_URL=http://localhost:28085/api"])
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-økonomi", "mock-oauth", "mock-pdl", "mock-task-repository")
@Tag("integration")
@AutoConfigureWireMock(port = 28085)
class BrevServiceTest(
        @Autowired private val fagsakService: FagsakService,
        @Autowired private val behandlingService: BehandlingService,
        @Autowired private val vedtakService: VedtakService,
        @Autowired private val vilkårsvurderingService: VilkårsvurderingService,
        @Autowired private val stegService: StegService,
        @Autowired private val persongrunnlagService: PersongrunnlagService,
        @Autowired private val brevService: BrevService,
) {

    @Test
    fun `test mapTilNyttVedtaksbrev for 'Vedtak endring' med ett barn`() {

        val behandlingEtterVilkårsvurderingSteg = kjørStegprosessForFGB(
                tilSteg = StegType.VILKÅRSVURDERING,
                søkerFnr = ClientMocks.søkerFnr[0],
                barnasIdenter = listOf(ClientMocks.barnFnr[0]),
                fagsakService = fagsakService,
                behandlingService = behandlingService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService
        )
        val vedtak: Vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandlingEtterVilkårsvurderingSteg.id)!!
        vedtak.vedtakBegrunnelser.add(
                VedtakBegrunnelse(
                        vedtak = vedtak,
                        fom = LocalDate.now(),
                        tom = LocalDate.now(),
                        brevBegrunnelse = "Begrunnelse",
                        begrunnelse = VedtakBegrunnelseSpesifikasjon.INNVILGET_NYFØDT_BARN
                )
        )

        var brevfelter = brevService.hentVedtaksbrevData(vedtak)

        Assertions.assertTrue(brevfelter is Førstegangsvedtak)
        brevfelter = brevfelter as Førstegangsvedtak
        Assertions.assertEquals(ClientMocks.søkerFnr[0], brevfelter.data.flettefelter.fodselsnummer[0])
        Assertions.assertEquals(listOf(formaterBeløp(1054)), brevfelter.data.delmalData.etterbetaling?.etterbetalingsbelop)
    }

    @Test
    fun `Skal kaste feil ved generering av brevdata før vilkårsvurdering er fullført`() {
        val behandlingEtterRegistrerSøknadSteg = kjørStegprosessForFGB(
                tilSteg = StegType.REGISTRERE_SØKNAD,
                søkerFnr = ClientMocks.søkerFnr[0],
                barnasIdenter = listOf(ClientMocks.barnFnr[0]),
                fagsakService = fagsakService,
                behandlingService = behandlingService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService
        )

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(
                        behandlingEtterRegistrerSøknadSteg.id,
                        ClientMocks.søkerFnr[0],
                        listOf(ClientMocks.barnFnr[0])
                )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vedtak = vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                personopplysningGrunnlag = personopplysningGrunnlag,
                behandling = behandlingEtterRegistrerSøknadSteg
        )

        println(vedtak.behandling.resultat)

        val feil = assertThrows<FunksjonellFeil> {
            brevService.hentVedtaksbrevData(vedtak)
        }
        Assertions.assertEquals(
                "Brev ikke støttet for behandlingstype=FØRSTEGANGSBEHANDLING og behandlingsresultat=IKKE_VURDERT",
                feil.message
        )
    }

}
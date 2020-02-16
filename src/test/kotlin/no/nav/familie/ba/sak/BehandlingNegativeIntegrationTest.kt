package no.nav.familie.ba.sak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.FagsakController
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.domene.vedtak.*
import no.nav.familie.ba.sak.behandling.domene.vedtak.NyBeregning
import no.nav.familie.ba.sak.behandling.domene.vedtak.NyttVedtak
import no.nav.familie.ba.sak.behandling.domene.vedtak.VedtakResultat
import no.nav.familie.ba.sak.util.DbContainerInitializer
import no.nav.familie.ba.sak.vilkår.vilkårsvurderingKomplettForBarnOgSøker
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
class BehandlingNegativeIntegrationTest(
        @Autowired
        private val fagsakController: FagsakController,

        @Autowired
        private val behandlingService: BehandlingService,

        @Autowired
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository) {


    @Test
    @Tag("integration")
    fun `Hent HTML vedtaksbrev Negative'`() {
        val failRess = behandlingService.hentHtmlVedtakForBehandling(100)
        Assertions.assertEquals(Ressurs.Status.FEILET, failRess.status)

        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent("6")
        val behandling =
                behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                              "sdf",
                                                              BehandlingType.FØRSTEGANGSBEHANDLING,
                                                              "sak1",
                                                              BehandlingKategori.NATIONAL,
                                                              BehandlingUnderkategori.ORDINÆR)
        Assertions.assertNotNull(behandling.fagsak.id)
        Assertions.assertNotNull(behandling.id)
    }

    @Test
    @Tag("integration")
    fun `Oppdater avslag vedtak med beregning`() {
        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent("1")
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       "sdf",
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       "123",
                                                                       BehandlingKategori.NATIONAL,
                                                                       BehandlingUnderkategori.ORDINÆR)
        Assertions.assertNotNull(behandling.fagsak.id)

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id!!, "1", "12345678910")
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        behandlingService.nyttVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                nyttVedtak = NyttVedtak(resultat = VedtakResultat.AVSLÅTT,
                                        samletVilkårResultat = vilkårsvurderingKomplettForBarnOgSøker("1", listOf("12345678910"))),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )
        val vedtak = behandlingService.hentVedtakHvisEksisterer(behandling.id)
        Assertions.assertNotNull(vedtak)

        val fagsakRes = fagsakController.oppdaterVedtakMedBeregning(fagsak.id!!, NyBeregning(
                arrayOf(
                        PersonBeregning(
                                fødselsnummer = "12345678910",
                                beløp = 1054,
                                stønadFom = LocalDate.now(),
                                ytelsetype = Ytelsetype.ORDINÆR_BARNETRYGD
                        ))
        ))

        Assertions.assertEquals(Ressurs.Status.FEILET, fagsakRes.body?.status)
        Assertions.assertEquals("Kan ikke lagre beregning på et avslått vedtak", fagsakRes.body?.melding)
    }
}

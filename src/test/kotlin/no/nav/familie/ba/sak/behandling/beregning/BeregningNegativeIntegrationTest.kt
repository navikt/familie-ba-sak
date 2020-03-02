package no.nav.familie.ba.sak.behandling.beregning

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.vedtak.*
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.util.DbContainerInitializer
import no.nav.familie.ba.sak.util.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.vilkår.vilkårsvurderingKomplettForBarnOgSøker
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
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

    @MockBean
    lateinit var integrasjonTjeneste: IntegrasjonTjeneste

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    private lateinit var vedtakService: VedtakService

    @BeforeEach
    fun setup() {
        Mockito.`when`(integrasjonTjeneste.hentAktørId(anyString())).thenReturn(AktørId("1"))
    }

    @Test
    @Tag("integration")
    fun `Oppdater avslag vedtak med beregning`() {
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent("1")
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       "sdf",
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       BehandlingKategori.NASJONAL,
                                                                       BehandlingUnderkategori.ORDINÆR)
        Assertions.assertNotNull(behandling.fagsak.id)

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, "1", "12345678910")
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        vedtakService.nyttVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                nyttVedtak = NyttVedtak(resultat = VedtakResultat.AVSLÅTT,
                                        samletVilkårResultat = vilkårsvurderingKomplettForBarnOgSøker(
                                                "1",
                                                listOf("12345678910")),
                                        begrunnelse = ""),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )
        val vedtak = vedtakService.hentAktivForBehandling(behandling.id)
        Assertions.assertNotNull(vedtak)

        val fagsakRes = beregningController.oppdaterVedtakMedBeregning(fagsak.id,
                                                                       NyBeregning(
                                                                               listOf(
                                                                                       BarnBeregning(
                                                                                               ident = "12345678910",
                                                                                               beløp = 1054,
                                                                                               stønadFom = LocalDate.of(2020,
                                                                                                                        1,
                                                                                                                        1),
                                                                                               ytelsetype = Ytelsetype.ORDINÆR_BARNETRYGD
                                                                                       ))
                                                                       ))

        Assertions.assertEquals(Ressurs.Status.FEILET, fagsakRes.body?.status)
        Assertions.assertEquals("Kan ikke lagre beregning på et avslått/opphørt vedtak", fagsakRes.body?.melding)
    }
}

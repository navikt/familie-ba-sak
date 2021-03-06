package no.nav.familie.ba.sak.kjerne.fødselshendelse

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FiltreringsreglerService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.utfall.FiltreringsregelIkkeOppfylt.BARNET_LEVER_IKKE
import no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.utfall.FiltreringsregelIkkeOppfylt.MOR_ER_UNDER_18_ÅR
import no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.utfall.FiltreringsregelIkkeOppfylt.MOR_LEVER_IKKE
import no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.utfall.FiltreringsregelOppfylt.BARNET_LEVER
import no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.utfall.FiltreringsregelOppfylt.MOR_ER_OVER_18_ÅR
import no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.utfall.FiltreringsregelOppfylt.MOR_LEVER
import no.nav.familie.ba.sak.kjerne.fødselshendelse.gdpr.GDPRService
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Evaluering
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Spesifikasjon
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.Assert
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppgaveBeskrivelseTest {

    private val infotrygdBarnetrygdClientMock = mockk<InfotrygdBarnetrygdClient>()
    private val personopplysningerServiceMock = mockk<PersonopplysningerService>()
    private val infotrygdFeedServiceMock = mockk<InfotrygdFeedService>()
    private val stegServiceMock = mockk<StegService>()
    private val vedtakServiceMock = mockk<VedtakService>()
    private val evaluerFiltreringsreglerForFødselshendelseMock = mockk<EvaluerFiltreringsreglerForFødselshendelse>()
    private val evaluerFiltreringsreglerServiceMock = mockk<FiltreringsreglerService>()
    private val taskRepositoryMock = mockk<TaskRepository>()
    private val behandlingResultatRepositoryMock = mockk<VilkårsvurderingRepository>()
    private val persongrunnlagServiceMock = mockk<PersongrunnlagService>()
    private val behandlingRepositoryMock = mockk<BehandlingRepository>()
    private val gdprServiceMock = mockk<GDPRService>()
    private val envServiceMock = mockk<EnvService>()

    private val fødselshendelseService = FødselshendelseServiceGammel(
            infotrygdFeedServiceMock,
            infotrygdBarnetrygdClientMock,
            stegServiceMock,
            vedtakServiceMock,
            evaluerFiltreringsreglerForFødselshendelseMock,
            taskRepositoryMock,
            personopplysningerServiceMock,
            behandlingResultatRepositoryMock,
            persongrunnlagServiceMock,
            behandlingRepositoryMock,
            gdprServiceMock,
            envServiceMock
    )

    @Test
    fun `hentBegrunnelseFraFiltreringsregler() skal returnere begrunnelse av første regel som feilet`() {
        var evaluering = testSpesifikasjoner.evaluer(
                TestFaktaForFiltreringsregler(barnetLever = false, morLever = false, morErOver18År = true))
        Assert.assertEquals("Det er registrert dødsdato på barnet.",
                            fødselshendelseService.hentBegrunnelseFraFiltreringsregler(evaluering))

        evaluering = testSpesifikasjoner.evaluer(
                TestFaktaForFiltreringsregler(barnetLever = true, morLever = false, morErOver18År = false))
        Assert.assertEquals("Det er registrert dødsdato på mor.",
                            fødselshendelseService.hentBegrunnelseFraFiltreringsregler(evaluering))

        evaluering = testSpesifikasjoner.evaluer(
                TestFaktaForFiltreringsregler(barnetLever = true, morLever = true, morErOver18År = false))
        Assert.assertEquals("Mor er under 18 år.",
                            fødselshendelseService.hentBegrunnelseFraFiltreringsregler(evaluering))
    }

    @Test
    fun `hentBegrunnelseFraFiltreringsregler() skal returnere null dersom ingen regler feiler`() {
        val testEvaluering = testSpesifikasjoner.evaluer(TestFaktaForFiltreringsregler(barnetLever = true,
                                                                                       morLever = true,
                                                                                       morErOver18År = true))
        val beskrivelse = fødselshendelseService.hentBegrunnelseFraFiltreringsregler(testEvaluering)
        Assert.assertNull(beskrivelse)
    }

    @Test
    fun `hentBegrunnelseFraVilkårsvurdering() skal returnere riktig beskrivelse basert på vilkårsrekkefølgen`() {
        every { behandlingRepositoryMock.finnBehandling(any()) } returns behandling
        every { persongrunnlagServiceMock.hentSøker(any()) } returns søker
        every { persongrunnlagServiceMock.hentBarna(any()) } returns listOf(barn)
        every { behandlingResultatRepositoryMock.findByBehandlingAndAktiv(any()) } returns genererBehandlingResultat(
                søkersVilkår = TestFaktaForSøkersVilkår(bosattIRiket = false, lovligOpphold = false),
                barnasVilkår = TestFaktaForBarnasVilkår(under18År = false,
                                                        borMedSøker = true,
                                                        giftPartnerskap = true,
                                                        bosattIRiket = true)
        )
        val beskrivelse0 = fødselshendelseService.hentBegrunnelseFraVilkårsvurdering(behandling.id)
        Assert.assertEquals("Mor er ikke bosatt i riket.", beskrivelse0)

        every { behandlingResultatRepositoryMock.findByBehandlingAndAktiv(any()) } returns genererBehandlingResultat(
                søkersVilkår = TestFaktaForSøkersVilkår(bosattIRiket = true, lovligOpphold = false),
                barnasVilkår = TestFaktaForBarnasVilkår(under18År = false,
                                                        borMedSøker = true,
                                                        giftPartnerskap = true,
                                                        bosattIRiket = true)
        )
        val beskrivelse1 = fødselshendelseService.hentBegrunnelseFraVilkårsvurdering(behandling.id)
        Assert.assertEquals("Begrunnelse fra vilkårsvurdering", beskrivelse1)

        every { behandlingResultatRepositoryMock.findByBehandlingAndAktiv(any()) } returns genererBehandlingResultat(
                søkersVilkår = TestFaktaForSøkersVilkår(bosattIRiket = true, lovligOpphold = true),
                barnasVilkår = TestFaktaForBarnasVilkår(under18År = false,
                                                        borMedSøker = true,
                                                        giftPartnerskap = false,
                                                        bosattIRiket = true)
        )
        val beskrivelse2 = fødselshendelseService.hentBegrunnelseFraVilkårsvurdering(behandling.id)
        Assert.assertTrue(beskrivelse2!!.contains("er over 18 år."))

        every { behandlingResultatRepositoryMock.findByBehandlingAndAktiv(any()) } returns genererBehandlingResultat(
                søkersVilkår = TestFaktaForSøkersVilkår(bosattIRiket = true, lovligOpphold = true),
                barnasVilkår = TestFaktaForBarnasVilkår(under18År = true,
                                                        borMedSøker = false,
                                                        giftPartnerskap = false,
                                                        bosattIRiket = true)
        )
        val beskrivelse3 = fødselshendelseService.hentBegrunnelseFraVilkårsvurdering(behandling.id)
        Assert.assertTrue(beskrivelse3!!.contains("er ikke bosatt med mor."))
    }

    @Test
    fun `hentBegrunnelseFraVilkårsvurdering() skal returnere null dersom ingen regler i vilkårsvurderingen feiler`() {
        every { behandlingRepositoryMock.finnBehandling(any()) } returns behandling
        every { persongrunnlagServiceMock.hentSøker(any()) } returns søker
        every { persongrunnlagServiceMock.hentBarna(any()) } returns listOf(barn)
        every { behandlingResultatRepositoryMock.findByBehandlingAndAktiv(any()) } returns genererBehandlingResultat(
                søkersVilkår = TestFaktaForSøkersVilkår(bosattIRiket = true, lovligOpphold = true),
                barnasVilkår = TestFaktaForBarnasVilkår(under18År = true,
                                                        borMedSøker = true,
                                                        giftPartnerskap = true,
                                                        bosattIRiket = true)
        )
        val beskrivelse = fødselshendelseService.hentBegrunnelseFraVilkårsvurdering(behandling.id)
        Assert.assertNull(beskrivelse)
    }

    class TestFaktaForFiltreringsregler(
            val barnetLever: Boolean,
            val morLever: Boolean,
            val morErOver18År: Boolean
    )

    class TestFaktaForSøkersVilkår(
            val bosattIRiket: Boolean,
            val lovligOpphold: Boolean
    )

    class TestFaktaForBarnasVilkår(
            val under18År: Boolean,
            val borMedSøker: Boolean,
            val giftPartnerskap: Boolean,
            val bosattIRiket: Boolean
    )


    companion object {

        private fun barnetLever(input: TestFaktaForFiltreringsregler): Evaluering {
            return if (input.barnetLever)
                Evaluering.oppfylt(BARNET_LEVER)
            else Evaluering.ikkeOppfylt(BARNET_LEVER_IKKE)
        }

        private fun morLever(input: TestFaktaForFiltreringsregler): Evaluering {
            return if (input.morLever)
                Evaluering.oppfylt(MOR_LEVER)
            else Evaluering.ikkeOppfylt(MOR_LEVER_IKKE)
        }

        private fun morErOver18år(input: TestFaktaForFiltreringsregler): Evaluering {
            return if (input.morErOver18År)
                Evaluering.oppfylt(MOR_ER_OVER_18_ÅR)
            else Evaluering.ikkeOppfylt(MOR_ER_UNDER_18_ÅR)
        }

        val testSpesifikasjoner = Spesifikasjon<TestFaktaForFiltreringsregler>(
                "Test Regel 0",
                "BARNET_LEVER",
                implementasjon = { barnetLever(this) }
        ) og Spesifikasjon<TestFaktaForFiltreringsregler>(
                "Test Regel 1",
                "MOR_LEVER",
                implementasjon = { morLever(this) }
        ) og Spesifikasjon<TestFaktaForFiltreringsregler>(
                "Test Regel 2",
                "MOR_ER_OVER_18_AAR",
                implementasjon = { morErOver18år(this) }
        )

        private fun genererBehandlingResultat(søkersVilkår: TestFaktaForSøkersVilkår,
                                              barnasVilkår: TestFaktaForBarnasVilkår): Vilkårsvurdering {
            return Vilkårsvurdering(
                    behandling = behandling
            ).also {
                it.personResultater = setOf(
                        PersonResultat(
                                vilkårsvurdering = it,
                                personIdent = søkersIdent,
                                vilkårResultater = mutableSetOf(
                                        VilkårResultat(
                                                personResultat = null,
                                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                                resultat = if (søkersVilkår.bosattIRiket) Resultat.OPPFYLT else Resultat.IKKE_OPPFYLT,
                                                begrunnelse = "whatever",
                                                behandlingId = behandling.id),
                                        VilkårResultat(
                                                personResultat = null,
                                                vilkårType = Vilkår.LOVLIG_OPPHOLD,
                                                resultat = if (søkersVilkår.lovligOpphold) Resultat.OPPFYLT else Resultat.IKKE_OPPFYLT,
                                                begrunnelse = "Begrunnelse fra vilkårsvurdering",
                                                behandlingId = behandling.id)
                                )
                        ),
                        PersonResultat(
                                vilkårsvurdering = it,
                                personIdent = barnetsIdent,
                                vilkårResultater = mutableSetOf(
                                        VilkårResultat(
                                                personResultat = null,
                                                vilkårType = Vilkår.UNDER_18_ÅR,
                                                resultat = if (barnasVilkår.under18År) Resultat.OPPFYLT else Resultat.IKKE_OPPFYLT,
                                                begrunnelse = "whatever",
                                                behandlingId = behandling.id),
                                        VilkårResultat(
                                                personResultat = null,
                                                vilkårType = Vilkår.BOR_MED_SØKER,
                                                resultat = if (barnasVilkår.borMedSøker) Resultat.OPPFYLT else Resultat.IKKE_OPPFYLT,
                                                begrunnelse = "whatever",
                                                behandlingId = behandling.id),
                                        VilkårResultat(
                                                personResultat = null,
                                                vilkårType = Vilkår.GIFT_PARTNERSKAP,
                                                resultat = if (barnasVilkår.giftPartnerskap) Resultat.OPPFYLT else Resultat.IKKE_OPPFYLT,
                                                begrunnelse = "whatever",
                                                behandlingId = behandling.id),
                                        VilkårResultat(
                                                personResultat = null,
                                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                                resultat = if (barnasVilkår.bosattIRiket) Resultat.OPPFYLT else Resultat.IKKE_OPPFYLT,
                                                begrunnelse = "whatever",
                                                behandlingId = behandling.id)
                                )
                        )
                )
            }
        }

        val søkersIdent = "12345678910"
        val barnetsIdent = "22345678910"
        val fagsak = Fagsak(søkerIdenter = emptySet())
        val behandling = lagBehandling(
                fagsak = fagsak,
                årsak = BehandlingÅrsak.FØDSELSHENDELSE,
                automatiskOpprettelse = true,
                behandlingKategori = BehandlingKategori.EØS
        )
        val søker = Person(type = PersonType.SØKER, fødselsdato = LocalDate.of(1990, 1, 12), kjønn = Kjønn.KVINNE,
                           personIdent = PersonIdent(søkersIdent),
                           personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id))
                .apply { sivilstander = listOf(GrSivilstand(type = SIVILSTAND.GIFT, person = this)) }
        val barn = Person(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 1, 12), kjønn = Kjønn.KVINNE,
                          personIdent = PersonIdent(barnetsIdent),
                          personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id))
                .apply { sivilstander = listOf(GrSivilstand(type = SIVILSTAND.GIFT, person = this)) }
    }
}
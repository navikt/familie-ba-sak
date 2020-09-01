package no.nav.familie.ba.sak.behandling.fødselshendelse

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.*
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.*
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.evaluations.Resultat
import no.nav.nare.core.specifications.Spesifikasjon
import org.junit.Assert
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppgaveBeskrivelseTest {
    val infotrygdBarnetrygdClientMock = mockk<InfotrygdBarnetrygdClient>()
    val personopplysningerServiceMock = mockk<PersonopplysningerService>()
    val infotrygdFeedServiceMock = mockk<InfotrygdFeedService>()
    val featureToggleServiceMock = mockk<FeatureToggleService>()
    val stegServiceMock = mockk<StegService>()
    val vedtakServiceMock = mockk<VedtakService>()
    val evaluerFiltreringsreglerForFødselshendelseMock = mockk<EvaluerFiltreringsreglerForFødselshendelse>()
    val taskRepositoryMock = mockk<TaskRepository>()
    val behandlingResultatRepositoryMock = mockk<BehandlingResultatRepository>()
    val persongrunnlagServiceMock = mockk<PersongrunnlagService>()
    val behandlingRepositoryMock = mockk<BehandlingRepository>()
    val fødselshendelseService = FødselshendelseService(infotrygdFeedServiceMock,
                                                        infotrygdBarnetrygdClientMock,
                                                        featureToggleServiceMock,
                                                        stegServiceMock,
                                                        vedtakServiceMock,
                                                        evaluerFiltreringsreglerForFødselshendelseMock,
                                                        taskRepositoryMock,
                                                        personopplysningerServiceMock,
                                                        behandlingResultatRepositoryMock,
                                                        persongrunnlagServiceMock,
                                                        behandlingRepositoryMock)

    @Test
    fun `hentBegrunnelseFraFiltreringsregler() skal returnere begrunnelse av første regel som feilet`() {
        val testEvaluering0 = testSpesifikasjoner.evaluer(TestFaktaForFiltreringsregler(morHarGyldigFodselsnummer = false,
                                                                                        barnetHarGyldigFodselsnummer = false,
                                                                                        barnetErUnder6Mnd = true))
        val beskrivelse0 = fødselshendelseService.hentBegrunnelseFraFiltreringsregler(testEvaluering0)
        Assert.assertEquals("mor har ikke gyldig fødselsnummer", beskrivelse0)

        val testEvaluering1 = testSpesifikasjoner.evaluer(TestFaktaForFiltreringsregler(morHarGyldigFodselsnummer = true,
                                                                                        barnetHarGyldigFodselsnummer = false,
                                                                                        barnetErUnder6Mnd = false))
        val beskrivelse1 = fødselshendelseService.hentBegrunnelseFraFiltreringsregler(testEvaluering1)
        Assert.assertEquals("barnet har ikke gyldig fødselsnummer", beskrivelse1)

        val testEvaluering2 = testSpesifikasjoner.evaluer(TestFaktaForFiltreringsregler(morHarGyldigFodselsnummer = true,
                                                                                        barnetHarGyldigFodselsnummer = true,
                                                                                        barnetErUnder6Mnd = false))
        val beskrivelse2 = fødselshendelseService.hentBegrunnelseFraFiltreringsregler(testEvaluering2)
        Assert.assertEquals("barnet er over 6 måneder", beskrivelse2)
    }

    @Test
    fun `hentBegrunnelseFraFiltreringsregler() skal returnere null dersom ingen regler feiler`() {
        val testEvaluering = testSpesifikasjoner.evaluer(TestFaktaForFiltreringsregler(morHarGyldigFodselsnummer = true,
                                                                                       barnetHarGyldigFodselsnummer = true,
                                                                                       barnetErUnder6Mnd = true))
        val beskrivelse = fødselshendelseService.hentBegrunnelseFraFiltreringsregler(testEvaluering)
        Assert.assertNull(beskrivelse)
    }

    @Test
    fun `hentBegrunnelseFraVilkårsvurdering() skal returnere riktig beskrivelse basert på vilkårsrekkefølgen`(){
        every{behandlingRepositoryMock.finnBehandling(any())} returns behandling
        every{persongrunnlagServiceMock.hentSøker(any())} returns søker
        every{persongrunnlagServiceMock.hentBarna(any())} returns listOf(barn)
        every{behandlingResultatRepositoryMock.findByBehandlingAndAktiv(any())} returns genererBehandlingResultat(
                søkersVilkår = TestFaktaForSøkersVilkår(bosattIRiket = false, lovligOpphold = false),
                barnasVilkår = TestFaktaForBarnasVilkår(under18År = false, borMedSøker = true, giftPartnerskap = true, bosattIRiket = true)
        )
        val beskrivelse0 = fødselshendelseService.hentBegrunnelseFraVilkårsvurdering(behandling.id)
        Assert.assertEquals("Mor er ikke bosatt i riket.", beskrivelse0)

        every{behandlingResultatRepositoryMock.findByBehandlingAndAktiv(any())} returns genererBehandlingResultat(
                søkersVilkår = TestFaktaForSøkersVilkår(bosattIRiket = true, lovligOpphold = false),
                barnasVilkår = TestFaktaForBarnasVilkår(under18År = false, borMedSøker = true, giftPartnerskap = true, bosattIRiket = true)
        )
        val beskrivelse1 = fødselshendelseService.hentBegrunnelseFraVilkårsvurdering(behandling.id)
        Assert.assertEquals("Begrunnelse fra vilkårsvurdering", beskrivelse1)

        every{behandlingResultatRepositoryMock.findByBehandlingAndAktiv(any())} returns genererBehandlingResultat(
                søkersVilkår = TestFaktaForSøkersVilkår(bosattIRiket = true, lovligOpphold = true),
                barnasVilkår = TestFaktaForBarnasVilkår(under18År = false, borMedSøker = true, giftPartnerskap = false, bosattIRiket = true)
        )
        val beskrivelse2 = fødselshendelseService.hentBegrunnelseFraVilkårsvurdering(behandling.id)
        Assert.assertEquals("Barnet er over 18 år.", beskrivelse2)

        every{behandlingResultatRepositoryMock.findByBehandlingAndAktiv(any())} returns genererBehandlingResultat(
                søkersVilkår = TestFaktaForSøkersVilkår(bosattIRiket = true, lovligOpphold = true),
                barnasVilkår = TestFaktaForBarnasVilkår(under18År = true, borMedSøker = false, giftPartnerskap = false, bosattIRiket = true)
        )
        val beskrivelse3 = fødselshendelseService.hentBegrunnelseFraVilkårsvurdering(behandling.id)
        Assert.assertEquals("Barnet er ikke bosatt med mor.", beskrivelse3)
    }

    @Test
    fun `hentBegrunnelseFraVilkårsvurdering() skal returnere null dersom ingen regler i vilkårsvurderingen feiler`(){
        every{behandlingRepositoryMock.finnBehandling(any())} returns behandling
        every{persongrunnlagServiceMock.hentSøker(any())} returns søker
        every{persongrunnlagServiceMock.hentBarna(any())} returns listOf(barn)
        every{behandlingResultatRepositoryMock.findByBehandlingAndAktiv(any())} returns genererBehandlingResultat(
                søkersVilkår = TestFaktaForSøkersVilkår(bosattIRiket = true, lovligOpphold = true),
                barnasVilkår = TestFaktaForBarnasVilkår(under18År = true, borMedSøker = true, giftPartnerskap = true, bosattIRiket = true)
        )
        val beskrivelse = fødselshendelseService.hentBegrunnelseFraVilkårsvurdering(behandling.id)
        Assert.assertNull(beskrivelse)
    }

    class TestFaktaForFiltreringsregler(
            val morHarGyldigFodselsnummer: Boolean,
            val barnetHarGyldigFodselsnummer: Boolean,
            val barnetErUnder6Mnd: Boolean
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

        private fun morHarGyldigFodselsnummer(input: TestFaktaForFiltreringsregler): Evaluering {
            return if (input.morHarGyldigFodselsnummer)
                Evaluering.ja("mor har gyldig fødselsnummer")
            else Evaluering.nei("mor har ikke gyldig fødselsnummer")
        }

        private fun barnetHarGyldigFodselsnummer(input: TestFaktaForFiltreringsregler): Evaluering {
            return if (input.barnetHarGyldigFodselsnummer)
                Evaluering.ja("barnet har gyldig fødselsnummer")
            else Evaluering.nei("barnet har ikke gyldig fødselsnummer")
        }

        private fun barnetErUnder6Mnd(input: TestFaktaForFiltreringsregler): Evaluering {
            return if (input.barnetErUnder6Mnd)
                Evaluering.ja("barnet er under 6 måneder")
            else Evaluering.nei("barnet er over 6 måneder")
        }

        val testSpesifikasjoner = Spesifikasjon<TestFaktaForFiltreringsregler>(
                "Test Regel 0",
                "MOR_HAR_GYLDIG_FOEDSELSNUMMER",
                implementasjon = { morHarGyldigFodselsnummer(this) }
        ) og Spesifikasjon<TestFaktaForFiltreringsregler>(
                "Test Regel 1",
                "BARNET_HAR_GYLDIG_FOEDSELSNUMMER",
                implementasjon = { barnetHarGyldigFodselsnummer(this) }
        ) og Spesifikasjon<TestFaktaForFiltreringsregler>(
                "Test Regel 2",
                "BARNET_ER_UNDER_6_MND",
                implementasjon = { barnetErUnder6Mnd(this) }
        )

        private fun genererBehandlingResultat(søkersVilkår: TestFaktaForSøkersVilkår,
                                              barnasVilkår: TestFaktaForBarnasVilkår): BehandlingResultat {
            return BehandlingResultat(
                    behandling = behandling
            ).also {
                it.personResultater = setOf(
                        PersonResultat(
                                behandlingResultat = it,
                                personIdent = søkersIdent,
                                vilkårResultater = mutableSetOf(
                                        VilkårResultat(
                                                personResultat = null,
                                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                                resultat = if (søkersVilkår.bosattIRiket) Resultat.JA else Resultat.NEI,
                                                begrunnelse = "whatever",
                                                behandlingId = behandling.id,
                                                regelInput = null,
                                                regelOutput = null),
                                        VilkårResultat(
                                                personResultat = null,
                                                vilkårType = Vilkår.LOVLIG_OPPHOLD,
                                                resultat = if (søkersVilkår.lovligOpphold) Resultat.JA else Resultat.NEI,
                                                begrunnelse = "Begrunnelse fra vilkårsvurdering",
                                                behandlingId = behandling.id,
                                                regelInput = null,
                                                regelOutput = null)
                                )
                        ),
                        PersonResultat(
                                behandlingResultat = it,
                                personIdent = barnetsIdent,
                                vilkårResultater = mutableSetOf(
                                        VilkårResultat(
                                                personResultat = null,
                                                vilkårType = Vilkår.UNDER_18_ÅR,
                                                resultat = if (barnasVilkår.under18År) Resultat.JA else Resultat.NEI,
                                                begrunnelse = "whatever",
                                                behandlingId = behandling.id,
                                                regelInput = null,
                                                regelOutput = null),
                                        VilkårResultat(
                                                personResultat = null,
                                                vilkårType = Vilkår.BOR_MED_SØKER,
                                                resultat = if (barnasVilkår.borMedSøker) Resultat.JA else Resultat.NEI,
                                                begrunnelse = "whatever",
                                                behandlingId = behandling.id,
                                                regelInput = null,
                                                regelOutput = null),
                                        VilkårResultat(
                                                personResultat = null,
                                                vilkårType = Vilkår.GIFT_PARTNERSKAP,
                                                resultat = if (barnasVilkår.giftPartnerskap) Resultat.JA else Resultat.NEI,
                                                begrunnelse = "whatever",
                                                behandlingId = behandling.id,
                                                regelInput = null,
                                                regelOutput = null),
                                        VilkårResultat(
                                                personResultat = null,
                                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                                resultat = if (barnasVilkår.bosattIRiket) Resultat.JA else Resultat.NEI,
                                                begrunnelse = "whatever",
                                                behandlingId = behandling.id,
                                                regelInput = null,
                                                regelOutput = null)
                                )
                        )
                )
            }
        }

        val søkersIdent = "12345678910"
        val barnetsIdent = "22345678910"
        val fagsak = Fagsak(status = FagsakStatus.OPPRETTET, søkerIdenter = emptySet())
        val behandling = Behandling(fagsak = fagsak,
                                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                    opprinnelse = BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE,
                                    kategori = BehandlingKategori.EØS,
                                    underkategori = BehandlingUnderkategori.ORDINÆR)
        val søker = Person(type = PersonType.SØKER, fødselsdato = LocalDate.of(1990, 1, 12), kjønn = Kjønn.KVINNE,
                           sivilstand = SIVILSTAND.GIFT, personIdent = PersonIdent(søkersIdent),
                           personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id))
        val barn = Person(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 1, 12), kjønn = Kjønn.KVINNE,
                          sivilstand = SIVILSTAND.GIFT, personIdent = PersonIdent(barnetsIdent),
                          personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id))
    }
}
package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.lang.IllegalStateException
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("dev")
class VilkårVurderingTest {

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var vilkårService: VilkårService

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Test
    fun `Hent relevante vilkår for persontype BARN`() {
        val relevanteVilkår = Vilkår.hentVilkårForPart(PersonType.BARN)
        val relevanteVilkårForDato = Vilkår.hentVilkårForPart(PersonType.BARN, LocalDate.now())
        val vilkårForBarn = setOf(Vilkår.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                                  Vilkår.STØNADSPERIODE,
                                  Vilkår.BOSATT_I_RIKET)
        Assertions.assertEquals(vilkårForBarn, relevanteVilkår)
        Assertions.assertEquals(vilkårForBarn, relevanteVilkårForDato)
    }

    @Test
    fun `Hent relevante vilkår for persontype SØKER`() {
        val relevanteVilkår = Vilkår.hentVilkårForPart(PersonType.SØKER)
        val vilkårForSøker = setOf(Vilkår.STØNADSPERIODE,
                                   Vilkår.BOSATT_I_RIKET)
        Assertions.assertEquals(vilkårForSøker, relevanteVilkår)
    }

    @Test
    fun `Hent relevante vilkår for saktype`() { //Banal test, legg til saktyper
        val relevanteVilkårSaktypeFinnes = Vilkår.hentVilkårForSakstype(SakType.VILKÅRGJELDERFOR)
        val relevanteVilkårSaktypeFinnesIkke = Vilkår.hentVilkårForSakstype(SakType.VILKÅRGJELDERIKKEFOR)
        val vilkårForSaktypeFinnes = setOf(Vilkår.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                                           Vilkår.STØNADSPERIODE,
                                           Vilkår.BOSATT_I_RIKET)
        val vilkårForSaktypFinnesIkke: Set<Vilkår> = emptySet()
        Assertions.assertEquals(vilkårForSaktypeFinnes, relevanteVilkårSaktypeFinnes)
        Assertions.assertEquals(vilkårForSaktypFinnesIkke, relevanteVilkårSaktypeFinnesIkke)
    }

    @Test
    fun `Hent relevante vilkår for persontype og saktype`() {
        val relevanteVilkår = Vilkår.hentVilkårFor(PersonType.BARN, SakType.VILKÅRGJELDERFOR)
        val vilkårForBarn = setOf(Vilkår.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                                  Vilkår.STØNADSPERIODE,
                                  Vilkår.BOSATT_I_RIKET)
        Assertions.assertEquals(vilkårForBarn, relevanteVilkår)
    }

    @Test
    fun `Henting og evaluering av fødselshendelse med flere barn kaster exception`() {

        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))

        personopplysningGrunnlag.personer.add(Person(aktørId = randomAktørId(),
                                                     personIdent = PersonIdent("11111111111"),
                                                     type = PersonType.BARN,
                                                     personopplysningGrunnlag = personopplysningGrunnlag,
                                                     fødselsdato = LocalDate.now(),
                                                     navn = "",
                                                     kjønn = Kjønn.MANN))

        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        assertThrows<IllegalStateException> {
            vilkårService.vurderVilkårForFødselshendelse(behandlingId = behandling.id)
        }
    }

    @Test
    fun `Henting og evaluering av fødselshendelse med oppfylte vilkår gir samlet resultat JA`() {

        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val periodeResultat = vilkårService.vurderVilkårForFødselshendelse(behandlingId = behandling.id)
        Assertions.assertEquals(Resultat.JA, periodeResultat.hentSamletResultat())
    }

    @Test
    fun `Henting og evaluering av fødselshendelse uten oppfylte vilkår gir samlet resultat NEI`() {

        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, fnr, emptyList())
        personopplysningGrunnlag.personer.add(Person(aktørId = randomAktørId(),
                                                     personIdent = PersonIdent("11111111111"),
                                                     type = PersonType.BARN,
                                                     personopplysningGrunnlag = personopplysningGrunnlag,
                                                     fødselsdato = LocalDate.of(1980, 1, 1), //Over 18år
                                                     navn = "",
                                                     kjønn = Kjønn.MANN))

        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)
        val periodeResultat = vilkårService.vurderVilkårForFødselshendelse(behandlingId = behandling.id)

        Assertions.assertEquals(Resultat.NEI, periodeResultat.hentSamletResultat())
    }

    @Test
    fun `Henting og evaluering av oppfylte vilkår gir rett antall samlede resultater`() {

        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val behandlingResultat = vilkårService.vurderVilkårForFødselshendelse(behandlingId = behandling.id)

        val forventetAntallVurderteVilkår = Vilkår.hentVilkårForPart(PersonType.BARN).size + Vilkår.hentVilkårForPart(PersonType.SØKER).size
        Assertions.assertEquals(forventetAntallVurderteVilkår,
                                behandlingResultat.periodeResultater.flatMap { periodeResultat -> periodeResultat.vilkårResultater }.size)
    }

    @Test
    fun `Sjekk gyldig vilkårsperiode`() {
        val ubegrensetGyldigVilkårsperiode = GyldigVilkårsperiode()
        assertTrue(ubegrensetGyldigVilkårsperiode.gyldigFor(LocalDate.now()))

        val begrensetGyldigVilkårsperiode = GyldigVilkårsperiode(
                gyldigFom = LocalDate.now().minusDays(5),
                gyldigTom = LocalDate.now().plusDays(5))
        assertTrue(begrensetGyldigVilkårsperiode.gyldigFor(LocalDate.now()))
        assertTrue(begrensetGyldigVilkårsperiode.gyldigFor(LocalDate.now().minusDays(5)))
        assertFalse(begrensetGyldigVilkårsperiode.gyldigFor(LocalDate.now().minusDays(6)))
        assertTrue(begrensetGyldigVilkårsperiode.gyldigFor(LocalDate.now().plusDays(5)))
        assertFalse(begrensetGyldigVilkårsperiode.gyldigFor(LocalDate.now().plusDays(6)))
    }
}
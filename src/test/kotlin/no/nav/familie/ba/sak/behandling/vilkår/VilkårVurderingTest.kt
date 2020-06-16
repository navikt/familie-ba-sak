package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.*
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VilkårVurderingTest(
        @Autowired
        private val behandlingService: BehandlingService,

        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
        private val vilkårService: VilkårService,

        @Autowired
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,

        @Autowired
        private val databaseCleanupService: DatabaseCleanupService
) {

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Hent relevante vilkår for persontype BARN`() {
        val relevanteVilkår = Vilkår.hentVilkårForPart(PersonType.BARN)
        val relevanteVilkårForDato = Vilkår.hentVilkårForPart(PersonType.BARN, LocalDate.now())
        val vilkårForBarn = setOf(Vilkår.UNDER_18_ÅR,
                                  Vilkår.BOR_MED_SØKER,
                                  Vilkår.GIFT_PARTNERSKAP,
                                  Vilkår.BOSATT_I_RIKET,
                                  Vilkår.LOVLIG_OPPHOLD)
        Assertions.assertEquals(vilkårForBarn, relevanteVilkår)
        Assertions.assertEquals(vilkårForBarn, relevanteVilkårForDato)
    }

    @Test
    fun `Hent relevante vilkår for persontype SØKER`() {
        val relevanteVilkår = Vilkår.hentVilkårForPart(PersonType.SØKER)
        val vilkårForSøker = setOf(Vilkår.BOSATT_I_RIKET,
                                   Vilkår.LOVLIG_OPPHOLD)
        Assertions.assertEquals(vilkårForSøker, relevanteVilkår)
    }

    @Test
    fun `Hent relevante vilkår for saktype`() {
        val vilkårForEøs = Vilkår.hentVilkårForSakstype(SakType.valueOfType(BehandlingKategori.EØS))
        val vilkårForNasjonal = Vilkår.hentVilkårForSakstype(SakType.valueOfType(BehandlingKategori.NASJONAL))
        Assertions.assertEquals(setOf(Vilkår.UNDER_18_ÅR,
                                      Vilkår.BOR_MED_SØKER,
                                      Vilkår.GIFT_PARTNERSKAP,
                                      Vilkår.BOSATT_I_RIKET,
                                      Vilkår.LOVLIG_OPPHOLD),
                                vilkårForEøs)
        Assertions.assertEquals(setOf(Vilkår.UNDER_18_ÅR,
                                      Vilkår.BOR_MED_SØKER,
                                      Vilkår.GIFT_PARTNERSKAP,
                                      Vilkår.BOSATT_I_RIKET),
                                vilkårForNasjonal)
    }

    @Test
    fun `Hent relevante vilkår for persontype og saktype`() {
        val relevanteVilkår = Vilkår.hentVilkårFor(PersonType.BARN, SakType.EØS)
        val vilkårForBarn = setOf(Vilkår.UNDER_18_ÅR,
                                  Vilkår.BOR_MED_SØKER,
                                  Vilkår.GIFT_PARTNERSKAP,
                                  Vilkår.BOSATT_I_RIKET,
                                  Vilkår.LOVLIG_OPPHOLD)
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
                                                     personIdent = PersonIdent(barnFnr),
                                                     type = PersonType.BARN,
                                                     personopplysningGrunnlag = personopplysningGrunnlag,
                                                     fødselsdato = LocalDate.now(),
                                                     navn = "",
                                                     kjønn = Kjønn.MANN
        ))

        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        assertThrows<IllegalStateException> {
            vilkårService.vurderVilkårForFødselshendelse(behandlingId = behandling.id)
        }
    }

    @Test
    fun `Henting og evaluering av fødselshendelse med oppfylte vilkår gir behandlingsresultat innvilget`() {

        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val behandlingResultat = vilkårService.vurderVilkårForFødselshendelse(behandlingId = behandling.id)
        Assertions.assertEquals(BehandlingResultatType.INNVILGET, behandlingResultat.hentSamletResultat())
    }

    @Test
    fun `Henting og evaluering av fødselshendelse uten oppfylte vilkår gir samlet behandlingsresultat avslått`() {

        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, emptyList())
        personopplysningGrunnlag.personer.add(Person(aktørId = randomAktørId(),
                                                     personIdent = PersonIdent(barnFnr),
                                                     type = PersonType.BARN,
                                                     personopplysningGrunnlag = personopplysningGrunnlag,
                                                     fødselsdato = LocalDate.of(1980, 1, 1), //Over 18år
                                                     navn = "",
                                                     kjønn = Kjønn.MANN))

        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)
        val behandlingResultat = vilkårService.vurderVilkårForFødselshendelse(behandlingId = behandling.id)

        Assertions.assertEquals(BehandlingResultatType.AVSLÅTT, behandlingResultat.hentSamletResultat())
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

        val forventetAntallVurderteVilkår =
                Vilkår.hentVilkårFor(PersonType.BARN, SakType.valueOfType(BehandlingKategori.NASJONAL)).size +
                Vilkår.hentVilkårFor(PersonType.SØKER, SakType.valueOfType(BehandlingKategori.NASJONAL)).size
        Assertions.assertEquals(forventetAntallVurderteVilkår,
                                behandlingResultat.personResultater.flatMap { personResultat -> personResultat.vilkårResultater }.size)
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

    private fun genererPerson(type: PersonType, personopplysningGrunnlag: PersonopplysningGrunnlag, bostedAddress: BostedsadressePdl?, kjønn: Kjønn= Kjønn.KVINNE) : Person{
        return  Person(aktørId = randomAktørId(),
                       personIdent = PersonIdent(randomFnr()),
                       type = type,
                       personopplysningGrunnlag = personopplysningGrunnlag,
                       fødselsdato = LocalDate.of(1991, 1, 1),
                       navn = "navn",
                       kjønn = kjønn,
                       bostedsadresse = bostedAddress)
    }

    @Test
    fun `Sjekk barn bor med søker`() {
        val søkerAddress = VegadressePdl("11", "B", "H022",
                                         "St. Olavsvegen", "1232", "whatever", "4322")
        val barnAddress = VegadressePdl("11", "B", "H024",
                                        "St. Olavsvegen", "1232", "whatever", "4322")
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 1)

        val søker= genererPerson(PersonType.SØKER, personopplysningGrunnlag, søkerAddress)
        personopplysningGrunnlag.personer.add(søker)

        val barn1 = genererPerson(PersonType.BARN, personopplysningGrunnlag, søkerAddress, Kjønn.MANN)
        personopplysningGrunnlag.personer.add(barn1)

        val barn2 = genererPerson(PersonType.BARN, personopplysningGrunnlag, barnAddress, Kjønn.MANN)
        personopplysningGrunnlag.personer.add(barn2)

        val barn3 = genererPerson(PersonType.BARN, personopplysningGrunnlag, null, Kjønn.MANN)
        personopplysningGrunnlag.personer.add(barn3)

        Assertions.assertEquals(Resultat.JA, barnBorMedMor (Fakta(barn1)).resultat)
        Assertions.assertEquals(Resultat.NEI, barnBorMedMor (Fakta(barn2)).resultat)
        Assertions.assertEquals(Resultat.NEI, barnBorMedMor (Fakta(barn3)).resultat)
    }

    @Test
    fun `Negative Test for sjekk barn bor med søker`() {
        val søkerAddress = VegadressePdl("11", "B", "H022",
                                         "St. Olavsvegen", "1232", "whatever", "4322")

        //Negative Scenario 1: søker and barn both have null bostedAddress
        val personopplysningGrunnlag1 = PersonopplysningGrunnlag(behandlingId = 2)

        val søker1 = genererPerson(PersonType.SØKER, personopplysningGrunnlag1, null)

        personopplysningGrunnlag1.personer.add(søker1)

        val barn11 = genererPerson(PersonType.BARN, personopplysningGrunnlag1, null)

        personopplysningGrunnlag1.personer.add(barn11)

        Assertions.assertEquals(Resultat.NEI, barnBorMedMor(Fakta(barn11)).resultat)

        //Negative Scenario 2: Two søker
        val personopplysningGrunnlag2 = PersonopplysningGrunnlag(behandlingId = 3)

        val søker21 = genererPerson(PersonType.SØKER, personopplysningGrunnlag2, søkerAddress)
        personopplysningGrunnlag2.personer.add(søker21)

        val søker22 = genererPerson(PersonType.SØKER, personopplysningGrunnlag2, søkerAddress)
        personopplysningGrunnlag2.personer.add(søker22)

        val barn21 = genererPerson(PersonType.BARN, personopplysningGrunnlag2, søkerAddress)
        personopplysningGrunnlag2.personer.add(barn21)

        Assertions.assertEquals(Resultat.NEI, barnBorMedMor(Fakta(barn21)).resultat)

        //Negative Scenario 3: No søker
        val personopplysningGrunnlag3 = PersonopplysningGrunnlag(behandlingId = 4)

        val barn31 = genererPerson(PersonType.BARN, personopplysningGrunnlag3, søkerAddress, Kjønn.MANN)
        personopplysningGrunnlag3.personer.add(barn31)

        Assertions.assertEquals(Resultat.NEI, barnBorMedMor(Fakta(barn31)).resultat)

        //Negative Scenario 4: Søker is not mother
        val personopplysningGrunnlag4= PersonopplysningGrunnlag(behandlingId = 5)
        val søker41= genererPerson(PersonType.SØKER, personopplysningGrunnlag4, søkerAddress, Kjønn.MANN)
        personopplysningGrunnlag4.personer.add(søker41)
        val barn41 = genererPerson(PersonType.BARN, personopplysningGrunnlag4, søkerAddress)
        personopplysningGrunnlag4.personer.add(barn41)

        Assertions.assertEquals(Resultat.NEI, barnBorMedMor(Fakta(barn41)).resultat)
    }
}
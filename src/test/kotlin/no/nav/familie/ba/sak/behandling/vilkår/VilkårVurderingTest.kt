package no.nav.familie.ba.sak.behandling.vilkår

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.*
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("dev", "mock-pdl")
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
        assertEquals(vilkårForBarn, relevanteVilkår)
        assertEquals(vilkårForBarn, relevanteVilkårForDato)
    }

    @Test
    fun `Hent relevante vilkår for persontype SØKER`() {
        val relevanteVilkår = Vilkår.hentVilkårForPart(PersonType.SØKER)
        val vilkårForSøker = setOf(Vilkår.BOSATT_I_RIKET,
                                   Vilkår.LOVLIG_OPPHOLD)
        assertEquals(vilkårForSøker, relevanteVilkår)
    }

    @Test
    fun `Hent relevante vilkår for saktype EØS`() {
        val vilkårForEøs = Vilkår.hentVilkårForSakstype(SakType.valueOfType(BehandlingKategori.EØS))
        assertEquals(setOf(Vilkår.UNDER_18_ÅR,
                           Vilkår.BOR_MED_SØKER,
                           Vilkår.GIFT_PARTNERSKAP,
                           Vilkår.BOSATT_I_RIKET,
                           Vilkår.LOVLIG_OPPHOLD),
                     vilkårForEøs)
    }

    @Test
    fun `Hent relevante vilkår for saktype Nasjonal`() {
        val vilkårForNasjonal = Vilkår.hentVilkårForSakstype(SakType.valueOfType(BehandlingKategori.NASJONAL))
        assertEquals(setOf(Vilkår.UNDER_18_ÅR,
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
        assertEquals(vilkårForBarn, relevanteVilkår)
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
                                                     kjønn = Kjønn.MANN,
                                                     sivilstand = SIVILSTAND.UGIFT
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
        assertEquals(BehandlingResultatType.INNVILGET, behandlingResultat.hentSamletResultat())

        behandlingResultat.personResultater.forEach {
            it.vilkårResultater.forEach {
                Assertions.assertNotNull(it.regelInput)
                val fakta = ObjectMapper().readValue(it.regelInput, Map::class.java)
                assertTrue(fakta.containsKey("personForVurdering"))
                Assertions.assertNotNull(it.regelOutput)
                val evaluering = ObjectMapper().readValue(it.regelOutput, Map::class.java)
                assertEquals(evaluering["resultat"], "JA")
            }
        }
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
                                                     kjønn = Kjønn.MANN,
                                                     sivilstand = SIVILSTAND.UGIFT))

        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)
        val behandlingResultat = vilkårService.vurderVilkårForFødselshendelse(behandlingId = behandling.id)

        assertEquals(BehandlingResultatType.AVSLÅTT, behandlingResultat.hentSamletResultat())
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
        assertEquals(forventetAntallVurderteVilkår,
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

    private fun genererPerson(type: PersonType,
                              personopplysningGrunnlag: PersonopplysningGrunnlag,
                              grBostedsadresse: GrBostedsadresse? = null,
                              kjønn: Kjønn = Kjønn.KVINNE,
                              sivilstand: SIVILSTAND = SIVILSTAND.UGIFT): Person {
        return Person(aktørId = randomAktørId(),
                      personIdent = PersonIdent(randomFnr()),
                      type = type,
                      personopplysningGrunnlag = personopplysningGrunnlag,
                      fødselsdato = LocalDate.of(1991, 1, 1),
                      navn = "navn",
                      kjønn = kjønn,
                      bostedsadresse = grBostedsadresse,
                      sivilstand = sivilstand)
    }

    @Test
    fun `Sjekk barn bor med søker`() {
        val søkerAddress = GrVegadresse(1234, "11", "B", "H022",
                                        "St. Olavsvegen", "1232", "whatever", "4322")
        val barnAddress = GrVegadresse(1234, "11", "B", "H024",
                                       "St. Olavsvegen", "1232", "whatever", "4322")
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 1)

        val søker = genererPerson(PersonType.SØKER, personopplysningGrunnlag, søkerAddress)
        personopplysningGrunnlag.personer.add(søker)

        val barn1 = genererPerson(PersonType.BARN, personopplysningGrunnlag, søkerAddress, Kjønn.MANN)
        personopplysningGrunnlag.personer.add(barn1)

        val barn2 = genererPerson(PersonType.BARN, personopplysningGrunnlag, barnAddress, Kjønn.MANN)
        personopplysningGrunnlag.personer.add(barn2)

        val barn3 = genererPerson(PersonType.BARN, personopplysningGrunnlag, null, Kjønn.MANN)
        personopplysningGrunnlag.personer.add(barn3)

        assertEquals(Resultat.JA, Vilkår.BOR_MED_SØKER.spesifikasjon.evaluer(Fakta(barn1)).resultat)
        assertEquals(Resultat.NEI, Vilkår.BOR_MED_SØKER.spesifikasjon.evaluer(Fakta(barn2)).resultat)
        assertEquals(Resultat.NEI, Vilkår.BOR_MED_SØKER.spesifikasjon.evaluer(Fakta(barn3)).resultat)
    }

    @Test
    fun `Negativ vurdering - Barn og søker har ikke adresse angitt`() {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 2)
        val søker = genererPerson(PersonType.SØKER, personopplysningGrunnlag, null)
        personopplysningGrunnlag.personer.add(søker)

        val barn = genererPerson(PersonType.BARN, personopplysningGrunnlag, null)
        personopplysningGrunnlag.personer.add(barn)

        assertEquals(Resultat.NEI, Vilkår.BOR_MED_SØKER.spesifikasjon.evaluer(Fakta(barn)).resultat)
    }

    @Test
    fun `Negativ vurdering - To søker`() {
        val søkerAddress = GrVegadresse(1234, "11", "B", "H022",
                                        "St. Olavsvegen", "1232", "whatever", "4322")

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 3)
        val søker1 = genererPerson(PersonType.SØKER, personopplysningGrunnlag, søkerAddress)
        personopplysningGrunnlag.personer.add(søker1)
        val søker2 = genererPerson(PersonType.SØKER, personopplysningGrunnlag, søkerAddress)
        personopplysningGrunnlag.personer.add(søker2)
        val barn = genererPerson(PersonType.BARN, personopplysningGrunnlag, søkerAddress)
        personopplysningGrunnlag.personer.add(barn)

        assertEquals(Resultat.NEI, Vilkår.BOR_MED_SØKER.spesifikasjon.evaluer(Fakta(barn)).resultat)
    }

    @Test
    fun `Negativ vurdering - ingen søker`() {
        val søkerAddress = GrVegadresse(1234, "11", "B", "H022",
                                        "St. Olavsvegen", "1232", "whatever", "4322")

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 4)
        val barn = genererPerson(PersonType.BARN, personopplysningGrunnlag, søkerAddress, Kjønn.MANN)
        personopplysningGrunnlag.personer.add(barn)

        assertEquals(Resultat.NEI, Vilkår.BOR_MED_SØKER.spesifikasjon.evaluer(Fakta(barn)).resultat)
    }

    @Test
    fun `Negativ vurdering - ikke mor som søker`() {
        val søkerAddress = GrVegadresse(2147483649, "11", "B", "H022",
                                        "St. Olavsvegen", "1232", "whatever", "4322")

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 5)
        val søker = genererPerson(PersonType.SØKER, personopplysningGrunnlag, søkerAddress, Kjønn.MANN)
        personopplysningGrunnlag.personer.add(søker)
        val barn = genererPerson(PersonType.BARN, personopplysningGrunnlag, søkerAddress)
        personopplysningGrunnlag.personer.add(barn)

        assertEquals(Resultat.NEI, Vilkår.BOR_MED_SØKER.spesifikasjon.evaluer(Fakta(barn)).resultat)
    }

    @Test
    fun `Negativ vurdering - søker har ukjentadresse`() {
        val ukjentbosted = GrUkjentBosted("Oslo")
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        val søker = genererPerson(PersonType.SØKER, personopplysningGrunnlag, ukjentbosted)
        personopplysningGrunnlag.personer.add(søker)
        val barn = genererPerson(PersonType.BARN, personopplysningGrunnlag, ukjentbosted)
        personopplysningGrunnlag.personer.add(barn)

        assertEquals(Resultat.NEI, Vilkår.BOR_MED_SØKER.spesifikasjon.evaluer(Fakta(barn)).resultat)
    }


    @Test
    fun `Sjekk at barn er ugift`() {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        var barn = genererPerson(PersonType.BARN, personopplysningGrunnlag)
        personopplysningGrunnlag.personer.add(barn)

        assertEquals(Resultat.JA, Vilkår.GIFT_PARTNERSKAP.spesifikasjon.evaluer(Fakta(barn)).resultat)
    }

    @Test
    fun `Negativ vurdering - barn er gift`() {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        var barn = genererPerson(PersonType.BARN, personopplysningGrunnlag, sivilstand = SIVILSTAND.GIFT)
        personopplysningGrunnlag.personer.add(barn)

        assertEquals(Resultat.NEI, Vilkår.GIFT_PARTNERSKAP.spesifikasjon.evaluer(Fakta(barn)).resultat)
    }

    @Test
    fun `Sjekk at søker er bosatt i norge`() {
        val ukjentbosted = GrUkjentBosted("Oslo")
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        var søker = genererPerson(PersonType.SØKER, personopplysningGrunnlag, ukjentbosted)
        personopplysningGrunnlag.personer.add(søker)

        assertEquals(Resultat.JA, Vilkår.BOSATT_I_RIKET.spesifikasjon.evaluer(Fakta(søker)).resultat)
    }

    @Test
    fun `Negativ vurdering - søker er ikke bosatt i norge`() {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        var søker = genererPerson(PersonType.SØKER, personopplysningGrunnlag, sivilstand = SIVILSTAND.GIFT)
        personopplysningGrunnlag.personer.add(søker)

        assertEquals(Resultat.NEI, Vilkår.BOSATT_I_RIKET.spesifikasjon.evaluer(Fakta(søker)).resultat)
    }

    @Test
    fun `Sjekk at mor er bosatt i norge`() {
        val ukjentbosted = GrUkjentBosted("Oslo")
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        var barn = genererPerson(PersonType.BARN, personopplysningGrunnlag, ukjentbosted)
        personopplysningGrunnlag.personer.add(barn)

        assertEquals(Resultat.JA, Vilkår.BOSATT_I_RIKET.spesifikasjon.evaluer(Fakta(barn)).resultat)
    }

    @Test
    fun `Negativ vurdering - mor er ikke bosatt i norge`() {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        var barn = genererPerson(PersonType.BARN, personopplysningGrunnlag, sivilstand = SIVILSTAND.GIFT)
        personopplysningGrunnlag.personer.add(barn)

        assertEquals(Resultat.NEI, Vilkår.BOSATT_I_RIKET.spesifikasjon.evaluer(Fakta(barn)).resultat)
    }

    @Test
    fun `Lovlig opphold - nordisk statsborger`() {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        var person = genererPerson(PersonType.BARN, personopplysningGrunnlag, sivilstand = SIVILSTAND.GIFT)
                .also { it.statsborgerskap =
                        listOf(
                                GrStatsborgerskap(gyldigPeriode = DatoIntervallEntitet(tom = null, fom = LocalDate.now().minusYears(1))
                                , landkode = "DNK", medlemskap = Medlemskap.NORDEN, person = it)
                        )
                }

        assertEquals(Resultat.JA, Vilkår.LOVLIG_OPPHOLD.spesifikasjon.evaluer(Fakta(person)).resultat)
    }

    @Test
    fun `Lovlig opphold - valider at alle gjeldende medlemskap blir returnert`() {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        val person = genererPerson(PersonType.BARN, personopplysningGrunnlag, sivilstand = SIVILSTAND.GIFT)
                .also { it.statsborgerskap =
                        listOf(
                                GrStatsborgerskap(gyldigPeriode = DatoIntervallEntitet(tom = null, fom = null)
                                , landkode = "DNK", medlemskap = Medlemskap.NORDEN, person = it),
                               GrStatsborgerskap(gyldigPeriode = DatoIntervallEntitet(tom = null, fom = LocalDate.now().minusYears(1))
                                , landkode = "DEU", medlemskap = Medlemskap.EØS, person = it),
                               GrStatsborgerskap(gyldigPeriode = DatoIntervallEntitet(tom = LocalDate.now().minusYears(2), fom = LocalDate.now().minusYears(2))
                                , landkode = "POL", medlemskap = Medlemskap.EØS, person = it)
                        )
                }

        val medlemskap = finnNåværendeMedlemskap(Fakta(person))

        assertEquals(2, medlemskap.size)
        assertEquals(Medlemskap.NORDEN, medlemskap[0])
        assertEquals(Medlemskap.EØS, medlemskap[1])
    }
}
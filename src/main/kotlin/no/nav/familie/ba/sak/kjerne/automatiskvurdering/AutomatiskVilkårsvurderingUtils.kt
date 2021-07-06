package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.erInnenfor
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import java.time.LocalDate

/*
//sommmerteam har laget for å vurdere saken automatisk basert på vilkår.
fun initierVilkårsvurdering(personopplysningGrunnlag: PersonopplysningGrunnlag,
                            nyeBarnsIdenter: List<String>, vilkårsvurdering: Vilkårsvurdering): List<PersonResultat> {
    //sommerteam antar at hvis mor har en registrert nåværende adresse er hun bosatt i riket
    val mor = personopplysningGrunnlag.søker
    val barna = personopplysningGrunnlag.barna.filter { nyeBarnsIdenter.contains(it.personIdent.ident) }
    val morsSisteBosted = if (mor.bostedsadresser.isEmpty()) null else mor.bostedsadresser.sisteAdresse()
    //Sommerteam hopper over sjekk om mor og barn har lovlig opphold

    val morsResultat = vurderMor(morsSisteBosted, vilkårsvurdering)
    val resultatListe = mutableListOf(morsResultat)
    barna.forEach { resultatListe.add(vurderBarn(it, morsSisteBosted, vilkårsvurdering)) }
    return personopplysningGrunnlag.personer.filter { it.type != PersonType.ANNENPART }.map { person ->
        val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering,
                                            personIdent = person.personIdent.ident)

        val samletSpesifikasjonForPerson = Vilkår.hentSamletSpesifikasjonForPerson(person.type)
        val faktaTilVilkårsvurdering = FaktaTilVilkårsvurdering(personForVurdering = person)
        val evalueringForVilkårsvurdering = samletSpesifikasjonForPerson.evaluer(faktaTilVilkårsvurdering)

        gdprService.oppdaterFødselshendelsePreLanseringMedVilkårsvurderingForPerson(behandlingId = vilkårsvurdering.behandling.id,
                                                                                    faktaTilVilkårsvurdering = faktaTilVilkårsvurdering,
                                                                                    evaluering = evalueringForVilkårsvurdering)

        personResultat.setSortedVilkårResultater(
                vilkårResultater(personResultat,
                                 person,
                                 faktaTilVilkårsvurdering,
                                 evalueringForVilkårsvurdering,
                                 fødselsdatoEldsteBarn)
        )
        personResultat
    }
}*/
/*
fun vurderMor(morsBosted: GrBostedsadresse?, vilkårsvurdering: Vilkårsvurdering): PersonResultat {
    return PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = mor)
    /*return PersonResultat(rolle = Rolle.MOR,
                          mutableListOf(Vilkårsresultat(
                                  VilkårType.MOR_ER_BOSTATT_I_RIKET,
                                  if (erMorBosattIRiket(morsBosted)) VilkårsVurdering.OPPFYLT else VilkårsVurdering.IKKE_OPPFYLT
                          )))*/
}*/
/*
fun vurderBarn(barn: Person, morsBosted: GrBostedsadresse?): PersonResultat {
    val vilkårAlder = Vilkårsresultat(VilkårType.BARN_ER_UNDER_18,
                                      (if (erPersonUnder18(barn.fødselsdato)) VilkårsVurdering.OPPFYLT else VilkårsVurdering.IKKE_OPPFYLT))
    val vilkårBorMedMor = Vilkårsresultat(VilkårType.BARN_BOR_MED_SØKER,
                                          (if (erBarnetBosattMedSøker(barn.bostedsadresser.sisteAdresse(),
                                                                      morsBosted)) VilkårsVurdering.OPPFYLT else VilkårsVurdering.IKKE_OPPFYLT))
    val vilkårSivilstand = Vilkårsresultat(VilkårType.BARN_ER_UGIFT,
                                           (if (erBarnetUgift(barn.sivilstander.sisteSivilstand())) VilkårsVurdering.OPPFYLT else VilkårsVurdering.IKKE_OPPFYLT))
    val vilkårBorIRiket = Vilkårsresultat(VilkårType.BARN_ER_BOSATT_I_RIKET,
                                          (if (erBarnBosattIRiket(barn.bostedsadresser.sisteAdresse())) VilkårsVurdering.OPPFYLT else VilkårsVurdering.IKKE_OPPFYLT))

    return PersonResultat(rolle = Rolle.BARN, mutableListOf(vilkårAlder,
                                                            vilkårBorMedMor,
                                                            vilkårSivilstand,
                                                            vilkårBorIRiket))
}*/

//antar at dersom perioden til sisteAdresse er null, så betyr det at personen fortsatt bor der
fun personErBosattIRiket(adresse: GrBostedsadresse?): Resultat {
    if ((adresse != null && adresse.periode == null) ||
        adresse?.periode?.erInnenfor(LocalDate.now()) == true) return Resultat.OPPFYLT
    return Resultat.IKKE_OPPFYLT
}

fun personErUnder18(fødselsdato: LocalDate): Resultat {
    if (fødselsdato.plusYears(18).isAfter(LocalDate.now())) return Resultat.OPPFYLT
    return Resultat.IKKE_OPPFYLT
}


fun barnetErBosattMedSøker(søkerAdresse: GrBostedsadresse?, barnAdresse: GrBostedsadresse?): Resultat {
    if (søkerAdresse == null) throw Feil("Finner ingen adresse på søker")
    if (barnAdresse == null) throw Feil("Finner ingen adresse på barn")
    val sammeSisteAdresse =
            GrBostedsadresse.erSammeAdresse(søkerAdresse, barnAdresse)

    //antar at dersom perioden til sisteAdresse er null, så betyr det at personen fortsatt bor der
    val barnBorPåSisteAdresse = barnAdresse.periode?.erInnenfor(LocalDate.now()) ?: true
    val søkerBorPåSisteAdresse = søkerAdresse.periode?.erInnenfor(LocalDate.now()) ?: true

    if (sammeSisteAdresse && barnBorPåSisteAdresse && søkerBorPåSisteAdresse) return Resultat.OPPFYLT
    return Resultat.IKKE_OPPFYLT
}

fun personErUgift(sivilstand: GrSivilstand?): Resultat {
    return when (sivilstand?.type ?: throw Feil("Finner ikke siviltilstand")) {
        SIVILSTAND.GIFT, SIVILSTAND.REGISTRERT_PARTNER -> Resultat.IKKE_OPPFYLT
        else -> Resultat.OPPFYLT
    }
}

fun personHarLovligOpphold(): Resultat {
    //alltid true i sommer-case
    return Resultat.OPPFYLT
}

data class under18RegelInput(
        val dagensDato: LocalDate,
        val fødselsdato: LocalDate,
)

data class borMedSøkerRegelInput(
        val søkerAdresse: GrBostedsadresse?,
        val barnAdresse: GrBostedsadresse?,
)

data class giftEllerPartnerskapRegelInput(
        val sivilstand: GrSivilstand?,
)

data class bosattIRiketRegelInput(
        val bostedsadresse: GrBostedsadresse?,
)

/*
fun erVilkårOppfylt(morOgBarnResultater: List<PersonResultat>): Boolean {
    val mor = morOgBarnResultater.filter { it.rolle == Rolle.MOR }
    val barn = morOgBarnResultater.filter { it.rolle == Rolle.BARN }
    return harMorOppfyltVilkår(mor.first()) && barn.all { harEtBarnOppfyltVilkår(it) }
}

fun harEtBarnOppfyltVilkår(BarnetsResultat: PersonResultat): Boolean {
    return (BarnetsResultat.vilkår.any { it.type == VilkårType.UNDER_18 && it.resultat == VilkårsVurdering.OPPFYLT } &&
            BarnetsResultat.vilkår.any { it.type == VilkårType.BOR_MED_SØKER && it.resultat == VilkårsVurdering.OPPFYLT } &&
            BarnetsResultat.vilkår.any { it.type == VilkårType.GIFT_PARTNERSKAP && it.resultat == VilkårsVurdering.OPPFYLT } &&
            BarnetsResultat.vilkår.any { it.type == VilkårType.BOSATT_I_RIKET && it.resultat == VilkårsVurdering.OPPFYLT })
}
fun harMorOppfyltVilkår(morsResultat: PersonResultat): Boolean {
    return morsResultat.vilkår.isNotEmpty() && morsResultat.vilkår.any { it.type == VilkårType.BOSTATT_I_RIKET && it.resultat == VilkårsVurdering.OPPFYLT }

}
*/
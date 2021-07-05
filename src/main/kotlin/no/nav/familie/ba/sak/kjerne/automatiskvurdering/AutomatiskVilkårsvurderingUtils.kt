package no.nav.familie.ba.sak.kjerne.automatiskvurdering

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
}

fun vurderMor(morsBosted: GrBostedsadresse?, vilkårsvurdering: Vilkårsvurdering): PersonResultat {
    return PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = mor)
    /*return PersonResultat(rolle = Rolle.MOR,
                          mutableListOf(Vilkårsresultat(
                                  VilkårType.MOR_ER_BOSTATT_I_RIKET,
                                  if (erMorBosattIRiket(morsBosted)) VilkårsVurdering.OPPFYLT else VilkårsVurdering.IKKE_OPPFYLT
                          )))*/
}

fun vurderBarn(barn: Person, morsBosted: GrBostedsadresse?): PersonResultat {
    val vilkårAlder = Vilkårsresultat(VilkårType.BARN_ER_UNDER_18,
                                      (if (erBarnetUnder18(barn.fødselsdato)) VilkårsVurdering.OPPFYLT else VilkårsVurdering.IKKE_OPPFYLT))
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
}


fun erMorBosattIRiket(morsSisteBosted: GrBostedsadresse?): Boolean {
    return ((morsSisteBosted != null && morsSisteBosted.periode == null) ||
            morsSisteBosted?.periode?.erInnenfor(LocalDate.now()) == true)
}

fun erBarnetUnder18(barnetsFødselsDataoer: LocalDate): Boolean {
    return barnetsFødselsDataoer.plusYears(18).isAfter(LocalDate.now())
}

fun erBarnetBosattMedSøker(barnetsAdresser: GrBostedsadresse?, morsSisteBosted: GrBostedsadresse?): Boolean {
    return GrBostedsadresse.erSammeAdresse(barnetsAdresser, morsSisteBosted) &&
           barnetsAdresser?.periode?.erInnenfor(LocalDate.now()) ?: false
}

fun erBarnetUgift(barnetsSivilstand: GrSivilstand?): Boolean {
    return (barnetsSivilstand?.type == SIVILSTAND.UGIFT || barnetsSivilstand?.type == SIVILSTAND.UOPPGITT)
}

fun erBarnBosattIRiket(barnetsAdresser: GrBostedsadresse?): Boolean {
    return (barnetsAdresser != null && barnetsAdresser.periode == null) ||
           barnetsAdresser?.periode?.erInnenfor(LocalDate.now()) == true
}

fun harMorOppfyltVilkår(morsResultat: PersonResultat): Boolean {
    return morsResultat.vilkår.isNotEmpty() && morsResultat.vilkår.any { it.type == VilkårType.MOR_ER_BOSTATT_I_RIKET && it.resultat == VilkårsVurdering.OPPFYLT }

}

fun harEtBarnOppfyltVilkår(BarnetsResultat: PersonResultat): Boolean {
    return (BarnetsResultat.vilkår.any { it.type == VilkårType.BARN_ER_UNDER_18 && it.resultat == VilkårsVurdering.OPPFYLT } &&
            BarnetsResultat.vilkår.any { it.type == VilkårType.BARN_BOR_MED_SØKER && it.resultat == VilkårsVurdering.OPPFYLT } &&
            BarnetsResultat.vilkår.any { it.type == VilkårType.BARN_ER_UGIFT && it.resultat == VilkårsVurdering.OPPFYLT } &&
            BarnetsResultat.vilkår.any { it.type == VilkårType.BARN_ER_BOSATT_I_RIKET && it.resultat == VilkårsVurdering.OPPFYLT })
}

fun erVilkårOppfylt(morOgBarnResultater: List<PersonResultat>): Boolean {
    val mor = morOgBarnResultater.filter { it.rolle == Rolle.MOR }
    val barn = morOgBarnResultater.filter { it.rolle == Rolle.BARN }
    return harMorOppfyltVilkår(mor.first()) && barn.all { harEtBarnOppfyltVilkår(it) }
}*/
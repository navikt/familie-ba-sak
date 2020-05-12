package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat

object VilkårDiff {

    fun oppdaterteBehandlingsresultater(aktivtResultat: BehandlingResultat,
                                        initiertResultat: BehandlingResultat): Pair<BehandlingResultat, BehandlingResultat> {

        // Identifiserer hvilke vilkår som skal legges til og hvilke som kan fjernes

        val personResultaterGammel = aktivtResultat.personResultater.toMutableSet()
        val personResultaterNy = mutableSetOf<PersonResultat>()
        initiertResultat.personResultater.forEach { personFraInitiert ->
            val personSomFinnes = personResultaterGammel.filter { it.personIdent == personFraInitiert.personIdent }.firstOrNull()
            if (personSomFinnes == null) {
                // Legg til ny person
                personResultaterNy.add(personFraInitiert)
            } else {
                // Fyll inn den initierte med person fra aktiv
                val personsVilkårResultaterGammel = personSomFinnes.vilkårResultater.toMutableSet()
                val personsVilkårResultaterNy = mutableSetOf<VilkårResultat>()
                personFraInitiert.vilkårResultater.forEach { initiertVilkårResultat ->
                    val vilkårResultaterFraGammel =
                            personSomFinnes.vilkårResultater.filter { it.vilkårType == initiertVilkårResultat.vilkårType }
                    if (vilkårResultaterFraGammel.isEmpty()) {
                        personsVilkårResultaterNy.add(initiertVilkårResultat)
                    } else {
                        personsVilkårResultaterNy.addAll(vilkårResultaterFraGammel)
                        personsVilkårResultaterGammel.removeAll(vilkårResultaterFraGammel)
                    }
                }
                personResultaterNy.add(
                        PersonResultat(
                                id = personSomFinnes.id,
                                personIdent = personSomFinnes.personIdent,
                                behandlingResultat = personSomFinnes.behandlingResultat,
                                vilkårResultater = personsVilkårResultaterNy))

                if (personsVilkårResultaterGammel.isEmpty()) {
                    personResultaterGammel.remove(personSomFinnes)
                } else {
                    personSomFinnes.vilkårResultater = personsVilkårResultaterGammel
                }
            }
        }
        aktivtResultat.personResultater = personResultaterGammel
        initiertResultat.personResultater = personResultaterNy

        return Pair(initiertResultat, aktivtResultat)
    }
}
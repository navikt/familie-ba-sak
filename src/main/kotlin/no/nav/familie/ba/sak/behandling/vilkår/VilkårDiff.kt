package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat

object VilkårDiff {

    fun oppdaterteBehandlingsresultater(behandling: Behandling,
                                        aktivtResultat: BehandlingResultat,
                                        initiertResultat: BehandlingResultat): Pair<BehandlingResultat, BehandlingResultat> {

        // Identifiserer hvilke vilkår som skal legges til og hvilke som kan fjernes
        val personResultaterGammel = aktivtResultat.personResultater.toMutableSet()
        val personResultaterNy = mutableSetOf<PersonResultat>()
        initiertResultat.personResultater.forEach { personFraInitiert ->
            val nyttPersonResultat = PersonResultat(behandlingResultat = initiertResultat,
                                                    personIdent = behandling.fagsak.personIdent.ident)
            val personenSomFinnes = personResultaterGammel.firstOrNull { it.personIdent == personFraInitiert.personIdent }

            if (personenSomFinnes == null) {
                // Legg til ny person
                nyttPersonResultat.vilkårResultater = personFraInitiert.vilkårResultater.map { it.kopierMedParent(nyttPersonResultat) }.toSet()
            } else {
                // Fyll inn den initierte med person fra aktiv
                val personsVilkårResultaterGammel = personenSomFinnes.vilkårResultater.toMutableSet()
                val personsVilkårResultaterNy = mutableSetOf<VilkårResultat>()
                personFraInitiert.vilkårResultater.forEach { initiertVilkårResultat ->
                    val vilkårResultaterFraGammel =
                            personenSomFinnes.vilkårResultater.filter { it.vilkårType == initiertVilkårResultat.vilkårType }
                    if (vilkårResultaterFraGammel.isEmpty()) {
                        personsVilkårResultaterNy.add(initiertVilkårResultat.kopierMedParent(nyttPersonResultat))
                    } else {
                        personsVilkårResultaterNy.addAll(vilkårResultaterFraGammel.map { it.kopierMedParent(nyttPersonResultat) })
                        personsVilkårResultaterGammel.removeAll(vilkårResultaterFraGammel)
                    }
                }
                nyttPersonResultat.vilkårResultater = personsVilkårResultaterNy

                if (personsVilkårResultaterGammel.isEmpty()) {
                    personResultaterGammel.remove(personenSomFinnes)
                } else {
                    personenSomFinnes.vilkårResultater = personsVilkårResultaterGammel
                }

            }
            personResultaterNy.add(nyttPersonResultat)
        }

        aktivtResultat.personResultater = personResultaterGammel
        initiertResultat.personResultater = personResultaterNy

        return Pair(initiertResultat, aktivtResultat)

    }

    fun lagFjernAdvarsel(personResultater: Set<PersonResultat>): String {
        var advarsel = "Følgende personer og vilkår fjernes:"
        personResultater.forEach {
            advarsel = advarsel.plus("\n${it.personIdent}:")
            it.vilkårResultater.forEach { vilkårResultat ->
                advarsel = advarsel.plus("\n   - ${vilkårResultat.vilkårType.spesifikasjon.beskrivelse}")
            }
            advarsel = advarsel.plus("\n")
        }

        return advarsel
    }
}

/*
// TODO: Årsak
personResultaterNy.add(
        PersonResultat(
                personIdent = personFraInitiert.personIdent,
                behandlingResultat = initiertResultat,
                vilkårResultater = personsVilkårResultaterNy))

                blir problematisk fordi vi prøver å lagre et personresultat med kobling til vilkårresultater som har kobling til noe som ikke finnes?
                Må lage personresultatet først og deretter legge til vilkårresultat som refererer til dette?
 */
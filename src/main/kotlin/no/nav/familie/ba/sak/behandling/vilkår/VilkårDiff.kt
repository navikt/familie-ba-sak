package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat

object VilkårDiff {

    fun oppdaterteBehandlingsresultater(behandling: Behandling,
                                        aktivtResultat: BehandlingResultat,
                                        initiertResultat: BehandlingResultat): Pair<BehandlingResultat, BehandlingResultat> {

        val nyttBehandlingResultat = BehandlingResultat(
                behandling = behandling,
                aktiv = true
        )

        // Identifiserer hvilke vilkår som skal legges til og hvilke som kan fjernes
        val personResultaterGammel = aktivtResultat.personResultater.toMutableSet()
        val personResultaterNy = mutableSetOf<PersonResultat>()
        initiertResultat.personResultater.forEach { personFraInitiert ->
            val nyttPersonResultat = PersonResultat(
                    behandlingResultat = nyttBehandlingResultat,
                    personIdent = behandling.fagsak.personIdent.ident
            )

            val personSomFinnes = personResultaterGammel.firstOrNull { it.personIdent == personFraInitiert.personIdent }
            if (personSomFinnes == null) {
                // Legg til ny person
                personResultaterNy.add(PersonResultat(
                        behandlingResultat = nyttBehandlingResultat,
                        vilkårResultater = personFraInitiert.vilkårResultater.map { it.kopier(nyttPersonResultat) }.toSet(),
                        personIdent = behandling.fagsak.personIdent.ident
                ))
            } else {
                // Fyll inn den initierte med person fra aktiv
                val personsVilkårResultaterGammel = personSomFinnes.vilkårResultater.toMutableSet()
                val personsVilkårResultaterNy = mutableSetOf<VilkårResultat>()
                personFraInitiert.vilkårResultater.forEach { initiertVilkårResultat ->
                    val vilkårResultaterFraGammel =
                            personSomFinnes.vilkårResultater.filter { it.vilkårType == initiertVilkårResultat.vilkårType }

                    if (vilkårResultaterFraGammel.isEmpty()) {
                        personsVilkårResultaterNy.add(initiertVilkårResultat.kopier(nyttPersonResultat))
                    } else {
                        personsVilkårResultaterNy.addAll(vilkårResultaterFraGammel.map {
                            it.kopier(nyttPersonResultat)
                        })
                        personsVilkårResultaterGammel.removeAll(vilkårResultaterFraGammel)
                    }
                }
                personResultaterNy.add(
                        PersonResultat(
                                personIdent = personFraInitiert.personIdent,
                                behandlingResultat = nyttBehandlingResultat,
                                vilkårResultater = personsVilkårResultaterNy))

                if (personsVilkårResultaterGammel.isEmpty()) {
                    personResultaterGammel.remove(personSomFinnes)
                } else {
                    personSomFinnes.vilkårResultater = personsVilkårResultaterGammel
                }
            }
        }

        aktivtResultat.personResultater = personResultaterGammel
        nyttBehandlingResultat.personResultater = personResultaterNy

        return Pair(nyttBehandlingResultat, aktivtResultat)
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
package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat

object VilkårsvurderingUtils {

    /**
     * 1 2 3
     * 2
     *
     * 1 3
     * 2
     */
    fun endreVurderingForPeriodePåVilkår(vilkårResultater: List<VilkårResultat>,
                                         restVilkårResultat: RestVilkårResultat) {
        val vilkårResultaterUtenEndretVilkår = vilkårResultater.filter { it.id != restVilkårResultat.id }

        vilkårResultaterUtenEndretVilkår.map {
            if ()
        }

    }

    /**
     * Dersom personer i initieltResultat har vurderte vilkår i aktivtResultat vil disse flyttes til initieltResultat
     * (altså vil tilsvarende vilkår overskrives i initieltResultat og slettes fra aktivtResultat).
     *
     * @param initieltBehandlingResultat - BehandlingResultat med vilkår basert på siste behandlignsgrunnlag. Skal bli neste aktive.
     * @param aktivtBehandlingResultat -  BehandlingResultat med vilkår basert på forrige behandlingsgrunnlag
     * @return oppdaterte versjoner av initieltResultat og aktivtResultat:
     * initieltResultat (neste aktivt) med vilkår som skal benyttes videre
     * aktivtResultat med hvilke vilkår som ikke skal benyttes videre
     */
    fun flyttResultaterTilInitielt(initieltBehandlingResultat: BehandlingResultat,
                                   aktivtBehandlingResultat: BehandlingResultat): Pair<BehandlingResultat, BehandlingResultat> {

        // Identifiserer hvilke vilkår som skal legges til og hvilke som kan fjernes
        val personResultaterAktivt = aktivtBehandlingResultat.personResultater.toMutableSet()
        val personResultaterOppdatert = mutableSetOf<PersonResultat>()
        initieltBehandlingResultat.personResultater.forEach { personFraInit ->
            val personTilOppdatert = PersonResultat(behandlingResultat = initieltBehandlingResultat,
                                                    personIdent = personFraInit.personIdent)
            val personenSomFinnes = personResultaterAktivt.firstOrNull { it.personIdent == personFraInit.personIdent }

            if (personenSomFinnes == null) {
                // Legg til ny person
                personTilOppdatert.vilkårResultater =
                        personFraInit.vilkårResultater.map { it.kopierMedParent(personTilOppdatert) }.toSet()
            } else {
                // Fyll inn den initierte med person fra aktiv
                val personsVilkårAktivt = personenSomFinnes.vilkårResultater.toMutableSet()
                val personsVilkårOppdatert = mutableSetOf<VilkårResultat>()
                personFraInit.vilkårResultater.forEach { vilkårFraInit ->
                    val vilkårSomFinnes = personenSomFinnes.vilkårResultater.filter { it.vilkårType == vilkårFraInit.vilkårType }
                    if (vilkårSomFinnes.isEmpty()) {
                        // Legg til nytt vilkår på person
                        personsVilkårOppdatert.add(vilkårFraInit.kopierMedParent(personTilOppdatert))
                    } else {
                        // Vilkår er vurdert på person - flytt fra aktivt og overskriv initierte
                        personsVilkårOppdatert.addAll(vilkårSomFinnes.map { it.kopierMedParent(personTilOppdatert) })
                        personsVilkårAktivt.removeAll(vilkårSomFinnes)
                    }
                }
                personTilOppdatert.vilkårResultater = personsVilkårOppdatert

                // Fjern person fra aktivt dersom alle vilkår er fjernet, ellers oppdater
                if (personsVilkårAktivt.isEmpty()) {
                    personResultaterAktivt.remove(personenSomFinnes)
                } else {
                    personenSomFinnes.vilkårResultater = personsVilkårAktivt
                }
            }
            personResultaterOppdatert.add(personTilOppdatert)
        }
        aktivtBehandlingResultat.personResultater = personResultaterAktivt
        initieltBehandlingResultat.personResultater = personResultaterOppdatert

        return Pair(initieltBehandlingResultat, aktivtBehandlingResultat)
    }

    fun lagFjernAdvarsel(personResultater: Set<PersonResultat>): String {
        var advarsel =
                "Du har gjort endringer i behandlingsgrunnlaget. Dersom du går videre vil vilkår for følgende personer fjernes:"
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
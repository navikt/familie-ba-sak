package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.common.*
import no.nav.nare.core.evaluations.Resultat
import java.time.LocalDate

object VilkårsvurderingUtils {

    /**
     * Funksjon som forsøker å slette en periode på et vilkår.
     * Dersom det kun finnes en periode eller perioden som skal slettes
     * lager en glippe. Isåfall nullstiller vi bare perioden.
     */
    fun muterPersonResultatDelete(personResultat: PersonResultat, vilkårResultatId: Long) {
        personResultat.slettEllerNullstill(vilkårResultatId = vilkårResultatId)
        fyllHullForVilkårResultater(personResultat)
    }

    /**
     * Funksjon som forsøker å legge til en periode på et vilkår.
     * Dersom det allerede finnes en uvurdet periode med samme vilkårstype
     * skal det kastes en feil.
     */
    fun muterPersonResultatPost(personResultat: PersonResultat, vilkårType: Vilkår) {
        val nyttVilkårResultat = VilkårResultat(personResultat = personResultat, vilkårType = vilkårType,
                                                resultat = Resultat.KANSKJE, begrunnelse = "")
        if (harUvurdertePerioder(personResultat, vilkårType)) {
            throw Feil("Det finnes allerede uvurderte vilkår av samme vilkårType")
        }
        personResultat.addVilkårResultat(vilkårResultat = nyttVilkårResultat)
    }

    /**
     * Funksjon som tar inn endret vilkår og muterer person resultatet til å få plass til den endrede perioden.
     */
    fun muterPersonResultatPut(personResultat: PersonResultat, restVilkårResultat: RestVilkårResultat) {
        personResultat.vilkårResultater.forEach {
            tilpassVilkårForEndretVilkår(
                    personResultat = personResultat,
                    vilkårResultat = it,
                    restVilkårResultat = restVilkårResultat
            )
        }

        fyllHullForVilkårResultater(personResultat)
    }

    fun harUvurdertePerioder(personResultat: PersonResultat, vilkårType: Vilkår): Boolean {
        val uvurdetePerioderMedSammeVilkårType = personResultat.vilkårResultater
                .filter { it.vilkårType == vilkårType }
                .find { it.resultat == Resultat.KANSKJE }
        return uvurdetePerioderMedSammeVilkårType != null
    }


    fun tilpassVilkårForEndretVilkår(personResultat: PersonResultat,
                                     vilkårResultat: VilkårResultat,
                                     restVilkårResultat: RestVilkårResultat) {
        val periodePåNyttVilkår: Periode = restVilkårResultat.toPeriode()

        if (vilkårResultat.id == restVilkårResultat.id) {
            vilkårResultat.periodeFom = restVilkårResultat.periodeFom
            vilkårResultat.periodeTom = restVilkårResultat.periodeTom
            vilkårResultat.begrunnelse = restVilkårResultat.begrunnelse
            vilkårResultat.resultat = restVilkårResultat.resultat
        } else if (vilkårResultat.vilkårType == restVilkårResultat.vilkårType) {
            val periode: Periode = vilkårResultat.toPeriode()

            var nyFom = periodePåNyttVilkår.tom;
            if (periodePåNyttVilkår.tom != LocalDate.MAX) {
                nyFom = periodePåNyttVilkår.tom.plusDays(1)
            }

            val nyTom = periodePåNyttVilkår.fom.minusDays(1)

            when {
                periodePåNyttVilkår.kanErstatte(periode) -> {
                    personResultat.removeVilkårResultat(vilkårResultatId = vilkårResultat.id)
                }
                periodePåNyttVilkår.kanSplitte(periode) -> {
                    personResultat.removeVilkårResultat(vilkårResultatId = vilkårResultat.id)
                    personResultat.addVilkårResultat(vilkårResultat.kopierMedNyPeriode(periode.fom, nyTom))
                    personResultat.addVilkårResultat(vilkårResultat.kopierMedNyPeriode(nyFom, periode.tom))
                }
                periodePåNyttVilkår.kanFlytteFom(periode) -> {
                    vilkårResultat.periodeFom = nyFom
                }
                periodePåNyttVilkår.kanFlytteTom(periode) -> {
                    vilkårResultat.periodeTom = nyTom
                }
            }
        }
    }

    fun fyllHullForVilkårResultater(personResultat: PersonResultat) {
        personResultat.sorterVilkårResultater()

        personResultat.vilkårResultater.forEach {
            personResultat.sorterVilkårResultater()

            val neste = personResultat.nextVilkårResultat(it)
            if (neste != null && it.vilkårType == neste.vilkårType) {
                when {
                    !it.erEtterfølgendePeriode(neste) -> {
                        val nyttVilkår =
                                lagUvurdertVilkårsresultat(
                                        personResultat = it.personResultat
                                                         ?: throw Feil(message = "Finner ikke personresultat ved opprettelse av uvurdert periode"),
                                        vilkårType = it.vilkårType,
                                        fom = it.toPeriode().tom.plusDays(
                                                1),
                                        tom = neste.toPeriode().fom.minusDays(
                                                1))
                        personResultat.addVilkårResultat(nyttVilkår)
                    }
                }
            }
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

    fun lagUvurdertVilkårsresultat(personResultat: PersonResultat,
                                   vilkårType: Vilkår,
                                   fom: LocalDate? = null,
                                   tom: LocalDate? = null): VilkårResultat {
        return VilkårResultat(personResultat = personResultat,
                              vilkårType = vilkårType,
                              resultat = Resultat.KANSKJE,
                              begrunnelse = "",
                              periodeFom = fom,
                              periodeTom = tom)
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
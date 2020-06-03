package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.common.*
import no.nav.nare.core.evaluations.Resultat
import java.time.LocalDate

object VilkårsvurderingUtils {

    /**
     * 1 2 3
     * 2
     *
     * 1 3
     * 2
     */
    fun endreVurderingForPeriodePåVilkår(vilkårResultater: List<VilkårResultat>,
                                         restVilkårResultat: RestVilkårResultat): List<VilkårResultat> {
        val vilkårResultaterUtenEndretVilkår = vilkårResultater.filter { it.id != restVilkårResultat.id }
        val endretVilkårResultat = vilkårResultater.find { it.id == restVilkårResultat.id }
                                   ?: throw Feil("Finner ikke innsendt restvilkår")

        val periodePåNyttVilkår: Periode = restVilkårResultat.toPeriode()

        val nyeVilkårResultater: MutableList<VilkårResultat> = mutableListOf<VilkårResultat>()

        vilkårResultaterUtenEndretVilkår.forEach foreach@{
            val periode: Periode = it.toPeriode()
            val nyFom = periodePåNyttVilkår.tom.plusDays(1)
            val nyTom = periodePåNyttVilkår.fom.minusDays(1)

            when {
                periodePåNyttVilkår.kanErstatte(periode) -> {
                    return@foreach
                }
                periodePåNyttVilkår.kanSplitte(periode) -> {
                    nyeVilkårResultater.add(it.kopierMedNyPeriode(periode.fom, nyTom))
                    nyeVilkårResultater.add(it.kopierMedNyPeriode(nyFom, periode.tom))
                }
                periodePåNyttVilkår.kanFlytteFom(periode) -> {
                    nyeVilkårResultater.add(it.kopierMedNyPeriode(nyFom, periode.tom))
                }
                periodePåNyttVilkår.kanFlytteTom(periode) -> {
                    nyeVilkårResultater.add(it.kopierMedNyPeriode(periode.fom, nyTom))
                }
                else -> {
                    nyeVilkårResultater.add(it)
                }
            }
        }
        nyeVilkårResultater.add(restVilkårResultat.mapNyVurdering(endretVilkårResultat))

        return sorterListe(nyeVilkårResultater)
                .fold(emptyList(), { acc: List<VilkårResultat>, vilkårResultat: VilkårResultat ->
                    val siste = acc.lastOrNull()

                    when {
                        siste == null -> {
                            listOf(vilkårResultat)
                        }
                        siste == vilkårResultat -> {
                            acc
                        }
                        siste.erEtterfølgendePeriode(vilkårResultat) -> {
                            acc + vilkårResultat
                        }
                        else -> {
                            val nyttVilkår = lagUvurdertVilkårsresultat(personResultat = vilkårResultat.personResultat,
                                                                        vilkårType = vilkårResultat.vilkårType,
                                                                        fom = siste.toPeriode().tom.plusDays(1),
                                                                        tom = vilkårResultat.toPeriode().fom.minusDays(1))
                            acc + nyttVilkår + vilkårResultat
                        }
                    }
                })
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

    fun sorterListe(liste: List<VilkårResultat>): List<VilkårResultat> {
        return liste.sortedBy { it.periodeFom }
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
package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.Period

object VilkårsvurderingResultatFlytter {

    /**
     * Dersom personer i initieltResultat har vurderte vilkår i aktivtResultat vil disse flyttes til initieltResultat
     * (altså vil tilsvarende vilkår overskrives i initieltResultat og slettes fra aktivtResultat).
     *
     * @param initiellVilkårsvurdering - Vilkårsvurdering med vilkår basert på siste behandlignsgrunnlag. Skal bli neste aktive.
     * @param aktivVilkårsvurdering -  Vilkårsvurdering med vilkår basert på forrige behandlingsgrunnlag
     * @param forrigeBehandlingVilkårsvurdering - Vilkårsvurdering fra forrige behandling (om den eksisterer).
     *                                            Brukes for å sjekke om utvidet-vilkåret skal kopieres med videre.
     * @param løpendeUnderkategori - Den løpende underkategorien for fagsaken. Brukes for å sjekke om utvidet-vilkåret skal kopieres med videre.
     * @return oppdaterte versjoner av initieltResultat og aktivtResultat:
     * initieltResultat (neste aktivt) med vilkår som skal benyttes videre
     * aktivtResultat med hvilke vilkår som ikke skal benyttes videre
     */
    fun flyttResultaterTilInitielt(
        initiellVilkårsvurdering: Vilkårsvurdering,
        aktivVilkårsvurdering: Vilkårsvurdering,
        løpendeUnderkategori: BehandlingUnderkategori? = null,
        personResultaterFraForrigeBehandling: Set<PersonResultat>?
    ): Pair<Vilkårsvurdering, Set<PersonResultat>> {
        // OBS!! MÅ jobbe på kopier av vilkårsvurderingen her for å ikke oppdatere databasen
        // Viktig at det er vår egen implementasjon av kopier som brukes, da kotlin sin copy-funksjon er en shallow copy
        val (oppdatert, aktivt) = finnOppdatertePersonResultater(
            initiellVilkårsvurdering = initiellVilkårsvurdering.kopier(),
            personResultaterFraForrigeBehandling = personResultaterFraForrigeBehandling,
            løpendeUnderkategori = løpendeUnderkategori,
            kopieringSkjerFraForrigeBehandling = initiellVilkårsvurdering.behandling.id != aktivVilkårsvurdering.behandling.id,
            personResultatAktiv = aktivVilkårsvurdering.kopier().personResultater
        )
        return Pair(initiellVilkårsvurdering.also { it.personResultater = oppdatert }, aktivt)
    }

    private fun finnOppdatertePersonResultater(
        initiellVilkårsvurdering: Vilkårsvurdering,
        personResultaterFraForrigeBehandling: Set<PersonResultat>? = null,
        løpendeUnderkategori: BehandlingUnderkategori? = null,
        kopieringSkjerFraForrigeBehandling: Boolean,
        personResultatAktiv: Set<PersonResultat>
    ): Pair<Set<PersonResultat>, Set<PersonResultat>> {
        // Identifiserer hvilke vilkår som skal legges til og hvilke som kan fjernes
        val personResultaterAktivt = personResultatAktiv.toMutableSet()
        val personResultaterOppdatert = mutableSetOf<PersonResultat>()
        initiellVilkårsvurdering.personResultater.forEach { personFraInit ->
            val personenSomFinnes = personResultaterAktivt.firstOrNull { it.aktør == personFraInit.aktør }

            val vilkårResultatet: Set<VilkårResultat>

            if (personenSomFinnes == null) {
                vilkårResultatet = personFraInit.vilkårResultater
            } else {
                // Fyll inn den initierte med person fra aktiv
                val vilkårSomSkalOppdateresPåEksisterendePerson = finnVilkårSomSkalOppdateresPåEksisterendePerson(
                    personFraInit = PersonFraInitRequest(
                        aktør = personFraInit.aktør,
                        vilkårResultater = personFraInit.vilkårResultater
                    ),
                    kopieringSkjerFraForrigeBehandling = kopieringSkjerFraForrigeBehandling,
                    løpendeUnderkategori = løpendeUnderkategori,
                    personenSomFinnesVilkårResultater = personenSomFinnes.vilkårResultater,
                    personResultaterFraForrigeBehandling = personResultaterFraForrigeBehandling
                )
                vilkårResultatet = vilkårSomSkalOppdateresPåEksisterendePerson.personsVilkårOppdatert

                // Mutering skjer herifrå og u
                // Fjern person fra aktivt dersom alle vilkår er fjernet, ellers oppdater
                if (vilkårSomSkalOppdateresPåEksisterendePerson.personsVilkårAktivt.isEmpty()) {
                    personResultaterAktivt.remove(personenSomFinnes)
                } else {
                    personenSomFinnes.setSortedVilkårResultater(vilkårSomSkalOppdateresPåEksisterendePerson.personsVilkårAktivt.toSet())
                }
            }
            personResultaterOppdatert.add(
                PersonResultat(
                    vilkårsvurdering = initiellVilkårsvurdering,
                    aktør = personFraInit.aktør
                ).also {
                    it.setSortedVilkårResultater(
                        vilkårResultatet.map { vilkårResultat ->
                            vilkårResultat.kopierMedParent(
                                it
                            )
                        }.toSet()
                    )
                }
            )
        }

        return Pair(personResultaterOppdatert, personResultaterAktivt)
    }

    private fun finnVilkårSomSkalOppdateresPåEksisterendePerson(
        personFraInit: PersonFraInitRequest,
        kopieringSkjerFraForrigeBehandling: Boolean,
        løpendeUnderkategori: BehandlingUnderkategori?,
        personenSomFinnesVilkårResultater: Set<VilkårResultat>,
        personResultaterFraForrigeBehandling: Set<PersonResultat>?
    ): OppdaterEksisterendePersonResponse {
        val personsVilkårAktivt = personenSomFinnesVilkårResultater.toMutableSet()
        val personsVilkårOppdatert = mutableSetOf<VilkårResultat>()
        personFraInit.vilkårResultater.forEach { vilkårFraInit ->
            val vilkårSomFinnes = personenSomFinnesVilkårResultater.filter { it.vilkårType == vilkårFraInit.vilkårType }

            val vilkårSomSkalKopieresOver = vilkårSomFinnes.filtrerVilkårÅKopiere(
                kopieringSkjerFraForrigeBehandling = kopieringSkjerFraForrigeBehandling
            )
            val vilkårSomSkalFjernesFraAktivt = vilkårSomFinnes - vilkårSomSkalKopieresOver
            personsVilkårAktivt.removeAll(vilkårSomSkalFjernesFraAktivt)

            if (vilkårSomSkalKopieresOver.isEmpty()) {
                // Legg til nytt vilkår på person
                personsVilkårOppdatert.add(vilkårFraInit)
            } else {
                /*  Vilkår er vurdert på person - flytt fra aktivt og overskriv initierte
                            ikke oppfylte eller ikke vurdert perioder skal ikke kopieres om minst en oppfylt
                            periode eksisterer. */

                personsVilkårOppdatert.addAll(vilkårSomSkalKopieresOver)
                personsVilkårAktivt.removeAll(vilkårSomSkalKopieresOver)
            }
        }

        if (forrigeBehandlingInneholdtUtvidetVilkåretEllerUnderkategorienErUtvidet(
                personsVilkårOppdatert,
                personResultaterFraForrigeBehandling,
                personFraInit,
                løpendeUnderkategori
            )
        ) {
            val utvidetVilkår =
                personenSomFinnesVilkårResultater.filter { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.UTVIDET_BARNETRYGD }
            personsVilkårOppdatert.addAll(utvidetVilkår.filtrerVilkårÅKopiere(kopieringSkjerFraForrigeBehandling = kopieringSkjerFraForrigeBehandling))
            personsVilkårAktivt.removeAll(utvidetVilkår)
        }

        return OppdaterEksisterendePersonResponse(
            personsVilkårAktivt = personsVilkårAktivt,
            personsVilkårOppdatert = personsVilkårOppdatert
        )
    }

    // Hvis forrige behandling inneholdt utvidet-vilkåret eller underkategorien er utvidet skal
    // utvidet-vilkåret kopieres med videre uansett nåværende underkategori
    private fun forrigeBehandlingInneholdtUtvidetVilkåretEllerUnderkategorienErUtvidet(
        personsVilkårOppdatert: MutableSet<VilkårResultat>,
        personResultaterFraForrigeBehandling: Set<PersonResultat>?,
        personFraInit: PersonFraInitRequest,
        løpendeUnderkategori: BehandlingUnderkategori?
    ) = personsVilkårOppdatert.none { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.UTVIDET_BARNETRYGD } &&
        (
            eksistererUtvidetVilkårPåForrigeBehandling(
                personResultaterFraForrigeBehandling,
                personFraInit
            ) || løpendeUnderkategori == BehandlingUnderkategori.UTVIDET
            )

    private fun eksistererUtvidetVilkårPåForrigeBehandling(
        personResultaterFraForrigeBehandling: Set<PersonResultat>?,
        personFraInit: PersonFraInitRequest
    ): Boolean = personResultaterFraForrigeBehandling
        ?.firstOrNull { it.aktør == personFraInit.aktør }
        ?.vilkårResultater
        ?.any {
            it.vilkårType == Vilkår.UTVIDET_BARNETRYGD &&
                it.resultat == Resultat.OPPFYLT &&
                // forrige behandling har minst et måned ubetalt utvidet barnetrygd
                it.differanseIPeriode().toTotalMonths() >= Period.ofMonths(1).months
        } ?: false

    private fun List<VilkårResultat>.filtrerVilkårÅKopiere(kopieringSkjerFraForrigeBehandling: Boolean): List<VilkårResultat> {
        return if (kopieringSkjerFraForrigeBehandling) {
            this.filter { it.resultat == Resultat.OPPFYLT }
        } else {
            this
        }
    }

    private data class PersonFraInitRequest(val aktør: Aktør, val vilkårResultater: Set<VilkårResultat>)

    private data class OppdaterEksisterendePersonResponse(
        val personsVilkårAktivt: Set<VilkårResultat>,
        val personsVilkårOppdatert: Set<VilkårResultat>
    )
}

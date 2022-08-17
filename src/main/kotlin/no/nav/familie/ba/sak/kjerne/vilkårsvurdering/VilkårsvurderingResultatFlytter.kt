package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
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
        forrigeBehandlingVilkårsvurdering: Vilkårsvurdering? = null,
        løpendeUnderkategori: BehandlingUnderkategori? = null
    ): Pair<Vilkårsvurdering, Vilkårsvurdering> {
        // OBS!! MÅ jobbe på kopier av vilkårsvurderingen her for å ikke oppdatere databasen
        // Viktig at det er vår egen implementasjon av kopier som brukes, da kotlin sin copy-funksjon er en shallow copy
        val initiellVilkårsvurderingKopi = initiellVilkårsvurdering.kopier()
        val aktivVilkårsvurderingKopi = aktivVilkårsvurdering.kopier()

        // Identifiserer hvilke vilkår som skal legges til og hvilke som kan fjernes
        val personResultaterAktivt = aktivVilkårsvurderingKopi.personResultater.toMutableSet()
        val personResultaterOppdatert = mutableSetOf<PersonResultat>()
        initiellVilkårsvurderingKopi.personResultater.forEach { personFraInit ->
            val personTilOppdatert = PersonResultat(
                vilkårsvurdering = initiellVilkårsvurderingKopi,
                aktør = personFraInit.aktør
            )
            val personenSomFinnes = personResultaterAktivt.firstOrNull { it.aktør == personFraInit.aktør }

            if (personenSomFinnes == null) {
                // Legg til ny person
                personTilOppdatert.setSortedVilkårResultater(
                    personFraInit.vilkårResultater.map { it.kopierMedParent(personTilOppdatert) }
                        .toSet()
                )
            } else {
                // Fyll inn den initierte med person fra aktiv
                oppdaterEksisterendePerson(
                    personenSomFinnes = personenSomFinnes,
                    personFraInit = personFraInit,
                    kopieringSkjerFraForrigeBehandling = initiellVilkårsvurderingKopi.behandling.id != aktivVilkårsvurderingKopi.behandling.id,
                    personTilOppdatert = personTilOppdatert,
                    forrigeBehandlingVilkårsvurdering = forrigeBehandlingVilkårsvurdering,
                    løpendeUnderkategori = løpendeUnderkategori,
                    personResultaterAktivt = personResultaterAktivt
                )
            }
            personResultaterOppdatert.add(personTilOppdatert)
        }

        aktivVilkårsvurderingKopi.personResultater = personResultaterAktivt
        initiellVilkårsvurderingKopi.personResultater = personResultaterOppdatert

        return Pair(initiellVilkårsvurderingKopi, aktivVilkårsvurderingKopi)
    }

    private fun oppdaterEksisterendePerson(
        personenSomFinnes: PersonResultat,
        personFraInit: PersonResultat,
        kopieringSkjerFraForrigeBehandling: Boolean,
        personTilOppdatert: PersonResultat,
        forrigeBehandlingVilkårsvurdering: Vilkårsvurdering?,
        løpendeUnderkategori: BehandlingUnderkategori?,
        personResultaterAktivt: MutableSet<PersonResultat>
    ) {
        // Fyll inn den initierte med person fra aktiv
        val personsVilkårAktivt = oppdaterEksisterendePerson(
            personenSomFinnes = personenSomFinnes,
            personFraInit = personFraInit,
            kopieringSkjerFraForrigeBehandling = kopieringSkjerFraForrigeBehandling,
            personTilOppdatert = personTilOppdatert,
            forrigeBehandlingVilkårsvurdering = forrigeBehandlingVilkårsvurdering,
            løpendeUnderkategori = løpendeUnderkategori
        )
        // Fjern person fra aktivt dersom alle vilkår er fjernet, ellers oppdater
        if (personsVilkårAktivt.isEmpty()) {
            personResultaterAktivt.remove(personenSomFinnes)
        } else {
            personenSomFinnes.setSortedVilkårResultater(personsVilkårAktivt.toSet())
        }
    }

    private fun oppdaterEksisterendePerson(
        personenSomFinnes: PersonResultat,
        personFraInit: PersonResultat,
        kopieringSkjerFraForrigeBehandling: Boolean,
        personTilOppdatert: PersonResultat,
        forrigeBehandlingVilkårsvurdering: Vilkårsvurdering?,
        løpendeUnderkategori: BehandlingUnderkategori?
    ): Set<VilkårResultat> {
        val personenSomFinnesVilkårResultater = personenSomFinnes.vilkårResultater
        val personsVilkårAktivt = personenSomFinnesVilkårResultater.toMutableSet()
        val personsVilkårOppdatert = mutableSetOf<VilkårResultat>()
        personFraInit.vilkårResultater.forEach { vilkårFraInit ->
            val vilkårSomFinnes =
                personenSomFinnesVilkårResultater.filter { it.vilkårType == vilkårFraInit.vilkårType }

            val vilkårSomSkalKopieresOver = vilkårSomFinnes.filtrerVilkårÅKopiere(
                kopieringSkjerFraForrigeBehandling = kopieringSkjerFraForrigeBehandling
            )
            val vilkårSomSkalFjernesFraAktivt = vilkårSomFinnes - vilkårSomSkalKopieresOver
            personsVilkårAktivt.removeAll(vilkårSomSkalFjernesFraAktivt)

            if (vilkårSomSkalKopieresOver.isEmpty()) {
                // Legg til nytt vilkår på person
                personsVilkårOppdatert.add(vilkårFraInit.kopierMedParent(personTilOppdatert))
            } else {
                /*  Vilkår er vurdert på person - flytt fra aktivt og overskriv initierte
                            ikke oppfylte eller ikke vurdert perioder skal ikke kopieres om minst en oppfylt
                            periode eksisterer. */

                personsVilkårOppdatert.addAll(
                    vilkårSomSkalKopieresOver.map { it.kopierMedParent(personTilOppdatert) }
                )
                personsVilkårAktivt.removeAll(vilkårSomSkalKopieresOver)
            }
        }
        val eksistererUtvidetVilkårPåForrigeBehandling =
            forrigeBehandlingVilkårsvurdering?.personResultater
                ?.firstOrNull { it.aktør == personFraInit.aktør }
                ?.vilkårResultater
                ?.any {
                    it.vilkårType == Vilkår.UTVIDET_BARNETRYGD &&
                        it.resultat == Resultat.OPPFYLT &&
                        // forrige behandling har minst et måned ubetalt utvidet barnetrygd
                        it.differanseIPeriode().toTotalMonths() >= Period.ofMonths(1).months
                } ?: false

        // Hvis forrige behandling inneholdt utvidet-vilkåret eller underkategorien er utvidet skal
        // utvidet-vilkåret kopieres med videre uansett nåværende underkategori
        if (personsVilkårOppdatert.none { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.UTVIDET_BARNETRYGD } &&
            (eksistererUtvidetVilkårPåForrigeBehandling || løpendeUnderkategori == BehandlingUnderkategori.UTVIDET)
        ) {
            val utvidetVilkår =
                personenSomFinnesVilkårResultater.filter { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.UTVIDET_BARNETRYGD }
            if (utvidetVilkår.isNotEmpty()) {
                personsVilkårOppdatert.addAll(
                    utvidetVilkår.filtrerVilkårÅKopiere(kopieringSkjerFraForrigeBehandling = kopieringSkjerFraForrigeBehandling)
                        .map { it.kopierMedParent(personTilOppdatert) }
                )
                personsVilkårAktivt.removeAll(utvidetVilkår)
            }
        }

        personTilOppdatert.setSortedVilkårResultater(personsVilkårOppdatert.toSet())
        return personsVilkårAktivt
    }

    private fun List<VilkårResultat>.filtrerVilkårÅKopiere(kopieringSkjerFraForrigeBehandling: Boolean): List<VilkårResultat> {
        return if (kopieringSkjerFraForrigeBehandling) {
            this.filter { it.resultat == Resultat.OPPFYLT }
        } else {
            this
        }
    }
}

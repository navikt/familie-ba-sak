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
     * @return oppdaterte mengder med personResultat for initieltResultat og aktivtResultat
     * mengden for initieltResultat (neste aktivt) med vilkår som skal benyttes videre
     * mengden for aktivtResultat med hvilke vilkår som ikke skal benyttes videre
     */
    fun flyttResultaterTilInitielt(
        initiellVilkårsvurdering: Vilkårsvurdering,
        aktivVilkårsvurdering: Vilkårsvurdering,
        løpendeUnderkategori: BehandlingUnderkategori? = null,
        personResultaterFraForrigeBehandling: Set<PersonResultat>? = null
    ): Pair<Vilkårsvurdering, Set<PersonResultat>> {
        // OBS!! MÅ jobbe på kopier av vilkårsvurderingen her for å ikke oppdatere databasen
        // Viktig at det er vår egen implementasjon av kopier som brukes, da kotlin sin copy-funksjon er en shallow copy
        val initiellVilkårsvurderingKopi = initiellVilkårsvurdering.kopier()
        val (oppdatert, aktivt, oppdateringer) = finnOppdatertePersonResultater(
            initiellVilkårsvurdering = initiellVilkårsvurderingKopi,
            personResultaterFraForrigeBehandling = personResultaterFraForrigeBehandling,
            løpendeUnderkategori = løpendeUnderkategori,
            kopieringSkjerFraForrigeBehandling = initiellVilkårsvurdering.behandling.id != aktivVilkårsvurdering.behandling.id,
            personResultatAktiv = aktivVilkårsvurdering.kopier().personResultater
        )
        oppdateringer.forEach { it.first.setSortedVilkårResultater(it.second) }
        return Pair(initiellVilkårsvurderingKopi.also { it.personResultater = oppdatert }, aktivt)
    }

    // Identifiserer hvilke vilkår som skal legges til og hvilke som kan fjernes
    private fun finnOppdatertePersonResultater(
        initiellVilkårsvurdering: Vilkårsvurdering,
        personResultaterFraForrigeBehandling: Set<PersonResultat>? = null,
        løpendeUnderkategori: BehandlingUnderkategori? = null,
        kopieringSkjerFraForrigeBehandling: Boolean,
        personResultatAktiv: Set<PersonResultat>
    ): InlineReturnobjekt {
        val finnesIkkeFraFør = initiellVilkårsvurdering.personResultater
            .filterNot { personFraInit -> personResultatAktiv.map { it.aktør }.contains(personFraInit.aktør) }
            .map { personFraInit ->
                tilPersonResultat(initiellVilkårsvurdering, personFraInit.aktør, personFraInit.vilkårResultater)
            }.toSet()

        val triple = initiellVilkårsvurdering.personResultater
            .filter { personFraInit -> personResultatAktiv.map { it.aktør }.contains(personFraInit.aktør) }
            .map { personFraInit ->
                val personenSomFinnes = personResultatAktiv.first { it.aktør == personFraInit.aktør }

                // Fyll inn den initierte med person fra aktiv
                val vilkårSomSkalOppdateresPåEksisterendePerson = finnVilkårSomSkalOppdateresPåEksisterendePerson(
                    kopieringSkjerFraForrigeBehandling = kopieringSkjerFraForrigeBehandling,
                    løpendeUnderkategori = løpendeUnderkategori,
                    personenSomFinnesVilkårResultater = personenSomFinnes.vilkårResultater,
                    personResultaterFraForrigeBehandling = personResultaterFraForrigeBehandling,
                    personFraInitVilkårResultater = personFraInit.vilkårResultater,
                    personFraInitAktør = personFraInit.aktør
                )
                Triple(
                    personenSomFinnes,
                    tilPersonResultat(
                        initiellVilkårsvurdering,
                        personFraInit.aktør,
                        vilkårSomSkalOppdateresPåEksisterendePerson.personsVilkårOppdatert
                    ),
                    vilkårSomSkalOppdateresPåEksisterendePerson.personsVilkårAktivt
                )
            }

        val personResultaterOppdatert = triple.map { it.second }.toSet()
        val personResultaterAktive =
            personResultatAktiv.toMutableSet() - triple.filter { it.third.isEmpty() }.map { it.first }.toSet()
        val oppdateringer = triple.filter { it.third.isNotEmpty() }.map { it.first to it.third }

        return InlineReturnobjekt(personResultaterOppdatert + finnesIkkeFraFør, personResultaterAktive, oppdateringer)
    }

    private fun tilPersonResultat(
        initiellVilkårsvurdering: Vilkårsvurdering,
        aktør: Aktør,
        vilkårResultatet: Set<VilkårResultat>
    ) =
        PersonResultat(vilkårsvurdering = initiellVilkårsvurdering, aktør = aktør)
            .also {
                it.setSortedVilkårResultater(
                    vilkårResultatet.map { vilkårResultat -> vilkårResultat.kopierMedParent(it) }
                        .toSet()
                )
            }

    private fun finnVilkårSomSkalOppdateresPåEksisterendePerson(
        kopieringSkjerFraForrigeBehandling: Boolean,
        løpendeUnderkategori: BehandlingUnderkategori?,
        personenSomFinnesVilkårResultater: Set<VilkårResultat>,
        personResultaterFraForrigeBehandling: Set<PersonResultat>?,
        personFraInitVilkårResultater: Set<VilkårResultat>,
        personFraInitAktør: Aktør
    ): AktivtOgOppdatertVilkårResultat {
        /*  Vilkår er vurdert på person - flytt fra aktivt og overskriv initierte
                    ikke oppfylte eller ikke vurdert perioder skal ikke kopieres om minst en oppfylt
                    periode eksisterer. */
        val (personsVilkårOppdatert, forrigeBehandlingInneholdtUtvidetVilkåretEllerUnderkategorienErUtvidet) = finnPersonsVilkårOppdatert(
            personFraInitVilkårResultater,
            personenSomFinnesVilkårResultater,
            kopieringSkjerFraForrigeBehandling,
            personResultaterFraForrigeBehandling,
            løpendeUnderkategori,
            personFraInitAktør
        )

        val personsVilkårAktivt = finnPersonsVilkårAktivt(
            personFraInitVilkårResultater,
            personenSomFinnesVilkårResultater,
            forrigeBehandlingInneholdtUtvidetVilkåretEllerUnderkategorienErUtvidet
        )

        return AktivtOgOppdatertVilkårResultat(
            personsVilkårAktivt = personsVilkårAktivt,
            personsVilkårOppdatert = personsVilkårOppdatert
        )
    }

    private fun finnPersonsVilkårOppdatert(
        personFraInitVilkårResultater: Set<VilkårResultat>,
        personenSomFinnesVilkårResultater: Set<VilkårResultat>,
        kopieringSkjerFraForrigeBehandling: Boolean,
        personResultaterFraForrigeBehandling: Set<PersonResultat>?,
        løpendeUnderkategori: BehandlingUnderkategori?,
        personFraInitAktør: Aktør
    ): Pair<MutableSet<VilkårResultat>, Boolean> {
        val personsVilkårOppdatert = personFraInitVilkårResultater.flatMap { vilkårFraInit ->
            personenSomFinnesVilkårResultater
                .filter { it.vilkårType == vilkårFraInit.vilkårType }
                .filtrerVilkårÅKopiere(kopieringSkjerFraForrigeBehandling = kopieringSkjerFraForrigeBehandling)
                .ifEmpty { setOf(vilkårFraInit) }
        }.toMutableSet()
        val forrigeBehandlingInneholdtUtvidetVilkåretEllerUnderkategorienErUtvidet =
            forrigeBehandlingInneholdtUtvidetVilkåretEllerUnderkategorienErUtvidet(
                personsVilkårOppdatert,
                personResultaterFraForrigeBehandling,
                løpendeUnderkategori,
                personFraInitAktør
            )

        if (forrigeBehandlingInneholdtUtvidetVilkåretEllerUnderkategorienErUtvidet
        ) {
            val utvidetVilkår = personenSomFinnesVilkårResultater
                .filter { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.UTVIDET_BARNETRYGD }
                .filtrerVilkårÅKopiere(kopieringSkjerFraForrigeBehandling = kopieringSkjerFraForrigeBehandling)
            personsVilkårOppdatert.addAll(utvidetVilkår)
        }
        return Pair(personsVilkårOppdatert, forrigeBehandlingInneholdtUtvidetVilkåretEllerUnderkategorienErUtvidet)
    }

    private fun finnPersonsVilkårAktivt(
        personFraInitVilkårResultater: Set<VilkårResultat>,
        personenSomFinnesVilkårResultater: Set<VilkårResultat>,
        forrigeBehandlingInneholdtUtvidetVilkåretEllerUnderkategorienErUtvidet: Boolean
    ): Set<VilkårResultat> = (
        personenSomFinnesVilkårResultater - personFraInitVilkårResultater
            .map { it.vilkårType }
            .flatMap { vilkårTypeFraInit ->
                personenSomFinnesVilkårResultater.filter { it.vilkårType == vilkårTypeFraInit }
            }
        )
        .filterNot {
            forrigeBehandlingInneholdtUtvidetVilkåretEllerUnderkategorienErUtvidet &&
                personenSomFinnesVilkårResultater.filter { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.UTVIDET_BARNETRYGD }
                    .contains(it)
        }.toSet()

    // Hvis forrige behandling inneholdt utvidet-vilkåret eller underkategorien er utvidet skal
    // utvidet-vilkåret kopieres med videre uansett nåværende underkategori
    private fun forrigeBehandlingInneholdtUtvidetVilkåretEllerUnderkategorienErUtvidet(
        personsVilkårOppdatert: MutableSet<VilkårResultat>,
        personResultaterFraForrigeBehandling: Set<PersonResultat>?,
        løpendeUnderkategori: BehandlingUnderkategori?,
        personFraInitAktør: Aktør
    ) = personsVilkårOppdatert.none { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.UTVIDET_BARNETRYGD } &&
        (
            eksistererUtvidetVilkårPåForrigeBehandling(
                personResultaterFraForrigeBehandling,
                personFraInitAktør
            ) || løpendeUnderkategori == BehandlingUnderkategori.UTVIDET
            )

    private fun eksistererUtvidetVilkårPåForrigeBehandling(
        personResultaterFraForrigeBehandling: Set<PersonResultat>?,
        personFraInitAktør: Aktør
    ): Boolean = personResultaterFraForrigeBehandling
        ?.firstOrNull { it.aktør == personFraInitAktør }
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

    private data class AktivtOgOppdatertVilkårResultat(
        val personsVilkårAktivt: Set<VilkårResultat>,
        val personsVilkårOppdatert: Set<VilkårResultat>
    )

    private data class InlineReturnobjekt(
        val personsVilkårOppdatert: Set<PersonResultat>,
        val personsVilkårAktivt: Set<PersonResultat>,
        val oppdateringer: List<Pair<PersonResultat, Set<VilkårResultat>>>
    )
}

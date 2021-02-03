package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.restDomene.RestVedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelse.Companion.finnVilkårFor
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.nare.Resultat
import java.util.*

object VilkårsvurderingUtils {

    /**
     * Funksjon som forsøker å slette en periode på et vilkår.
     * Dersom det kun finnes en periode eller perioden som skal slettes
     * lager en glippe. Isåfall nullstiller vi bare perioden.
     */
    fun muterPersonResultatDelete(personResultat: PersonResultat, vilkårResultatId: Long) {
        personResultat.slettEllerNullstill(vilkårResultatId = vilkårResultatId)
    }

    /**
     * Funksjon som forsøker å legge til en periode på et vilkår.
     * Dersom det allerede finnes en uvurdet periode med samme vilkårstype
     * skal det kastes en feil.
     */
    fun muterPersonResultatPost(personResultat: PersonResultat, vilkårType: Vilkår) {
        val nyttVilkårResultat = VilkårResultat(personResultat = personResultat,
                                                vilkårType = vilkårType,
                                                resultat = Resultat.IKKE_VURDERT,
                                                begrunnelse = "",
                                                behandlingId = personResultat.vilkårsvurdering.behandling.id,
                                                regelInput = null,
                                                regelOutput = null)
        if (harUvurdertePerioder(personResultat, vilkårType)) {
            throw FunksjonellFeil(melding = "Det finnes allerede uvurderte vilkår av samme vilkårType",
                                  frontendFeilmelding = "Du må ferdigstille vilkårsvurderingen på en periode som allerede er påbegynt, før du kan legge til en ny periode")
        }
        personResultat.addVilkårResultat(vilkårResultat = nyttVilkårResultat)
    }

    /**
     * Funksjon som tar inn endret vilkår og muterer person resultatet til å få plass til den endrede perioden.
     */
    fun muterPersonResultatPut(personResultat: PersonResultat, restVilkårResultat: RestVilkårResultat) {
        val kopiAvVilkårResultater = personResultat.vilkårResultater.toList()

        kopiAvVilkårResultater.forEach {
            tilpassVilkårForEndretVilkår(
                    personResultat = personResultat,
                    vilkårResultat = it,
                    restVilkårResultat = restVilkårResultat
            )
        }
    }

    fun harUvurdertePerioder(personResultat: PersonResultat, vilkårType: Vilkår): Boolean {
        val uvurdetePerioderMedSammeVilkårType = personResultat.vilkårResultater
                .filter { it.vilkårType == vilkårType }
                .find { it.resultat == Resultat.IKKE_VURDERT }
        return uvurdetePerioderMedSammeVilkårType != null
    }


    fun tilpassVilkårForEndretVilkår(personResultat: PersonResultat,
                                     vilkårResultat: VilkårResultat,
                                     restVilkårResultat: RestVilkårResultat) {
        val periodePåNyttVilkår: Periode = restVilkårResultat.toPeriode()

        if (vilkårResultat.id == restVilkårResultat.id) {
            vilkårResultat.oppdater(restVilkårResultat)
        } else if (vilkårResultat.vilkårType == restVilkårResultat.vilkårType) {
            val periode: Periode = vilkårResultat.toPeriode()

            var nyFom = periodePåNyttVilkår.tom
            if (periodePåNyttVilkår.tom != TIDENES_ENDE) {
                nyFom = periodePåNyttVilkår.tom.plusDays(1)
            }

            val nyTom = periodePåNyttVilkår.fom.minusDays(1)

            when {
                periodePåNyttVilkår.kanErstatte(periode) -> {
                    personResultat.removeVilkårResultat(vilkårResultatId = vilkårResultat.id)
                }
                periodePåNyttVilkår.kanSplitte(periode) -> {
                    personResultat.removeVilkårResultat(vilkårResultatId = vilkårResultat.id)
                    personResultat.addVilkårResultat(
                            vilkårResultat.kopierMedNyPeriode(fom = periode.fom,
                                                              tom = nyTom,
                                                              behandlingId = personResultat.vilkårsvurdering.behandling.id))
                    personResultat.addVilkårResultat(
                            vilkårResultat.kopierMedNyPeriode(fom = nyFom,
                                                              tom = periode.tom,
                                                              behandlingId = personResultat.vilkårsvurdering.behandling.id))
                }
                periodePåNyttVilkår.kanFlytteFom(periode) -> {
                    vilkårResultat.periodeFom = nyFom
                    vilkårResultat.erAutomatiskVurdert = false
                    vilkårResultat.oppdaterPekerTilBehandling()
                }
                periodePåNyttVilkår.kanFlytteTom(periode) -> {
                    vilkårResultat.periodeTom = nyTom
                    vilkårResultat.erAutomatiskVurdert = false
                    vilkårResultat.oppdaterPekerTilBehandling()
                }
            }
        }
    }

    /**
     * Dersom personer i initieltResultat har vurderte vilkår i aktivtResultat vil disse flyttes til initieltResultat
     * (altså vil tilsvarende vilkår overskrives i initieltResultat og slettes fra aktivtResultat).
     *
     * @param initiellVilkårsvurdering - Vilkårsvurdering med vilkår basert på siste behandlignsgrunnlag. Skal bli neste aktive.
     * @param aktivVilkårsvurdering -  Vilkårsvurdering med vilkår basert på forrige behandlingsgrunnlag
     * @return oppdaterte versjoner av initieltResultat og aktivtResultat:
     * initieltResultat (neste aktivt) med vilkår som skal benyttes videre
     * aktivtResultat med hvilke vilkår som ikke skal benyttes videre
     */
    fun flyttResultaterTilInitielt(initiellVilkårsvurdering: Vilkårsvurdering,
                                   aktivVilkårsvurdering: Vilkårsvurdering): Pair<Vilkårsvurdering, Vilkårsvurdering> {

        // Identifiserer hvilke vilkår som skal legges til og hvilke som kan fjernes
        val personResultaterAktivt = aktivVilkårsvurdering.personResultater.toMutableSet()
        val personResultaterOppdatert = mutableSetOf<PersonResultat>()
        initiellVilkårsvurdering.personResultater.forEach { personFraInit ->
            val personTilOppdatert = PersonResultat(vilkårsvurdering = initiellVilkårsvurdering,
                                                    personIdent = personFraInit.personIdent)
            val personenSomFinnes = personResultaterAktivt.firstOrNull { it.personIdent == personFraInit.personIdent }

            if (personenSomFinnes == null) {
                // Legg til ny person
                personTilOppdatert.setVilkårResultater(
                        personFraInit.vilkårResultater.map { it.kopierMedParent(personTilOppdatert) }
                                .toSet())
            } else {
                // Fyll inn den initierte med person fra aktiv
                val personsVilkårAktivt = personenSomFinnes.vilkårResultater.toMutableSet()
                val personsVilkårOppdatert = mutableSetOf<VilkårResultat>()
                personFraInit.vilkårResultater.forEach { vilkårFraInit ->
                    val vilkårSomFinnes = personenSomFinnes.vilkårResultater.filter { it.resultat == Resultat.OPPFYLT }
                            .filter { it.vilkårType == vilkårFraInit.vilkårType }
                    if (vilkårSomFinnes.isEmpty()) {
                        // Legg til nytt vilkår på person
                        personsVilkårOppdatert.add(vilkårFraInit.kopierMedParent(personTilOppdatert))
                    } else {
                        // Vilkår er vurdert på person - flytt fra aktivt og overskriv initierte
                        personsVilkårOppdatert.addAll(vilkårSomFinnes.map { it.kopierMedParent(personTilOppdatert) })
                        personsVilkårAktivt.removeAll(vilkårSomFinnes)
                    }
                }
                personTilOppdatert.setVilkårResultater(personsVilkårOppdatert.toSet())

                // Fjern person fra aktivt dersom alle vilkår er fjernet, ellers oppdater
                if (personsVilkårAktivt.isEmpty()) {
                    personResultaterAktivt.remove(personenSomFinnes)
                } else {
                    personenSomFinnes.setVilkårResultater(personsVilkårAktivt.toSet())
                }
            }
            personResultaterOppdatert.add(personTilOppdatert)
        }
        aktivVilkårsvurdering.personResultater = personResultaterAktivt
        initiellVilkårsvurdering.personResultater = personResultaterOppdatert

        return Pair(initiellVilkårsvurdering, aktivVilkårsvurdering)
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

    fun hentVilkårsbegrunnelser(): Map<VedtakBegrunnelseType, List<RestVedtakBegrunnelse>> = VedtakBegrunnelse.values()
            .groupBy { it.vedtakBegrunnelseType }
            .mapValues { begrunnelseGruppe ->
                begrunnelseGruppe.value
                        .filter { !VedtakBegrunnelseSerivce.ikkeStøttet.contains(it) }
                        .map { vedtakBegrunnelse ->
                            RestVedtakBegrunnelse(id = vedtakBegrunnelse,
                                                  navn = vedtakBegrunnelse.tittel,
                                                  vilkår = vedtakBegrunnelse.finnVilkårFor()
                            )
                        }
            }
}
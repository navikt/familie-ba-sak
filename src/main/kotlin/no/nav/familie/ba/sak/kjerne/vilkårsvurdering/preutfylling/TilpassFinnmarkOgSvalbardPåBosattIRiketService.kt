package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresser
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.erUkraina
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class TilpassFinnmarkOgSvalbardPåBosattIRiketService(
    private val pdlRestKlient: SystemOnlyPdlRestKlient,
) {
    fun tilpassFinnmarkOgSvalbard(
        vilkårsvurdering: Vilkårsvurdering,
        identerVilkårSkalPreutfyllesFor: List<String>? = null,
        cutOffFomDato: LocalDate? = null,
    ) {
        val behandling = vilkårsvurdering.behandling
        val identer =
            vilkårsvurdering
                .personResultater
                .map { it.aktør.aktivFødselsnummer() }
                .filter { identerVilkårSkalPreutfyllesFor?.contains(it) ?: true }

        val adresser = pdlRestKlient.hentAdresserForPersoner(identer)

        vilkårsvurdering
            .personResultater
            .filter { it.aktør.aktivFødselsnummer() in identer }
            .filterNot {
                val erUkrainskStatsborger = hentErUkrainskStatsborger(it.aktør)
                erUkrainskStatsborger && !behandling.erFinnmarksEllerSvalbardtillegg()
            }.forEach { personResultat ->

                val adresserForPerson = Adresser.opprettFra(adresser[personResultat.aktør.aktivFødselsnummer()])

                val nyeBosattIRiketVilkårResultater =
                    tilpassFinnmarkOgSvalbardtilleggPåBosattIRiketVilkårResultat(
                        personResultat = personResultat,
                        adresserForPerson = adresserForPerson,
                        behandling = behandling,
                        cutOffFomDato = cutOffFomDato!!,
                    )

                if (nyeBosattIRiketVilkårResultater.isNotEmpty()) {
                    personResultat.vilkårResultater.removeIf { it.vilkårType == BOSATT_I_RIKET }
                    personResultat.vilkårResultater.addAll(nyeBosattIRiketVilkårResultater)
                }
            }
    }

    fun tilpassFinnmarkOgSvalbardtilleggPåBosattIRiketVilkårResultat(
        personResultat: PersonResultat,
        adresserForPerson: Adresser,
        behandling: Behandling,
        cutOffFomDato: LocalDate,
    ): List<VilkårResultat> {
        val erBostedsadresseIFinnmarkEllerNordTromsTidslinje = lagErBostedsadresseIFinnmarkEllerNordTromsTidslinje(adresserForPerson, personResultat)
        val erDeltBostedIFinnmarkEllerNordTromsTidslinje = lagErDeltBostedIFinnmarkEllerNordTromsTidslinje(adresserForPerson, personResultat)
        val erOppholdsadressePåSvalbardTidslinje = lagErOppholdsadresserPåSvalbardTidslinje(adresserForPerson, personResultat)

        validerKombinasjonerAvAdresserForFinnmarksOgSvalbardtileggbehandlinger(
            behandling = behandling,
            erDeltBostedIFinnmarkEllerNordTromsTidslinje = erDeltBostedIFinnmarkEllerNordTromsTidslinje,
            erOppholdsadressePåSvalbardTidslinje = erOppholdsadressePåSvalbardTidslinje,
        )

        val erBosattIFinnmarkEllerNordTromsTidslinje =
            erBostedsadresseIFinnmarkEllerNordTromsTidslinje.kombinerMed(erDeltBostedIFinnmarkEllerNordTromsTidslinje) { erBostedsadresseIFinnmarkEllerNordTroms, erDeltBostedIFinnmarkEllerNordTroms ->
                erBostedsadresseIFinnmarkEllerNordTroms == true || erDeltBostedIFinnmarkEllerNordTroms == true
            }

        val finnmarkEllerSvalbardtilleggTidslinje =
            erBosattIFinnmarkEllerNordTromsTidslinje
                .kombinerMed(erOppholdsadressePåSvalbardTidslinje) { erBosattIFinnmarkEllerNordTroms, erOppholdsadressePåSvalbard ->
                    when {
                        erOppholdsadressePåSvalbard == true -> listOf(BOSATT_PÅ_SVALBARD)
                        erBosattIFinnmarkEllerNordTroms == true -> listOf(BOSATT_I_FINNMARK_NORD_TROMS)
                        else -> emptyList()
                    }
                }.beskjærFraOgMed(cutOffFomDato)

        val eksisterendeBosattIRiketVilkårResultater = personResultat.vilkårResultater.filter { it.vilkårType == BOSATT_I_RIKET }

        return eksisterendeBosattIRiketVilkårResultater
            .tilTidslinje()
            .kombinerMed(finnmarkEllerSvalbardtilleggTidslinje) { eksisterendeVilkårResultat, finnmarkEllerSvalbardtillegg ->
                if (eksisterendeVilkårResultat == null) {
                    return@kombinerMed null
                }
                val utdypendeVilkårsvurderingUtenFinnmarkOgSvalbardtillegg = eksisterendeVilkårResultat.utdypendeVilkårsvurderinger.filter { it != BOSATT_I_FINNMARK_NORD_TROMS && it != BOSATT_PÅ_SVALBARD }
                if (finnmarkEllerSvalbardtillegg == null) {
                    return@kombinerMed eksisterendeVilkårResultat.copy(utdypendeVilkårsvurderinger = utdypendeVilkårsvurderingUtenFinnmarkOgSvalbardtillegg)
                }
                val utdypendeVilkårsvurderingMedFinnmarkOgSvalbardtillegg = utdypendeVilkårsvurderingUtenFinnmarkOgSvalbardtillegg.plus(finnmarkEllerSvalbardtillegg)
                eksisterendeVilkårResultat.copy(utdypendeVilkårsvurderinger = utdypendeVilkårsvurderingMedFinnmarkOgSvalbardtillegg, begrunnelse = PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
            }.tilPerioderIkkeNull()
            .map {
                it.verdi.copy(
                    periodeFom = it.fom,
                    periodeTom = it.tom,
                )
            }
    }

    private fun hentErUkrainskStatsborger(aktør: Aktør): Boolean {
        val statsborgerskap = pdlRestKlient.hentStatsborgerskap(aktør)
        return statsborgerskap.any { it.erUkraina() }
    }
}

fun validerKombinasjonerAvAdresserForFinnmarksOgSvalbardtileggbehandlinger(
    behandling: Behandling,
    erDeltBostedIFinnmarkEllerNordTromsTidslinje: Tidslinje<Boolean>,
    erOppholdsadressePåSvalbardTidslinje: Tidslinje<Boolean>,
) {
    val harDeltBostedIFinnmarkOgOppholdsadressePåSvalbardISammePeriode =
        erDeltBostedIFinnmarkEllerNordTromsTidslinje
            .kombinerMed(erOppholdsadressePåSvalbardTidslinje) { erDeltBostedIFinnmarkEllerNordTroms, erOppholdsadressePåSvalbard ->
                erDeltBostedIFinnmarkEllerNordTroms == true && erOppholdsadressePåSvalbard == true
            }.tilPerioder()
            .any { it.verdi == true }

    if (harDeltBostedIFinnmarkOgOppholdsadressePåSvalbardISammePeriode) {
        throw Feil("Kan ikke behandle ${behandling.opprettetÅrsak.visningsnavn} automatisk, fordi barn har delt bosted i Finnmark/Nord-Troms og oppholdsadresse på Svalbard")
    }
}

package no.nav.familie.ba.sak.kjerne.autovedtak

import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresser
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.springframework.stereotype.Service
import java.time.LocalDate

private val FINNMARK_OG_SVALBARD_MERKING_CUT_OFF_FOM_DATO = LocalDate.of(2025, 9, 1)

@Service
class OppdaterUtdypendeVilkårForBosattIRiketMedFinnmarkOgSvalbardService(
    private val persongrunnlagService: PersongrunnlagService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) {
    fun oppdaterUtdypendeVilkårForBosattIRiketMedFinnmarkOgSvalbard(
        vilkårsvurdering: Vilkårsvurdering,
        identerVilkårSkalPreutfyllesFor: List<String>? = null,
    ) {
        val behandling = vilkårsvurdering.behandling
        val identer =
            vilkårsvurdering
                .personResultater
                .map { it.aktør.aktivFødselsnummer() }
                .filter { identerVilkårSkalPreutfyllesFor?.contains(it) ?: true }

        vilkårsvurdering
            .personResultater
            .filter { it.aktør.aktivFødselsnummer() in identer }
            .forEach { personResultat ->
                val personOpplysningsgrunnlag = persongrunnlagService.hentAktivThrows(behandling.id)
                val person = personOpplysningsgrunnlag.personer.find { it.aktør == personResultat.aktør } ?: throw Feil("Aktør ${personResultat.aktør.aktørId} har personresultat men ikke persongrunnlag")

                val nyeBosattIRiketVilkårResultater =
                    oppdaterUtdypendeVilkårForEttEnkeltVilkårResultat(
                        personResultat = personResultat,
                        adresserForPerson = Adresser.opprettFra(person),
                        behandling = behandling,
                    )

                if (nyeBosattIRiketVilkårResultater.isNotEmpty()) {
                    personResultat.vilkårResultater.removeIf { it.vilkårType == BOSATT_I_RIKET }
                    personResultat.vilkårResultater.addAll(nyeBosattIRiketVilkårResultater)
                }
            }
    }

    private fun oppdaterUtdypendeVilkårForEttEnkeltVilkårResultat(
        personResultat: PersonResultat,
        adresserForPerson: Adresser,
        behandling: Behandling,
    ): List<VilkårResultat> {
        val erBostedsadresseIFinnmarkEllerNordTromsTidslinje = adresserForPerson.lagErBostedsadresseIFinnmarkEllerNordTromsTidslinje(personResultat)
        val erDeltBostedIFinnmarkEllerNordTromsTidslinje = adresserForPerson.lagErDeltBostedIFinnmarkEllerNordTromsTidslinje(personResultat.aktør.aktørId)
        val erOppholdsadressePåSvalbardTidslinje = adresserForPerson.lagErOppholdsadresserPåSvalbardTidslinje(personResultat)
        val andelForAktør = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, personResultat.aktør)

        validerKombinasjonerAvAdresserForFinnmarksOgSvalbardtileggbehandlinger(
            behandling = behandling,
            erDeltBostedIFinnmarkEllerNordTromsTidslinje = erDeltBostedIFinnmarkEllerNordTromsTidslinje,
            erOppholdsadressePåSvalbardTidslinje = erOppholdsadressePåSvalbardTidslinje,
            andelForAktør = andelForAktør,
        )

        val erBosattIFinnmarkEllerNordTromsTidslinje =
            erBostedsadresseIFinnmarkEllerNordTromsTidslinje.kombinerMed(erDeltBostedIFinnmarkEllerNordTromsTidslinje) { erBostedsadresseIFinnmarkEllerNordTroms, erDeltBostedIFinnmarkEllerNordTroms ->
                erBostedsadresseIFinnmarkEllerNordTroms == true || erDeltBostedIFinnmarkEllerNordTroms == true
            }

        val finnmarkEllerSvalbardmerkingTidslinje =
            erBosattIFinnmarkEllerNordTromsTidslinje
                .kombinerMed(erOppholdsadressePåSvalbardTidslinje) { erBosattIFinnmarkEllerNordTroms, erOppholdsadressePåSvalbard ->
                    when {
                        erOppholdsadressePåSvalbard == true -> listOf(BOSATT_PÅ_SVALBARD)
                        erBosattIFinnmarkEllerNordTroms == true -> listOf(BOSATT_I_FINNMARK_NORD_TROMS)
                        else -> emptyList()
                    }
                }.beskjærFraOgMed(FINNMARK_OG_SVALBARD_MERKING_CUT_OFF_FOM_DATO)

        val eksisterendeBosattIRiketVilkårResultater = personResultat.vilkårResultater.filter { it.vilkårType == BOSATT_I_RIKET }

        return eksisterendeBosattIRiketVilkårResultater
            .tilTidslinje()
            .kombinerMed(finnmarkEllerSvalbardmerkingTidslinje) { eksisterendeVilkårResultat, finnmarkEllerSvalbardmerking ->
                if (eksisterendeVilkårResultat == null) {
                    return@kombinerMed null
                }

                val gjeldendeFinnmarkEllerSvalbardMarkeringer = finnmarkEllerSvalbardmerking.orEmpty().toSet()
                val eksisterendeFinnmarkEllerSvalbardMarkeringer = eksisterendeVilkårResultat.utdypendeVilkårsvurderinger.filter { it == BOSATT_I_FINNMARK_NORD_TROMS || it == BOSATT_PÅ_SVALBARD }.toSet()
                val utdypendeVilkårsvurderingMåOppdateres = eksisterendeFinnmarkEllerSvalbardMarkeringer != gjeldendeFinnmarkEllerSvalbardMarkeringer

                if (utdypendeVilkårsvurderingMåOppdateres) {
                    val oppdaterteUtdypendeVilkårsvurderinger =
                        eksisterendeVilkårResultat.utdypendeVilkårsvurderinger
                            .filter { it != BOSATT_I_FINNMARK_NORD_TROMS && it != BOSATT_PÅ_SVALBARD }
                            .plus(gjeldendeFinnmarkEllerSvalbardMarkeringer)

                    eksisterendeVilkårResultat.copy(utdypendeVilkårsvurderinger = oppdaterteUtdypendeVilkårsvurderinger, begrunnelse = PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                } else {
                    eksisterendeVilkårResultat
                }
            }.tilPerioderIkkeNull()
            .map {
                val periodeErEndret = it.fom != it.verdi.periodeFom || it.tom != it.verdi.periodeTom
                it.verdi.copy(
                    periodeFom = it.fom,
                    periodeTom = it.tom,
                    begrunnelse = if (periodeErEndret) PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT else it.verdi.begrunnelse,
                )
            }
    }
}

fun validerKombinasjonerAvAdresserForFinnmarksOgSvalbardtileggbehandlinger(
    behandling: Behandling,
    erDeltBostedIFinnmarkEllerNordTromsTidslinje: Tidslinje<Boolean>,
    erOppholdsadressePåSvalbardTidslinje: Tidslinje<Boolean>,
    andelForAktør: List<AndelTilkjentYtelse>,
) {
    val harLøpendeAndelTidslinje =
        andelForAktør
            .filter { it.kalkulertUtbetalingsbeløp > 0 && it.type == YtelseType.ORDINÆR_BARNETRYGD }
            .map { Periode(true, it.stønadFom.toLocalDate(), it.stønadTom.toLocalDate()) }
            .tilTidslinje()

    val harDeltBostedIFinnmarkOgOppholdsadressePåSvalbardISammePeriode =
        erDeltBostedIFinnmarkEllerNordTromsTidslinje
            .kombinerMed(erOppholdsadressePåSvalbardTidslinje, harLøpendeAndelTidslinje) { erDeltBostedIFinnmarkEllerNordTroms, erOppholdsadressePåSvalbard, harLøpendeAndel ->
                erDeltBostedIFinnmarkEllerNordTroms == true && erOppholdsadressePåSvalbard == true && harLøpendeAndel == true
            }.tilPerioder()
            .any { it.verdi == true }

    if (harDeltBostedIFinnmarkOgOppholdsadressePåSvalbardISammePeriode) {
        throw AutovedtakMåBehandlesManueltFeil(beskrivelse = "${behandling.opprettetÅrsak.visningsnavn} kan ikke behandles automatisk som følge av adresseendring. Barn har delt bosted i Finnmark/Nord-Troms og oppholdsadresse på Svalbard.\nEndring av ${behandling.opprettetÅrsak.visningsnavn} må håndteres manuelt.")
    }
}

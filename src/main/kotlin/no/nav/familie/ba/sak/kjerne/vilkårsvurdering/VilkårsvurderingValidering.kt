package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Utils.slåSammen
import no.nav.familie.ba.sak.common.VilkårFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.common.toPeriode
import no.nav.familie.ba.sak.ekstern.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjer
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonEnkel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.søker
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.lagForskjøvetTidslinjeForOppfylteVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.DELT_BOSTED
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.DELT_BOSTED_SKAL_IKKE_DELES
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOR_MED_SØKER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.slf4j.LoggerFactory
import java.time.LocalDate

val logger = LoggerFactory.getLogger("VilkårsvurderingValidering.kt")

fun validerIngenVilkårSattEtterSøkersDød(
    søkerOgBarn: List<PersonEnkel>,
    vilkårsvurdering: Vilkårsvurdering,
) {
    val søker = søkerOgBarn.søker()
    val vilkårResultaterSøker =
        vilkårsvurdering.hentPersonResultaterTil(søker.aktør.aktørId)
    val søkersDød = søker.dødsfallDato ?: LocalDate.now()

    val vilkårSomEnderEtterSøkersDød =
        vilkårResultaterSøker
            .groupBy { it.vilkårType }
            .mapNotNull { (vilkårType, vilkårResultater) ->
                vilkårType.takeIf {
                    vilkårResultater.any {
                        it.periodeTom?.isAfter(søkersDød) ?: true
                    }
                }
            }

    if (vilkårSomEnderEtterSøkersDød.isNotEmpty()) {
        throw FunksjonellFeil(
            "Ved behandlingsårsak \"Dødsfall Bruker\" må vilkårene på søker avsluttes " +
                "senest dagen søker døde, men " +
                vilkårSomEnderEtterSøkersDød.map { "\"" + it.beskrivelse + "\"" }.slåSammen() +
                " vilkåret til søker slutter etter søkers død.",
        )
    }
}

fun validerIkkeBlandetRegelverk(
    søkerOgBarn: List<PersonEnkel>,
    vilkårsvurdering: Vilkårsvurdering,
    behandling: Behandling,
) {
    val vilkårsvurderingTidslinjer = VilkårsvurderingTidslinjer(vilkårsvurdering, søkerOgBarn)
    if (vilkårsvurderingTidslinjer.harBlandetRegelverk()) {
        val feilmelding = "Det er forskjellig regelverk for en eller flere perioder for søker eller barna."

        if (behandling.erSatsendringEllerMånedligValutajustering() || behandling.erFinnmarksEllerSvalbardtillegg()) {
            logger.warn("$feilmelding Gjelder $behandling")
        } else {
            throw FunksjonellFeil(melding = feilmelding)
        }
    }
}

fun valider18ÅrsVilkårEksistererFraFødselsdato(
    søkerOgBarn: List<PersonEnkel>,
    vilkårsvurdering: Vilkårsvurdering,
    behandling: Behandling,
) {
    vilkårsvurdering.personResultater.forEach { personResultat ->
        val person = søkerOgBarn.find { it.aktør == personResultat.aktør }
        if (person?.type == PersonType.BARN && !personResultat.vilkårResultater.finnesUnder18VilkårFraFødselsdato(person.fødselsdato)) {
            if (behandling.erSatsendringMånedligValutajusteringFinnmarkstilleggEllerSvalbardtillegg() || behandling.opprettetÅrsak.erOmregningsårsak()) {
                secureLogger.warn(
                    "Fødselsdato ${person.fødselsdato} ulik fom ${
                        personResultat.vilkårResultater.filter { it.vilkårType == Vilkår.UNDER_18_ÅR }
                            .sortedBy { it.periodeFom }.first().periodeFom
                    } i 18års-vilkåret i fagsak ${behandling.fagsak.id}.",
                )
            } else {
                throw FunksjonellFeil(
                    melding = "Barn født ${person.fødselsdato} har ikke fått under 18-vilkåret vurdert fra fødselsdato",
                    frontendFeilmelding = "Det må være en periode på 18-års vilkåret som starter på barnets fødselsdato",
                )
            }
        }
    }
}

fun validerAtManIkkeBorIBådeFinnmarkOgSvalbardSamtidig(
    søkerOgBarn: List<PersonEnkel>,
    vilkårsvurdering: Vilkårsvurdering,
) = vilkårsvurdering.personResultater.forEach { personResultat ->
    val person = søkerOgBarn.find { it.aktør == personResultat.aktør }
    val vilkårResultater = personResultat.vilkårResultater
    val bosattIRiketVilkår = vilkårResultater.filter { it.vilkårType == BOSATT_I_RIKET }

    bosattIRiketVilkår.forEach { vilkår ->
        val finnmarkOgSvalbardSattISammePeriode =
            BOSATT_PÅ_SVALBARD in vilkår.utdypendeVilkårsvurderinger &&
                BOSATT_I_FINNMARK_NORD_TROMS in vilkår.utdypendeVilkårsvurderinger

        if (finnmarkOgSvalbardSattISammePeriode) {
            throw FunksjonellFeil(
                melding = "Barn født ${person?.fødselsdato} kan ikke bo i Finnmark og på Svalbard samtidig.",
                frontendFeilmelding = "Barn født ${person?.fødselsdato} kan ikke bo i Finnmark og på Svalbard samtidig.",
            )
        }
    }
}

fun validerAtDetIkkeFinnesDeltBostedForBarnSomIkkeBorMedSøkerIFinnmark(vilkårsvurdering: Vilkårsvurdering): Boolean = validerAtDetIkkeFinnesDeltBostedForBarnSomIkkeBorMedSøkerITilleggssone(vilkårsvurdering, BOSATT_I_FINNMARK_NORD_TROMS)

fun validerAtDetIkkeFinnesDeltBostedForBarnSomIkkeBorMedSøkerPåSvalbard(vilkårsvurdering: Vilkårsvurdering): Boolean = validerAtDetIkkeFinnesDeltBostedForBarnSomIkkeBorMedSøkerITilleggssone(vilkårsvurdering, BOSATT_PÅ_SVALBARD)

private fun validerAtDetIkkeFinnesDeltBostedForBarnSomIkkeBorMedSøkerITilleggssone(
    vilkårsvurdering: Vilkårsvurdering,
    utdypendeVilkårsvurdering: UtdypendeVilkårsvurdering,
): Boolean {
    val søkersPersonResultat = vilkårsvurdering.personResultater.find { it.erSøkersResultater() } ?: return false

    val søkerBosattITilleggssoneTidslinje =
        søkersPersonResultat.vilkårResultater
            .filter { it.vilkårType == BOSATT_I_RIKET && utdypendeVilkårsvurdering in it.utdypendeVilkårsvurderinger }
            .lagForskjøvetTidslinjeForOppfylteVilkår(BOSATT_I_RIKET)

    val finnesPerioderDerBarnMedDeltBostedIkkeBorSammenMedSøkerITilleggssone =
        vilkårsvurdering
            .personResultater
            .filterNot { it.erSøkersResultater() }
            .any { personResultat ->
                val barnBosattITilleggssoneTidslinje =
                    personResultat.vilkårResultater
                        .filter { it.vilkårType == BOSATT_I_RIKET && utdypendeVilkårsvurdering in it.utdypendeVilkårsvurderinger }
                        .lagForskjøvetTidslinjeForOppfylteVilkår(BOSATT_I_RIKET)

                val barnDeltBostedTidslinje =
                    personResultat.vilkårResultater
                        .filter { it.vilkårType == BOR_MED_SØKER && (DELT_BOSTED in it.utdypendeVilkårsvurderinger || DELT_BOSTED_SKAL_IKKE_DELES in it.utdypendeVilkårsvurderinger) }
                        .lagForskjøvetTidslinjeForOppfylteVilkår(BOR_MED_SØKER)

                søkerBosattITilleggssoneTidslinje
                    .kombinerMed(barnBosattITilleggssoneTidslinje, barnDeltBostedTidslinje) { søkerBosattITilleggssone, barnBosattITilleggssone, barnDeltBosted ->
                        søkerBosattITilleggssone != null && barnBosattITilleggssone == null && barnDeltBosted != null
                    }.tilPerioder()
                    .any { it.verdi == true }
            }

    if (finnesPerioderDerBarnMedDeltBostedIkkeBorSammenMedSøkerITilleggssone) {
        logger.warn("For fagsak ${vilkårsvurdering.behandling.fagsak.id} finnes det perioder der søker er $utdypendeVilkårsvurdering samtidig som et barn med delt bosted ikke er $utdypendeVilkårsvurdering.")
    }

    return finnesPerioderDerBarnMedDeltBostedIkkeBorSammenMedSøkerITilleggssone
}

fun validerResultatBegrunnelse(restVilkårResultat: RestVilkårResultat) {
    val resultat = restVilkårResultat.resultat
    val vilkårType = restVilkårResultat.vilkårType
    val resultatBegrunnelse = restVilkårResultat.resultatBegrunnelse
    val regelverk = restVilkårResultat.vurderesEtter

    if (resultatBegrunnelse != null) {
        if (!resultatBegrunnelse.gyldigForVilkår.contains(vilkårType)) {
            "Resultatbegrunnelsen $resultatBegrunnelse kan ikke kombineres med vilkåret $vilkårType".apply {
                throw FunksjonellFeil(this, this)
            }
        }
        if (!resultatBegrunnelse.gyldigIKombinasjonMedResultat.contains(resultat)) {
            "Resultatbegrunnelsen $resultatBegrunnelse kan ikke kombineres med resultatet $resultat".apply {
                throw FunksjonellFeil(this, this)
            }
        }
        if (!resultatBegrunnelse.gyldigForRegelverk.contains(regelverk)) {
            "Resultatbegrunnelsen $resultatBegrunnelse kan ikke kombineres med regelverket $regelverk".apply {
                throw FunksjonellFeil(this, this)
            }
        }
    }
}

fun validerBarnasVilkår(
    barna: List<PersonEnkel>,
    vilkårsvurdering: Vilkårsvurdering,
) {
    val listeAvFeil = mutableListOf<String>()

    barna.map { barn ->
        vilkårsvurdering.personResultater
            .flatMap { it.vilkårResultater }
            .filter { it.personResultat?.aktør == barn.aktør }
            .forEach { vilkårResultat ->
                if (vilkårResultat.resultat == Resultat.OPPFYLT && vilkårResultat.periodeFom == null) {
                    listeAvFeil.add("Vilkår '${vilkårResultat.vilkårType}' for barn med fødselsdato ${barn.fødselsdato.tilDagMånedÅr()} mangler fom dato.")
                }
                if (vilkårResultat.periodeFom != null && vilkårResultat.toPeriode().fom.isBefore(barn.fødselsdato)) {
                    listeAvFeil.add("Vilkår '${vilkårResultat.vilkårType}' for barn med fødselsdato ${barn.fødselsdato.tilDagMånedÅr()} har fra-og-med dato før barnets fødselsdato.")
                }
                if (vilkårResultat.periodeFom != null &&
                    vilkårResultat.toPeriode().fom.isAfter(barn.fødselsdato.plusYears(18)) &&
                    vilkårResultat.vilkårType == Vilkår.UNDER_18_ÅR &&
                    vilkårResultat.erEksplisittAvslagPåSøknad != true
                ) {
                    listeAvFeil.add("Vilkår '${vilkårResultat.vilkårType}' for barn med fødselsdato ${barn.fødselsdato.tilDagMånedÅr()} har fra-og-med dato etter barnet har fylt 18.")
                }
            }
    }

    if (listeAvFeil.isNotEmpty()) {
        throw VilkårFeil(listeAvFeil.joinToString(separator = "\n"))
    }
}

private fun Set<VilkårResultat>.finnesUnder18VilkårFraFødselsdato(fødselsdato: LocalDate): Boolean = this.filter { it.vilkårType == Vilkår.UNDER_18_ÅR }.any { it.periodeFom == fødselsdato }

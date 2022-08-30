package no.nav.familie.ba.sak.kjerne.brev.domene.eøs

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertKompetanse
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertRestPersonResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertVilkårResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.AnnenForeldersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.BarnetsBostedsland
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.landkodeTilBarnetsBostedsland
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertRestPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype

fun hentMinimerteKompetanserGyldigeForEØSBegrunnelse(
    eøsBegrunnelseMedTriggere: EØSBegrunnelseMedTriggere,
    minimerteKompetanser: List<MinimertKompetanse>
) = minimerteKompetanser.filter {
    eøsBegrunnelseMedTriggere.erGyldigForKompetanseMedData(
        annenForeldersAktivitetFraKompetanse = it.annenForeldersAktivitet,
        barnetsBostedslandFraKompetanse = when (it.barnetsBostedslandNavn.navn) {
            "Norge" -> BarnetsBostedsland.NORGE
            else -> BarnetsBostedsland.IKKE_NORGE
        },
        resultatFraKompetanse = it.resultat
    )
}

fun hentKompetanserGyldigeForEØSBegrunnelse(
    eøsBegrunnelseMedTriggere: EØSBegrunnelseMedTriggere,
    kompetanser: List<Kompetanse>
) = kompetanser.filter {
    eøsBegrunnelseMedTriggere.erGyldigForKompetanseMedData(
        annenForeldersAktivitetFraKompetanse = it.annenForeldersAktivitet!!,
        barnetsBostedslandFraKompetanse = landkodeTilBarnetsBostedsland(it.barnetsBostedsland!!),
        resultatFraKompetanse = it.resultat!!
    )
}

fun EØSBegrunnelseMedTriggere.erGyldigForKompetanseMedData(
    annenForeldersAktivitetFraKompetanse: AnnenForeldersAktivitet,
    barnetsBostedslandFraKompetanse: BarnetsBostedsland,
    resultatFraKompetanse: KompetanseResultat
): Boolean = sanityEØSBegrunnelse.annenForeldersAktivitet
    .contains(annenForeldersAktivitetFraKompetanse) &&
    sanityEØSBegrunnelse.barnetsBostedsland
        .contains(barnetsBostedslandFraKompetanse) &&
    sanityEØSBegrunnelse.kompetanseResultat.contains(
        resultatFraKompetanse
    )

fun hentBarnFraVilkårResultaterSomPasserMedBegrunnelseOgPeriode(
    eøsBegrunnelse: EØSBegrunnelseMedTriggere,
    minimertePersonResultater: List<MinimertRestPersonResultat>,
    personerPåBehandling: List<MinimertRestPerson>,
    minimertVedtaksperiode: MinimertVedtaksperiode,
): List<MinimertRestPerson> {
    val relevantePersonResultaterForBegrunnelseOgPeriode =
        minimertePersonResultater.filter {
            it.hentVilkårResultaterPasserMedBegrunnelseOgPeriode(
                vedtaksperiode = minimertVedtaksperiode,
                begrunnelse = eøsBegrunnelse.sanityEØSBegrunnelse
            ).isNotEmpty()
        }

    val barnIdenterMedRelevanteVilkårForBegrunnelseOgPeriode =
        relevantePersonResultaterForBegrunnelseOgPeriode.map { it.personIdent }

    return barnIdenterMedRelevanteVilkårForBegrunnelseOgPeriode
        .map { personIdent -> personerPåBehandling.single { it.personIdent == personIdent } }
}

fun MinimertRestPersonResultat.hentVilkårResultaterPasserMedBegrunnelseOgPeriode(
    begrunnelse: SanityEØSBegrunnelse,
    vedtaksperiode: MinimertVedtaksperiode,
): List<MinimertVilkårResultat> {
    val vilkårResultaterSomErRelevanteForVedtaksperiode = when (vedtaksperiode.type) {
        Vedtaksperiodetype.UTBETALING -> emptyList()
        Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING -> emptyList()
        Vedtaksperiodetype.OPPHØR,
        Vedtaksperiodetype.AVSLAG -> this.finnAvsluttedeVilkårResultaterSomSkalBegrunnesFor(vedtaksperiode)
        Vedtaksperiodetype.FORTSATT_INNVILGET -> emptyList()
        Vedtaksperiodetype.ENDRET_UTBETALING -> throw Feil("Endret utbetaling skal ikke brukes")
    }

    return vilkårResultaterSomErRelevanteForVedtaksperiode.filter {
        it.utdypendeVilkårsvurderinger == begrunnelse.utdypendeVilkårsvurderinger
    }
}

fun MinimertRestPersonResultat.finnAvsluttedeVilkårResultaterSomSkalBegrunnesFor(
    vedtaksperiode: MinimertVedtaksperiode
): List<MinimertVilkårResultat> = this.minimerteVilkårResultater.filter { vilkårResultat ->
    val etterfølgendeVilkårResultatAvSammeType =
        finnEtterfølgendeVilkårResultatAvSammeType(vilkårResultat, this)

    vilkårResultat.erAvsluttetOgSkalBegrunnesIdennePerioden(
        fomVedtaksperiode = vedtaksperiode.fom,
        etterfølgendeVilkårResultatAvSammeType = etterfølgendeVilkårResultatAvSammeType
    )
}

private fun finnEtterfølgendeVilkårResultatAvSammeType(
    vilkårResultat: MinimertVilkårResultat,
    personResultat: MinimertRestPersonResultat
): MinimertVilkårResultat? = vilkårResultat.periodeTom?.let { tom ->
    personResultat.minimerteVilkårResultater.find {
        it.vilkårType == vilkårResultat.vilkårType && it.periodeFom?.isEqual(tom.plusDays(1)) ?: false
    }
}
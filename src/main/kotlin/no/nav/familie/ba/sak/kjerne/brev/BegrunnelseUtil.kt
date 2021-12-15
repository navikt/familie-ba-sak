package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevGrunnlag
import no.nav.familie.ba.sak.kjerne.brev.domene.harPersonerSomManglerOpplysninger
import no.nav.familie.ba.sak.kjerne.brev.domene.somOverlapper
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.hentPersonerForEtterEndretUtbetalingsperiode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertPerson
import no.nav.familie.ba.sak.kjerne.vedtak.domene.barnMedSeksårsdagPåFom
import no.nav.familie.ba.sak.kjerne.vedtak.hentPersonerForAlleUtgjørendeVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

fun hentPersonidenterGjeldendeForBegrunnelse(
    triggesAv: TriggesAv,
    periode: NullablePeriode,
    vedtakBegrunnelseType: VedtakBegrunnelseType,
    brevGrunnlag: BrevGrunnlag,
    identerMedUtbetalingPåPeriode: List<String>,
    erFørsteVedtaksperiodePåFagsak: Boolean,
): List<String> {

    val erFortsattInnvilgetBegrunnelse = vedtakBegrunnelseType == VedtakBegrunnelseType.FORTSATT_INNVILGET

    return when {
        triggesAv.vilkår.contains(Vilkår.UTVIDET_BARNETRYGD) || triggesAv.småbarnstillegg ->
            hentPersonerForUtvidetOgSmåbarnstilleggBegrunnelse(
                identerMedUtbetaling = identerMedUtbetalingPåPeriode,
                brevGrunnlag = brevGrunnlag,
                periode = periode,
            )

        triggesAv.barnMedSeksårsdag ->
            brevGrunnlag.personerPåBehandling.barnMedSeksårsdagPåFom(periode.fom)
                .map { person -> person.personIdent }

        triggesAv.personerManglerOpplysninger ->
            if (brevGrunnlag.minimertePersonResultater.harPersonerSomManglerOpplysninger())
                emptyList()
            else
                error("Legg til opplysningsplikt ikke oppfylt begrunnelse men det er ikke person med det resultat")

        erFortsattInnvilgetBegrunnelse -> identerMedUtbetalingPåPeriode

        triggesAv.etterEndretUtbetaling ->
            hentPersonerForEtterEndretUtbetalingsperiode(
                minimerteEndredeUtbetalingAndeler = brevGrunnlag.minimerteEndredeUtbetalingAndeler,
                fom = periode.fom,
                endringsaarsaker = triggesAv.endringsaarsaker
            )

        else ->
            hentPersonerForAlleUtgjørendeVilkår(
                minimertePersonResultater = brevGrunnlag.minimertePersonResultater,
                vedtaksperiode = Periode(
                    fom = periode.fom ?: TIDENES_MORGEN,
                    tom = periode.tom ?: TIDENES_ENDE
                ),
                oppdatertBegrunnelseType = vedtakBegrunnelseType,
                aktuellePersonerForVedtaksperiode = hentAktuellePersonerForVedtaksperiode(
                    brevGrunnlag.personerPåBehandling,
                    vedtakBegrunnelseType,
                    identerMedUtbetalingPåPeriode
                ),
                triggesAv = triggesAv,
                erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak
            ).map { person -> person.personIdent }
    }.toSet().toList()
}

/**
 * Selv om utvidet kun gjelder for søker ønsker vi å si noe om hvilke barn søker får utvidet for.
 * Dette vil være alle barn med utbetaling og alle barn med endret utbetaling i samme periode.
 *
 * For eksempel om søker oppfyller vilkårene til delt bosted og utvidet barnetrygd, men barnetrygden allerede er
 * sendt ut til partner, og delt bosted er endret til at det ikke er noen utbetaling, ønsker vi fremdeles å ta med
 * barna uten utbetaling i begrunnelsen.
 */
private fun hentPersonerForUtvidetOgSmåbarnstilleggBegrunnelse(
    identerMedUtbetaling: List<String>,
    brevGrunnlag: BrevGrunnlag,
    periode: NullablePeriode
): List<String> {
    val identerFraSammenfallendeEndringsperioder = brevGrunnlag
        .minimerteEndredeUtbetalingAndeler
        .somOverlapper(periode.tilNullableMånedPeriode())
        .map { it.personIdent }

    return identerMedUtbetaling +
        identerFraSammenfallendeEndringsperioder
}

private fun hentAktuellePersonerForVedtaksperiode(
    personerPåBehandling: List<MinimertPerson>,
    vedtakBegrunnelseType: VedtakBegrunnelseType,
    identerMedUtbetalingPåPeriode: List<String>
): List<MinimertPerson> = personerPåBehandling.filter { person ->
    if (vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGET) {
        identerMedUtbetalingPåPeriode.contains(person.personIdent) || person.type == PersonType.SØKER
    } else true
}
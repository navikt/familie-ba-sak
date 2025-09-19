package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.brev.domene.ISanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.ØvrigTrigger
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.AndelForVedtaksbegrunnelse
import java.time.LocalDate

fun ISanityBegrunnelse.skalFiltreresPåHendelser(
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
    fomVedtaksperiode: LocalDate?,
    tomVedtaksperiode: LocalDate?,
): Boolean =
    if (!begrunnelseGrunnlag.dennePerioden.erOrdinæreVilkårInnvilget()) {
        val person = begrunnelseGrunnlag.dennePerioden.person

        this.erBarnDød(person, fomVedtaksperiode)
    } else {
        val person = begrunnelseGrunnlag.dennePerioden.person

        this.erBarn6År(person, fomVedtaksperiode) ||
            this.erSatsendring(
                person,
                begrunnelseGrunnlag.dennePerioden.andeler,
                fomVedtaksperiode,
                tomVedtaksperiode,
            )
    }

fun ISanityBegrunnelse.erBarn6År(
    person: Person,
    fomVedtaksperiode: LocalDate?,
): Boolean {
    val blirPerson6DennePerioden = person.hentSeksårsdag().toYearMonth() == fomVedtaksperiode?.toYearMonth()

    return blirPerson6DennePerioden && this.øvrigeTriggere.contains(ØvrigTrigger.BARN_MED_6_ÅRS_DAG)
}

fun ISanityBegrunnelse.erBarnDød(
    person: Person,
    fomVedtaksperiode: LocalDate?,
): Boolean {
    val dødsfall = person.dødsfall
    val personDødeForrigeMåned =
        dødsfall != null && dødsfall.dødsfallDato.toYearMonth().plusMonths(1) == fomVedtaksperiode?.toYearMonth()

    return personDødeForrigeMåned &&
        person.type == PersonType.BARN &&
        this.øvrigeTriggere.contains(ØvrigTrigger.BARN_DØD)
}

fun ISanityBegrunnelse.erSatsendring(
    person: Person,
    andeler: Iterable<AndelForVedtaksbegrunnelse>,
    fomVedtaksperiode: LocalDate?,
    tomVedtaksperiode: LocalDate?,
): Boolean {
    // Bruker fomVedtaksperiode siden satsendring alltid er i starten av perioden
    val satstyperPåAndelene =
        andeler
            .flatMap {
                it.type.tilSatsType(
                    person = person,
                    fom = (fomVedtaksperiode ?: TIDENES_MORGEN).toYearMonth(),
                    tom = (tomVedtaksperiode ?: TIDENES_ENDE).toYearMonth(),
                )
            }.toSet()

    val erSatsendringIPeriodenForPerson =
        satstyperPåAndelene.any { satstype ->
            SatsService.finnAlleSatserFor(satstype).any { it.gyldigFom == fomVedtaksperiode }
        }

    return erSatsendringIPeriodenForPerson && this.øvrigeTriggere.contains(ØvrigTrigger.SATSENDRING)
}

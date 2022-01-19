package no.nav.familie.ba.sak.kjerne.vedtak.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.ekstern.restDomene.RestPerson
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevPeriodeGrunnlag
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevPeriodePersonForLogging
import no.nav.familie.ba.sak.kjerne.brev.domene.EndretUtbetalingAndelPåPersonForLogging
import no.nav.familie.ba.sak.kjerne.brev.domene.RestBehandlingsgrunnlagForBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.UtbetalingPåPersonForLogging
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import java.time.LocalDate

/**
 * NB: Bør ikke brukes internt, men kun ut mot eksterne tjenester siden klassen
 * inneholder aktiv personIdent og ikke aktørId.
 */
data class MinimertRestPerson(
    val personIdent: String,
    val fødselsdato: LocalDate,
    val type: PersonType
) {
    fun hentSeksårsdag(): LocalDate = fødselsdato.plusYears(6)
}

fun RestPerson.tilMinimertPerson() = MinimertRestPerson(
    personIdent = this.personIdent,
    fødselsdato = fødselsdato ?: throw Feil("Fødselsdato mangler"),
    type = this.type
)

fun List<MinimertRestPerson>.barnMedSeksårsdagPåFom(fom: LocalDate?): List<MinimertRestPerson> {
    return this
        .filter { it.type == PersonType.BARN }
        .filter { person ->
            person.hentSeksårsdag().toYearMonth() == (
                fom?.toYearMonth()
                    ?: TIDENES_ENDE.toYearMonth()
                )
        }
}

fun List<MinimertRestPerson>.harBarnMedSeksårsdagPåFom(fom: LocalDate?) =
    this.any { person ->
        person
            .hentSeksårsdag()
            .toYearMonth() == (fom?.toYearMonth() ?: TIDENES_ENDE.toYearMonth())
    }

fun List<MinimertRestPerson>.hentSøker() =
    this.firstOrNull { it.type == PersonType.SØKER }
        ?: throw Feil("Fant ikke søker blant begrunnelsepersonene")

fun Person.tilMinimertPerson() = MinimertRestPerson(
    personIdent = this.aktør.aktivFødselsnummer(),
    fødselsdato = this.fødselsdato,
    type = this.type
)

fun List<MinimertRestPerson>.tilBarnasFødselsdatoer(): String =
    Utils.slåSammen(
        this
            .filter { it.type == PersonType.BARN }
            .sortedBy { person ->
                person.fødselsdato
            }
            .map { person ->
                person.fødselsdato.tilKortString() ?: ""
            }
    )

fun MinimertRestPerson.tilBrevPeriodeTestPerson(
    brevPeriodeGrunnlag: BrevPeriodeGrunnlag,
    restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
): BrevPeriodePersonForLogging {
    val minimertePersonResultater =
        restBehandlingsgrunnlagForBrev.minimertePersonResultater.firstOrNull { it.personIdent == this.personIdent }!!
    val minimerteEndretUtbetalingAndelPåPerson =
        restBehandlingsgrunnlagForBrev.minimerteEndredeUtbetalingAndeler.filter { it.personIdent == this.personIdent }
    val minimerteUtbetalingsperiodeDetaljer = brevPeriodeGrunnlag.minimerteUtbetalingsperiodeDetaljer.filter {
        it.person.personIdent == this.personIdent
    }

    return BrevPeriodePersonForLogging(
        fødselsdato = this.fødselsdato,
        type = this.type,
        overstyrteVilkårresultater = minimertePersonResultater.minimerteVilkårResultater,
        andreVurderinger = minimertePersonResultater.andreVurderinger,
        endredeUtbetalinger = minimerteEndretUtbetalingAndelPåPerson.map {
            EndretUtbetalingAndelPåPersonForLogging(
                periode = it.periode,
                årsak = it.årsak
            )
        },
        utbetalinger = minimerteUtbetalingsperiodeDetaljer.map {
            UtbetalingPåPersonForLogging(
                it.ytelseType,
                it.utbetaltPerMnd,
                it.erPåvirketAvEndring,
                it.prosent
            )
        },
    )
}

package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.fpsak.tidsserie.LocalDateSegment
import java.time.YearMonth

data class BehandlingsresultatPerson(
    val personIdent: String = "",
    val personType: PersonType,
    val søktForPerson: Boolean,
    val eksplisittAvslag: Boolean = false,
    val forrigeAndeler: List<BehandlingsresultatAndelTilkjentYtelse> = emptyList(),
    val andeler: List<BehandlingsresultatAndelTilkjentYtelse>,
) {

    /**
     * Utleder krav for personer framstilt nå og/eller tidligere.
     * Disse populeres med behandlingens utfall for enkeltpersonene (YtelsePerson),
     * som igjen brukes for å utlede det totale BehandlingResultat.
     *
     * @return Informasjon om hvordan person påvirkes i behandlingen (se YtelsePerson-doc)
     */
    fun utledYtelsePerson(): YtelsePerson {
        return YtelsePerson(
            personIdent = personIdent,
            ytelseType = utledYtelseType(),
            kravOpprinnelse = utledKravOpprinnelser(),
        )
    }

    private fun utledYtelseType(): YtelseType {
        return when (personType) {
            PersonType.BARN -> YtelseType.ORDINÆR_BARNETRYGD
            PersonType.SØKER -> YtelseType.UTVIDET_BARNETRYGD
            PersonType.ANNENPART -> throw Feil("Kan ikke utlede krav for annen part")
        }
    }

    private fun utledKravOpprinnelser(): List<KravOpprinnelse> {
        return when {
            forrigeAndeler.isNotEmpty() && !søktForPerson -> listOf(KravOpprinnelse.TIDLIGERE)
            forrigeAndeler.isNotEmpty() && søktForPerson -> listOf(
                KravOpprinnelse.TIDLIGERE,
                KravOpprinnelse.INNEVÆRENDE
            )
            else -> listOf(KravOpprinnelse.INNEVÆRENDE)
        }
    }
}

data class BehandlingsresultatAndelTilkjentYtelse(
    val stønadFom: YearMonth,
    val stønadTom: YearMonth,
    val kalkulertUtbetalingsbeløp: Int,
) {

    fun erLøpende(inneværendeMåned: YearMonth): Boolean {
        return this.stønadTom > inneværendeMåned
    }
}

fun LocalDateSegment<BehandlingsresultatAndelTilkjentYtelse>.erLøpende() = this.tom > inneværendeMåned().sisteDagIInneværendeMåned()

fun lagBehandlingsresultatAndelTilkjentYtelse(
    fom: String,
    tom: String,
    kalkulertUtbetalingsbeløp: Int
): BehandlingsresultatAndelTilkjentYtelse {

    return BehandlingsresultatAndelTilkjentYtelse(
        stønadFom = YearMonth.parse(fom),
        stønadTom = YearMonth.parse(tom),
        kalkulertUtbetalingsbeløp = kalkulertUtbetalingsbeløp
    )
}

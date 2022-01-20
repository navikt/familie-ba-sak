package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import java.time.LocalDate
import java.time.Period
import java.time.YearMonth

data class BehandlingsresultatPerson(
    val aktør: Aktør,
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
            aktør = aktør,
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

    override fun toString(): String {
        return "BehandlingsresultatPerson(" +
            "personType=$personType, " +
            "søktForPerson=$søktForPerson, " +
            "eksplisittAvslag=$eksplisittAvslag, " +
            "forrigeAndeler=$forrigeAndeler, " +
            "andeler=$andeler)"
    }

    fun toSecureString(): String {
        return "BehandlingsresultatPerson(" +
            "aktør=$aktør, " +
            "personType=$personType, " +
            "søktForPerson=$søktForPerson, " +
            "eksplisittAvslag=$eksplisittAvslag, " +
            "forrigeAndeler=$forrigeAndeler, " +
            "andeler=$andeler)"
    }

    fun settUgyldigAktør() = this.copy(aktør = defaultUgyldigAktør)

    companion object {
        val defaultUgyldigAktør: Aktør = Aktør(aktørId = "")
    }
}

data class MinimertUregistrertBarn(
    val personIdent: String,
    val navn: String,
    val fødselsdato: LocalDate? = null
)

fun BarnMedOpplysninger.tilMinimertUregisrertBarn() = MinimertUregistrertBarn(
    personIdent = this.ident,
    navn = this.navn,
    fødselsdato = this.fødselsdato
)

data class BehandlingsresultatAndelTilkjentYtelse(
    val stønadFom: YearMonth,
    val stønadTom: YearMonth,
    val kalkulertUtbetalingsbeløp: Int,
) {
    val periode
        get() = MånedPeriode(stønadFom, stønadTom)

    fun erLøpende(inneværendeMåned: YearMonth): Boolean {
        return this.stønadTom > inneværendeMåned
    }

    fun sumForPeriode(): Int {
        val between = Period.between(
            stønadFom.førsteDagIInneværendeMåned(),
            stønadTom.sisteDagIInneværendeMåned()
        )
        val antallMåneder = (between.years * 12) + between.months

        return antallMåneder * kalkulertUtbetalingsbeløp
    }
}

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

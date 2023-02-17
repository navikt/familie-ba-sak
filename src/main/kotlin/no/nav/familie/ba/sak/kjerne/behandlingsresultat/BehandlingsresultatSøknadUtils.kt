package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.EndretUtbetalingAndelTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat

internal enum class Søknadsresultat {
    INNVILGET,
    AVSLÅTT,
    DELVIS_INNVILGET,
    INGEN_RELEVANTE_ENDRINGER
}

object BehandlingsresultatSøknadUtils {

    internal fun utledResultatPåSøknad(
        forrigeAndeler: List<AndelTilkjentYtelse>,
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        nåværendePersonResultater: Set<PersonResultat>,
        personerFremstiltKravFor: List<Aktør>,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
        behandlingÅrsak: BehandlingÅrsak,
        finnesUregistrerteBarn: Boolean
    ): Søknadsresultat {
        val resultaterFraAndeler = utledSøknadResultatFraAndelerTilkjentYtelse(
            forrigeAndeler = forrigeAndeler,
            nåværendeAndeler = nåværendeAndeler,
            personerFremstiltKravFor = personerFremstiltKravFor,
            endretUtbetalingAndeler = endretUtbetalingAndeler
        )

        val erEksplisittAvslagPåMinstEnPersonFremstiltKravFor = erEksplisittAvslagPåMinstEnPersonFremstiltKravForEllerSøker(
            nåværendePersonResultater = nåværendePersonResultater,
            personerFremstiltKravFor = personerFremstiltKravFor
        )

        val erFødselshendelseMedAvslag = if (behandlingÅrsak == BehandlingÅrsak.FØDSELSHENDELSE) {
            nåværendePersonResultater.any { personResultat ->
                personResultat.vilkårResultater
                    .any { it.resultat == Resultat.IKKE_OPPFYLT || it.resultat == Resultat.IKKE_VURDERT }
            }
        } else {
            false
        }

        val alleResultater = (
            if (erEksplisittAvslagPåMinstEnPersonFremstiltKravFor || erFødselshendelseMedAvslag || finnesUregistrerteBarn) {
                resultaterFraAndeler.plus(Søknadsresultat.AVSLÅTT)
            } else {
                resultaterFraAndeler
            }
            ).distinct()

        return alleResultater.kombinerSøknadsresultater()
    }

    internal fun utledSøknadResultatFraAndelerTilkjentYtelse(
        forrigeAndeler: List<AndelTilkjentYtelse>,
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        personerFremstiltKravFor: List<Aktør>,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>
    ): List<Søknadsresultat> {
        val alleSøknadsresultater = personerFremstiltKravFor.flatMap { aktør ->
            val ytelseTyper = (forrigeAndeler.map { it.type } + nåværendeAndeler.map { it.type }).distinct()

            ytelseTyper.flatMap { ytelseType ->
                utledSøknadResultatFraAndelerTilkjentYtelsePerPersonOgType(
                    forrigeAndelerForPerson = forrigeAndeler.filter { it.aktør == aktør && it.type == ytelseType },
                    nåværendeAndelerForPerson = nåværendeAndeler.filter { it.aktør == aktør && it.type == ytelseType },
                    endretUtbetalingAndelerForPerson = endretUtbetalingAndeler.filter { it.person?.aktør == aktør }
                )
            }
        }

        return alleSøknadsresultater.distinct()
    }

    private fun utledSøknadResultatFraAndelerTilkjentYtelsePerPersonOgType(
        forrigeAndelerForPerson: List<AndelTilkjentYtelse>,
        nåværendeAndelerForPerson: List<AndelTilkjentYtelse>,
        endretUtbetalingAndelerForPerson: List<EndretUtbetalingAndel>
    ): List<Søknadsresultat> {
        val forrigeTidslinje = AndelTilkjentYtelseTidslinje(forrigeAndelerForPerson)
        val nåværendeTidslinje = AndelTilkjentYtelseTidslinje(nåværendeAndelerForPerson)
        val endretUtbetalingTidslinje = EndretUtbetalingAndelTidslinje(endretUtbetalingAndelerForPerson)

        val resultatTidslinje = nåværendeTidslinje.kombinerMed(forrigeTidslinje, endretUtbetalingTidslinje) { nåværende, forrige, endretUtbetalingAndel ->
            val forrigeBeløp = forrige?.kalkulertUtbetalingsbeløp
            val nåværendeBeløp = nåværende?.kalkulertUtbetalingsbeløp

            when {
                nåværendeBeløp == forrigeBeløp || nåværendeBeløp == null -> Søknadsresultat.INGEN_RELEVANTE_ENDRINGER // Ingen endring eller fjernet en andel
                nåværendeBeløp > 0 -> Søknadsresultat.INNVILGET // Innvilget beløp som er annerledes enn forrige gang
                nåværendeBeløp == 0 -> {
                    val endringsperiodeÅrsak = endretUtbetalingAndel?.årsak

                    when {
                        nåværende.differanseberegnetPeriodebeløp != null -> Søknadsresultat.INNVILGET
                        endringsperiodeÅrsak == Årsak.DELT_BOSTED -> Søknadsresultat.INNVILGET
                        (endringsperiodeÅrsak == Årsak.ALLEREDE_UTBETALT) ||
                            (endringsperiodeÅrsak == Årsak.ENDRE_MOTTAKER) ||
                            (endringsperiodeÅrsak == Årsak.ETTERBETALING_3ÅR) -> Søknadsresultat.AVSLÅTT
                        else -> Søknadsresultat.INGEN_RELEVANTE_ENDRINGER
                    }
                }
                else -> Søknadsresultat.INGEN_RELEVANTE_ENDRINGER
            }
        }

        return resultatTidslinje.perioder().mapNotNull { it.innhold }.distinct()
    }

    private fun erEksplisittAvslagPåMinstEnPersonFremstiltKravForEllerSøker(
        nåværendePersonResultater: Set<PersonResultat>,
        personerFremstiltKravFor: List<Aktør>
    ): Boolean =
        nåværendePersonResultater
            .filter { personerFremstiltKravFor.contains(it.aktør) || it.erSøkersResultater() }
            .any {
                it.harEksplisittAvslag()
            }

    internal fun List<Søknadsresultat>.kombinerSøknadsresultater(): Søknadsresultat {
        val resultaterUtenIngenEndringer = this.filter { it != Søknadsresultat.INGEN_RELEVANTE_ENDRINGER }

        return when {
            this.isEmpty() -> throw Feil("Klarer ikke utlede søknadsresultat")
            this.size == 1 -> this.single()
            resultaterUtenIngenEndringer.size == 1 -> resultaterUtenIngenEndringer.single()
            resultaterUtenIngenEndringer.size == 2 && resultaterUtenIngenEndringer.containsAll(
                listOf(
                    Søknadsresultat.INNVILGET,
                    Søknadsresultat.AVSLÅTT
                )
            ) -> Søknadsresultat.DELVIS_INNVILGET
            else -> throw Feil("Klarer ikke kombinere søknadsresultater: $this")
        }
    }
}

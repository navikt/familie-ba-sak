package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Utils.avrundetHeltallAvProsent
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerPersonOgType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNullMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import java.time.LocalDate

object TilkjentYtelseSatsendringUtils {

    internal fun beregnTilkjentYtelseMedNySatsForSatsendring(
        andelerFraForrigeBehandling: List<AndelTilkjentYtelse>,
        behandling: Behandling,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
    ): TilkjentYtelse {
        val tilkjentYtelse = TilkjentYtelse(
            behandling = behandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
        )

        val tidlinjerMedForrigeAndelerPerPersonOgType = andelerFraForrigeBehandling.tilTidslinjerPerPersonOgType()

        val nyeAndeler = tidlinjerMedForrigeAndelerPerPersonOgType.flatMap { (aktørOgYtelse, tidlinje) ->
            tidlinje.lagAndelerMedNySatsForPersonOgYtelsetype(
                ytelseType = aktørOgYtelse.second,
                person = personopplysningGrunnlag.personer.find { it.aktør == aktørOgYtelse.first } ?: throw Feil("Må finnes"),
                behandlingId = behandling.id,
                tilkjentYtelse = tilkjentYtelse,
            )
        }

        tilkjentYtelse.andelerTilkjentYtelse.addAll(nyeAndeler)
        return tilkjentYtelse
    }

    internal fun Tidslinje<AndelTilkjentYtelse, Måned>.lagAndelerMedNySatsForPersonOgYtelsetype(
        ytelseType: YtelseType,
        person: Person,
        behandlingId: Long,
        tilkjentYtelse: TilkjentYtelse,
    ): List<AndelTilkjentYtelse> {
        if (this.perioder().any { it.innhold?.type != ytelseType }) throw Feil("Prøver å oppdatere en andel med $ytelseType sats, som ikke har denne satsen fra før ")
        if (this.perioder().any { it.innhold?.aktør != person.aktør }) throw Feil("Prøver å oppdatere sats på en andel som ikke er knyttet til aktørId ${person.aktør.aktørId}")

        val satsTidslinje = when (ytelseType) {
            YtelseType.ORDINÆR_BARNETRYGD -> lagOrdinærTidslinje(barn = person)
            YtelseType.UTVIDET_BARNETRYGD -> satstypeTidslinje(SatsType.UTVIDET_BARNETRYGD)
            YtelseType.SMÅBARNSTILLEGG -> satstypeTidslinje(SatsType.SMA)
        }

        val andelOgNySatsTidslinje = this.kombinerUtenNullMed(satsTidslinje) { forrigeAndel, sats ->
            AndelOgSats(forrigeAndel, sats)
        }

        val nyeAndeler = andelOgNySatsTidslinje.perioder().map {
            val forrigeAndel = it.innhold?.tidligereAndel ?: throw Feil("Finner ikke forrige andel i utledningen av nye andeler for satsendring")
            val nySats = it.innhold.nySats

            val utbetaltBeløp = nySats.avrundetHeltallAvProsent(forrigeAndel.prosent)

            AndelTilkjentYtelse(
                behandlingId = behandlingId,
                tilkjentYtelse = tilkjentYtelse,
                aktør = forrigeAndel.aktør,
                stønadFom = it.fraOgMed.tilYearMonth(),
                stønadTom = it.tilOgMed.tilYearMonth(),
                kalkulertUtbetalingsbeløp = utbetaltBeløp,
                nasjonaltPeriodebeløp = utbetaltBeløp,
                type = forrigeAndel.type,
                sats = nySats,
                prosent = forrigeAndel.prosent,
            )
        }

        return nyeAndeler
    }

    private data class AndelOgSats(
        val tidligereAndel: AndelTilkjentYtelse,
        val nySats: Int,
    )
}

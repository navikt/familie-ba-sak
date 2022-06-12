package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseUtils
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.kopiMedAndeler
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEntitet
import no.nav.familie.ba.sak.kjerne.eøs.util.SkjemaBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.UtenlandskPeriodebeløpBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.ValutakursBuilder
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.byggTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOR_MED_SØKER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.GIFT_PARTNERSKAP
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.LOVLIG_OPPHOLD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.UNDER_18_ÅR
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.YearMonth

class TilkjentYtelseDifferanseberegningTest {

    @Test
    fun `skal gjøre differanseberegning på en tilkjent ytelse med endringsperioder`() {

        val barnsFødselsdato = 13.jan(2020)
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato.tilLocalDate())
        val barn2 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato.tilLocalDate())

        val behandling = lagBehandling()
        val behandlingId = BehandlingId(behandling.id)
        val startMåned = barnsFødselsdato.tilInneværendeMåned()

        val vilkårsvurderingBygger = VilkårsvurderingBuilder<Måned>(behandling)
            .forPerson(søker, startMåned)
            .medVilkår("EEEEEEEEEEEEEEEEEEEEEEE", BOSATT_I_RIKET)
            .medVilkår("EEEEEEEEEEEEEEEEEEEEEEE", LOVLIG_OPPHOLD)
            .forPerson(barn1, startMåned)
            .medVilkår("+>", UNDER_18_ÅR, GIFT_PARTNERSKAP)
            .medVilkår("E>", BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER)
            .forPerson(barn2, startMåned)
            .medVilkår("+>", UNDER_18_ÅR, GIFT_PARTNERSKAP)
            .medVilkår("E>", BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER)
            .byggPerson()

        val tilkjentYtelse = vilkårsvurderingBygger.byggTilkjentYtelse()

        assertEquals(6, tilkjentYtelse.andelerTilkjentYtelse.size)

        val tilkjentYtelseMedEndringer = DeltBostedBuilder(startMåned, tilkjentYtelse)
            .medDeltBosted(" //////000000000011111>", barn1, barn2)
            .byggTilkjentYtelse()

        assertEquals(8, tilkjentYtelseMedEndringer.andelerTilkjentYtelse.size)

        val utenlandskePeriodebeløp = UtenlandskPeriodebeløpBuilder(startMåned, behandlingId)
            .medBeløp(" 44555666>", "EUR", "fr", barn1, barn2)
            .bygg()

        val valutakurser = ValutakursBuilder(startMåned, behandlingId)
            .medKurs(" 888899999>", "EUR", barn1, barn2)
            .bygg()

        val differanseBeregnetTilkjentYtelse =
            beregnDifferanse(tilkjentYtelseMedEndringer, utenlandskePeriodebeløp, valutakurser)

        assertEquals(14, differanseBeregnetTilkjentYtelse.andelerTilkjentYtelse.size)
    }
}

class DeltBostedBuilder(
    startMåned: Tidspunkt<Måned> = jan(2020),
    val tilkjentYtelse: TilkjentYtelse
) : SkjemaBuilder<DeltBosted, DeltBostedBuilder>(startMåned, BehandlingId(tilkjentYtelse.behandling.id)) {

    fun medDeltBosted(k: String, vararg barn: Person) = medSkjema(k, barn.toList()) {
        when (it) {
            '0' -> DeltBosted(prosent = 0)
            '/' -> DeltBosted(prosent = 50)
            '1' -> DeltBosted(prosent = 100)
            else -> null
        }
    }
}

data class DeltBosted(
    override val fom: YearMonth? = null,
    override val tom: YearMonth? = null,
    override val barnAktører: Set<Aktør> = emptySet(),
    val prosent: Int?
) : PeriodeOgBarnSkjemaEntitet<DeltBosted>() {
    override fun utenInnhold() = copy(prosent = null)
    override fun kopier(fom: YearMonth?, tom: YearMonth?, barnAktører: Set<Aktør>) =
        copy(
            fom = fom,
            tom = tom,
            barnAktører = barnAktører.map { it.copy() }.toSet()
        )

    override var id: Long = 0
    override var behandlingId: Long = 0
}

fun DeltBostedBuilder.byggTilkjentYtelse(): TilkjentYtelse {
    val andelerTilkjentYtelserEtterEUA =
        TilkjentYtelseUtils.oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
            tilkjentYtelse.andelerTilkjentYtelse,
            bygg().tilEndreteUtebetalingAndeler()
        )

    return tilkjentYtelse.kopiMedAndeler(andelerTilkjentYtelserEtterEUA)
}

fun Iterable<DeltBosted>.tilEndreteUtebetalingAndeler(): List<EndretUtbetalingAndel> {
    return this
        .filter { deltBosted -> deltBosted.fom != null && deltBosted.tom != null && deltBosted.prosent != null }
        .flatMap { deltBosted ->
            deltBosted.barnAktører.map {
                lagEndretUtbetalingAndel(
                    deltBosted.behandlingId,
                    lagPerson(aktør = it, type = PersonType.BARN),
                    deltBosted.fom!!,
                    deltBosted.tom!!,
                    deltBosted.prosent!!
                )
            }
        }
}

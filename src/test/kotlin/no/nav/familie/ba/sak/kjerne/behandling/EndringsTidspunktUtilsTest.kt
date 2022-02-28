package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.dataGenerator.vilkårsvurdering.lagVilkårsvurderingMedOverstyrendeResultater
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseUtils
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseUtils.oppdaterTilkjentYtelseMedEndretUtbetalingAndeler
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class EndringsTidspunktUtilsTest {
    val behandling = lagBehandling()

    val søker = lagPerson(type = PersonType.SØKER)
    val barn1 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(1))
    val barn2 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(1))

    val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
        behandlingId = behandling.id,
        søker, barn1, barn2
    )

    val periodeBehandling1 = Periode(barn1.fødselsdato.plusMonths(1), LocalDate.now().plusMonths(1))
    val periodeBehandling2 = Periode(barn1.fødselsdato, LocalDate.now().plusMonths(1))

    val overstyrendeVilkårResultaterBehandling1 =
        mapOf(
            søker.aktør.aktørId to emptyList(),
            barn1.aktør.aktørId to
                listOf(
                    lagVilkårResultat(
                        vilkårType = Vilkår.BOR_MED_SØKER,
                        periodeFom = periodeBehandling1.fom,
                        periodeTom = periodeBehandling1.tom,
                    )
                ),
            barn2.aktør.aktørId to emptyList(),
        )

    val overstyrendeVilkårResultaterBehandling2 =
        mapOf(
            søker.aktør.aktørId to emptyList(),
            barn1.aktør.aktørId to
                listOf(
                    lagVilkårResultat(
                        vilkårType = Vilkår.BOR_MED_SØKER,
                        periodeFom = periodeBehandling2.fom,
                        periodeTom = periodeBehandling2.tom
                    )
                ),
            barn2.aktør.aktørId to emptyList(),
        )

    val vilkårsvurdering1 = lagVilkårsvurderingMedOverstyrendeResultater(
        søker = søker,
        barna = listOf(barn1, barn2),
        overstyrendeVilkårResultater = overstyrendeVilkårResultaterBehandling1
    )
    val vilkårsvurdering2 = lagVilkårsvurderingMedOverstyrendeResultater(
        søker = søker,
        barna = listOf(barn1, barn2),
        overstyrendeVilkårResultater = overstyrendeVilkårResultaterBehandling2
    )

    val tilkjentYtelse1 = TilkjentYtelseUtils.beregnTilkjentYtelse(
        vilkårsvurdering = vilkårsvurdering1,
        personopplysningGrunnlag = personopplysningGrunnlag,
        behandling = behandling,
    )

    val tilkjentYtelse2 = TilkjentYtelseUtils.beregnTilkjentYtelse(
        vilkårsvurdering = vilkårsvurdering2,
        personopplysningGrunnlag = personopplysningGrunnlag,
        behandling = behandling,
    )

    @Test
    fun `Skal gi null dersom det ikke er noen endring`() {
        Assertions.assertEquals(
            null,
            finnEndringstidspunkt(
                nyVilkårsvurdering = vilkårsvurdering1,
                gammelVilkårsvurdering = vilkårsvurdering1,
                nyeAndelerTilkjentYtelse = tilkjentYtelse1.andelerTilkjentYtelse.toList(),
                gamleAndelerTilkjentYtelse = tilkjentYtelse1.andelerTilkjentYtelse.toList(),
                nyttPersonopplysningGrunnlag = personopplysningGrunnlag,
                gammeltPersonopplysningGrunnlag = personopplysningGrunnlag,
            )
        )
    }

    @Test
    fun `Skal oppdage endring i tilkjent ytelse dato`() {
        Assertions.assertEquals(
            barn1.fødselsdato.førsteDagIInneværendeMåned().plusMonths(1),
            finnEndringstidspunkt(
                nyVilkårsvurdering = vilkårsvurdering2,
                gammelVilkårsvurdering = vilkårsvurdering1,
                nyeAndelerTilkjentYtelse = tilkjentYtelse2.andelerTilkjentYtelse.toList(),
                gamleAndelerTilkjentYtelse = tilkjentYtelse1.andelerTilkjentYtelse.toList(),
                nyttPersonopplysningGrunnlag = personopplysningGrunnlag,
                gammeltPersonopplysningGrunnlag = personopplysningGrunnlag,
            )
        )
    }

    @Test
    fun `Skal oppdage endringer i overstyrende vilkårtesultater og endret utbetaling`() {
        val overstyrendeVilkårResultaterMedDeltBosted =
            mapOf(
                søker.aktør.aktørId to emptyList(),
                barn1.aktør.aktørId to
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.BOR_MED_SØKER,
                            periodeFom = periodeBehandling1.fom,
                            periodeTom = periodeBehandling1.tom,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED)
                        )
                    ),
                barn2.aktør.aktørId to emptyList(),
            )

        val vilkårsvurderingMedDeltBosted = lagVilkårsvurderingMedOverstyrendeResultater(
            søker = søker,
            barna = listOf(barn1, barn2),
            overstyrendeVilkårResultater = overstyrendeVilkårResultaterMedDeltBosted
        )

        val tilkjentYtelseMedDeltBosted = TilkjentYtelseUtils.beregnTilkjentYtelse(
            vilkårsvurdering = vilkårsvurderingMedDeltBosted,
            personopplysningGrunnlag = personopplysningGrunnlag,
            behandling = behandling,
        )

        val tilkjentYtelseMedEndringDeltBosted =
            oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
                tilkjentYtelse1.andelerTilkjentYtelse,
                listOf(
                    lagEndretUtbetalingAndel(
                        person = barn1,
                        fom = periodeBehandling1.fom.toYearMonth(),
                        tom = YearMonth.now()
                    )
                )
            )

        Assertions.assertEquals(
            periodeBehandling1.fom.førsteDagIInneværendeMåned().plusMonths(1),
            finnEndringstidspunkt(
                nyVilkårsvurdering = vilkårsvurderingMedDeltBosted,
                gammelVilkårsvurdering = vilkårsvurdering1,
                nyeAndelerTilkjentYtelse = tilkjentYtelseMedDeltBosted.andelerTilkjentYtelse.toList(),
                gamleAndelerTilkjentYtelse = tilkjentYtelse1.andelerTilkjentYtelse.toList(),
                nyttPersonopplysningGrunnlag = personopplysningGrunnlag,
                gammeltPersonopplysningGrunnlag = personopplysningGrunnlag,
            )
        )

        Assertions.assertEquals(
            periodeBehandling1.fom.førsteDagIInneværendeMåned(),
            finnEndringstidspunkt(
                nyVilkårsvurdering = vilkårsvurderingMedDeltBosted,
                gammelVilkårsvurdering = vilkårsvurderingMedDeltBosted,
                nyeAndelerTilkjentYtelse = tilkjentYtelseMedDeltBosted.andelerTilkjentYtelse.toList(),
                gamleAndelerTilkjentYtelse = tilkjentYtelseMedEndringDeltBosted.toList(),
                nyttPersonopplysningGrunnlag = personopplysningGrunnlag,
                gammeltPersonopplysningGrunnlag = personopplysningGrunnlag,
            )
        )
    }
}

package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.util.DeltBostedBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.UtenlandskPeriodebeløpBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.ValutakursBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.oppdaterTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
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

/**
 * Merk at operasjoner som tilsynelatende lager en ny instans av TilkjentYtelse, faktisk returner samme.
 * Det skyldes at JPA krever muterbare objekter.
 * Ikke-muterbarhet krever en omskrivning av koden. F.eks å koble vekk EndretUtbetalingPeriode fra AndelTilkjentYtelse
 */
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

        DeltBostedBuilder(startMåned, tilkjentYtelse)
            .medDeltBosted(" //////000000000011111>", barn1, barn2)
            .oppdaterTilkjentYtelse()

        assertEquals(8, tilkjentYtelse.andelerTilkjentYtelse.size)

        val utenlandskePeriodebeløp = UtenlandskPeriodebeløpBuilder(startMåned, behandlingId)
            .medBeløp(" 44555666>", "EUR", "fr", barn1, barn2)
            .bygg()

        val valutakurser = ValutakursBuilder(startMåned, behandlingId)
            .medKurs(" 888899999>", "EUR", barn1, barn2)
            .bygg()

        beregnDifferanseOgOppdaterTilkjentYtelse(tilkjentYtelse, utenlandskePeriodebeløp, valutakurser)

        assertEquals(14, tilkjentYtelse.andelerTilkjentYtelse.size)
    }

    @Test
    fun `skal fjerne differanseberegning når utenlandsk periodebeløp eller valutakurs nullstilles`() {
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

        val tilkjentYtelse = vilkårsvurderingBygger.byggTilkjentYtelse().copy()
        assertEquals(6, tilkjentYtelse.andelerTilkjentYtelse.size)

        val utenlandskePeriodebeløp = UtenlandskPeriodebeløpBuilder(startMåned, behandlingId)
            .medBeløp(" 44555666>", "EUR", "fr", barn1, barn2)
            .bygg()

        val valutakurser = ValutakursBuilder(startMåned, behandlingId)
            .medKurs(" 888899999>", "EUR", barn1, barn2)
            .bygg()

        // Er samme instans som tilkjentYtelse
        beregnDifferanseOgOppdaterTilkjentYtelse(tilkjentYtelse, utenlandskePeriodebeløp, valutakurser)

        assertEquals(12, tilkjentYtelse.andelerTilkjentYtelse.size)

        val blanktUtenlandskPeridebeløp = UtenlandskPeriodebeløpBuilder(startMåned, behandlingId)
            .medBeløp(" >", null, null, barn1, barn2)
            .bygg()

        beregnDifferanseOgOppdaterTilkjentYtelse(tilkjentYtelse, blanktUtenlandskPeridebeløp, valutakurser)

        assertEquals(6, tilkjentYtelse.andelerTilkjentYtelse.size)

        beregnDifferanseOgOppdaterTilkjentYtelse(tilkjentYtelse, utenlandskePeriodebeløp, valutakurser)

        assertEquals(12, tilkjentYtelse.andelerTilkjentYtelse.size)
    }
}

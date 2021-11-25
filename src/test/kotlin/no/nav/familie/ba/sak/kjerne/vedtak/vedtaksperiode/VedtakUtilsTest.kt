package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import io.mockk.mockk
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagTriggesAv
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vedtak.erFørstePeriodeOgVilkårIkkeOppfylt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtakUtilsTest {

    val vedtaksperiode: Periode = Periode(
        fom = LocalDate.now().minusMonths(2),
        tom = LocalDate.now().plusMonths(4)
    )
    val andelerTilkjentYtelseMedYtelseFørPeriode: List<AndelTilkjentYtelse> =
        listOf(
            lagAndelTilkjentYtelse(
                fom = vedtaksperiode.fom.minusMonths(2).toYearMonth(),
                tom = vedtaksperiode.fom.minusMonths(1).toYearMonth()
            )
        )
    val andelerTilkjentYtelseUtenYtelseFørPeriode: List<AndelTilkjentYtelse> =
        listOf(
            lagAndelTilkjentYtelse(
                fom = vedtaksperiode.tom.plusMonths(1).toYearMonth(),
                tom = vedtaksperiode.tom.plusMonths(2).toYearMonth()
            )
        )
    val triggesAv = lagTriggesAv(deltbosted = false, vurderingAnnetGrunnlag = false, medlemskap = false)
    val vilkårResultatIkkeOppfylt: VilkårResultat = lagVilkårResultat(
        resultat = Resultat.IKKE_OPPFYLT,
        periodeFom = vedtaksperiode.fom,
        periodeTom = vedtaksperiode.tom,
        personResultat = mockk(relaxed = true)
    )
    val vilkårResultatIkkeOppfyltDelvisOverlapp: VilkårResultat = lagVilkårResultat(
        resultat = Resultat.IKKE_OPPFYLT,
        periodeFom = vedtaksperiode.fom.minusMonths(1),
        periodeTom = vedtaksperiode.tom.plusMonths(1),
        personResultat = mockk(relaxed = true)
    )
    val vilkårResultatUtenforPeriode: VilkårResultat = lagVilkårResultat(
        resultat = Resultat.IKKE_OPPFYLT,
        periodeFom = vedtaksperiode.tom.plusMonths(1),
        periodeTom = vedtaksperiode.tom.plusMonths(3),
        personResultat = mockk(relaxed = true)
    )

    val vilkårResultatOppfylt: VilkårResultat = lagVilkårResultat(
        resultat = Resultat.OPPFYLT,
        periodeFom = vedtaksperiode.fom,
        periodeTom = vedtaksperiode.tom,
        personResultat = mockk(relaxed = true)
    )

    @Test
    fun `skal gi true dersom resultat ikke er godkjent og det ikke er noen andeler tilkjent ytelse før perioden`() {
        Assertions.assertTrue(
            erFørstePeriodeOgVilkårIkkeOppfylt(
                andelerTilkjentYtelse = andelerTilkjentYtelseUtenYtelseFørPeriode,
                vilkårResultat = vilkårResultatIkkeOppfylt,
                vedtaksperiode = vedtaksperiode,
                triggesAv = triggesAv
            )
        )
        Assertions.assertTrue(
            erFørstePeriodeOgVilkårIkkeOppfylt(
                andelerTilkjentYtelse = andelerTilkjentYtelseUtenYtelseFørPeriode,
                vilkårResultat = vilkårResultatIkkeOppfyltDelvisOverlapp,
                vedtaksperiode = vedtaksperiode,
                triggesAv = triggesAv
            )
        )
    }

    @Test
    fun `skal gi false dersom det er en andel tilkjent ytelse før perioden`() {
        Assertions.assertFalse(
            erFørstePeriodeOgVilkårIkkeOppfylt(
                andelerTilkjentYtelse = andelerTilkjentYtelseMedYtelseFørPeriode,
                vilkårResultat = vilkårResultatIkkeOppfylt,
                vedtaksperiode = vedtaksperiode,
                triggesAv = triggesAv
            )
        )
    }

    @Test
    fun `skal gi false dersom vilkårResultatet er oppfylt`() {
        Assertions.assertFalse(
            erFørstePeriodeOgVilkårIkkeOppfylt(
                andelerTilkjentYtelse = andelerTilkjentYtelseUtenYtelseFørPeriode,
                vilkårResultat = vilkårResultatOppfylt,
                vedtaksperiode = vedtaksperiode,
                triggesAv = triggesAv
            )
        )
    }

    @Test
    fun `skal gi false dersom vilkårResultatet ikke overlapper med periode`() {
        Assertions.assertFalse(
            erFørstePeriodeOgVilkårIkkeOppfylt(
                andelerTilkjentYtelse = andelerTilkjentYtelseUtenYtelseFørPeriode,
                vilkårResultat = vilkårResultatUtenforPeriode,
                vedtaksperiode = vedtaksperiode,
                triggesAv = triggesAv
            )
        )
    }
}

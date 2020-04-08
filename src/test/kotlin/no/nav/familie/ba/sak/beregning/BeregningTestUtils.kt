package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.vilkår.PeriodeResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.nare.core.evaluations.Resultat
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

fun lagTestUtbetalingsoppdragForFGB(personIdent: String,
                                    fagsakId: String,
                                    behandlingId: Long,
                                    vedtakDato: LocalDate,
                                    datoFom: LocalDate,
                                    datoTom: LocalDate)
        : Utbetalingsoppdrag {
    return Utbetalingsoppdrag(
            Utbetalingsoppdrag.KodeEndring.NY,
            "BA",
            fagsakId,
            UUID.randomUUID().toString(),
            "SAKSBEHANDLERID",
            LocalDateTime.now(),
            listOf(Utbetalingsperiode(false,
                    null,
                    1,
                    null,
                    vedtakDato,
                    "BATR",
                    datoFom,
                    datoTom,
                    BigDecimal.ONE,
                    Utbetalingsperiode.SatsType.MND,
                    personIdent,
                    behandlingId
            ))
    )
}

fun lagTestUtbetalingsoppdragForOpphør(personIdent: String,
                                       fagsakId: String,
                                       behandlingId: Long,
                                       vedtakDato: LocalDate,
                                       datoFom: LocalDate,
                                       datoTom: LocalDate,
                                       opphørFom: LocalDate): Utbetalingsoppdrag {

    return Utbetalingsoppdrag(
            Utbetalingsoppdrag.KodeEndring.NY,
            "BA",
            fagsakId,
            UUID.randomUUID().toString(),
            "SAKSBEHANDLERID",
            LocalDateTime.now(),
            listOf(Utbetalingsperiode(true,
                    Opphør(opphørFom),
                    1,
                    null,
                    vedtakDato,
                    "BATR",
                    datoFom,
                    datoTom,
                    BigDecimal.ONE,
                    Utbetalingsperiode.SatsType.MND,
                    personIdent,
                    behandlingId
            ))
    )
}

fun lagTestUtbetalingsoppdragForRevurdering(personIdent: String,
                                            fagsakId: String,
                                            behandlingId: Long,
                                            forrigeBehandlingId: Long,
                                            vedtakDato: LocalDate,
                                            opphørFom: LocalDate,
                                            datoTom: LocalDate,
                                            revurderingFom: LocalDate): Utbetalingsoppdrag {
    return Utbetalingsoppdrag(
            Utbetalingsoppdrag.KodeEndring.NY,
            "BA",
            fagsakId,
            UUID.randomUUID().toString(),
            "SAKSBEHANDLERID",
            LocalDateTime.now(),
            listOf(Utbetalingsperiode(true,
                    Opphør(opphørFom),
                    1,
                    null,
                    vedtakDato,
                    "BATR",
                    opphørFom,
                    datoTom,
                    BigDecimal.ONE,
                    Utbetalingsperiode.SatsType.MND,
                    personIdent,
                    forrigeBehandlingId
            ), Utbetalingsperiode(false,
                    null,
                    2,
                    1,
                    vedtakDato,
                    "BATR",
                    revurderingFom,
                    datoTom,
                    BigDecimal.ONE,
                    Utbetalingsperiode.SatsType.MND,
                    personIdent,
                    behandlingId
            ))
    )
}

fun lagPeriodeResultat(fnr: String, resultat: Resultat, periodeFom: LocalDate?, periodeTom: LocalDate?, behandlingResultat: BehandlingResultat): PeriodeResultat {
    val periodeResultat = PeriodeResultat(
            behandlingResultat = behandlingResultat,
            personIdent = fnr,
            periodeFom = periodeFom,
            periodeTom = periodeTom)
    periodeResultat.vilkårResultater =
            setOf(VilkårResultat(periodeResultat = periodeResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = resultat,
                    begrunnelse = ""))
    return periodeResultat
}
package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode

fun no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag.tilUtbetalingsoppdragDto(): Utbetalingsoppdrag =
    Utbetalingsoppdrag(
        kodeEndring = Utbetalingsoppdrag.KodeEndring.valueOf(this.kodeEndring.name),
        fagSystem = this.fagSystem,
        saksnummer = this.saksnummer,
        aktoer = this.aktoer,
        saksbehandlerId = this.saksbehandlerId,
        avstemmingTidspunkt = this.avstemmingTidspunkt,
        utbetalingsperiode = this.utbetalingsperiode.map { it.tilUtbetalingsperiodeDto() },
        gOmregning = this.gOmregning,
    )

private fun no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode.tilUtbetalingsperiodeDto(): Utbetalingsperiode =
    Utbetalingsperiode(
        erEndringPåEksisterendePeriode = this.erEndringPåEksisterendePeriode,
        opphør = this.opphør?.let { Opphør(it.opphørDatoFom) },
        periodeId = this.periodeId,
        forrigePeriodeId = this.forrigePeriodeId,
        datoForVedtak = this.datoForVedtak,
        klassifisering = this.klassifisering,
        vedtakdatoFom = this.vedtakdatoFom,
        vedtakdatoTom = this.vedtakdatoTom,
        sats = this.sats,
        satsType = Utbetalingsperiode.SatsType.valueOf(this.satsType.name),
        utbetalesTil = this.utbetalesTil,
        behandlingId = this.behandlingId,
        utbetalingsgrad = this.utbetalingsgrad,
    )

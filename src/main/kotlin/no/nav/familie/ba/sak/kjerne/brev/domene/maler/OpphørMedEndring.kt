package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.utbetalingEøs.UtbetalingMndEøs

data class OpphørMedEndring(
    override val mal: Brevmal,
    override val data: OpphørMedEndringData,
) : Vedtaksbrev {
    constructor(
        mal: Brevmal = Brevmal.VEDTAK_OPPHØR_MED_ENDRING,
        vedtakFellesfelter: VedtakFellesfelter,
        etterbetaling: Etterbetaling? = null,
        erFeilutbetalingPåBehandling: Boolean,
        etterbetalingInstitusjon: EtterbetalingInstitusjon? = null,
        refusjonEosAvklart: RefusjonEøsAvklart? = null,
        refusjonEosUavklart: RefusjonEøsUavklart? = null,
        erKlage: Boolean,
        utbetalingstabellAutomatiskValutajustering: UtbetalingstabellAutomatiskValutajustering? = null,
    ) :
        this(
            mal = mal,
            data =
                OpphørMedEndringData(
                    delmalData =
                        OpphørMedEndringData.Delmaler(
                            signaturVedtak =
                                SignaturVedtak(
                                    enhet = vedtakFellesfelter.enhet,
                                    saksbehandler = vedtakFellesfelter.saksbehandler,
                                    beslutter = vedtakFellesfelter.beslutter,
                                ),
                            hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                            feilutbetaling = erFeilutbetalingPåBehandling,
                            etterbetaling = etterbetaling,
                            etterbetalingInstitusjon = etterbetalingInstitusjon,
                            korrigertVedtak = vedtakFellesfelter.korrigertVedtakData,
                            refusjonEosAvklart = refusjonEosAvklart,
                            refusjonEosUavklart = refusjonEosUavklart,
                            klage = erKlage,
                            utbetalingstabellAutomatiskValutajustering = utbetalingstabellAutomatiskValutajustering,
                        ),
                    flettefelter =
                        FlettefelterForDokumentImpl(
                            gjelder = flettefelt(vedtakFellesfelter.gjelder),
                            navn = flettefelt(vedtakFellesfelter.søkerNavn),
                            fodselsnummer = flettefelt(vedtakFellesfelter.søkerFødselsnummer),
                            organisasjonsnummer = flettefelt(vedtakFellesfelter.organisasjonsnummer),
                        ),
                    perioder = vedtakFellesfelter.perioder,
                    utbetalingerPerMndEøs = vedtakFellesfelter.utbetalingerPerMndEøs,
                ),
        )
}

data class OpphørMedEndringData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokument,
    override val perioder: List<BrevPeriode>,
    override val utbetalingerPerMndEøs: Map<String, UtbetalingMndEøs>? = null,
) : VedtaksbrevStandardData {
    data class Delmaler(
        override val signaturVedtak: SignaturVedtak,
        override val feilutbetaling: Boolean,
        override val etterbetaling: Etterbetaling?,
        override val etterbetalingInstitusjon: EtterbetalingInstitusjon?,
        override val korrigertVedtak: KorrigertVedtakData?,
        override val refusjonEosAvklart: RefusjonEøsAvklart?,
        override val refusjonEosUavklart: RefusjonEøsUavklart?,
        override val klage: Boolean,
        override val utbetalingstabellAutomatiskValutajustering: UtbetalingstabellAutomatiskValutajustering?,
        val hjemmeltekst: Hjemmeltekst,
    ) : OpphørMedEndringDelmaler
}

interface OpphørMedEndringDelmaler {
    val signaturVedtak: SignaturVedtak
    val feilutbetaling: Boolean
    val etterbetaling: Etterbetaling?
    val etterbetalingInstitusjon: EtterbetalingInstitusjon?
    val korrigertVedtak: KorrigertVedtakData?
    val refusjonEosAvklart: RefusjonEøsAvklart?
    val refusjonEosUavklart: RefusjonEøsUavklart?
    val klage: Boolean
    val utbetalingstabellAutomatiskValutajustering: UtbetalingstabellAutomatiskValutajustering?
}

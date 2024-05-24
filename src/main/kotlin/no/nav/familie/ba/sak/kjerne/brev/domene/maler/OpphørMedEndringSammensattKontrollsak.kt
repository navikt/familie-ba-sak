package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.utbetalingEøs.UtbetalingMndEøs

data class OpphørMedEndringSammensattKontrollsak(
    override val mal: Brevmal,
    override val data: OpphørMedEndringSammensattKontrollsakData,
) : Vedtaksbrev {
    constructor(
        mal: Brevmal = Brevmal.VEDTAK_OPPHØR_MED_ENDRING,
        vedtakFellesfelter: VedtakFellesfelterSammensattKontrollsak,
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
                OpphørMedEndringSammensattKontrollsakData(
                    delmalData =
                        OpphørMedEndringSammensattKontrollsakData.Delmaler(
                            signaturVedtak =
                                SignaturVedtak(
                                    enhet = vedtakFellesfelter.enhet,
                                    saksbehandler = vedtakFellesfelter.saksbehandler,
                                    beslutter = vedtakFellesfelter.beslutter,
                                ),
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
                    utbetalingerPerMndEøs = vedtakFellesfelter.utbetalingerPerMndEøs,
                    sammensattKontrollsakFritekst = vedtakFellesfelter.sammensattKontrollsakFritekst,
                ),
        )
}

data class OpphørMedEndringSammensattKontrollsakData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokument,
    override val utbetalingerPerMndEøs: Map<String, UtbetalingMndEøs>? = null,
    override val sammensattKontrollsakFritekst: String,
) : VedtaksbrevSammensattKontrollsak {
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
    ) : OpphørMedEndringDelmaler
}

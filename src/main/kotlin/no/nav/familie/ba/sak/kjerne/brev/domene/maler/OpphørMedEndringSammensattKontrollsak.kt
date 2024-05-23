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
        val signaturVedtak: SignaturVedtak,
        val feilutbetaling: Boolean,
        val etterbetaling: Etterbetaling?,
        val etterbetalingInstitusjon: EtterbetalingInstitusjon?,
        val korrigertVedtak: KorrigertVedtakData?,
        val refusjonEosAvklart: RefusjonEøsAvklart?,
        val refusjonEosUavklart: RefusjonEøsUavklart?,
        val klage: Boolean,
        val utbetalingstabellAutomatiskValutajustering: UtbetalingstabellAutomatiskValutajustering?,
    )
}

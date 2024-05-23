package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.utbetalingEøs.UtbetalingMndEøs

data class OpphørtSammensattKontrollsak(
    override val mal: Brevmal,
    override val data: OpphørtSammensattKontrollsakData,
) : Vedtaksbrev {
    constructor(
        mal: Brevmal = Brevmal.VEDTAK_OPPHØRT,
        vedtakFellesfelterSammensattKontrollsak: VedtakFellesfelterSammensattKontrollsak,
        erFeilutbetalingPåBehandling: Boolean,
    ) :
        this(
            mal = mal,
            data =
                OpphørtSammensattKontrollsakData(
                    delmalData =
                        OpphørtSammensattKontrollsakData.Delmaler(
                            signaturVedtak =
                                SignaturVedtak(
                                    enhet = vedtakFellesfelterSammensattKontrollsak.enhet,
                                    saksbehandler = vedtakFellesfelterSammensattKontrollsak.saksbehandler,
                                    beslutter = vedtakFellesfelterSammensattKontrollsak.beslutter,
                                ),
                            feilutbetaling = erFeilutbetalingPåBehandling,
                            korrigertVedtak = vedtakFellesfelterSammensattKontrollsak.korrigertVedtakData,
                        ),
                    flettefelter =
                        FlettefelterForDokumentImpl(
                            navn = vedtakFellesfelterSammensattKontrollsak.søkerNavn,
                            fodselsnummer = vedtakFellesfelterSammensattKontrollsak.søkerFødselsnummer,
                            organisasjonsnummer = vedtakFellesfelterSammensattKontrollsak.organisasjonsnummer,
                            gjelder = vedtakFellesfelterSammensattKontrollsak.gjelder,
                        ),
                    sammensattKontrollsakFritekst = vedtakFellesfelterSammensattKontrollsak.sammensattKontrollsakFritekst,
                ),
        )
}

data class OpphørtSammensattKontrollsakData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentImpl,
    override val utbetalingerPerMndEøs: Map<String, UtbetalingMndEøs>? = null,
    override val sammensattKontrollsakFritekst: String,
) : VedtaksbrevSammensattKontrollsak {
    data class Delmaler(
        val signaturVedtak: SignaturVedtak,
        val feilutbetaling: Boolean,
        val korrigertVedtak: KorrigertVedtakData?,
    )
}

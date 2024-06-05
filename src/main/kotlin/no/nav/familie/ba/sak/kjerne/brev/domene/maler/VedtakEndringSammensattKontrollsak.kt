package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.utbetalingEøs.UtbetalingMndEøs

data class VedtakEndringSammensattKontrollsak(
    override val mal: Brevmal,
    override val data: EndringVedtakSammensattKontrollsakData,
) : Vedtaksbrev {
    constructor(
        mal: Brevmal = Brevmal.VEDTAK_ENDRING,
        vedtakFellesfelter: VedtakFellesfelterSammensattKontrollsak,
        etterbetaling: Etterbetaling? = null,
        erFeilutbetalingPåBehandling: Boolean,
        erKlage: Boolean,
        etterbetalingInstitusjon: EtterbetalingInstitusjon? = null,
        informasjonOmAarligKontroll: Boolean,
        feilutbetaltValuta: FeilutbetaltValuta? = null,
        refusjonEosAvklart: RefusjonEøsAvklart? = null,
        refusjonEosUavklart: RefusjonEøsUavklart? = null,
        duMåMeldeFraOmEndringer: Boolean = true,
        duMåMeldeFraOmEndringerEøsSelvstendigRett: Boolean = false,
        informasjonOmUtbetaling: Boolean = false,
        utbetalingstabellAutomatiskValutajustering: UtbetalingstabellAutomatiskValutajustering? = null,
    ) :
        this(
            mal = mal,
            data =
                EndringVedtakSammensattKontrollsakData(
                    delmalData =
                        EndringVedtakSammensattKontrollsakData.Delmaler(
                            signaturVedtak =
                                SignaturVedtak(
                                    enhet = vedtakFellesfelter.enhet,
                                    saksbehandler = vedtakFellesfelter.saksbehandler,
                                    beslutter = vedtakFellesfelter.beslutter,
                                ),
                            etterbetaling = etterbetaling,
                            klage = erKlage,
                            klageInstitusjon = erKlage,
                            feilutbetaling = erFeilutbetalingPåBehandling,
                            etterbetalingInstitusjon = etterbetalingInstitusjon,
                            korrigertVedtak = vedtakFellesfelter.korrigertVedtakData,
                            informasjonOmAarligKontroll = informasjonOmAarligKontroll,
                            forMyeUtbetaltBarnetrygd = feilutbetaltValuta,
                            refusjonEosAvklart = refusjonEosAvklart,
                            refusjonEosUavklart = refusjonEosUavklart,
                            duMaaMeldeFraOmEndringerEosSelvstendigRett = duMåMeldeFraOmEndringerEøsSelvstendigRett,
                            duMaaMeldeFraOmEndringer = duMåMeldeFraOmEndringer,
                            informasjonOmUtbetaling = informasjonOmUtbetaling,
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

data class EndringVedtakSammensattKontrollsakData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokument,
    override val utbetalingerPerMndEøs: Map<String, UtbetalingMndEøs>? = null,
    override val sammensattKontrollsakFritekst: String,
) : VedtaksbrevSammensattKontrollsak {
    data class Delmaler(
        override val signaturVedtak: SignaturVedtak,
        override val etterbetaling: Etterbetaling?,
        override val feilutbetaling: Boolean,
        override val klage: Boolean,
        override val klageInstitusjon: Boolean,
        override val etterbetalingInstitusjon: EtterbetalingInstitusjon?,
        override val korrigertVedtak: KorrigertVedtakData?,
        override val informasjonOmAarligKontroll: Boolean,
        override val forMyeUtbetaltBarnetrygd: FeilutbetaltValuta?,
        override val refusjonEosAvklart: RefusjonEøsAvklart?,
        override val refusjonEosUavklart: RefusjonEøsUavklart?,
        override val duMaaMeldeFraOmEndringerEosSelvstendigRett: Boolean,
        override val duMaaMeldeFraOmEndringer: Boolean,
        override val informasjonOmUtbetaling: Boolean,
        override val utbetalingstabellAutomatiskValutajustering: UtbetalingstabellAutomatiskValutajustering?,
    ) : EndringVedtakDelmaler
}

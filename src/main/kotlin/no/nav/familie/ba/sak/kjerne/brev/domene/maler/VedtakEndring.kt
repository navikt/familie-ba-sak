package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode

data class VedtakEndring(
    override val mal: Brevmal,
    override val data: EndringVedtakData
) : Vedtaksbrev {

    constructor(
        mal: Brevmal = Brevmal.VEDTAK_ENDRING,
        vedtakFellesfelter: VedtakFellesfelter,
        etterbetaling: Etterbetaling? = null,
        erFeilutbetalingPåBehandling: Boolean,
        erKlage: Boolean,
        etterbetalingInstitusjon: EtterbetalingInstitusjon? = null
    ) :
        this(
            mal = mal,
            data = EndringVedtakData(
                delmalData = EndringVedtakData.Delmaler(
                    signaturVedtak = SignaturVedtak(
                        enhet = vedtakFellesfelter.enhet,
                        saksbehandler = vedtakFellesfelter.saksbehandler,
                        beslutter = vedtakFellesfelter.beslutter
                    ),
                    etterbetaling = etterbetaling,
                    hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                    klage = erKlage,
                    klageInstitusjon = erKlage,
                    feilutbetaling = erFeilutbetalingPåBehandling,
                    etterbetalingInstitusjon = etterbetalingInstitusjon,
                    korrigertVedtak = vedtakFellesfelter.korrigertVedtakData
                ),
                flettefelter = FlettefelterForDokumentImpl(
                    navn = vedtakFellesfelter.søkerNavn,
                    fodselsnummer = vedtakFellesfelter.søkerFødselsnummer,
                    organisasjonsnummer = vedtakFellesfelter.organisasjonsnummer,
                    gjelder = vedtakFellesfelter.gjelder
                ),
                perioder = vedtakFellesfelter.perioder
            )
        )
}

data class EndringVedtakData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentImpl,
    override val perioder: List<BrevPeriode>
) : VedtaksbrevData {

    data class Delmaler(
        val signaturVedtak: SignaturVedtak,
        val etterbetaling: Etterbetaling?,
        val feilutbetaling: Boolean,
        val hjemmeltekst: Hjemmeltekst,
        val klage: Boolean,
        val klageInstitusjon: Boolean,
        val etterbetalingInstitusjon: EtterbetalingInstitusjon?,
        val korrigertVedtak: KorrigertVedtakData?
    )
}

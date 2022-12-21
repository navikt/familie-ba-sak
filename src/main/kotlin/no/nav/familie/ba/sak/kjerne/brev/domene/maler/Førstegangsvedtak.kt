package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode

data class Førstegangsvedtak(
    override val mal: Brevmal,
    override val data: FørstegangsvedtakData
) : Vedtaksbrev {

    constructor(
        mal: Brevmal = Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
        vedtakFellesfelter: VedtakFellesfelter,
        etterbetaling: Etterbetaling? = null,
        etterbetalingInstitusjon: EtterbetalingInstitusjon? = null,
        informasjonOmAarligKontroll: Boolean = false
    ) :
        this(
            mal = mal,
            data = FørstegangsvedtakData(
                delmalData = FørstegangsvedtakData.Delmaler(
                    signaturVedtak = SignaturVedtak(
                        enhet = vedtakFellesfelter.enhet,
                        saksbehandler = vedtakFellesfelter.saksbehandler,
                        beslutter = vedtakFellesfelter.beslutter
                    ),
                    etterbetaling = etterbetaling,
                    hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                    etterbetalingInstitusjon = etterbetalingInstitusjon,
                    korrigertVedtak = vedtakFellesfelter.korrigertVedtakData,
                    informasjonOmAarligKontroll = informasjonOmAarligKontroll
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

data class FørstegangsvedtakData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentImpl,
    override val perioder: List<BrevPeriode>
) : VedtaksbrevData {

    data class Delmaler(
        val signaturVedtak: SignaturVedtak,
        val etterbetaling: Etterbetaling?,
        val hjemmeltekst: Hjemmeltekst,
        val etterbetalingInstitusjon: EtterbetalingInstitusjon?,
        val korrigertVedtak: KorrigertVedtakData?,
        val informasjonOmAarligKontroll: Boolean
    )
}

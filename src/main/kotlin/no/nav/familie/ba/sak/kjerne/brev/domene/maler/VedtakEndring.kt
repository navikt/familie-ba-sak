package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import java.time.LocalDate

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
        etterbetalingInstitusjon: EtterbetalingInstitusjon? = null,
        informasjonOmAarligKontroll: Boolean,
        feilutbetaltValuta: FeilutbetaltValuta? = null

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
                    korrigertVedtak = vedtakFellesfelter.korrigertVedtakData,
                    informasjonOmAarligKontroll = informasjonOmAarligKontroll,
                    forMyeUtbetaltBarnetrygd = feilutbetaltValuta != null
                ),
                flettefelter = object : FlettefelterForDokument {
                    val perioderMedForMyeUtbetalt: Flettefelt = feilutbetaltValuta?.perioderMedForMyeUtbetalt
                    override val navn = flettefelt(vedtakFellesfelter.søkerNavn)
                    override val fodselsnummer = flettefelt(vedtakFellesfelter.søkerFødselsnummer)
                    override val brevOpprettetDato = flettefelt(LocalDate.now().tilDagMånedÅr())
                    override val organisasjonsnummer = flettefelt(vedtakFellesfelter.organisasjonsnummer)
                    override val gjelder = flettefelt(vedtakFellesfelter.gjelder)
                },
                perioder = vedtakFellesfelter.perioder
            )
        )
}

data class EndringVedtakData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokument,
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
        val korrigertVedtak: KorrigertVedtakData?,
        val informasjonOmAarligKontroll: Boolean,
        val forMyeUtbetaltBarnetrygd: Boolean
    )
}

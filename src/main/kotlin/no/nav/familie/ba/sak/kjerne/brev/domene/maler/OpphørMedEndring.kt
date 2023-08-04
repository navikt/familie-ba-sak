package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import java.time.LocalDate

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
    ) :
        this(
            mal = mal,
            data = OpphørMedEndringData(
                delmalData = OpphørMedEndringData.Delmaler(
                    signaturVedtak = SignaturVedtak(
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
                ),
                flettefelter = object : FlettefelterForDokument {
                    @Deprecated("Skal bort når vi har lagt til støtte for å sende inn felttefelter som types i delmaler i familie-brev")
                    val perioderMedRefusjonEosAvklart: Flettefelt = refusjonEosAvklart?.perioderMedRefusjonEosAvklart

                    @Deprecated("Skal bort når vi har lagt til støtte for å sende inn felttefelter som types i delmaler i familie-brev")
                    val perioderMedRefusjonEosUavklart: Flettefelt = refusjonEosUavklart?.perioderMedRefusjonEosUavklart
                    override val brevOpprettetDato = flettefelt(LocalDate.now().tilDagMånedÅr())
                    override val navn = flettefelt(vedtakFellesfelter.søkerNavn)
                    override val fodselsnummer = flettefelt(vedtakFellesfelter.søkerFødselsnummer)
                    override val organisasjonsnummer = flettefelt(vedtakFellesfelter.organisasjonsnummer)
                    override val gjelder = flettefelt(vedtakFellesfelter.gjelder)
                },
                perioder = vedtakFellesfelter.perioder,
            ),
        )
}

data class OpphørMedEndringData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokument,
    override val perioder: List<BrevPeriode>,
) : VedtaksbrevData {
    data class Delmaler(
        val signaturVedtak: SignaturVedtak,
        val feilutbetaling: Boolean,
        val hjemmeltekst: Hjemmeltekst,
        val etterbetaling: Etterbetaling?,
        val etterbetalingInstitusjon: EtterbetalingInstitusjon?,
        val korrigertVedtak: KorrigertVedtakData?,
        val refusjonEosAvklart: RefusjonEøsAvklart?,
        val refusjonEosUavklart: RefusjonEøsUavklart?,
    )
}

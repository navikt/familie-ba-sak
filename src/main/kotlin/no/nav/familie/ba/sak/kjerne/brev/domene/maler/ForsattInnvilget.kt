package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import java.time.LocalDate

data class ForsattInnvilget(
    override val mal: Brevmal,
    override val data: ForsattInnvilgetData,
) : Vedtaksbrev {

    constructor(
        mal: Brevmal = Brevmal.VEDTAK_FORTSATT_INNVILGET,
        vedtakFellesfelter: VedtakFellesfelter,
        etterbetaling: Etterbetaling? = null,
        etterbetalingInstitusjon: EtterbetalingInstitusjon? = null,
        informasjonOmAarligKontroll: Boolean = false,
        refusjonEosAvklart: RefusjonEøsAvklart? = null,
        refusjonEosUavklart: RefusjonEøsUavklart? = null,
    ) :
        this(
            mal = mal,
            data = ForsattInnvilgetData(
                delmalData = ForsattInnvilgetData.Delmaler(
                    signaturVedtak = SignaturVedtak(
                        enhet = vedtakFellesfelter.enhet,
                        saksbehandler = vedtakFellesfelter.saksbehandler,
                        beslutter = vedtakFellesfelter.beslutter,
                    ),
                    hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                    etterbetaling = etterbetaling,
                    etterbetalingInstitusjon = etterbetalingInstitusjon,
                    korrigertVedtak = vedtakFellesfelter.korrigertVedtakData,
                    informasjonOmAarligKontroll = informasjonOmAarligKontroll,
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

data class ForsattInnvilgetData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokument,
    override val perioder: List<BrevPeriode>,
) : VedtaksbrevData {

    data class Delmaler(
        val signaturVedtak: SignaturVedtak,
        val hjemmeltekst: Hjemmeltekst,
        val etterbetaling: Etterbetaling?,
        val etterbetalingInstitusjon: EtterbetalingInstitusjon?,
        val korrigertVedtak: KorrigertVedtakData?,
        val informasjonOmAarligKontroll: Boolean,
        val refusjonEosAvklart: RefusjonEøsAvklart?,
        val refusjonEosUavklart: RefusjonEøsUavklart?,
    )
}

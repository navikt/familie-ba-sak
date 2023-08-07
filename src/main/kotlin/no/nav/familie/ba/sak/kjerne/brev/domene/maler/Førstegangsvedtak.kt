package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import java.time.LocalDate

data class Førstegangsvedtak(
    override val mal: Brevmal,
    override val data: FørstegangsvedtakData,
) : Vedtaksbrev {

    constructor(
        mal: Brevmal = Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
        vedtakFellesfelter: VedtakFellesfelter,
        etterbetaling: Etterbetaling? = null,
        etterbetalingInstitusjon: EtterbetalingInstitusjon? = null,
        informasjonOmAarligKontroll: Boolean = false,
        refusjonEosAvklart: RefusjonEøsAvklart? = null,
        refusjonEosUavklart: RefusjonEøsUavklart? = null,
    ) :
        this(
            mal = mal,
            data = FørstegangsvedtakData(
                delmalData = FørstegangsvedtakData.Delmaler(
                    signaturVedtak = SignaturVedtak(
                        enhet = vedtakFellesfelter.enhet,
                        saksbehandler = vedtakFellesfelter.saksbehandler,
                        beslutter = vedtakFellesfelter.beslutter,
                    ),
                    etterbetaling = etterbetaling,
                    hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                    etterbetalingInstitusjon = etterbetalingInstitusjon,
                    korrigertVedtak = vedtakFellesfelter.korrigertVedtakData,
                    informasjonOmAarligKontroll = informasjonOmAarligKontroll,
                    refusjonEosAvklart = refusjonEosAvklart,
                    refusjonEosUavklart = refusjonEosUavklart,
                ),
                perioder = vedtakFellesfelter.perioder,
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
            ),
        )
}

data class FørstegangsvedtakData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokument,
    override val perioder: List<BrevPeriode>,
) : VedtaksbrevData {

    data class Delmaler(
        val signaturVedtak: SignaturVedtak,
        val etterbetaling: Etterbetaling?,
        val hjemmeltekst: Hjemmeltekst,
        val etterbetalingInstitusjon: EtterbetalingInstitusjon?,
        val korrigertVedtak: KorrigertVedtakData?,
        val informasjonOmAarligKontroll: Boolean,
        val refusjonEosAvklart: RefusjonEøsAvklart?,
        val refusjonEosUavklart: RefusjonEøsUavklart?,
    )
}

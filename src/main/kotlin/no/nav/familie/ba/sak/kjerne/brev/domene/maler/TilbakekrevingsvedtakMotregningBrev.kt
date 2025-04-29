package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning.TilbakekrevingsvedtakMotregning
import java.time.LocalDate

data class TilbakekrevingsvedtakMotregningBrev(
    override val mal: Brevmal,
    override val data: TilbakekrevingsvedtakMotregningBrevData,
) : Brev

data class TilbakekrevingsvedtakMotregningBrevData(
    override val delmalData: DelmalData,
    override val flettefelter: Flettefelter,
) : BrevData {
    data class Flettefelter(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
        val aarsakTilFeilutbetaling: Flettefelt,
        val vurderingAvSkyld: Flettefelt,
        val varselDato: Flettefelt,
        val sumAvFeilutbetaling: Flettefelt,
        val avregningperioder: Flettefelt,
    ) : FlettefelterForDokument {
        constructor(
            navn: String,
            fodselsnummer: String,
            brevOpprettetDato: LocalDate,
            tilbakekrevingsvedtakMotregning: TilbakekrevingsvedtakMotregning,
            sumAvFeilutbetaling: String,
            avregningperioder: List<String>,
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
            brevOpprettetDato = flettefelt(brevOpprettetDato.tilDagMånedÅr()),
            aarsakTilFeilutbetaling = flettefelt(tilbakekrevingsvedtakMotregning.årsakTilFeilutbetaling),
            vurderingAvSkyld = flettefelt(tilbakekrevingsvedtakMotregning.vurderingAvSkyld),
            varselDato = flettefelt(tilbakekrevingsvedtakMotregning.varselDato.tilDagMånedÅr()),
            sumAvFeilutbetaling = flettefelt(sumAvFeilutbetaling),
            avregningperioder = flettefelt(avregningperioder),
        )
    }

    data class DelmalData(
        val signaturVedtak: SignaturVedtak,
    )
}

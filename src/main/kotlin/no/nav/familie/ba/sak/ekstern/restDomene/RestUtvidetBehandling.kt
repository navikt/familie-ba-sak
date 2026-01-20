package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.VurderingsstrategiForValutakurser
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Utbetalingsperiode
import java.time.LocalDate
import java.time.LocalDateTime

data class RestUtvidetBehandling(
    val behandlingId: Long,
    val steg: StegType,
    val stegTilstand: List<BehandlingStegTilstandDto>,
    val status: BehandlingStatus,
    val resultat: Behandlingsresultat,
    val skalBehandlesAutomatisk: Boolean,
    val type: BehandlingType,
    val kategori: BehandlingKategori,
    val underkategori: BehandlingUnderkategoriDTO,
    val årsak: BehandlingÅrsak,
    val opprettetTidspunkt: LocalDateTime,
    val endretAv: String,
    val arbeidsfordelingPåBehandling: ArbeidsfordelingPåBehandlingDto,
    val søknadsgrunnlag: SøknadDTO?,
    val personer: List<PersonDto>,
    val personResultater: List<PersonResultatDto>,
    val fødselshendelsefiltreringResultater: List<FødselshendelsefiltreringResultatDto>,
    val utbetalingsperioder: List<Utbetalingsperiode>,
    val personerMedAndelerTilkjentYtelse: List<RestPersonMedAndeler>,
    val endretUtbetalingAndeler: List<EndretUtbetalingAndelDto>,
    val kompetanser: List<KompetanseDto>,
    val tilbakekreving: RestTilbakekreving?,
    val vedtak: RestVedtak?,
    val totrinnskontroll: RestTotrinnskontroll?,
    val aktivSettPåVent: RestSettPåVent?,
    val migreringsdato: LocalDate?,
    val valutakurser: List<RestValutakurs>,
    val utenlandskePeriodebeløp: List<RestUtenlandskPeriodebeløp>,
    val korrigertEtterbetaling: KorrigertEtterbetalingDto?,
    val korrigertVedtak: KorrigertVedtakDto?,
    val feilutbetaltValuta: List<FeilutbetaltValutaDto>,
    val brevmottakere: List<BrevmottakerDto>,
    val refusjonEøs: List<RestRefusjonEøs>,
    val vurderingsstrategiForValutakurser: VurderingsstrategiForValutakurser? = VurderingsstrategiForValutakurser.AUTOMATISK,
    val søknadMottattDato: LocalDateTime?,
    val tilbakekrevingsvedtakMotregning: RestTilbakekrevingsvedtakMotregning?,
    val manglendeSvalbardmerking: List<ManglendeFinnmarkSvalbardMerkingDto>,
    val manglendeFinnmarkmerking: ManglendeFinnmarkSvalbardMerkingDto?,
)

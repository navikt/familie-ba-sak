package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.restDomene.RestBeregningOversikt
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakUtils
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.TilkjentYtelseUtils
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.dokument.DokumentController.ManueltBrevRequest
import no.nav.familie.ba.sak.dokument.domene.BrevType
import no.nav.familie.ba.sak.dokument.domene.MalMedData
import no.nav.familie.ba.sak.dokument.domene.maler.*
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.stereotype.Service

@Service
class MalerService(
        private val totrinnskontrollService: TotrinnskontrollService,
        private val beregningService: BeregningService,
        private val persongrunnlagService: PersongrunnlagService,
        private val arbeidsfordelingService: ArbeidsfordelingService,
        private val økonomiService: ØkonomiService
) {

    fun mapTilVedtakBrevfelter(vedtak: Vedtak,
                               behandlingResultatType: BehandlingResultatType): MalMedData {

        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = vedtak.behandling.id)
                                       ?: throw Feil(message = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev",
                                                     frontendFeilmelding = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev")

        return MalMedData(
                mal = malNavnForMedlemskapOgResultatType(behandlingResultatType,
                                                         vedtak.behandling.opprettetÅrsak,
                                                         vedtak.behandling.type),
                fletteFelter = when (behandlingResultatType) {
                    BehandlingResultatType.INNVILGET -> mapTilInnvilgetBrevFelter(vedtak, personopplysningGrunnlag)
                    else -> throw FunksjonellFeil(melding = "Brev ikke støttet for behandlingsresultat=$behandlingResultatType",
                                                  frontendFeilmelding = "Brev ikke støttet for behandlingsresultat=$behandlingResultatType")
                }
        )
    }

    fun mapTilManuellMalMedData(behandling: Behandling, manueltBrevRequest: ManueltBrevRequest): MalMedData {
        return when (manueltBrevRequest.brevmal) {
            BrevType.INNHENTE_OPPLYSNINGER -> mapTilInnhenteOpplysningerBrevfelter(behandling,
                                                                                   manueltBrevRequest)
            BrevType.VARSEL_OM_REVURDERING -> mapTilVarselOmRevurderingBrevfelter(behandling,
                                                                                  manueltBrevRequest)
            else -> throw Feil(message = "Brevmal ${manueltBrevRequest.brevmal} er ikke støttet for manuelle brev.",
                               frontendFeilmelding = "Klarte ikke generere brev. Brevmal ${manueltBrevRequest.brevmal.malId} er ikke støttet.")
        }
    }

    fun mapTilVarselOmRevurderingBrevfelter(behandling: Behandling, manueltBrevRequest: ManueltBrevRequest): MalMedData {
        val (enhetNavn, målform) = hentMålformOgEnhetNavn(behandling)

        return MalMedData(
                mal = manueltBrevRequest.brevmal.malId,
                fletteFelter = objectMapper.writeValueAsString(VarselOmRevurdering(
                        fritekst = manueltBrevRequest.fritekst,
                        enhet = enhetNavn,
                        aarsaker = manueltBrevRequest.multiselectVerdier,
                        saksbehandler = SikkerhetContext.hentSaksbehandlerNavn(),
                        maalform = målform
                ))
        )
    }

    fun mapTilInnhenteOpplysningerBrevfelter(behandling: Behandling, manueltBrevRequest: ManueltBrevRequest): MalMedData {
        val (enhetNavn, målform) = hentMålformOgEnhetNavn(behandling)

        return MalMedData(
                mal = manueltBrevRequest.brevmal.malId,
                fletteFelter = objectMapper.writeValueAsString(InnhenteOpplysninger(
                        fritekst = manueltBrevRequest.fritekst,
                        enhet = enhetNavn,
                        dokumenter = manueltBrevRequest.multiselectVerdier,
                        saksbehandler = SikkerhetContext.hentSaksbehandlerNavn(),
                        maalform = målform
                ))
        )
    }

    private fun mapTilInnvilgetBrevFelter(vedtak: Vedtak, personopplysningGrunnlag: PersonopplysningGrunnlag): String {
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = vedtak.behandling.id)
        val forrigeTilkjentYtelse = beregningService.hentSisteTilkjentYtelseFørBehandling(behandling = vedtak.behandling)
        val beregningOversikt = TilkjentYtelseUtils.hentBeregningOversikt(
                tilkjentYtelseForBehandling = tilkjentYtelse,
                tilkjentYtelseForForrigeBehandling = forrigeTilkjentYtelse,
                personopplysningGrunnlag = personopplysningGrunnlag)
                .sortedBy { it.periodeFom }

        val (enhetNavn, målform) = hentMålformOgEnhetNavn(vedtak.behandling)

        return if (vedtak.behandling.skalBehandlesAutomatisk) {
            autovedtakBrevFelter(vedtak, personopplysningGrunnlag, beregningOversikt, enhetNavn)
        } else {
            manueltVedtakBrevFelter(vedtak, beregningOversikt, enhetNavn, målform)
        }
    }

    private fun manueltVedtakBrevFelter(vedtak: Vedtak,
                                        beregningOversikt: List<RestBeregningOversikt>,
                                        enhet: String,
                                        målform: Målform): String {
        val (saksbehandler, beslutter) = DokumentUtils.hentSaksbehandlerOgBeslutter(
                behandling = vedtak.behandling,
                totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(vedtak.behandling.id)
        )

        val etterbetalingsbeløp = økonomiService.hentEtterbetalingsbeløp(vedtak).etterbetaling.takeIf { it > 0 }
        val innvilget = Innvilget(
                enhet = enhet,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
                hjemler = VedtakUtils.hentHjemlerBruktIVedtak(vedtak),
                maalform = målform,
                etterbetalingsbelop = etterbetalingsbeløp?.run { Utils.formaterBeløp(this) } ?: "",
                erFeilutbetaling = tilbakekrevingsbeløpFraSimulering() > 0,
        )

        innvilget.duFaar = beregningOversikt
                .filter { it.endring.trengerBegrunnelse }
                .map { utbetalingsperiode ->
                    val barnasFødselsdatoer =
                            Utils.slåSammen(utbetalingsperiode.beregningDetaljer
                                                    .filter { restBeregningDetalj -> restBeregningDetalj.person.type == PersonType.BARN }
                                                    .sortedBy { restBeregningDetalj -> restBeregningDetalj.person.fødselsdato }
                                                    .map { restBeregningDetalj ->
                                                        restBeregningDetalj.person.fødselsdato?.tilKortString() ?: ""
                                                    })

                    val begrunnelser =
                            vedtak.utbetalingBegrunnelser
                                    .filter { it.fom == utbetalingsperiode.periodeFom && it.tom == utbetalingsperiode.periodeTom }
                                    .map {
                                        it.brevBegrunnelse?.lines() ?: listOf("Ikke satt")
                                    }
                                    .flatten()

                    DuFårSeksjon(
                            fom = utbetalingsperiode.periodeFom.tilDagMånedÅr(),
                            tom = if (!utbetalingsperiode.periodeTom.erSenereEnnNesteMåned()) utbetalingsperiode.periodeTom.tilDagMånedÅr() else "",
                            belop = Utils.formaterBeløp(utbetalingsperiode.utbetaltPerMnd),
                            antallBarn = utbetalingsperiode.antallBarn,
                            barnasFodselsdatoer = barnasFødselsdatoer,
                            begrunnelser = begrunnelser
                    )
                }

        return objectMapper.writeValueAsString(innvilget)
    }

    private fun autovedtakBrevFelter(vedtak: Vedtak,
                                     personopplysningGrunnlag: PersonopplysningGrunnlag,
                                     beregningOversikt: List<RestBeregningOversikt>,
                                     enhet: String): String {
        val barnaSortert = personopplysningGrunnlag.barna.sortedByDescending { it.fødselsdato }
        val etterbetalingsbeløp = økonomiService.hentEtterbetalingsbeløp(vedtak).etterbetaling.takeIf { it > 0 }

        val flettefelter = InnvilgetAutovedtak(navn = personopplysningGrunnlag.søker.navn,
                                               fodselsnummer = vedtak.behandling.fagsak.hentAktivIdent().ident,
                                               fodselsdato = Utils.slåSammen(barnaSortert.map { it.fødselsdato.tilKortString() }),
                                               belop = Utils.formaterBeløp(TilkjentYtelseUtils.beregnNåværendeBeløp(
                                                       beregningOversikt,
                                                       vedtak)),
                                               antallBarn = barnaSortert.size,
                                               virkningstidspunkt = barnaSortert.first().fødselsdato.plusMonths(1).tilMånedÅr(),
                                               enhet = enhet,
                                               etterbetalingsbelop = etterbetalingsbeløp?.run { Utils.formaterBeløp(this) })
        return objectMapper.writeValueAsString(flettefelter)
    }

    private fun tilbakekrevingsbeløpFraSimulering() = 0 //TODO Må legges inn senere når simulering er implementert.
    // Inntil da er det tryggest å utelate denne informasjonen fra brevet.

    private fun hentMålformOgEnhetNavn(behandling: Behandling): Pair<String, Målform> {
        return Pair(arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandling.id).behandlendeEnhetNavn,
                    persongrunnlagService.hentSøker(behandling)?.målform ?: Målform.NB)
    }

    companion object {

        fun malNavnForMedlemskapOgResultatType(resultatType: BehandlingResultatType,
                                               behandlingÅrsak: BehandlingÅrsak,
                                               behandlingType: BehandlingType): String {
            return if (behandlingÅrsak == BehandlingÅrsak.FØDSELSHENDELSE) {
                "${resultatType.brevMal}-autovedtak"
            } else {
                val malNavn = resultatType.brevMal
                when (behandlingType) {
                    BehandlingType.FØRSTEGANGSBEHANDLING -> malNavn
                    else -> "${malNavn}-${behandlingType.toString().toLowerCase()}"
                }
            }
        }
    }
}

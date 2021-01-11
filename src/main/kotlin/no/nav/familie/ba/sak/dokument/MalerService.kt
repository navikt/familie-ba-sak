package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.restDomene.Utbetalingsperiode
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakUtils
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelse
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
import java.time.LocalDate.now
import java.util.*

@Service
class MalerService(
        private val totrinnskontrollService: TotrinnskontrollService,
        private val beregningService: BeregningService,
        private val persongrunnlagService: PersongrunnlagService,
        private val arbeidsfordelingService: ArbeidsfordelingService,
        private val økonomiService: ØkonomiService
) {

    fun mapTilVedtakBrevfelter(vedtak: Vedtak, behandlingResultat: BehandlingResultat): MalMedData {

        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = vedtak.behandling.id)
                                       ?: throw Feil(message = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev",
                                                     frontendFeilmelding = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev")

        return MalMedData(
                mal = malNavnForMedlemskapOgResultatType(behandlingResultat,
                                                         vedtak.behandling.opprettetÅrsak,
                                                         vedtak.behandling.type),
                fletteFelter = when (behandlingResultat) {
                    BehandlingResultat.INNVILGET -> mapTilInnvilgetBrevFelter(vedtak, personopplysningGrunnlag)
                    BehandlingResultat.INNVILGET_OG_OPPHØRT -> mapTilInnvilgetBrevFelter(vedtak, personopplysningGrunnlag)
                    BehandlingResultat.OPPHØRT -> mapTilOpphørtBrevFelter(vedtak, personopplysningGrunnlag)
                    BehandlingResultat.FORTSATT_INNVILGET -> mapTilAutovedtakFortsattInnvilgetBrevFelter(vedtak,
                                                                                                         personopplysningGrunnlag)
                    else -> throw FunksjonellFeil(melding = "Brev ikke støttet for behandlingsresultat=$behandlingResultat",
                                                  frontendFeilmelding = "Brev ikke støttet for behandlingsresultat=$behandlingResultat")
                }
        )
    }

    fun mapTilManuellMalMedData(behandling: Behandling, manueltBrevRequest: ManueltBrevRequest): MalMedData {
        return when (manueltBrevRequest.brevmal) {
            BrevType.INNHENTE_OPPLYSNINGER -> mapTilInnhenteOpplysningerBrevfelter(behandling,
                                                                                   manueltBrevRequest)
            BrevType.VARSEL_OM_REVURDERING -> mapTilVarselOmRevurderingBrevfelter(behandling,
                                                                                  manueltBrevRequest)
            BrevType.HENLEGGE_TRUKKET_SØKNAD -> mapTilHenleggTrukketSoknadBrevfelter(behandling, manueltBrevRequest)
            else -> throw Feil(message = "Brevmal ${manueltBrevRequest.brevmal} er ikke støttet for manuelle brev.",
                               frontendFeilmelding = "Klarte ikke generere brev. Brevmal ${manueltBrevRequest.brevmal.malId} er ikke støttet.")
        }
    }

    fun mapTilVarselOmRevurderingBrevfelter(behandling: Behandling, manueltBrevRequest: ManueltBrevRequest): MalMedData {
        val (enhetNavn, målform) = hentMålformOgEnhetNavn(behandling)

        return MalMedData(
                mal = manueltBrevRequest.brevmal.malId,
                fletteFelter = objectMapper.writeValueAsString(VarselOmRevurdering(
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
                        enhet = enhetNavn,
                        dokumenter = manueltBrevRequest.multiselectVerdier,
                        saksbehandler = SikkerhetContext.hentSaksbehandlerNavn(),
                        maalform = målform
                ))
        )
    }

    fun mapTilHenleggTrukketSoknadBrevfelter(behandling: Behandling, manueltBrevRequest: ManueltBrevRequest): MalMedData {
        val (enhetNavn, målform) = hentMålformOgEnhetNavn(behandling)

        return MalMedData(
                mal = manueltBrevRequest.brevmal.malId,
                fletteFelter = objectMapper.writeValueAsString(Henleggelse(
                        enhet = enhetNavn,
                        saksbehandler = SikkerhetContext.hentSaksbehandlerNavn(),
                        maalform = målform
                ))
        )
    }

    private fun mapTilInnvilgetBrevFelter(vedtak: Vedtak, personopplysningGrunnlag: PersonopplysningGrunnlag): String {
        val utbetalingsperioder = finnUtbetalingsperioder(vedtak, personopplysningGrunnlag)

        val (enhetNavn, målform) = hentMålformOgEnhetNavn(vedtak.behandling)

        return if (vedtak.behandling.skalBehandlesAutomatisk) {
            autovedtakBrevFelter(vedtak, personopplysningGrunnlag, utbetalingsperioder, enhetNavn)
        } else {
            manueltVedtakBrevFelter(vedtak, utbetalingsperioder, enhetNavn, målform)
        }
    }

    private fun mapTilAutovedtakFortsattInnvilgetBrevFelter(vedtak: Vedtak,
                                                            personopplysningGrunnlag: PersonopplysningGrunnlag): String {
        val utbetalingsperiodeInneværendeMåned = finnUtbetalingsperiodeInneværendeMåned(vedtak, personopplysningGrunnlag)
        val (enhetNavn, målform) = hentMålformOgEnhetNavn(vedtak.behandling)

        return autovedtakFortsattInnvilgetBrevFelter(vedtak,
                                              utbetalingsperiodeInneværendeMåned,
                                              enhetNavn,
                                              målform)
    }

    private fun mapTilOpphørtBrevFelter(vedtak: Vedtak, personopplysningGrunnlag: PersonopplysningGrunnlag): String {
        val utbetalingsperioder = finnUtbetalingsperioder(vedtak, personopplysningGrunnlag)

        val (enhetNavn, målform) = hentMålformOgEnhetNavn(vedtak.behandling)
        return opphørtVedtakBrevFelter(vedtak, utbetalingsperioder, enhetNavn, målform)
    }

    private fun opphørtVedtakBrevFelter(vedtak: Vedtak,
                                        utbetalingsperioder: List<Utbetalingsperiode>,
                                        enhet: String,
                                        målform: Målform): String {
        val (saksbehandler, beslutter) = DokumentUtils.hentSaksbehandlerOgBeslutter(
                behandling = vedtak.behandling,
                totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(vedtak.behandling.id)
        )

        val begrunnelser =
                vedtak.utbetalingBegrunnelser
                        .map {
                            it.brevBegrunnelse?.lines() ?: listOf("Ikke satt")
                        }
                        .flatten()

        val opphørDato = utbetalingsperioder.maxByOrNull { it.periodeTom }?.periodeTom?.plusDays(1)
                         ?: throw Feil("Opphør mangler utbetalingsperioder.")

        val opphørt = Opphørt(
                enhet = enhet,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
                hjemler = VedtakUtils.hentHjemlerBruktIVedtak(vedtak),
                maalform = målform,
                opphor = Opphør(dato = opphørDato.tilDagMånedÅr(),
                                begrunnelser = begrunnelser)
        )

        return objectMapper.writeValueAsString(opphørt)
    }

    private fun finnUtbetalingsperioder(vedtak: Vedtak,
                                        personopplysningGrunnlag: PersonopplysningGrunnlag): List<Utbetalingsperiode> {

        val andelerTilkjentYtelse = beregningService.hentAndelerTilkjentYtelseForBehandling(behandlingId = vedtak.behandling.id)
        return TilkjentYtelseUtils.mapTilUtbetalingsperioder(
                andelerTilPersoner = andelerTilkjentYtelse,
                personopplysningGrunnlag = personopplysningGrunnlag)
                .sortedBy { it.periodeFom }
    }

    private fun finnUtbetalingsperiodeInneværendeMåned(vedtak: Vedtak,
                                                       personopplysningGrunnlag: PersonopplysningGrunnlag): Utbetalingsperiode =
            finnUtbetalingsperioder(vedtak,
                                    personopplysningGrunnlag).firstOrNull { it.periodeFom <= now() && it.periodeTom >= now() }
            ?: throw Error("Har ikke utbetalingsperiode i nåværende måned.")


    private fun manueltVedtakBrevFelter(vedtak: Vedtak,
                                        utbetalingsperioder: List<Utbetalingsperiode>,
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
                hjemler = hentHjemlerForInnvilgetVedtak(vedtak),
                maalform = målform,
                etterbetalingsbelop = etterbetalingsbeløp?.run { Utils.formaterBeløp(this) } ?: "",
                erFeilutbetaling = tilbakekrevingsbeløpFraSimulering() > 0,
        )

        innvilget.duFaar = utbetalingsperioder
                .fold(mutableListOf()) { acc, utbetalingsperiode ->
                    val barnasFødselsdatoer =
                            Utils.slåSammen(utbetalingsperiode.utbetalingsperiodeDetaljer
                                                    .filter { utbetalingsperiodeDetalj ->
                                                        utbetalingsperiodeDetalj.person.type == PersonType.BARN
                                                    }
                                                    .sortedBy { utbetalingsperiodeDetalj ->
                                                        utbetalingsperiodeDetalj.person.fødselsdato
                                                    }
                                                    .map { utbetalingsperiodeDetalj ->
                                                        utbetalingsperiodeDetalj.person.fødselsdato?.tilKortString() ?: ""
                                                    })

                    val begrunnelser =
                            vedtak.utbetalingBegrunnelser
                                    .filter { it.fom == utbetalingsperiode.periodeFom && it.tom == utbetalingsperiode.periodeTom }
                                    .map {
                                        it.brevBegrunnelse?.lines() ?: listOf("Ikke satt")
                                    }
                                    .flatten()

                    if (begrunnelser.isNotEmpty()) {
                        acc.add(DuFårSeksjon(
                                fom = utbetalingsperiode.periodeFom.tilDagMånedÅr(),
                                tom = if (!utbetalingsperiode.periodeTom.erSenereEnnInneværendeMåned())
                                    utbetalingsperiode.periodeTom.tilDagMånedÅr() else "",
                                belop = Utils.formaterBeløp(utbetalingsperiode.utbetaltPerMnd),
                                antallBarn = utbetalingsperiode.antallBarn,
                                barnasFodselsdatoer = barnasFødselsdatoer,
                                begrunnelser = begrunnelser
                        ))
                    }
                    acc
                }

        return objectMapper.writeValueAsString(innvilget)
    }

    private fun autovedtakFortsattInnvilgetBrevFelter(vedtak: Vedtak,
                                                      utbetalingsperiode: Utbetalingsperiode,
                                                      enhet: String,
                                                      målform: Målform): String {
        val (saksbehandler, beslutter) = DokumentUtils.hentSaksbehandlerOgBeslutter(
                behandling = vedtak.behandling,
                totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(vedtak.behandling.id)
        )

        val innvilget = Innvilget(
                enhet = enhet,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
                hjemler = hentHjemlerForInnvilgetVedtak(vedtak),
                maalform = målform,
                erFeilutbetaling = tilbakekrevingsbeløpFraSimulering() > 0,
        )

        val barnasFødselsdatoer =
                Utils.slåSammen(utbetalingsperiode.utbetalingsperiodeDetaljer
                                        .filter { utbetalingsperiodeDetalj ->
                                            utbetalingsperiodeDetalj.person.type == PersonType.BARN
                                        }
                                        .sortedBy { utbetalingsperiodeDetalj ->
                                            utbetalingsperiodeDetalj.person.fødselsdato
                                        }
                                        .map { utbetalingsperiodeDetalj ->
                                            utbetalingsperiodeDetalj.person.fødselsdato?.tilKortString() ?: ""
                                        })

        val begrunnelser =
                vedtak.utbetalingBegrunnelser
                        .filter { it.fom == utbetalingsperiode.periodeFom && it.tom == utbetalingsperiode.periodeTom }
                        .map {
                            it.brevBegrunnelse?.lines() ?: listOf("Ikke satt")
                        }
                        .flatten()

        innvilget.duFaar = listOf(
                DuFårSeksjon(fom = utbetalingsperiode.periodeFom.tilDagMånedÅr(),
                             tom = "",
                             belop = Utils.formaterBeløp(utbetalingsperiode.utbetaltPerMnd),
                             antallBarn = utbetalingsperiode.antallBarn,
                             barnasFodselsdatoer = barnasFødselsdatoer,
                             begrunnelser = begrunnelser

                             //vedtakBegrunnelse.hentBeskrivelse(barnasFødselsdatoer = barnasFødselsdatoer,
                             //                                                 målform = målform).lines())
        ))

        return objectMapper.writeValueAsString(innvilget)
    }

    private fun hentHjemlerForInnvilgetVedtak(vedtak: Vedtak): SortedSet<Int> =
            when (vedtak.behandling.opprettetÅrsak) {
                BehandlingÅrsak.OMREGNING_18ÅR -> VedtakBegrunnelse.REDUKSJON_UNDER_18_ÅR.hentHjemler().toSortedSet()
                BehandlingÅrsak.OMREGNING_6ÅR -> VedtakBegrunnelse.REDUKSJON_UNDER_6_ÅR.hentHjemler().toSortedSet()
                else -> VedtakUtils.hentHjemlerBruktIVedtak(vedtak)
            }

    private fun autovedtakBrevFelter(vedtak: Vedtak,
                                     personopplysningGrunnlag: PersonopplysningGrunnlag,
                                     utbetalingsperioder: List<Utbetalingsperiode>,
                                     enhet: String): String {
        val barnaSortert = personopplysningGrunnlag.barna.sortedByDescending { it.fødselsdato }
        val etterbetalingsbeløp = økonomiService.hentEtterbetalingsbeløp(vedtak).etterbetaling.takeIf { it > 0 }

        val flettefelter = InnvilgetAutovedtak(navn = personopplysningGrunnlag.søker.navn,
                                               fodselsnummer = vedtak.behandling.fagsak.hentAktivIdent().ident,
                                               fodselsdato = Utils.slåSammen(barnaSortert.map { it.fødselsdato.tilKortString() }),
                                               belop = Utils.formaterBeløp(TilkjentYtelseUtils.beregnNåværendeBeløp(
                                                       utbetalingsperioder,
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
                    persongrunnlagService.hentSøker(behandling.id)?.målform ?: Målform.NB)
    }

    companion object {

        fun malNavnForMedlemskapOgResultatType(behandlingResultat: BehandlingResultat,
                                               behandlingÅrsak: BehandlingÅrsak,
                                               behandlingType: BehandlingType): String {

            return if (behandlingÅrsak == BehandlingÅrsak.FØDSELSHENDELSE) {
                "${behandlingResultat.brevMal}-autovedtak"
            } else {
                val malNavn = behandlingResultat.brevMal
                when (behandlingType) {
                    BehandlingType.FØRSTEGANGSBEHANDLING -> malNavn
                    else -> "${malNavn}-${behandlingType.toString().toLowerCase()}"
                }
            }
        }
    }
}

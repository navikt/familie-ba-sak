package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat.DELVIS_INNVILGET
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat.ENDRET_OG_FORTSATT_INNVILGET
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat.ENDRET_OG_OPPHØRT
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat.FORTSATT_INNVILGET
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat.INNVILGET
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat.INNVILGET_OG_OPPHØRT
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat.OPPHØRT
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.restDomene.Utbetalingsperiode
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakUtils
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseType
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.TilkjentYtelseUtils
import no.nav.familie.ba.sak.brev.domene.maler.BrevPeriode
import no.nav.familie.ba.sak.brev.domene.maler.Førstegangsvedtak
import no.nav.familie.ba.sak.brev.domene.maler.PeriodeType
import no.nav.familie.ba.sak.brev.domene.maler.VedtakEndring
import no.nav.familie.ba.sak.brev.domene.maler.Vedtaksbrev
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.erSenereEnnInneværendeMåned
import no.nav.familie.ba.sak.common.erSenereEnnPåfølgendeDag
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.dokument.DokumentController.ManueltBrevRequest
import no.nav.familie.ba.sak.dokument.domene.BrevType
import no.nav.familie.ba.sak.dokument.domene.MalMedData
import no.nav.familie.ba.sak.dokument.domene.maler.DuFårSeksjon
import no.nav.familie.ba.sak.dokument.domene.maler.Henleggelse
import no.nav.familie.ba.sak.dokument.domene.maler.InnhenteOpplysninger
import no.nav.familie.ba.sak.dokument.domene.maler.Innvilget
import no.nav.familie.ba.sak.dokument.domene.maler.InnvilgetAutovedtak
import no.nav.familie.ba.sak.dokument.domene.maler.Opphør
import no.nav.familie.ba.sak.dokument.domene.maler.Opphørt
import no.nav.familie.ba.sak.dokument.domene.maler.VarselOmRevurdering
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.stereotype.Service
import java.time.LocalDate
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

    fun mapTilNyttVedtaksbrev(vedtak: Vedtak, behandlingResultat: BehandlingResultat): Vedtaksbrev {
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = vedtak.behandling.id)
                                       ?: throw Feil(message = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev",
                                                     frontendFeilmelding = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev")
        return when (vedtak.behandling.type) {
            BehandlingType.FØRSTEGANGSBEHANDLING ->
                mapTilFørstegangsvedtak(behandlingResultat, vedtak, personopplysningGrunnlag)
            BehandlingType.REVURDERING ->
                mapTilRevurdering(behandlingResultat, vedtak, personopplysningGrunnlag)
            else -> throw FunksjonellFeil(melding = "Brev ikke støttet for behandlingstype=${vedtak.behandling.type}",
                                          frontendFeilmelding = "Brev ikke støttet for behandlingstype=${vedtak.behandling.type}")
        }

    }

    private fun mapTilRevurdering(behandlingResultat: BehandlingResultat,
                                  vedtak: Vedtak,
                                  personopplysningGrunnlag: PersonopplysningGrunnlag): Vedtaksbrev {
        return when (behandlingResultat) {

            INNVILGET, DELVIS_INNVILGET, ENDRET_OG_FORTSATT_INNVILGET -> if (vedtak.behandling.skalBehandlesAutomatisk) {
                throw Feil("Det er ikke laget funksjonalitet for automatisk revurdering med ny brevløsning.")
            } else {
                mapTilManueltRevurderingVedtakEndring(vedtak, personopplysningGrunnlag)
            }

            OPPHØRT -> throw throw Feil("Det er ikke laget funksjonalitet revurdering med ny brevløsning.")

            // TODO: "Delvis innvilget og opphørt" skal inn her når det blir en behandlingResultat-type
            INNVILGET_OG_OPPHØRT, ENDRET_OG_OPPHØRT -> throw Feil("Det er ikke laget funksjonalitet revurdering med ny brevløsning.")
            else -> throw FunksjonellFeil(melding = "Brev ikke støttet for behandlingstype=${vedtak.behandling.type} og behandlingsresultat=${behandlingResultat}",
                                          frontendFeilmelding = "Brev ikke støttet for behandlingstype=${vedtak.behandling.type} og behandlingsresultat=${behandlingResultat}")
        }
    }

    private fun mapTilFørstegangsvedtak(behandlingResultat: BehandlingResultat,
                                        vedtak: Vedtak,
                                        personopplysningGrunnlag: PersonopplysningGrunnlag): Førstegangsvedtak {
        return when (behandlingResultat) {
            INNVILGET, INNVILGET_OG_OPPHØRT, DELVIS_INNVILGET -> if (vedtak.behandling.skalBehandlesAutomatisk) {
                throw Feil("Det er ikke laget funksjonalitet for automatisk behandlet førstegangsvedtak med ny brevløsning.")
            } else {
                mapTilManueltFørstegangsvedtak(vedtak, personopplysningGrunnlag)
            }
            else -> throw FunksjonellFeil(melding = "Brev ikke støttet for behandlingstype=${vedtak.behandling.type} og behandlingsresultat=${behandlingResultat}",
                                          frontendFeilmelding = "Brev ikke støttet for behandlingstype=${vedtak.behandling.type} og behandlingsresultat=${behandlingResultat}")
        }
    }

    @Deprecated("Gammel løsning fra dokgen")
    fun mapTilVedtakBrevfelter(vedtak: Vedtak, behandlingResultat: BehandlingResultat): MalMedData {

        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = vedtak.behandling.id)
                                       ?: throw Feil(message = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev",
                                                     frontendFeilmelding = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev")

        return MalMedData(
                mal = malNavnForMedlemskapOgResultatType(behandlingResultat,
                                                         vedtak.behandling.opprettetÅrsak,
                                                         vedtak.behandling.type),
                fletteFelter = when (behandlingResultat) {
                    INNVILGET -> mapTilInnvilgetBrevFelter(vedtak, personopplysningGrunnlag)
                    INNVILGET_OG_OPPHØRT -> mapTilInnvilgetBrevFelter(vedtak, personopplysningGrunnlag)
                    ENDRET_OG_OPPHØRT -> mapTilEndretOgOpphørtBrevFelter(vedtak, personopplysningGrunnlag)
                    ENDRET_OG_FORTSATT_INNVILGET -> mapTilInnvilgetBrevFelter(vedtak, personopplysningGrunnlag)
                    OPPHØRT -> mapTilOpphørtBrevFelter(vedtak, personopplysningGrunnlag)
                    FORTSATT_INNVILGET -> mapTilAutovedtakFortsattInnvilgetBrevFelter(vedtak, personopplysningGrunnlag)
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
        val (enhetNavn, målform) = hentEnhetnavnOgMålform(behandling)

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
        val (enhetNavn, målform) = hentEnhetnavnOgMålform(behandling)

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
        val (enhetNavn, målform) = hentEnhetnavnOgMålform(behandling)

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

        val (enhetNavn, målform) = hentEnhetnavnOgMålform(vedtak.behandling)

        return if (vedtak.behandling.skalBehandlesAutomatisk) {
            autovedtakBrevFelter(vedtak, personopplysningGrunnlag, utbetalingsperioder, enhetNavn)
        } else {
            manueltVedtakBrevFelter(vedtak, utbetalingsperioder, enhetNavn, målform)
        }
    }

    private fun mapTilAutovedtakFortsattInnvilgetBrevFelter(vedtak: Vedtak,
                                                            personopplysningGrunnlag: PersonopplysningGrunnlag): String {
        val utbetalingsperiodeInneværendeMåned = finnUtbetalingsperiodeInneværendeMåned(vedtak, personopplysningGrunnlag)
        val (enhetNavn, målform) = hentEnhetnavnOgMålform(vedtak.behandling)

        return autovedtakFortsattInnvilgetBrevFelter(vedtak,
                                                     utbetalingsperiodeInneværendeMåned,
                                                     enhetNavn,
                                                     målform)
    }

    private fun mapTilOpphørtBrevFelter(vedtak: Vedtak, personopplysningGrunnlag: PersonopplysningGrunnlag): String {
        val utbetalingsperioder = finnUtbetalingsperioder(vedtak, personopplysningGrunnlag)

        val (enhetNavn, målform) = hentEnhetnavnOgMålform(vedtak.behandling)
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
                vedtak.vedtakBegrunnelser
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

    private fun mapTilEndretOgOpphørtBrevFelter(vedtak: Vedtak, personopplysningGrunnlag: PersonopplysningGrunnlag): String {
        val utbetalingsperioder = finnUtbetalingsperioder(vedtak, personopplysningGrunnlag)

        val (enhetNavn, målform) = hentEnhetnavnOgMålform(vedtak.behandling)
        return manueltVedtakBrevFelter(vedtak, utbetalingsperioder, enhetNavn, målform)
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
                hjemler = hentHjemlerForVedtak(vedtak),
                maalform = målform,
                etterbetalingsbelop = etterbetalingsbeløp?.run { Utils.formaterBeløp(this) } ?: "",
                erFeilutbetaling = tilbakekrevingsbeløpFraSimulering() > 0,
                erKlage = vedtak.behandling.erKlage()
        )

        innvilget.duFaar = hentVedtaksperioder(utbetalingsperioder, vedtak).reversed()

        return objectMapper.writeValueAsString(innvilget)
    }

    private fun hentVedtaksperioder(utbetalingsperioder: List<Utbetalingsperiode>,
                                    vedtak: Vedtak): MutableList<DuFårSeksjon> =
            utbetalingsperioder
                    .foldRightIndexed(mutableListOf()) { idx, utbetalingsperiode, acc ->
                        /* Temporær løsning for å støtte begrunnelse av perioder som er opphørt eller avslått.
                    * Begrunnelsen settes på den tidligere (før den opphøret- eller avslåtteperioden) innvilgte perioden.
                    */
                        val nesteUtbetalingsperiodeFom = if (idx < utbetalingsperioder.lastIndex) {
                            utbetalingsperioder[idx + 1].periodeFom
                        } else {
                            null
                        }

                        val begrunnelserOpphør =
                                filtrerBegrunnelserForPeriodeOgVedtaksType(vedtak,
                                                                           utbetalingsperiode,
                                                                           listOf(VedtakBegrunnelseType.OPPHØR))

                        if (etterfølgesAvOpphørtEllerAvslåttPeriode(nesteUtbetalingsperiodeFom, utbetalingsperiode.periodeTom) &&
                            begrunnelserOpphør.isNotEmpty())

                            acc.add(DuFårSeksjon(
                                    fom = utbetalingsperiode.periodeTom.plusDays(1).tilDagMånedÅr(),
                                    tom = if (nesteUtbetalingsperiodeFom != null) nesteUtbetalingsperiodeFom.minusDays(1)
                                            .tilDagMånedÅr() else "",
                                    belop = "0",
                                    antallBarn = 0,
                                    barnasFodselsdatoer = "",
                                    begrunnelser = begrunnelserOpphør,
                                    begrunnelseType = VedtakBegrunnelseType.OPPHØR.name
                            ))
                        /* Slutt temporær løsning */

                        val barnasFødselsdatoer = finnAlleBarnsFødselsDatoerForPerioden(utbetalingsperiode)

                        val begrunnelser =
                                filtrerBegrunnelserForPeriodeOgVedtaksType(vedtak, utbetalingsperiode,
                                                                           listOf(VedtakBegrunnelseType.INNVILGELSE,
                                                                                  VedtakBegrunnelseType.REDUKSJON))

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
                hjemler = hentHjemlerForVedtak(vedtak),
                maalform = målform,
                erFeilutbetaling = tilbakekrevingsbeløpFraSimulering() > 0,
        )

        val barnasFødselsdatoer = finnAlleBarnsFødselsDatoerForPerioden(utbetalingsperiode)

        val begrunnelser = filtrerBegrunnelserForPeriodeOgVedtaksType(vedtak, utbetalingsperiode,
                                                                      listOf(VedtakBegrunnelseType.INNVILGELSE,
                                                                             VedtakBegrunnelseType.REDUKSJON))


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

    private fun finnAlleBarnsFødselsDatoerForPerioden(utbetalingsperiode: Utbetalingsperiode) =
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

    private fun hentHjemlerForVedtak(vedtak: Vedtak): SortedSet<Int> =
            when (vedtak.behandling.opprettetÅrsak) {
                BehandlingÅrsak.OMREGNING_18ÅR -> VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_18_ÅR.hentHjemler().toSortedSet()
                BehandlingÅrsak.OMREGNING_6ÅR -> VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR.hentHjemler().toSortedSet()
                else -> VedtakUtils.hentHjemlerBruktIVedtak(vedtak)
            }

    private fun hentHjemlerTekstForVedtak(vedtak: Vedtak): String {
        val hjemmelArray = hentHjemlerForVedtak(vedtak).toIntArray().map { it.toString() }

        return when (hjemmelArray.size) {
            0 -> throw Feil("Fikk ikke med noen hjemler for vedtak")
            1 -> "§ ${hjemmelArray[0]}"
            else -> "§§ ${Utils.slåSammen(hjemmelArray)}"
        }
    }

    private fun etterfølgesAvOpphørtEllerAvslåttPeriode(nesteUtbetalingsperiodeFom: LocalDate?,
                                                        utbetalingsperiodeTom: LocalDate) =
            nesteUtbetalingsperiodeFom == null ||
            nesteUtbetalingsperiodeFom.erSenereEnnPåfølgendeDag(utbetalingsperiodeTom)

    private fun filtrerBegrunnelserForPeriodeOgVedtaksType(vedtak: Vedtak,
                                                           utbetalingsperiode: Utbetalingsperiode,
                                                           vedtakBegrunnelseTyper: List<VedtakBegrunnelseType>) =
            vedtak.vedtakBegrunnelser
                    .filter { it.fom == utbetalingsperiode.periodeFom && it.tom == utbetalingsperiode.periodeTom }
                    .filter { vedtakBegrunnelseTyper.contains(it.begrunnelse?.vedtakBegrunnelseType) }
                    .map {
                        it.brevBegrunnelse?.lines() ?: listOf("Ikke satt")
                    }
                    .flatten()

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

    private fun hentEnhetnavnOgMålform(behandling: Behandling): Pair<String, Målform> {
        return Pair(arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandling.id).behandlendeEnhetNavn,
                    persongrunnlagService.hentSøker(behandling.id)?.målform ?: Målform.NB)
    }

    private fun mapTilManueltFørstegangsvedtak(vedtak: Vedtak,
                                               personopplysningGrunnlag: PersonopplysningGrunnlag): Førstegangsvedtak {
        val enhet = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(vedtak.behandling.id).behandlendeEnhetNavn


        val (saksbehandler, beslutter) = DokumentUtils.hentSaksbehandlerOgBeslutter(
                behandling = vedtak.behandling,
                totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(vedtak.behandling.id)
        )

        val etterbetalingsbeløp =
                økonomiService.hentEtterbetalingsbeløp(vedtak).etterbetaling.takeIf { it > 0 }?.run { Utils.formaterBeløp(this) }

        val utbetalingsperioder = finnUtbetalingsperioder(vedtak, personopplysningGrunnlag)

        return Førstegangsvedtak(
                enhet = enhet,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
                etterbetalingsbeløp = etterbetalingsbeløp,
                hjemlter = hentHjemlerTekstForVedtak(vedtak),
                søkerNavn = personopplysningGrunnlag.søker.navn,
                søkerFødselsnummer = personopplysningGrunnlag.søker.personIdent.ident,
                perioder = hentNyBrevløsningVedtaksperioder(utbetalingsperioder, vedtak).reversed(),
        )
    }

    private fun mapTilManueltRevurderingVedtakEndring(vedtak: Vedtak,
                                                      personopplysningGrunnlag: PersonopplysningGrunnlag): VedtakEndring {
        val enhet = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(vedtak.behandling.id).behandlendeEnhetNavn

        val (saksbehandler, beslutter) = DokumentUtils.hentSaksbehandlerOgBeslutter(
                behandling = vedtak.behandling,
                totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(vedtak.behandling.id)
        )

        val etterbetalingsbeløp =
                økonomiService.hentEtterbetalingsbeløp(vedtak).etterbetaling.takeIf { it > 0 }?.run { Utils.formaterBeløp(this) }

        val utbetalingsperioder = finnUtbetalingsperioder(vedtak, personopplysningGrunnlag)

        return VedtakEndring(
                enhet = enhet,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
                etterbetalingsbeløp = etterbetalingsbeløp,
                hjemlter = hentHjemlerTekstForVedtak(vedtak),
                søkerNavn = personopplysningGrunnlag.søker.navn,
                søkerFødselsnummer = personopplysningGrunnlag.søker.personIdent.ident,
                perioder = hentNyBrevløsningVedtaksperioder(utbetalingsperioder, vedtak).reversed(),
                klage = false,
                feilutbetaling = false
        )
    }

    private fun hentNyBrevløsningVedtaksperioder(utbetalingsperioder: List<Utbetalingsperiode>,
                                                 vedtak: Vedtak): List<BrevPeriode> {

        return utbetalingsperioder
                .foldRightIndexed(mutableListOf<BrevPeriode>()) { idx, utbetalingsperiode, acc ->
                    /* Temporær løsning for å støtte begrunnelse av perioder som er opphørt eller avslått.
                * Begrunnelsen settes på den tidligere (før den opphøret- eller avslåtteperioden) innvilgte perioden.
                */
                    val nesteUtbetalingsperiodeFom = if (idx < utbetalingsperioder.lastIndex) {
                        utbetalingsperioder[idx + 1].periodeFom
                    } else {
                        null
                    }

                    val begrunnelserOpphør =
                            filtrerBegrunnelserForPeriodeOgVedtaksType(vedtak,
                                                                       utbetalingsperiode,
                                                                       listOf(VedtakBegrunnelseType.OPPHØR))

                    if (etterfølgesAvOpphørtEllerAvslåttPeriode(nesteUtbetalingsperiodeFom, utbetalingsperiode.periodeTom) &&
                        begrunnelserOpphør.isNotEmpty())

                        acc.add(BrevPeriode(
                                fom = utbetalingsperiode.periodeTom.plusDays(1).tilDagMånedÅr(),
                                tom = nesteUtbetalingsperiodeFom?.minusDays(1)?.tilDagMånedÅr(),
                                belop = "0",
                                antallBarn = "0",
                                barnasFodselsdager = "",
                                begrunnelser = begrunnelserOpphør,
                                type = PeriodeType.OPPHOR
                        ))
                    /* Slutt temporær løsning */

                    val barnasFødselsdatoer = finnAlleBarnsFødselsDatoerForPerioden(utbetalingsperiode)

                    val begrunnelser =
                            filtrerBegrunnelserForPeriodeOgVedtaksType(vedtak, utbetalingsperiode,
                                                                       listOf(VedtakBegrunnelseType.INNVILGELSE,
                                                                              VedtakBegrunnelseType.REDUKSJON))

                    if (begrunnelser.isNotEmpty()) {
                        acc.add(BrevPeriode(
                                fom = utbetalingsperiode.periodeFom.tilDagMånedÅr(),
                                tom = if (!utbetalingsperiode.periodeTom.erSenereEnnInneværendeMåned())
                                    utbetalingsperiode.periodeTom.tilDagMånedÅr() else null,
                                belop = Utils.formaterBeløp(utbetalingsperiode.utbetaltPerMnd),
                                antallBarn = utbetalingsperiode.antallBarn.toString(),
                                barnasFodselsdager = barnasFødselsdatoer,
                                begrunnelser = begrunnelser,
                                // TODO: Hvilken vedtakstype skal egentlig inn her? 🤔
                                type = PeriodeType.INNVILGELSE
                        ))
                    }

                    acc
                }
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
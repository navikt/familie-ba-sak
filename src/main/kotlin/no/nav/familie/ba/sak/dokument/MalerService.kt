package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.*
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.RestBeregningOversikt
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.vilkår.finnNåværendeMedlemskap
import no.nav.familie.ba.sak.behandling.vilkår.finnSterkesteMedlemskap
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.TilkjentYtelseUtils
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.dokument.DokumentController.ManueltBrevRequest
import no.nav.familie.ba.sak.dokument.domene.MalMedData
import no.nav.familie.ba.sak.dokument.domene.maler.DuFårSeksjon
import no.nav.familie.ba.sak.dokument.domene.maler.InnhenteOpplysninger
import no.nav.familie.ba.sak.dokument.domene.maler.Innvilget
import no.nav.familie.ba.sak.dokument.domene.maler.InnvilgetAutovedtak
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.stereotype.Service

@Service
class MalerService(
        private val totrinnskontrollService: TotrinnskontrollService,
        private val beregningService: BeregningService,
        private val persongrunnlagService: PersongrunnlagService,
        private val arbeidsfordelingService: ArbeidsfordelingService,
        private val søknadGrunnlagService: SøknadGrunnlagService,
) {

    fun mapTilVedtakBrevfelter(vedtak: Vedtak,
                               behandlingResultatType: BehandlingResultatType): MalMedData {

        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = vedtak.behandling.id)
                                       ?: throw Feil(message = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev",
                                                     frontendFeilmelding = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev")

        return MalMedData(
                mal = malNavnForMedlemskapOgResultatType(behandlingResultatType,
                                                         vedtak.behandling.opprinnelse,
                                                         vedtak.behandling.type),
                fletteFelter = when (behandlingResultatType) {
                    BehandlingResultatType.INNVILGET -> mapTilInnvilgetBrevFelter(vedtak, personopplysningGrunnlag)
                    BehandlingResultatType.AVSLÅTT -> mapTilAvslagBrevFelter(vedtak)
                    BehandlingResultatType.OPPHØRT -> mapTilOpphørtBrevFelter(vedtak)
                    else -> error("Invalid/unsupported behandling resultat type")
                }
        )
    }

    fun mapTilInnhenteOpplysningerBrevfelter(behandling: Behandling, manueltBrevRequest: ManueltBrevRequest): MalMedData {
        val enhetNavn = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandling.id).behandlendeEnhetNavn
        val søknadsDato = søknadGrunnlagService.hentAktiv(behandlingId = behandling.id)?.opprettetTidspunkt
                          ?: error("Finner ikke et aktivt søknadsgrunnlag ved sending av manuelt brev.")

        val felter = objectMapper.writeValueAsString(InnhenteOpplysninger(
                soknadDato = søknadsDato.toLocalDate().tilDagMånedÅr().toString(),
                fritekst = manueltBrevRequest.fritekst,
                enhet = enhetNavn,
                saksbehandler = SikkerhetContext.hentSaksbehandlerNavn()
        ))
        return MalMedData(
                mal = "innhente-opplysninger",
                fletteFelter = felter
        )
    }

    private fun mapTilOpphørtBrevFelter(vedtak: Vedtak): String {
        val behandling = vedtak.behandling
        return "{\"fodselsnummer\": \"${behandling.fagsak.hentAktivIdent().ident}\",\n" +
               "\"navn\": \"No Name\",\n" +
               "\"tdato\": \"01.01.01\",\n" +
               "\"hjemmel\": \"\",\n" +
               "\"fritekst\": \"${""}\"}" //TODO: Begrunnelse her
    }

    private fun mapTilInnvilgetBrevFelter(vedtak: Vedtak, personopplysningGrunnlag: PersonopplysningGrunnlag): String {
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = vedtak.behandling.id)
        val forrigeTilkjentYtelse = beregningService.hentSisteTilkjentYtelseFørBehandling(behandling = vedtak.behandling)
        val beregningOversikt = TilkjentYtelseUtils.hentBeregningOversikt(
                tilkjentYtelseForBehandling = tilkjentYtelse,
                tilkjentYtelseForForrigeBehandling = forrigeTilkjentYtelse,
                personopplysningGrunnlag = personopplysningGrunnlag)
                .sortedBy { it.periodeFom }

        val enhetNavn = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(vedtak.behandling.id).behandlendeEnhetNavn

        return if (vedtak.behandling.opprinnelse == BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE) {
            autovedtakBrevFelter(vedtak, personopplysningGrunnlag, beregningOversikt, enhetNavn)
        } else {
            val målform = personopplysningGrunnlag.søker.målform
            manueltVedtakBrevFelter(vedtak, beregningOversikt, enhetNavn, målform)
        }
    }

    private fun manueltVedtakBrevFelter(vedtak: Vedtak,
                                        beregningOversikt: List<RestBeregningOversikt>,
                                        enhet: String,
                                        målform: Målform): String {
        val totrinnskontroll = totrinnskontrollService.opprettEllerHentTotrinnskontroll(vedtak.behandling)

        val innvilget = Innvilget(
                enhet = enhet,
                saksbehandler = totrinnskontroll.saksbehandler,
                beslutter = totrinnskontroll.beslutter
                            ?: totrinnskontroll.saksbehandler,
                hjemmel = Utils.slåSammen(listOf("§§ 2", "4", "11")),
                maalform = målform.toString()
        )

        innvilget.duFaar = beregningOversikt
                .filter { it.endring.trengerBegrunnelse }
                .map {
                    val barnasFødselsdatoer =
                            Utils.slåSammen(it.beregningDetaljer
                                                    .filter { restBeregningDetalj -> restBeregningDetalj.person.type == PersonType.BARN }
                                                    .sortedBy { restBeregningDetalj -> restBeregningDetalj.person.fødselsdato }
                                                    .map { restBeregningDetalj ->
                                                        restBeregningDetalj.person.fødselsdato?.tilKortString() ?: ""
                                                    })

                    val begrunnelse =
                            vedtak.utbetalingBegrunnelser.filter { stønadBrevBegrunnelse ->
                                stønadBrevBegrunnelse.fom == it.periodeFom && stønadBrevBegrunnelse.tom == it.periodeTom
                            }.toMutableSet().map { utbetalingBegrunnelse ->
                                utbetalingBegrunnelse.brevBegrunnelse
                                ?: "Ikke satt"
                            }.toList()

                    DuFårSeksjon(
                            fom = it.periodeFom.tilMånedÅr(),
                            tom = if (!it.periodeTom.erSenereEnnNesteMåned()) it.periodeTom.tilMånedÅr() else "",
                            belop = Utils.formaterBeløp(it.utbetaltPerMnd),
                            antallBarn = it.antallBarn,
                            barnasFodselsdatoer = barnasFødselsdatoer,
                            begrunnelser = begrunnelse
                    )
                }

        return objectMapper.writeValueAsString(innvilget)
    }

    private fun autovedtakBrevFelter(vedtak: Vedtak,
                                     personopplysningGrunnlag: PersonopplysningGrunnlag,
                                     beregningOversikt: List<RestBeregningOversikt>,
                                     enhet: String): String {
        val barnaSortert = personopplysningGrunnlag.barna.sortedByDescending { it.fødselsdato }
        val etterbetalingsbeløp = etterbetalingsbeløpFraSimulering().takeIf { it > 0 }
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

    private fun etterbetalingsbeløpFraSimulering() = 0 //TODO Må legges inn senere når simulering er implementert.
    // Inntil da er det tryggest å utelate denne informasjonen fra brevet.

    private fun mapTilAvslagBrevFelter(vedtak: Vedtak): String {
        val behandling = vedtak.behandling

        //TODO: sett navn, hjemmel og fritekst
        return "{\"fodselsnummer\": \"${behandling.fagsak.hentAktivIdent().ident}\",\n" +
               "\"navn\": \"No Name\",\n" +
               "\"hjemmel\": \"\",\n" +
               "\"fritekst\": \"${""}\"}" //TODO: Begrunnelse her
    }

    companion object {

        fun malNavnForMedlemskapOgResultatType(resultatType: BehandlingResultatType,
                                               behandlingOpprinnelse: BehandlingOpprinnelse = BehandlingOpprinnelse.MANUELL,
                                               behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING): String {
            return if (behandlingOpprinnelse == BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE) {
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

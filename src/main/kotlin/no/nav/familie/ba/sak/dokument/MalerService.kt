package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadDTO
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.TypeSøker
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vilkår.VilkårService
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.beregnUtbetalingsperioderUtenKlassifisering
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.dokument.domene.MalMedData
import no.nav.familie.ba.sak.dokument.domene.maler.Innvilget
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class MalerService(
        private val vilkårService: VilkårService,
        private val beregningService: BeregningService,
        private val persongrunnlagService: PersongrunnlagService
) {

    fun mapTilBrevfelter(vedtak: Vedtak,
                         søknad: SøknadDTO?,
                         behandlingResultatType: BehandlingResultatType): MalMedData {

        return MalMedData(
                mal = "${behandlingResultatType.brevMal}${if (søknad?.typeSøker == TypeSøker.TREDJELANDSBORGER) "-Tredjelandsborger" else ""}",
                fletteFelter = when (behandlingResultatType) {
                    BehandlingResultatType.INNVILGET -> mapTilInnvilgetBrevFelter(vedtak)
                    BehandlingResultatType.AVSLÅTT -> mapTilAvslagBrevFelter(vedtak)
                    BehandlingResultatType.OPPHØRT -> mapTilOpphørtBrevFelter(vedtak)
                    else -> error("Invalid/unsupported behandling resultat type")
                }
        )
    }

    private fun mapTilOpphørtBrevFelter(vedtak: Vedtak): String {
        val behandling = vedtak.behandling
        return "{\"fodselsnummer\": \"${behandling.fagsak.personIdent.ident}\",\n" +
               "\"navn\": \"No Name\",\n" +
               "\"tdato\": \"01.01.01\",\n" +
               "\"hjemmel\": \"\",\n" +
               "\"fritekst\": \"${""}\"}" //TODO: Begrunnelse her
    }

    private fun mapTilInnvilgetBrevFelter(vedtak: Vedtak): String {
        val behandling = vedtak.behandling
        val barna = persongrunnlagService.hentBarna(behandling)

        val andelTilkjentYtelse = beregningService.hentAndelerTilkjentYtelseForBehandling(behandling.id)
        val utbetalingsperioder = beregnUtbetalingsperioderUtenKlassifisering(andelTilkjentYtelse.toSet())
        val beløp = utbetalingsperioder.toSegments()
                            .find { segment -> segment.fom <= LocalDate.now() && segment.tom >= LocalDate.now() }?.value
                    ?: utbetalingsperioder.toSegments().first().value
                    ?: error("Finner ikke beløp for vedtaksbrev")

        val vilkårsdato = vilkårService.hentVilkårsdato(behandling = behandling)
                          ?: error("Finner ikke vilkårsdato for vedtaksbrev")

        val barnasFødselsdatoer = Utils.slåSammen(barna.map { it.fødselsdato.tilKortString() })

        val innvilget = Innvilget(
                enhet = "enhet",
                saksbehandler = vedtak.ansvarligSaksbehandler,
                beslutter = vedtak.ansvarligBeslutter
                            ?: SikkerhetContext.hentSaksbehandlerNavn(),
                barnasFodselsdatoer = barnasFødselsdatoer,
                virkningsdato = utbetalingsperioder.minLocalDate.tilMånedÅr(),
                vilkårsdato = vilkårsdato.tilKortString(),
                vedtaksdato = vedtak.vedtaksdato.tilKortString(),
                belop = beløp,
                antallBarn = barna.size,
                flereBarn = barna.size > 1,
                hjemmel = Utils.slåSammen(listOf("§2", "§4", "§11"))
        )

        return innvilget.toString()
    }

    private fun mapTilAvslagBrevFelter(vedtak: Vedtak): String {
        val behandling = vedtak.behandling

        //TODO: sett navn, hjemmel og fritekst
        return "{\"fodselsnummer\": \"${behandling.fagsak.personIdent.ident}\",\n" +
               "\"navn\": \"No Name\",\n" +
               "\"hjemmel\": \"\",\n" +
               "\"fritekst\": \"${""}\"}" //TODO: Begrunnelse her
    }
}
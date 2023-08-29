package no.nav.familie.ba.sak.cucumber

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Og
import io.cucumber.java.no.Så
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.cucumber.domeneparser.BrevBegrunnelseParser.mapStandardBegrunnelser
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.brev.domene.RestSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.RestSanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.brevBegrunnelseProdusent.hentGyldigeBegrunnelserForPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.tilUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.BehandlingsGrunnlagForVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.genererVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate

class BegrunnelseTeksterStepDefinition {

    private var fagsaker: Map<Long, Fagsak> = emptyMap()
    private var behandlinger = mutableMapOf<Long, Behandling>()
    private var behandlingTilForrigeBehandling = mutableMapOf<Long, Long?>()
    private var vedtaksliste = mutableListOf<Vedtak>()
    private var persongrunnlag = mutableMapOf<Long, PersonopplysningGrunnlag>()
    private var personResultater = mutableMapOf<Long, Set<PersonResultat>>()
    private var vedtaksperioderMedBegrunnelser = listOf<VedtaksperiodeMedBegrunnelser>()
    private var kompetanser = mutableMapOf<Long, List<Kompetanse>>()
    private var endredeUtbetalinger = mutableMapOf<Long, List<EndretUtbetalingAndel>>()
    private var andelerTilkjentYtelse = mutableMapOf<Long, List<AndelTilkjentYtelse>>()
    private var overstyrteEndringstidspunkt = mutableMapOf<Long, LocalDate>()

    private var gjeldendeBehandlingId: Long? = null

    private var utvidetVedtaksperiodeMedBegrunnelser = mutableListOf<UtvidetVedtaksperiodeMedBegrunnelser>()

    /**
     * Mulige verdier: | FagsakId | Fagsaktype |
     */
    @Gitt("følgende fagsaker for begrunnelse")
    fun `følgende fagsaker for begrunnelse`(dataTable: DataTable) {
        fagsaker = lagFagsaker(dataTable)
    }

    /**
     * Mulige felter:
     * | BehandlingId | FagsakId | ForrigeBehandlingId |
     */
    @Gitt("følgende behandling")
    fun `følgende behandling`(dataTable: DataTable) {
        lagVedtak(
            dataTable = dataTable,
            behandlinger = behandlinger,
            behandlingTilForrigeBehandling = behandlingTilForrigeBehandling,
            vedtaksListe = vedtaksliste,
            fagsaker = fagsaker,
        )
    }

    /**
     * Mulige verdier: | BehandlingId |  AktørId | Persontype | Fødselsdato |
     */
    @Og("følgende persongrunnlag for begrunnelse")
    fun `følgende persongrunnlag for begrunnelse`(dataTable: DataTable) {
        persongrunnlag.putAll(lagPersonGrunnlag(dataTable))
    }

    @Og("lag personresultater for begrunnelse for behandling {}")
    fun `lag personresultater for begrunnelse`(behandlingId: Long) {
        val persongrunnlagForBehandling = persongrunnlag.finnPersonGrunnlagForBehandling(behandlingId)
        val behandling = behandlinger.finnBehandling(behandlingId)
        personResultater[behandlingId] = lagPersonresultater(persongrunnlagForBehandling, behandling)
    }

    /**
     * Mulige verdier: | AktørId | Vilkår | Utdypende vilkår | Fra dato | Til dato | Resultat | Er eksplisitt avslag |
     */
    @Og("legg til nye vilkårresultater for begrunnelse for behandling {}")
    fun `legg til nye vilkårresultater for behandling`(behandlingId: Long, dataTable: DataTable) {
        val vilkårResultaterPerPerson =
            dataTable.asMaps().groupBy { VedtaksperiodeMedBegrunnelserParser.parseAktørId(it) }
        val personResultatForBehandling = personResultater[behandlingId]
            ?: error("Finner ikke personresultater for behandling med id $behandlingId")

        personResultater[behandlingId] =
            leggTilVilkårResultatPåPersonResultat(personResultatForBehandling, vilkårResultaterPerPerson, behandlingId)
    }

    /**
     * Mulige felt:
     * | AktørId | Fra dato | Til dato | Resultat | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
     */
    @Og("med kompetanser for begrunnelse")
    fun `med kompetanser for begrunnelse`(dataTable: DataTable) {
        val nyeKompetanserPerBarn = dataTable.asMaps()
        kompetanser = lagKompetanser(nyeKompetanserPerBarn, persongrunnlag)
    }

    /**
     * Mulige verdier: | AktørId | Fra dato | Til dato | BehandlingId |  Årsak | Prosent |
     */
    @Og("med endrede utbetalinger for begrunnelse")
    fun `med endrede utbetalinger for begrunnelse`(dataTable: DataTable) {
        val nyeEndredeUtbetalingAndeler = dataTable.asMaps()
        endredeUtbetalinger = lagEndredeUtbetalinger(nyeEndredeUtbetalingAndeler, persongrunnlag)
    }

    /**
     * Mulige verdier: | AktørId | BehandlingId | Fra dato | Til dato | Beløp | Ytelse type | Prosent |
     */
    @Og("med andeler tilkjent ytelse for begrunnelse")
    fun `med andeler tilkjent ytelse for begrunnelse`(dataTable: DataTable) {
        andelerTilkjentYtelse = lagAndelerTilkjentYtelse(dataTable, behandlinger, persongrunnlag)
    }

    @Når("begrunnelsetekster genereres for behandling {}")
    fun `generer begrunnelsetekst for `(behandlingId: Long) {
        gjeldendeBehandlingId = behandlingId
        val behandling = behandlinger.finnBehandling(behandlingId)

        val vedtak = vedtaksliste.find { it.behandling.id == behandlingId && it.aktiv } ?: error("Finner ikke vedtak")

        vedtak.behandling.overstyrtEndringstidspunkt = overstyrteEndringstidspunkt[behandlingId]

        val grunnlagForVedtaksperiode = BehandlingsGrunnlagForVedtaksperioder(
            persongrunnlag = persongrunnlag.finnPersonGrunnlagForBehandling(behandlingId),
            personResultater = personResultater[behandlingId] ?: error("Finner ikke personresultater"),
            fagsakType = vedtak.behandling.fagsak.type,
            kompetanser = kompetanser[behandlingId] ?: emptyList(),
            endredeUtbetalinger = endredeUtbetalinger[behandlingId] ?: emptyList(),
            andelerTilkjentYtelse = andelerTilkjentYtelse[behandlingId] ?: emptyList(),
            perioderOvergangsstønad = emptyList(),
            uregistrerteBarn = emptyList(),
        )
        val forrigeBehandlingId = behandlingTilForrigeBehandling[behandlingId]

        val grunnlagForVedtaksperiodeForrigeBehandling = forrigeBehandlingId?.let {
            val forrigeVedtak =
                vedtaksliste.find { it.behandling.id == forrigeBehandlingId && it.aktiv } ?: error("Finner ikke vedtak")
            BehandlingsGrunnlagForVedtaksperioder(
                persongrunnlag = persongrunnlag.finnPersonGrunnlagForBehandling(forrigeBehandlingId),
                personResultater = personResultater[forrigeBehandlingId] ?: error("Finner ikke personresultater"),
                fagsakType = forrigeVedtak.behandling.fagsak.type,
                kompetanser = kompetanser[forrigeBehandlingId] ?: emptyList(),
                endredeUtbetalinger = endredeUtbetalinger[forrigeBehandlingId] ?: emptyList(),
                andelerTilkjentYtelse = andelerTilkjentYtelse[forrigeBehandlingId] ?: emptyList(),
                perioderOvergangsstønad = emptyList(),
                uregistrerteBarn = emptyList(),
            )
        }

        vedtaksperioderMedBegrunnelser = genererVedtaksperioder(
            vedtak = vedtak,
            grunnlagForVedtakPerioder = grunnlagForVedtaksperiode,
            grunnlagForVedtakPerioderForrigeBehandling = grunnlagForVedtaksperiodeForrigeBehandling,
        )

        val utvidedeVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser.map {
            it.tilUtvidetVedtaksperiodeMedBegrunnelser(
                personopplysningGrunnlag = persongrunnlag.finnPersonGrunnlagForBehandling(behandlingId),
                andelerTilkjentYtelse = andelerTilkjentYtelse[behandlingId]?.map {
                    AndelTilkjentYtelseMedEndreteUtbetalinger(
                        it,
                        endredeUtbetalinger[behandlingId] ?: emptySet(),
                    )
                } ?: emptyList(),
                skalBrukeNyVedtaksperiodeLøsning = true,
            )
        }

        utvidetVedtaksperiodeMedBegrunnelser = utvidedeVedtaksperioderMedBegrunnelser.map {
            it.copy(
                gyldigeBegrunnelser = it.hentGyldigeBegrunnelserForPeriode(
                    grunnlagForVedtaksperiode,
                    grunnlagForVedtaksperiodeForrigeBehandling,
                    mockHentSanityBegrunnelser(),
                    mockHentSanityEØSBegrunnelser(),
                    behandling.underkategori,
                ).toList(),
            )
        }.toMutableList()
    }

    /**
     * Mulige verdier: | Fra dato | Til dato | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser | Ekskluderte Begrunnelser |
     */
    @Så("forvent følgende standardBegrunnelser")
    fun `forvent følgende standardBegrunnelser`(dataTable: DataTable) {
        val forventedeStandardBegrunnelser = mapStandardBegrunnelser(dataTable).toSet()

        forventedeStandardBegrunnelser.forEach { forventet ->
            val faktisk =
                utvidetVedtaksperiodeMedBegrunnelser.find { it.fom == forventet.fom && it.tom == forventet.tom }
                    ?: throw Feil(
                        "Forventet å finne en vedtaksperiode med  \n" +
                            "   Fom: ${forventet.fom} og Tom: ${forventet.tom}. \n" +
                            "Faktiske vedtaksperioder var \n${
                                utvidetVedtaksperiodeMedBegrunnelser.joinToString("\n") {
                                    "   Fom: ${it.fom}, Tom: ${it.tom}"
                                }
                            }",
                    )
            assertThat(faktisk.gyldigeBegrunnelser)
                .`as`("For periode: ${forventet.fom} til ${forventet.tom}")
                .containsAll(forventet.inkluderteStandardBegrunnelser)

            if (faktisk.gyldigeBegrunnelser.isNotEmpty() && forventet.ekskluderteStandardBegrunnelser.isNotEmpty()) {
                assertThat(faktisk.gyldigeBegrunnelser).doesNotContainAnyElementsOf(forventet.ekskluderteStandardBegrunnelser)
            }
        }
    }

    // For å laste ned begrunnelsene på nytt anbefales https://familie-brev.sanity.studio/ba-test/vision med query fra SanityQueries.kt .
    // Kopier URL fra resultatet og kjør
    // curl -XGET <URL> | jq '.result' > <Path-til-familie-ba-sak>/familie-ba-sak/src/test/resources/no/nav/familie/ba/sak/cucumber/begrunnelsetekster/restSanityTestBegrunnelser.json
    private fun mockHentSanityBegrunnelser(): Map<Standardbegrunnelse, SanityBegrunnelse> {
        val restSanityBegrunnelserJson =
            this::class.java.getResource("/no/nav/familie/ba/sak/cucumber/begrunnelsetekster/restSanityBegrunnelser.json")!!

        val restSanityBegrunnelser =
            objectMapper.readValue(restSanityBegrunnelserJson.readText(), Array<RestSanityBegrunnelse>::class.java)
                .toList()

        val enumPåApiNavn = Standardbegrunnelse.values().associateBy { it.sanityApiNavn }
        val sanityBegrunnelser = restSanityBegrunnelser.mapNotNull { it.tilSanityBegrunnelse() }

        return sanityBegrunnelser
            .mapNotNull {
                val begrunnelseEnum = enumPåApiNavn[it.apiNavn]
                if (begrunnelseEnum == null) {
                    null
                } else {
                    begrunnelseEnum to it
                }
            }.toMap()
    }

    private fun mockHentSanityEØSBegrunnelser(): Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse> {
        val restSanityEØSBegrunnelserJson =
            this::class.java.getResource("/no/nav/familie/ba/sak/cucumber/begrunnelsetekster/restSanityEØSBegrunnelser.json")!!

        val restSanityEØSBegrunnelser =
            objectMapper.readValue(
                restSanityEØSBegrunnelserJson.readText(),
                Array<RestSanityEØSBegrunnelse>::class.java,
            )
                .toList()

        val enumPåApiNavn = EØSStandardbegrunnelse.values().associateBy { it.sanityApiNavn }
        val sanityEØSBegrunnelser = restSanityEØSBegrunnelser.mapNotNull { it.tilSanityEØSBegrunnelse() }

        return sanityEØSBegrunnelser
            .mapNotNull {
                val begrunnelseEnum = enumPåApiNavn[it.apiNavn]
                if (begrunnelseEnum == null) {
                    null
                } else {
                    begrunnelseEnum to it
                }
            }.toMap()
    }
}

data class SammenlignbarBegrunnelse(
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val inkluderteStandardBegrunnelser: Set<IVedtakBegrunnelse>,
    val ekskluderteStandardBegrunnelser: Set<IVedtakBegrunnelse> = emptySet<IVedtakBegrunnelse>(),
)

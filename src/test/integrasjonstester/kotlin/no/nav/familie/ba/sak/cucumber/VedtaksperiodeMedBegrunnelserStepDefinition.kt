package no.nav.familie.ba.sak.cucumber

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Og
import io.cucumber.java.no.Så
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.cucumber.domeneparser.Domenebegrep
import no.nav.familie.ba.sak.cucumber.domeneparser.DomeneparserUtil.groupByBehandlingId
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.ANNEN_FORELDERS_AKTIVITET
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.ANNEN_FORELDERS_AKTIVITETSLAND
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.BARNETS_BOSTEDSLAND
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.RESULTAT
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.SØKERS_AKTIVITET
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.SØKERS_AKTIVITETSLAND
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepPersongrunnlag
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.mapForventetVedtaksperioderMedBegrunnelser
import no.nav.familie.ba.sak.cucumber.domeneparser.parseDato
import no.nav.familie.ba.sak.cucumber.domeneparser.parseEnum
import no.nav.familie.ba.sak.cucumber.domeneparser.parseEnumListe
import no.nav.familie.ba.sak.cucumber.domeneparser.parseInt
import no.nav.familie.ba.sak.cucumber.domeneparser.parseList
import no.nav.familie.ba.sak.cucumber.domeneparser.parseLong
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriDato
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriEnum
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriString
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.AnnenForeldersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.SøkersAktivitet
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.utledVedtaksPerioderMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import org.assertj.core.api.Assertions
import java.math.BigDecimal
import java.time.LocalDate

class VedtaksperiodeMedBegrunnelserStepDefinition {

    private var behandlinger = mapOf<Long, Behandling>()
    private var vedtaksliste = listOf<Vedtak>()
    private var persongrunnlag = mapOf<Long, PersonopplysningGrunnlag>()
    private var personResultater = mutableMapOf<Long, Set<PersonResultat>>()
    private var vedtaksperioderMedBegrunnelser = listOf<VedtaksperiodeMedBegrunnelser>()
    private var kompetanser = mutableMapOf<Long, List<Kompetanse>>()
    private var endredeUtbetalinger = mutableMapOf<Long, List<EndretUtbetalingAndel>>()
    private var andelerTilkjentYtelse = mutableMapOf<Long, List<AndelTilkjentYtelse>>()

    @Gitt("følgende vedtak")
    fun `følgende vedtak`(dataTable: DataTable) {
        genererVedtak(dataTable)
    }

    @Og("følgende persongrunnlag")
    fun `følgende persongrunnlag`(dataTable: DataTable) {
        lagPersonGrunnlag(dataTable)
    }

    @Og("lag personresultater for behandling {}")
    fun `lag personresultater`(behandlingId: Long) {
        personResultater[behandlingId] = persongrunnlagForBehandling(behandlingId).personer.map { person ->
            lagPersonResultat(
                vilkårsvurdering = lagVilkårsvurdering(person.aktør, finnBehandling(behandlingId), Resultat.OPPFYLT),
                person = person,
                resultat = Resultat.OPPFYLT,
                personType = person.type,
                lagFullstendigVilkårResultat = true,
                periodeFom = null,
                periodeTom = null
            )
        }.toSet()
    }

    @Og("legg til nye vilkårresultater for behandling {}")
    fun `legg til nye vilkårresultater for behandling`(behandlingId: Long, dataTable: DataTable) {
        val nyeVilkårPerPerson = dataTable.asMaps().groupBy { parseInt(DomenebegrepPersongrunnlag.PERSON_ID, it) }
        val personResultatForBehandling =
            personResultater[behandlingId] ?: error("Finner ikke personresultater for behandling med id $behandlingId")
        personResultater[behandlingId] = personResultatForBehandling.map { personResultat ->
            val nyeVilkårForPerson = finnVilkårForPerson(
                personResultat = personResultat,
                overstyringerPerPerson = nyeVilkårPerPerson,
                personopplysningGrunnlag = persongrunnlagForBehandling(behandlingId)
            )

            val nyeVilkårResultater = tilVilkårResultater(nyeVilkårForPerson, behandlingId, personResultat)
            personResultat.vilkårResultater.addAll(nyeVilkårResultater)
            personResultat
        }.toSet()
    }

    @Og("med kompetanser for behandling {}")
    fun `med kompetanser for behandling`(behandlingId: Long, dataTable: DataTable) {
        val nyeKompetanserPerBarn = dataTable.asMaps()
        kompetanser[behandlingId] = nyeKompetanserPerBarn.map { kompetanse ->
            val aktørerForKompetane = parseList(DomenebegrepPersongrunnlag.PERSON_ID, kompetanse)
            Kompetanse(
                fom = parseValgfriDato(Domenebegrep.FRA_DATO, kompetanse)?.toYearMonth(),
                tom = parseValgfriDato(Domenebegrep.TIL_DATO, kompetanse)?.toYearMonth(),
                barnAktører = persongrunnlagForBehandling(behandlingId).personer
                    .filter { aktørerForKompetane.contains(it.id) }
                    .map { it.aktør }
                    .toSet(),
                søkersAktivitet = parseValgfriEnum<SøkersAktivitet>(SØKERS_AKTIVITET, kompetanse)
                    ?: SøkersAktivitet.ARBEIDER,
                annenForeldersAktivitet =
                parseValgfriEnum<AnnenForeldersAktivitet>(ANNEN_FORELDERS_AKTIVITET, kompetanse)
                    ?: AnnenForeldersAktivitet.I_ARBEID,
                søkersAktivitetsland = parseValgfriString(SØKERS_AKTIVITETSLAND, kompetanse) ?: "PL",
                annenForeldersAktivitetsland = parseValgfriString(ANNEN_FORELDERS_AKTIVITETSLAND, kompetanse)?: "NO",
                barnetsBostedsland = parseValgfriString(BARNETS_BOSTEDSLAND, kompetanse) ?: "NO",
                resultat = parseEnum<KompetanseResultat>(RESULTAT, kompetanse),
            )
        }
    }

    @Og("med endrede utbetalinger for behandling {}")
    fun `med endrede utbetalinger for behandling`(behandlingId: Long, dataTable: DataTable) {
        val nyeEndredeUtbetalingAndeler = dataTable.asMaps()
        endredeUtbetalinger[behandlingId] = nyeEndredeUtbetalingAndeler.map { endretUtbetaling ->
            val personId = parseLong(DomenebegrepPersongrunnlag.PERSON_ID, endretUtbetaling)
            EndretUtbetalingAndel(
                behandlingId = behandlingId,
                fom = parseValgfriDato(Domenebegrep.FRA_DATO, endretUtbetaling)?.toYearMonth(),
                tom = parseValgfriDato(Domenebegrep.TIL_DATO, endretUtbetaling)?.toYearMonth(),
                person = persongrunnlagForBehandling(behandlingId).personer.find { personId == it.id },
                prosent = BigDecimal.ZERO,
                årsak = Årsak.ALLEREDE_UTBETALT,
                søknadstidspunkt = LocalDate.now(),
                begrunnelse = "Fordi at..."
            )
        }
    }

    @Og("med andeler tilkjent ytelse for behandling {}")
    fun `med andeler tilkjent ytelse for behandling`(behandlingId: Long, dataTable: DataTable) {
        val nyeAndelerTilkjentYtelse = dataTable.asMaps()
        andelerTilkjentYtelse[behandlingId] = nyeAndelerTilkjentYtelse.map { andelerTilkjentYtelse ->
            val personId = parseLong(DomenebegrepPersongrunnlag.PERSON_ID, andelerTilkjentYtelse)
            lagAndelTilkjentYtelse(
                fom = parseValgfriDato(Domenebegrep.FRA_DATO, andelerTilkjentYtelse)?.toYearMonth()
                    ?: LocalDate.MIN.toYearMonth(),
                tom = parseValgfriDato(Domenebegrep.TIL_DATO, andelerTilkjentYtelse)?.toYearMonth()
                    ?: LocalDate.MAX.toYearMonth(),
                behandling = finnBehandling(behandlingId),
                person = persongrunnlagForBehandling(behandlingId).personer.find { personId == it.id }!!,
                beløp = parseInt(DomenebegrepVedtaksperiodeMedBegrunnelser.BELØP, andelerTilkjentYtelse)
            )
        }
    }

    @Og("med overstyring av vilkår for behandling {}")
    fun overstyrPersonResultater(behandlingId: Long, dataTable: DataTable) {
        val overstyringerPerPerson = dataTable.asMaps().groupBy { parseInt(DomenebegrepPersongrunnlag.PERSON_ID, it) }
        val personResultatForBehandling =
            personResultater[behandlingId] ?: error("Finner ikke personresultater for behandling med id $behandlingId")
        personResultater[behandlingId] = personResultatForBehandling.map { personResultat ->
            val overstyringerForPerson = finnVilkårForPerson(
                personResultat = personResultat,
                overstyringerPerPerson = overstyringerPerPerson,
                personopplysningGrunnlag = persongrunnlagForBehandling(behandlingId)
            )
            personResultat.vilkårResultater.forEach { vilkårResultat ->
                oppdaterVilkårResultat(
                    vilkårResultat,
                    overstyringerForPerson
                )
            }
            personResultat
        }.toSet()
    }

    @Når("vedtaksperioder med begrunnelser genereres for behandling {}")
    fun `generer vedtaksperiode med begrunnelse`(behandlingId: Long) {
        vedtaksperioderMedBegrunnelser = utledVedtaksPerioderMedBegrunnelser(
            persongrunnlag = persongrunnlagForBehandling(behandlingId),
            personResultater = personResultater[behandlingId] ?: error("Finner ikke personresultater"),
            vedtak = vedtaksliste.find { it.behandling.id == behandlingId && it.aktiv } ?: error("Finner ikke vedtak"),
            kompetanser = kompetanser[behandlingId] ?: emptyList(),
            endredeUtbetalinger = endredeUtbetalinger[behandlingId] ?: emptyList(),
            andelerTilkjentYtelse = andelerTilkjentYtelse[behandlingId] ?: emptyList()
        )
    }

    @Så("forvent følgende vedtaksperioder med begrunnelser")
    fun `forvent følgende vedtaksperioder med begrunnelser`(dataTable: DataTable) {
        val forventedeVedtaksperioder = mapForventetVedtaksperioderMedBegrunnelser(
            dataTable,
            vedtak = vedtaksliste.last()
        )

        val vedtaksperioderComparator = compareBy<VedtaksperiodeMedBegrunnelser>({ it.type }, { it.fom }, { it.tom })
        Assertions.assertThat(forventedeVedtaksperioder.sortedWith(vedtaksperioderComparator))
            .isEqualTo(vedtaksperioderMedBegrunnelser.sortedWith(vedtaksperioderComparator))
    }

    private fun tilVilkårResultater(
        nyeVilkårForPerson: List<MutableMap<String, String>>?,
        behandlingId: Long,
        personResultat: PersonResultat
    ) =
        nyeVilkårForPerson?.map {
            VilkårResultat(
                vilkårType = parseEnum(DomenebegrepVedtaksperiodeMedBegrunnelser.VILKÅR, it),
                resultat = parseEnum(DomenebegrepVedtaksperiodeMedBegrunnelser.RESULTAT, it),
                periodeFom = parseValgfriDato(Domenebegrep.FRA_DATO, it),
                periodeTom = parseValgfriDato(Domenebegrep.TIL_DATO, it),
                begrunnelse = "",
                behandlingId = behandlingId,
                personResultat = personResultat
            )
        } ?: emptyList()

    private fun oppdaterVilkårResultat(
        vilkårResultat: VilkårResultat,
        overstyringerForPerson: List<MutableMap<String, String>>?
    ) {
        val overstyringForVilkår = overstyringerForPerson?.find {
            parseEnumListe<Vilkår>(
                DomenebegrepVedtaksperiodeMedBegrunnelser.VILKÅR,
                it
            ).contains(vilkårResultat.vilkårType)
        }
        if (overstyringForVilkår != null) {
            vilkårResultat.resultat =
                parseEnum(DomenebegrepVedtaksperiodeMedBegrunnelser.RESULTAT, overstyringForVilkår)
            vilkårResultat.periodeFom = parseValgfriDato(Domenebegrep.FRA_DATO, overstyringForVilkår)
            vilkårResultat.periodeTom = parseValgfriDato(Domenebegrep.TIL_DATO, overstyringForVilkår)
        }
    }

    private fun finnVilkårForPerson(
        personResultat: PersonResultat,
        overstyringerPerPerson: Map<Int, List<MutableMap<String, String>>>,
        personopplysningGrunnlag: PersonopplysningGrunnlag
    ): List<MutableMap<String, String>>? {
        val aktørId = personResultat.aktør.aktørId
        val personId = personopplysningGrunnlag.personer.find { it.aktør.aktørId == aktørId }?.id?.toInt()
        return overstyringerPerPerson[personId]
    }

    private fun genererVedtak(dataTable: DataTable) {
        val fagsak = defaultFagsak()
        behandlinger = dataTable.groupByBehandlingId()
            .map { lagBehandling(fagsak = fagsak).copy(id = it.key) }
            .associateBy { it.id }
        vedtaksliste = dataTable.groupByBehandlingId()
            .map { lagVedtak(behandlinger[it.key] ?: error("Finner ikke behandling")) }
    }

    private fun lagPersonGrunnlag(dataTable: DataTable) {
        persongrunnlag = dataTable.groupByBehandlingId()
            .map { behandlingMedPersongrunnlag ->
                PersonopplysningGrunnlag(
                    behandlingId = behandlingMedPersongrunnlag.key,
                    personer = behandlingMedPersongrunnlag.value.map {
                        tilfeldigPerson(
                            personType = parseEnum(
                                DomenebegrepPersongrunnlag.PERSON_TYPE,
                                it
                            ),
                            fødselsdato = parseDato(DomenebegrepPersongrunnlag.FØDSELSDATO, it),
                            personId = parseInt(DomenebegrepPersongrunnlag.PERSON_ID, it).toLong()

                        )
                    }.toMutableSet()
                )
            }.associateBy { it.behandlingId }
    }

    private fun persongrunnlagForBehandling(behandlingId: Long): PersonopplysningGrunnlag =
        persongrunnlag[behandlingId] ?: error("Finner ikke persongrunnlag for behandling med id $behandlingId")

    private fun finnBehandling(behandlingId: Long) =
        behandlinger[behandlingId] ?: error("Finner ikke behandling med id $behandlingId")
}

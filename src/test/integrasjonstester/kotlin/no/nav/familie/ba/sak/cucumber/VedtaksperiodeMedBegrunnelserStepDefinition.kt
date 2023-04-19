package no.nav.familie.ba.sak.cucumber

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Og
import io.cucumber.java.no.Så
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.cucumber.domeneparser.Domenebegrep
import no.nav.familie.ba.sak.cucumber.domeneparser.DomeneparserUtil.groupByBehandlingId
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepPersongrunnlag
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.mapForventetVedtaksperioderMedBegrunnelser
import no.nav.familie.ba.sak.cucumber.domeneparser.parseDato
import no.nav.familie.ba.sak.cucumber.domeneparser.parseEnum
import no.nav.familie.ba.sak.cucumber.domeneparser.parseEnumListe
import no.nav.familie.ba.sak.cucumber.domeneparser.parseInt
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriDato
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.utledVedtaksPerioderMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import org.assertj.core.api.Assertions

class VedtaksperiodeMedBegrunnelserStepDefinition {

    private var behandlinger = mapOf<Long, Behandling>()
    private var vedtaksliste = listOf<Vedtak>()
    private var persongrunnlag = mapOf<Long, PersonopplysningGrunnlag>()
    private var personResultater = mutableMapOf<Long, Set<PersonResultat>>()
    private var vedtaksperioderMedBegrunnelser = listOf<VedtaksperiodeMedBegrunnelser>()

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

    private fun tilVilkårResultater(
        nyeVilkårForPerson: List<MutableMap<String, String>>?,
        behandlingId: Long,
        personResultat: PersonResultat
    ) =
        nyeVilkårForPerson?.map {
            it
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
                parseEnum<Resultat>(DomenebegrepVedtaksperiodeMedBegrunnelser.RESULTAT, overstyringForVilkår)
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

    @Når("vedtaksperioder med begrunnelser genereres for behandling {}")
    fun `generer vedtaksperiode med begrunnelse`(behandlingId: Long) {
        vedtaksperioderMedBegrunnelser = utledVedtaksPerioderMedBegrunnelser(
            persongrunnlagForBehandling(behandlingId),
            personResultater[behandlingId] ?: error("Finner ikke personresultater"),
            vedtaksliste.find { it.behandling.id == behandlingId && it.aktiv } ?: error("Finner ikke vedtak")
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

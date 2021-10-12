package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestNyttVilkår
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPersonResultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType.MIGRERING_FRA_INFOTRYGD
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand.Companion.sisteSivilstand
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils.flyttResultaterTilInitielt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils.lagFjernAdvarsel
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils.muterPersonResultatDelete
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils.muterPersonResultatPost
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils.muterPersonVilkårResultaterPut
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat.Companion.VilkårResultatComparator
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class VilkårService(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val vilkårsvurderingMetrics: VilkårsvurderingMetrics,
    private val behandlingService: BehandlingService,
    private val featureToggleService: FeatureToggleService,
) {

    fun hentVilkårsvurdering(behandlingId: Long): Vilkårsvurdering? = vilkårsvurderingService.hentAktivForBehandling(
        behandlingId = behandlingId
    )

    @Transactional
    fun endreVilkår(
        behandlingId: Long,
        vilkårId: Long,
        restPersonResultat: RestPersonResultat
    ): List<RestPersonResultat> {
        val vilkårsvurdering = hentVilkårsvurdering(behandlingId = behandlingId)
            ?: throw Feil(
                message = "Fant ikke aktiv vilkårsvurdering ved endring på vilkår",
                frontendFeilmelding = "Fant ikke aktiv vilkårsvurdering"
            )

        val restVilkårResultat = restPersonResultat.vilkårResultater.singleOrNull { it.id == vilkårId }
            ?: throw Feil("Fant ikke vilkårResultat med id $vilkårId ved opppdatering av vikår")
        val personResultat =
            vilkårsvurdering.personResultater.singleOrNull { it.personIdent == restPersonResultat.personIdent }
                ?: throw Feil(
                    message = "Fant ikke vilkårsvurdering for person",
                    frontendFeilmelding = "Fant ikke vilkårsvurdering for person med ident '${restPersonResultat.personIdent}"
                )

        muterPersonVilkårResultaterPut(personResultat, restVilkårResultat)

        val vilkårResultat = personResultat.vilkårResultater.singleOrNull { it.id == vilkårId }
            ?: error("Finner ikke vilkår med vilkårId $vilkårId på personResultat ${personResultat.id}")

        vilkårResultat.also {
            it.vedtakBegrunnelseSpesifikasjoner = restVilkårResultat.avslagBegrunnelser ?: emptyList()
        }

        return vilkårsvurderingService.oppdater(vilkårsvurdering).personResultater.map { it.tilRestPersonResultat() }
    }

    @Transactional
    fun deleteVilkår(behandlingId: Long, vilkårId: Long, personIdent: String): List<RestPersonResultat> {
        val vilkårsvurdering = hentVilkårsvurdering(behandlingId = behandlingId)
            ?: throw Feil(
                message = "Fant ikke aktiv vilkårsvurdering ved sletting av vilkår",
                frontendFeilmelding = "Fant ikke aktiv vilkårsvurdering"
            )

        val personResultat = vilkårsvurdering.personResultater.find { it.personIdent == personIdent }
            ?: throw Feil(
                message = "Fant ikke vilkårsvurdering for person",
                frontendFeilmelding = "Fant ikke vilkårsvurdering for person med ident '$personIdent"
            )

        muterPersonResultatDelete(personResultat, vilkårId)

        return vilkårsvurderingService.oppdater(vilkårsvurdering).personResultater.map { it.tilRestPersonResultat() }
    }

    @Transactional
    fun postVilkår(behandlingId: Long, restNyttVilkår: RestNyttVilkår): List<RestPersonResultat> {
        val vilkårsvurdering = hentVilkårsvurdering(behandlingId = behandlingId)
            ?: throw Feil(
                message = "Fant ikke aktiv vilkårsvurdering ved opprettelse av vilkår",
                frontendFeilmelding = "Fant ikke aktiv vilkårsvurdering"
            )

        val personResultat = vilkårsvurdering.personResultater.find { it.personIdent == restNyttVilkår.personIdent }
            ?: throw Feil(
                message = "Fant ikke vilkårsvurdering for person",
                frontendFeilmelding =
                "Fant ikke vilkårsvurdering for person med ident '${restNyttVilkår.personIdent}"
            )

        muterPersonResultatPost(personResultat, restNyttVilkår.vilkårType)

        return vilkårsvurderingService.oppdater(vilkårsvurdering).personResultater.map { it.tilRestPersonResultat() }
    }

    fun initierVilkårsvurderingForBehandling(
        behandling: Behandling,
        bekreftEndringerViaFrontend: Boolean,
        forrigeBehandling: Behandling? = null
    ): Vilkårsvurdering {
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
            ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling ${behandling.id}")

        if (behandling.skalBehandlesAutomatisk) {
            val barna = personopplysningGrunnlag.personer.filter { person -> person.type === PersonType.BARN }
            if (barna.isEmpty()) {
                throw IllegalStateException("PersonopplysningGrunnlag for fødselshendelse skal inneholde minst ett barn")
            }
        }

        val aktivVilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandling.id)
        val barnaSomAlleredeErVurdert = aktivVilkårsvurdering?.personResultater?.mapNotNull {
            personopplysningGrunnlag.barna.firstOrNull { barn -> barn.personIdent.ident == it.personIdent }
        }?.filter { it.type == PersonType.BARN }?.map { it.personIdent.ident } ?: emptyList()

        val initiellVilkårsvurdering =
            genererInitiellVilkårsvurdering(
                behandling = behandling,
                barnaSomAlleredeErVurdert = barnaSomAlleredeErVurdert
            )

        return if (forrigeBehandling != null && aktivVilkårsvurdering == null) {
            val vilkårsvurdering =
                genererInitiellVilkårsvurderingFraAnnenBehandling(
                    initiellVilkårsvurdering = initiellVilkårsvurdering,
                    annenBehandling = forrigeBehandling
                )
            return vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
        } else {
            if (aktivVilkårsvurdering != null) {
                val (initieltSomErOppdatert, aktivtSomErRedusert) = flyttResultaterTilInitielt(
                    initiellVilkårsvurdering = initiellVilkårsvurdering,
                    aktivVilkårsvurdering = aktivVilkårsvurdering,
                    løpendeUnderkategori = behandlingService.hentLøpendeUnderkategori(initiellVilkårsvurdering.behandling.fagsak.id)
                )

                if (aktivtSomErRedusert.personResultater.isNotEmpty() && !bekreftEndringerViaFrontend) {
                    throw FunksjonellFeil(
                        melding = "Saksbehandler forsøker å fjerne vilkår fra vilkårsvurdering",
                        frontendFeilmelding = lagFjernAdvarsel(aktivtSomErRedusert.personResultater)
                    )
                }
                return vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = initieltSomErOppdatert)
            } else {
                vilkårsvurderingService.lagreInitielt(initiellVilkårsvurdering)
            }
        }
    }

    fun genererInitiellVilkårsvurderingFraAnnenBehandling(
        annenBehandling: Behandling,
        initiellVilkårsvurdering: Vilkårsvurdering
    ): Vilkårsvurdering {

        val annenVilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = annenBehandling.id)
            ?: throw Feil(message = "Finner ikke vilkårsvurdering fra annen behandling.")

        val annenBehandlingErHenlagt = behandlingService.hent(annenBehandling.id).erHenlagt()

        if (annenBehandlingErHenlagt)
            throw Feil(message = "vilkårsvurdering skal ikke kopieres fra henlagt behandling.")
        val (oppdatert) = flyttResultaterTilInitielt(
            aktivVilkårsvurdering = annenVilkårsvurdering,
            initiellVilkårsvurdering = initiellVilkårsvurdering,
            løpendeUnderkategori = behandlingService.hentLøpendeUnderkategori(initiellVilkårsvurdering.behandling.fagsak.id),
            forrigeBehandlingVilkårsvurdering = hentVilkårsvurdering(annenBehandling.id)
        )
        return oppdatert
    }

    fun genererInitiellVilkårsvurdering(
        behandling: Behandling,
        barnaSomAlleredeErVurdert: List<String>
    ): Vilkårsvurdering {
        return Vilkårsvurdering(behandling = behandling).apply {
            when {
                behandling.type == MIGRERING_FRA_INFOTRYGD -> {
                    personResultater = lagVilkårsvurderingForMigreringsbehandling(this)
                }
                behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE -> {
                    if (featureToggleService.isEnabled(FeatureToggleConfig.AUTOMATISK_FØDSELSHENDELSE)) {
                        personResultater = lagVilkårsvurderingForFødselshendelse(this, barnaSomAlleredeErVurdert)
                    }

                    if (førstegangskjøringAvVilkårsvurdering(this)) {
                        vilkårsvurderingMetrics.tellMetrikker(this)
                    }
                }
                !behandling.skalBehandlesAutomatisk -> {
                    personResultater = lagManuellVilkårsvurdering(this)
                }
                else -> personResultater = lagTomVilkårsvurdering(this)
            }
        }
    }

    private fun lagTomVilkårsvurdering(vilkårsvurdering: Vilkårsvurdering): Set<PersonResultat> {
        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(vilkårsvurdering.behandling.id)
                ?: throw Feil(message = "Fant ikke personopplysninggrunnlag for behandling ${vilkårsvurdering.behandling.id}")

        return personopplysningGrunnlag.personer.map { person ->
            val personResultat = PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                personIdent = person.personIdent.ident
            )

            val vilkårForPerson = Vilkår.hentVilkårFor(person.type)

            val vilkårResultater = vilkårForPerson.map { vilkår ->
                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = true,
                    resultat = Resultat.IKKE_VURDERT,
                    vilkårType = vilkår,
                    begrunnelse = "",
                    behandlingId = personResultat.vilkårsvurdering.behandling.id,
                )
            }.toSortedSet(VilkårResultatComparator)

            personResultat.setSortedVilkårResultater(vilkårResultater)

            personResultat
        }.toSet()
    }

    private fun lagManuellVilkårsvurdering(vilkårsvurdering: Vilkårsvurdering): Set<PersonResultat> {
        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(vilkårsvurdering.behandling.id)
                ?: throw Feil(message = "Fant ikke personopplysninggrunnlag for behandling ${vilkårsvurdering.behandling.id}")

        return personopplysningGrunnlag.personer.map { person ->
            val personResultat = PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                personIdent = person.personIdent.ident
            )

            val vilkårForPerson = Vilkår.hentVilkårFor(
                personType = person.type,
                ytelseType = vilkårsvurdering.behandling.hentYtelseTypeTilVilkår()
            )

            val vilkårResultater = vilkårForPerson.map { vilkår ->
                val fom = if (vilkår.gjelderAlltidFraBarnetsFødselsdato()) person.fødselsdato else null

                val tom: LocalDate? =
                    if (vilkår == Vilkår.UNDER_18_ÅR) {
                        person.fødselsdato.plusYears(18).minusDays(1)
                    } else null

                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = when (vilkår) {
                        Vilkår.UNDER_18_ÅR, Vilkår.GIFT_PARTNERSKAP -> true
                        else -> false
                    },
                    resultat = when (vilkår) {
                        Vilkår.UNDER_18_ÅR -> Resultat.OPPFYLT
                        Vilkår.GIFT_PARTNERSKAP -> if (person.sivilstander.isEmpty() || person.sivilstander.sisteSivilstand()?.type?.somForventetHosBarn() == true)
                            Resultat.OPPFYLT else Resultat.IKKE_VURDERT
                        else -> Resultat.IKKE_VURDERT
                    },
                    vilkårType = vilkår,
                    periodeFom = fom,
                    periodeTom = tom,
                    begrunnelse = when (vilkår) {
                        Vilkår.UNDER_18_ÅR -> "Vurdert og satt automatisk"
                        Vilkår.GIFT_PARTNERSKAP -> if (person.sivilstander.sisteSivilstand()?.type?.somForventetHosBarn() == false)
                            "Vilkåret er forsøkt behandlet automatisk, men barnet er registrert som gift i " +
                                "folkeregisteret. Vurder hvilke konsekvenser dette skal ha for behandlingen" else ""
                        else -> ""
                    },
                    behandlingId = personResultat.vilkårsvurdering.behandling.id
                )
            }.toSortedSet(VilkårResultatComparator)

            personResultat.setSortedVilkårResultater(vilkårResultater)

            personResultat
        }.toSet()
    }

    private fun lagVilkårsvurderingForFødselshendelse(
        vilkårsvurdering: Vilkårsvurdering,
        barnaSomAlleredeErVurdert: List<String>
    ): Set<PersonResultat> {
        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(vilkårsvurdering.behandling.id)
                ?: throw Feil(message = "Fant ikke personopplysninggrunnlag for behandling ${vilkårsvurdering.behandling.id}")

        val eldsteBarnSomVurderesSinFødselsdato =
            personopplysningGrunnlag.barna.filter { !barnaSomAlleredeErVurdert.contains(it.personIdent.ident) }
                .maxByOrNull { it.fødselsdato }?.fødselsdato
                ?: throw Feil("Finner ingen barn på persongrunnlag")

        return personopplysningGrunnlag.personer.map { person ->
            val personResultat = PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                personIdent = person.personIdent.ident
            )

            val vilkårForPerson = Vilkår.hentVilkårFor(person.type)

            val vilkårResultater = vilkårForPerson.map { vilkår ->
                genererVilkårResultatForEtVilkårPåEnPerson(
                    person,
                    eldsteBarnSomVurderesSinFødselsdato,
                    personResultat,
                    vilkår
                )
            }

            personResultat.setSortedVilkårResultater(vilkårResultater.toSet())

            personResultat
        }.toSet()
    }

    private fun genererVilkårResultatForEtVilkårPåEnPerson(
        person: Person,
        eldsteBarnSinFødselsdato: LocalDate,
        personResultat: PersonResultat,
        vilkår: Vilkår
    ): VilkårResultat {
        val automatiskVurderingResultat = vilkår.vurderVilkår(person, eldsteBarnSinFødselsdato)

        val fom = if (eldsteBarnSinFødselsdato >= person.fødselsdato) eldsteBarnSinFødselsdato else person.fødselsdato

        val tom: LocalDate? =
            if (vilkår == Vilkår.UNDER_18_ÅR) {
                person.fødselsdato.plusYears(18).minusDays(1)
            } else null

        return VilkårResultat(
            regelInput = automatiskVurderingResultat.regelInput,
            personResultat = personResultat,
            erAutomatiskVurdert = true,
            resultat = automatiskVurderingResultat.resultat,
            vilkårType = vilkår,
            periodeFom = fom,
            periodeTom = tom,
            begrunnelse = "Vurdert og satt automatisk: ${automatiskVurderingResultat.evaluering.begrunnelse}",
            behandlingId = personResultat.vilkårsvurdering.behandling.id,
            evalueringÅrsaker = automatiskVurderingResultat.evaluering.evalueringÅrsaker.map { it.toString() }
        )
    }

    private fun lagVilkårsvurderingForMigreringsbehandling(vilkårsvurdering: Vilkårsvurdering): Set<PersonResultat> {
        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(vilkårsvurdering.behandling.id)
                ?: throw Feil(message = "Fant ikke personopplysninggrunnlag for behandling ${vilkårsvurdering.behandling.id}")

        return personopplysningGrunnlag.personer.filter { it.type != PersonType.ANNENPART }.map { person ->
            val personResultat = PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                personIdent = person.personIdent.ident
            )

            val vilkårTyperForPerson = Vilkår.hentVilkårFor(person.type)

            val vilkårResultater = vilkårTyperForPerson.map { vilkår ->
                val fom = if (vilkår.gjelderAlltidFraBarnetsFødselsdato()) person.fødselsdato else null

                val tom: LocalDate? =
                    if (vilkår == Vilkår.UNDER_18_ÅR) person.fødselsdato.plusYears(18).minusDays(1) else null

                val begrunnelse = "Migrering"

                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = false,
                    resultat = Resultat.OPPFYLT,
                    vilkårType = vilkår,
                    periodeFom = fom,
                    periodeTom = tom,
                    begrunnelse = begrunnelse,
                    behandlingId = personResultat.vilkårsvurdering.behandling.id
                )
            }.toSortedSet(VilkårResultatComparator)

            personResultat.setSortedVilkårResultater(vilkårResultater)

            personResultat
        }.toSet()
    }

    private fun førstegangskjøringAvVilkårsvurdering(vilkårsvurdering: Vilkårsvurdering): Boolean {
        return vilkårsvurderingService
            .hentAktivForBehandling(behandlingId = vilkårsvurdering.behandling.id) == null
    }
}

fun Vilkår.gjelderAlltidFraBarnetsFødselsdato() = this == Vilkår.GIFT_PARTNERSKAP || this == Vilkår.UNDER_18_ÅR

fun SIVILSTAND.somForventetHosBarn() = this == SIVILSTAND.UOPPGITT || this == SIVILSTAND.UGIFT

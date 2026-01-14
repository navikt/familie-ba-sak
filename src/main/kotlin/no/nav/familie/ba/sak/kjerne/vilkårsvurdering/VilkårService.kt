package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle.VALIDER_ENDRING_AV_PREUTFYLTE_VILKÅR
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestNyttVilkår
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestSlettVilkår
import no.nav.familie.ba.sak.ekstern.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPersonResultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils.muterPersonResultatDelete
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils.muterPersonResultatPost
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils.muterPersonVilkårResultaterPut
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.EndringIPreutfyltVilkårLogg.Companion.opprettLoggForEndringIPreutfyltVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.EndringIPreutfyltVilkårLoggRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VilkårService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingstemaService: BehandlingstemaService,
    private val behandlingService: BehandlingService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val personidentService: PersonidentService,
    private val persongrunnlagService: PersongrunnlagService,
    private val endringIPreutfyltVilkårLoggRepository: EndringIPreutfyltVilkårLoggRepository,
    private val featureToggleService: FeatureToggleService,
) {
    fun hentVilkårsvurdering(behandlingId: Long): Vilkårsvurdering? =
        vilkårsvurderingService.hentAktivForBehandling(
            behandlingId = behandlingId,
        )

    fun hentVilkårsvurderingThrows(behandlingId: Long): Vilkårsvurdering =
        hentVilkårsvurdering(behandlingId) ?: throw Feil(
            message = "Fant ikke aktiv vilkårsvurdering for behandling $behandlingId",
            frontendFeilmelding = FANT_IKKE_AKTIV_VILKÅRSVURDERING_FEILMELDING,
        )

    @Transactional
    fun endreVilkår(
        behandlingId: Long,
        vilkårId: Long,
        restPersonResultat: RestPersonResultat,
    ): List<RestPersonResultat> {
        val vilkårsvurdering = hentVilkårsvurderingThrows(behandlingId)

        val restVilkårResultat =
            restPersonResultat.vilkårResultater.singleOrNull { it.id == vilkårId }
                ?: throw Feil("Fant ikke vilkårResultat med id $vilkårId ved oppdatering av vilkår")

        validerResultatBegrunnelse(restVilkårResultat)

        val personResultat =
            finnPersonResultatForPersonThrows(vilkårsvurdering.personResultater, restPersonResultat.personIdent)

        if (featureToggleService.isEnabled(VALIDER_ENDRING_AV_PREUTFYLTE_VILKÅR)) {
            val eksisterendeVilkårResultat =
                personResultat.vilkårResultater.singleOrNull { it.id == vilkårId }
                    ?: throw Feil("Finner ikke vilkår med vilkårId $vilkårId på personResultat ${personResultat.id}")

            if (eksisterendeVilkårResultat.erOpprinneligPreutfylt) {
                val erEndringIBegrunnelse = eksisterendeVilkårResultat.begrunnelse != restVilkårResultat.begrunnelse
                val erEndringIAnnetFeltEnnBegrunnelse = erEndringIVilkår(eksisterendeVilkårResultat, restVilkårResultat)

                if (!erEndringIBegrunnelse && !erEndringIAnnetFeltEnnBegrunnelse) {
                    return vilkårsvurdering.personResultater.map { it.tilRestPersonResultat() }
                }

                val begrunnelseErTomEllerAutomatiskUtfylt =
                    restVilkårResultat.begrunnelse.run { isBlank() || startsWith(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT) }

                if (erEndringIAnnetFeltEnnBegrunnelse && begrunnelseErTomEllerAutomatiskUtfylt) {
                    throw FunksjonellFeil(
                        melding = "Begrunnelse må være fylt inn ved oppdatering av preutfylt vilkår",
                        frontendFeilmelding = "Du har endret vilkåret, og må derfor fjerne den automatiske begrunnelsen og fylle inn en ny begrunnelse.",
                    )
                }

                endringIPreutfyltVilkårLoggRepository.save(
                    opprettLoggForEndringIPreutfyltVilkår(
                        behandling = vilkårsvurdering.behandling,
                        forrigeVilkår = eksisterendeVilkårResultat,
                        nyttVilkår = restVilkårResultat,
                    ),
                )
            }
        }

        muterPersonVilkårResultaterPut(personResultat, restVilkårResultat)

        val vilkårResultat =
            personResultat.vilkårResultater.singleOrNull { it.id == vilkårId }
                ?: throw Feil("Finner ikke vilkår med vilkårId $vilkårId på personResultat ${personResultat.id}")

        vilkårResultat.also {
            it.standardbegrunnelser = restVilkårResultat.avslagBegrunnelser ?: emptyList()
        }

        val migreringsdatoPåFagsak =
            behandlingService.hentMigreringsdatoPåFagsak(fagsakId = vilkårsvurdering.behandling.fagsak.id)
        validerVilkårStarterIkkeFørMigreringsdatoForMigreringsbehandling(
            vilkårsvurdering,
            vilkårResultat,
            migreringsdatoPåFagsak,
        )

        return vilkårsvurderingService.oppdater(vilkårsvurdering).personResultater.map { it.tilRestPersonResultat() }
    }

    private fun erEndringIVilkår(
        vilkårResultat: VilkårResultat,
        restVilkårResultat: RestVilkårResultat,
    ): Boolean =
        vilkårResultat.periodeFom != restVilkårResultat.periodeFom ||
            vilkårResultat.periodeTom != restVilkårResultat.periodeTom ||
            vilkårResultat.resultat != restVilkårResultat.resultat ||
            vilkårResultat.resultatBegrunnelse != restVilkårResultat.resultatBegrunnelse ||
            vilkårResultat.erEksplisittAvslagPåSøknad != restVilkårResultat.erEksplisittAvslagPåSøknad ||
            vilkårResultat.vurderesEtter != restVilkårResultat.vurderesEtter ||
            vilkårResultat.utdypendeVilkårsvurderinger.toSet() != restVilkårResultat.utdypendeVilkårsvurderinger.toSet() ||
            vilkårResultat.standardbegrunnelser.toSet() != restVilkårResultat.avslagBegrunnelser.orEmpty().toSet()

    @Transactional
    fun deleteVilkårsperiode(
        behandlingId: Long,
        vilkårId: Long,
        aktør: Aktør,
    ): List<RestPersonResultat> {
        val vilkårsvurdering = hentVilkårsvurderingThrows(behandlingId)

        val personResultat =
            finnPersonResultatForPersonThrows(vilkårsvurdering.personResultater, aktør.aktivFødselsnummer())

        muterPersonResultatDelete(personResultat, vilkårId)

        return vilkårsvurderingService.oppdater(vilkårsvurdering).personResultater.map { it.tilRestPersonResultat() }
    }

    @Transactional
    fun deleteVilkår(
        behandlingId: Long,
        restSlettVilkår: RestSlettVilkår,
    ): List<RestPersonResultat> {
        val vilkårsvurdering = hentVilkårsvurderingThrows(behandlingId)
        val personResultat =
            finnPersonResultatForPersonThrows(vilkårsvurdering.personResultater, restSlettVilkår.personIdent)
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        if (!behandling.kanLeggeTilOgFjerneUtvidetVilkår() ||
            Vilkår.UTVIDET_BARNETRYGD != restSlettVilkår.vilkårType ||
            finnesUtvidetBarnetrydIForrigeBehandling(behandling, restSlettVilkår.personIdent)
        ) {
            throw FunksjonellFeil(
                melding =
                    "Vilkår ${restSlettVilkår.vilkårType.beskrivelse} kan ikke slettes " +
                        "for behandling $behandlingId",
                frontendFeilmelding =
                    "Vilkår ${restSlettVilkår.vilkårType.beskrivelse} kan ikke slettes " +
                        "for behandling $behandlingId",
            )
        }

        personResultat.vilkårResultater
            .filter { it.vilkårType == restSlettVilkår.vilkårType }
            .forEach { personResultat.removeVilkårResultat(it.id) }

        if (restSlettVilkår.vilkårType == Vilkår.UTVIDET_BARNETRYGD) {
            behandlingstemaService.oppdaterBehandlingstemaForVilkår(
                behandling = behandling,
                overstyrtUnderkategori = BehandlingUnderkategori.ORDINÆR,
            )
        }

        return vilkårsvurderingService.oppdater(vilkårsvurdering).personResultater.map { it.tilRestPersonResultat() }
    }

    @Transactional
    fun postVilkår(
        behandlingId: Long,
        restNyttVilkår: RestNyttVilkår,
    ): List<RestPersonResultat> {
        val vilkårsvurdering = hentVilkårsvurderingThrows(behandlingId)

        val behandling = vilkårsvurdering.behandling

        if (restNyttVilkår.vilkårType == Vilkår.UTVIDET_BARNETRYGD) {
            validerFørLeggeTilUtvidetBarnetrygd(behandling, restNyttVilkår, vilkårsvurdering)

            behandlingstemaService.oppdaterBehandlingstemaForVilkår(
                behandling = behandling,
                overstyrtUnderkategori = BehandlingUnderkategori.UTVIDET,
            )
        }

        val personResultat =
            finnPersonResultatForPersonThrows(vilkårsvurdering.personResultater, restNyttVilkår.personIdent)

        muterPersonResultatPost(personResultat, restNyttVilkår.vilkårType)

        return vilkårsvurderingService.oppdater(vilkårsvurdering).personResultater.map { it.tilRestPersonResultat() }
    }

    private fun validerFørLeggeTilUtvidetBarnetrygd(
        behandling: Behandling,
        restNyttVilkår: RestNyttVilkår,
        vilkårsvurdering: Vilkårsvurdering,
    ) {
        if (!behandling.kanLeggeTilOgFjerneUtvidetVilkår() && !harUtvidetVilkår(vilkårsvurdering)) {
            throw FunksjonellFeil(
                melding =
                    "${restNyttVilkår.vilkårType.beskrivelse} kan ikke legges til for behandling ${behandling.id} " +
                        "med behandlingsårsak ${behandling.opprettetÅrsak.visningsnavn}",
                frontendFeilmelding =
                    "${restNyttVilkår.vilkårType.beskrivelse} kan ikke legges til " +
                        "for behandling ${behandling.id} med behandlingsårsak ${behandling.opprettetÅrsak.visningsnavn}",
            )
        }

        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandling.id)
        if (personopplysningGrunnlag.søkerOgBarn
                .single { it.aktør == personidentService.hentAktør(restNyttVilkår.personIdent) }
                .type != PersonType.SØKER
        ) {
            throw FunksjonellFeil(
                melding = "${Vilkår.UTVIDET_BARNETRYGD.beskrivelse} kan ikke legges til for BARN",
                frontendFeilmelding = "${Vilkår.UTVIDET_BARNETRYGD.beskrivelse} kan ikke legges til for BARN",
            )
        }
    }

    private fun harUtvidetVilkår(vilkårsvurdering: Vilkårsvurdering): Boolean =
        vilkårsvurdering.personResultater
            .find { it.erSøkersResultater() }
            ?.vilkårResultater
            ?.any { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD } == true

    private fun finnesUtvidetBarnetrydIForrigeBehandling(
        behandling: Behandling,
        personIdent: String,
    ): Boolean {
        val forrigeBehandlingSomErVedtatt =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)
        if (forrigeBehandlingSomErVedtatt != null) {
            val forrigeBehandlingsvilkårsvurdering =
                hentVilkårsvurdering(forrigeBehandlingSomErVedtatt.id) ?: throw Feil(
                    message =
                        "Forrige behandling $${forrigeBehandlingSomErVedtatt.id} " +
                            "har ikke en aktiv vilkårsvurdering",
                )
            val aktør = personidentService.hentAktør(personIdent)
            return forrigeBehandlingsvilkårsvurdering.personResultater
                .single { it.aktør == aktør }
                .vilkårResultater
                .any { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }
        }
        return false
    }

    private fun finnPersonResultatForPersonThrows(
        personResultater: Set<PersonResultat>,
        personIdent: String,
    ): PersonResultat {
        val aktør = personidentService.hentAktør(personIdent)
        return personResultater.find { it.aktør == aktør } ?: throw Feil(
            message = FANT_IKKE_VILKÅRSVURDERING_FOR_PERSON_FEILMELDING,
            frontendFeilmelding = "Fant ikke vilkårsvurdering for person med ident $personIdent",
        )
    }

    companion object {
        const val FANT_IKKE_AKTIV_VILKÅRSVURDERING_FEILMELDING = "Fant ikke aktiv vilkårsvurdering"
        const val FANT_IKKE_VILKÅRSVURDERING_FOR_PERSON_FEILMELDING = "Fant ikke vilkårsvurdering for person"
    }
}

fun Vilkår.gjelderAlltidFraBarnetsFødselsdato() = this == Vilkår.GIFT_PARTNERSKAP || this == Vilkår.UNDER_18_ÅR

fun SIVILSTANDTYPE.somForventetHosBarn() = this == SIVILSTANDTYPE.UOPPGITT || this == SIVILSTANDTYPE.UGIFT

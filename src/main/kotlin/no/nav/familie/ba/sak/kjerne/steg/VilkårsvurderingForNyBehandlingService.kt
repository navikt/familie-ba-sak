package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingMetrics
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.genererPersonResultatForPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.gjelderAlltidFraBarnetsFødselsdato
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class VilkårsvurderingForNyBehandlingService(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val behandlingService: BehandlingService,
    private val persongrunnlagService: PersongrunnlagService,
    private val personidentService: PersonidentService,
    private val behandlingstemaService: BehandlingstemaService,
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
    private val vilkårsvurderingMetrics: VilkårsvurderingMetrics
) {

    fun opprettVilkårsvurderingUtenomHovedflyt(
        behandling: Behandling,
        forrigeBehandlingSomErVedtatt: Behandling?,
        nyMigreringsdato: LocalDate? = null
    ) {
        when (behandling.opprettetÅrsak) {
            BehandlingÅrsak.ENDRE_MIGRERINGSDATO -> {
                genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
                    behandling = behandling,
                    forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErVedtatt
                        ?: throw Feil("Kan ikke opprette behandling med årsak 'Endre migreringsdato' hvis det ikke finnes en tidligere behandling som er iverksatt"),
                    nyMigreringsdato = nyMigreringsdato
                        ?: throw Feil("Kan ikke opprette behandling med årsak 'Endre migreringsdato' uten en migreringsdato")
                )
                // Lagre ned migreringsdato
                behandlingService.lagreNedMigreringsdato(nyMigreringsdato, behandling)
            }
            BehandlingÅrsak.HELMANUELL_MIGRERING -> {
                genererVilkårsvurderingForHelmanuellMigrering(
                    behandling = behandling,
                    nyMigreringsdato = nyMigreringsdato
                        ?: throw Feil("Kan ikke opprette behandling med årsak 'Helmanuell migrering' uten en migreringsdato")
                )
                // Lagre ned migreringsdato
                behandlingService.lagreNedMigreringsdato(nyMigreringsdato, behandling)
            }
            !in listOf(BehandlingÅrsak.SØKNAD, BehandlingÅrsak.FØDSELSHENDELSE) -> {
                initierVilkårsvurderingForBehandling(
                    behandling = behandling,
                    bekreftEndringerViaFrontend = true,
                    forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErVedtatt
                )
            }
            else -> logger.info(
                "Perioder i vilkårsvurdering generer ikke automatisk for " +
                    behandling.opprettetÅrsak.visningsnavn
            )
        }
    }

    fun genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
        behandling: Behandling,
        forrigeBehandlingSomErVedtatt: Behandling,
        nyMigreringsdato: LocalDate
    ): Vilkårsvurdering {

        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling).apply {
            personResultater =
                VilkårsvurderingForNyBehandlingUtils.lagPersonResultaterForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
                    vilkårsvurdering = this,
                    nyMigreringsdato = nyMigreringsdato,
                    forrigeBehandlingVilkårsvurdering = hentForrigeBehandlingVilkårsvurdering(
                        forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErVedtatt,
                        nyBehandlingId = behandling.id
                    ),
                    personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandling.id)
                )
        }
        return vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
    }

    private fun hentForrigeBehandlingVilkårsvurdering(
        forrigeBehandlingSomErVedtatt: Behandling,
        nyBehandlingId: Long
    ) = (
        vilkårsvurderingService
            .hentAktivForBehandling(forrigeBehandlingSomErVedtatt.id)
            ?: throw Feil(
                "Kan ikke kopiere vilkårsvurdering fra forrige behandling ${forrigeBehandlingSomErVedtatt.id}" +
                    "til behandling $nyBehandlingId"
            )
        )

    fun genererVilkårsvurderingForHelmanuellMigrering(
        behandling: Behandling,
        nyMigreringsdato: LocalDate
    ): Vilkårsvurdering {
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling).apply {
            personResultater = VilkårsvurderingForNyBehandlingUtils.lagPersonResultaterForHelmanuellMigrering(
                vilkårsvurdering = this,
                nyMigreringsdato = nyMigreringsdato,
                personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandling.id)
            )
        }
        return vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
    }

    fun initierVilkårsvurderingForBehandling(
        behandling: Behandling,
        bekreftEndringerViaFrontend: Boolean,
        forrigeBehandlingSomErVedtatt: Behandling? = null
    ): Vilkårsvurdering {
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandling.id)

        if (behandling.skalBehandlesAutomatisk && personopplysningGrunnlag.barna.isEmpty()) {
            throw IllegalStateException("PersonopplysningGrunnlag for fødselshendelse skal inneholde minst ett barn")
        }

        val aktivVilkårsvurdering = hentVilkårsvurdering(behandling.id)
        val barnaSomAlleredeErVurdert = aktivVilkårsvurdering?.personResultater?.mapNotNull {
            personopplysningGrunnlag.barna.firstOrNull { barn -> barn.aktør == it.aktør }
        }?.filter { it.type == PersonType.BARN }?.map { it.aktør.aktørId } ?: emptyList()

        val initiellVilkårsvurdering =
            genererInitiellVilkårsvurdering(
                behandling = behandling,
                barnaSomAlleredeErVurdert = barnaSomAlleredeErVurdert
            )

        return if (forrigeBehandlingSomErVedtatt != null && aktivVilkårsvurdering == null) {
            val vilkårsvurdering = genererVilkårsvurderingFraForrigeVedtattBehandling(
                initiellVilkårsvurdering,
                forrigeBehandlingSomErVedtatt,
                behandling,
                personopplysningGrunnlag
            )
            vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
        } else if (aktivVilkårsvurdering != null) {
            val (initieltSomErOppdatert, aktivtSomErRedusert) = VilkårsvurderingUtils.flyttResultaterTilInitielt(
                initiellVilkårsvurdering = initiellVilkårsvurdering,
                aktivVilkårsvurdering = aktivVilkårsvurdering,
                løpendeUnderkategori = behandlingstemaService.hentLøpendeUnderkategori(initiellVilkårsvurdering.behandling.fagsak.id),
                forrigeBehandlingVilkårsvurdering = if (forrigeBehandlingSomErVedtatt != null) hentVilkårsvurdering(
                    forrigeBehandlingSomErVedtatt.id
                ) else null
            )

            if (aktivtSomErRedusert.personResultater.isNotEmpty() && !bekreftEndringerViaFrontend) {
                throw FunksjonellFeil(
                    melding = "Saksbehandler forsøker å fjerne vilkår fra vilkårsvurdering",
                    frontendFeilmelding = VilkårsvurderingUtils.lagFjernAdvarsel(aktivtSomErRedusert.personResultater)
                )
            }
            vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = initieltSomErOppdatert)
        } else {
            vilkårsvurderingService.lagreInitielt(initiellVilkårsvurdering)
        }
    }

    fun genererInitiellVilkårsvurdering(
        behandling: Behandling,
        barnaSomAlleredeErVurdert: List<String>
    ): Vilkårsvurdering {
        return Vilkårsvurdering(behandling = behandling).apply {
            when {
                behandling.type == BehandlingType.MIGRERING_FRA_INFOTRYGD &&
                    behandling.opprettetÅrsak == BehandlingÅrsak.MIGRERING -> {
                    personResultater = lagVilkårsvurderingForMigreringsbehandling(this)
                }
                behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE -> {
                    personResultater = lagVilkårsvurderingForFødselshendelse(this, barnaSomAlleredeErVurdert)

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

    private fun genererVilkårsvurderingFraForrigeVedtattBehandling(
        initiellVilkårsvurdering: Vilkårsvurdering,
        forrigeBehandlingSomErVedtatt: Behandling,
        behandling: Behandling,
        personopplysningGrunnlag: PersonopplysningGrunnlag
    ): Vilkårsvurdering {
        val forrigeBehandlingsVilkårsvurdering = hentVilkårsvurderingThrows(forrigeBehandlingSomErVedtatt.id)
        val (vilkårsvurdering) = VilkårsvurderingUtils.flyttResultaterTilInitielt(
            aktivVilkårsvurdering = forrigeBehandlingsVilkårsvurdering,
            initiellVilkårsvurdering = initiellVilkårsvurdering,
            løpendeUnderkategori = behandlingstemaService.hentLøpendeUnderkategori(initiellVilkårsvurdering.behandling.fagsak.id),
            forrigeBehandlingVilkårsvurdering = forrigeBehandlingsVilkårsvurdering
        )

        if (behandling.type == BehandlingType.REVURDERING && behandling.opprettetÅrsak == BehandlingÅrsak.DØDSFALL_BRUKER) {
            vilkårsvurdering.personResultater.single { it.erSøkersResultater() }.vilkårResultater.forEach { vilkårResultat ->
                vilkårResultat.periodeTom = personopplysningGrunnlag.søker.dødsfall?.dødsfallDato
            }
        }
        endretUtbetalingAndelService.kopierEndretUtbetalingAndelFraForrigeBehandling(
            behandling,
            forrigeBehandlingSomErVedtatt
        )
        return vilkårsvurdering
    }

    private fun lagVilkårsvurderingForMigreringsbehandling(vilkårsvurdering: Vilkårsvurdering): Set<PersonResultat> {
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id)

        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = person.aktør)

            // NB Dette må gjøres om når vi skal begynne å migrere EØS-saker
            val ytelseType = if (person.type == PersonType.SØKER) when (vilkårsvurdering.behandling.underkategori) {
                BehandlingUnderkategori.UTVIDET -> YtelseType.UTVIDET_BARNETRYGD
                BehandlingUnderkategori.ORDINÆR -> YtelseType.ORDINÆR_BARNETRYGD
            } else YtelseType.ORDINÆR_BARNETRYGD

            val vilkårTyperForPerson = Vilkår.hentVilkårFor(person.type, ytelseType = ytelseType)

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
            }.toSortedSet(VilkårResultat.VilkårResultatComparator)

            personResultat.setSortedVilkårResultater(vilkårResultater)

            personResultat
        }.toSet()
    }

    private fun lagVilkårsvurderingForFødselshendelse(
        vilkårsvurdering: Vilkårsvurdering,
        barnaSomAlleredeErVurdert: List<String>
    ): Set<PersonResultat> {
        val barnaAktørSomAlleredeErVurdert = personidentService.hentAktørIder(barnaSomAlleredeErVurdert)

        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id)
        val annenForelder = personopplysningGrunnlag.annenForelder
        val eldsteBarnSomVurderesSinFødselsdato =
            personopplysningGrunnlag.barna.filter { !barnaAktørSomAlleredeErVurdert.contains(it.aktør) }
                .maxByOrNull { it.fødselsdato }?.fødselsdato
                ?: throw Feil("Finner ingen barn på persongrunnlag")

        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = person.aktør)

            val vilkårForPerson = Vilkår.hentVilkårFor(person.type)

            val vilkårResultater = vilkårForPerson.map { vilkår ->
                genererVilkårResultatForEtVilkårPåEnPerson(
                    person = person,
                    annenForelder = annenForelder,
                    eldsteBarnSinFødselsdato = eldsteBarnSomVurderesSinFødselsdato,
                    personResultat = personResultat,
                    vilkår = vilkår
                )
            }

            personResultat.setSortedVilkårResultater(vilkårResultater.toSet())

            personResultat
        }.toSet()
    }

    private fun lagManuellVilkårsvurdering(vilkårsvurdering: Vilkårsvurdering): Set<PersonResultat> {
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id)

        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            genererPersonResultatForPerson(vilkårsvurdering, person)
        }.toSet()
    }

    private fun lagTomVilkårsvurdering(vilkårsvurdering: Vilkårsvurdering): Set<PersonResultat> {
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id)

        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = person.aktør)

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
            }.toSortedSet(VilkårResultat.VilkårResultatComparator)

            personResultat.setSortedVilkårResultater(vilkårResultater)

            personResultat
        }.toSet()
    }

    private fun genererVilkårResultatForEtVilkårPåEnPerson(
        person: Person,
        eldsteBarnSinFødselsdato: LocalDate,
        personResultat: PersonResultat,
        vilkår: Vilkår,
        annenForelder: Person? = null,
    ): VilkårResultat {
        val automatiskVurderingResultat = vilkår.vurderVilkår(
            person = person,
            annenForelder = annenForelder,
            vurderFra = eldsteBarnSinFødselsdato
        )

        val fom = if (eldsteBarnSinFødselsdato >= person.fødselsdato) eldsteBarnSinFødselsdato else person.fødselsdato

        val tom: LocalDate? =
            if (vilkår == Vilkår.UNDER_18_ÅR) {
                person.fødselsdato.til18ÅrsVilkårsdato()
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

    private fun førstegangskjøringAvVilkårsvurdering(vilkårsvurdering: Vilkårsvurdering): Boolean {
        return hentVilkårsvurdering(vilkårsvurdering.behandling.id) == null
    }

    fun hentVilkårsvurdering(behandlingId: Long): Vilkårsvurdering? = vilkårsvurderingService.hentAktivForBehandling(
        behandlingId = behandlingId
    )

    fun hentVilkårsvurderingThrows(behandlingId: Long): Vilkårsvurdering =
        hentVilkårsvurdering(behandlingId) ?: throw Feil(
            message = "Fant ikke aktiv vilkårsvurdering for behandling $behandlingId",
            frontendFeilmelding = VilkårService.fantIkkeAktivVilkårsvurderingFeilmelding
        )

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}

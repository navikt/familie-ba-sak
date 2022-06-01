package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/test/vilkaarsvurdering")
@ProtectedWithClaims(issuer = "azuread")
@Validated
@Profile("!prod")
class VilkårsvurderingTestController(
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val aktørIdRepository: AktørIdRepository,
    private val kompetanseService: KompetanseService
) {

    @PostMapping()
    fun opprettBehandlingMedVilkårsvurdering(
        @RequestBody personresultater: Map<LocalDate, Map<Vilkår, String>>
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val personer = personresultater.tilPersoner()
            .map { it.copy(aktør = aktørIdRepository.saveAndFlush(it.aktør)) }

        val søker = personer.first { it.type == PersonType.SØKER }
        val barn = personer.filter { it.type == PersonType.BARN }

        fagsakService.hentEllerOpprettFagsak(søker.aktør.aktivFødselsnummer())

        val behandling = behandlingService.opprettBehandling(
            NyBehandling(
                kategori = BehandlingKategori.EØS,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                søkersIdent = søker.aktør.aktivFødselsnummer(),
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                søknadMottattDato = LocalDate.now(),
                barnasIdenter = barn.map { it.aktør.aktivFødselsnummer() }
            )
        )

        // Opprett persongrunnlag
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.save(
            lagTestPersonopplysningGrunnlag(behandling.id, *personer.toTypedArray())
        )

        // Opprett og lagre vilkårsvurdering
        val vilkårsvurdering = personresultater.tilVilkårsvurdering(
            behandling,
            personopplysningGrunnlag
        )

        vilkårsvurderingService.lagreInitielt(
            vilkårsvurdering
        )

        kompetanseService.tilpassKompetanserTilRegelverk(behandling.id)
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandling.id)))
    }
}

private fun Map<LocalDate, Map<Vilkår, String>>.tilPersoner(): List<Person> {
    return this.keys.mapIndexed { indeks, startTidspunkt ->
        when (indeks) {
            0 -> tilfeldigPerson(personType = PersonType.SØKER, fødselsdato = startTidspunkt)
            else -> tilfeldigPerson(personType = PersonType.BARN, fødselsdato = startTidspunkt)
        }
    }.map {
        it.copy(id = 0).also { it.sivilstander.clear() }
    } // tilfeldigPerson inneholder litt for mye, så fjerner det
}

fun Map<LocalDate, Map<Vilkår, String>>.tilVilkårsvurdering(
    behandling: Behandling,
    personopplysningGrunnlag: PersonopplysningGrunnlag
): Vilkårsvurdering {

    val builder = VilkårsvurderingBuilder<Måned>(behandling)

    this.entries.forEach { (startTidspunkt, vilkårsresultater) ->
        val person = personopplysningGrunnlag.personer.first { it.fødselsdato == startTidspunkt }

        val personBuilder = builder.forPerson(person, startTidspunkt.tilMånedTidspunkt())
        vilkårsresultater.forEach { (vilkår, tidslinje) -> personBuilder.medVilkår(tidslinje, vilkår) }
        personBuilder.byggPerson()
    }

    return builder.byggVilkårsvurdering()
}

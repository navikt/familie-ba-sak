import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.gjelderAlltidFraBarnetsFødselsdato
import java.time.LocalDate
import java.time.YearMonth

fun lagPersonResultaterForSøkerOgToBarn(
    vilkårsvurdering: Vilkårsvurdering,
    søkerAktør: Aktør,
    barn1Aktør: Aktør,
    barn2Aktør: Aktør,
    stønadFom: LocalDate,
    stønadTom: LocalDate,
    erDeltBosted: Boolean = false,
): Set<PersonResultat> =
    setOf(
        lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            person = lagPerson(type = PersonType.SØKER, aktør = søkerAktør),
            resultat = Resultat.OPPFYLT,
            periodeFom = stønadFom,
            periodeTom = stønadTom,
            lagFullstendigVilkårResultat = true,
            personType = PersonType.SØKER,
        ),
        lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            person =
                lagPerson(
                    type = PersonType.BARN,
                    aktør = barn1Aktør,
                    fødselsdato = stønadFom,
                ),
            resultat = Resultat.OPPFYLT,
            periodeFom = stønadFom,
            periodeTom = stønadTom,
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN,
            erDeltBosted = erDeltBosted,
        ),
        lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            person = lagPerson(type = PersonType.BARN, aktør = barn2Aktør, fødselsdato = stønadFom),
            resultat = Resultat.OPPFYLT,
            periodeFom = stønadFom,
            periodeTom = stønadTom,
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN,
            erDeltBosted = erDeltBosted,
        ),
    )

fun lagPersonResultat(
    vilkårsvurdering: Vilkårsvurdering,
    person: Person,
    resultat: Resultat,
    periodeFom: LocalDate?,
    periodeTom: LocalDate?,
    lagFullstendigVilkårResultat: Boolean = false,
    personType: PersonType = PersonType.BARN,
    vilkårType: Vilkår = Vilkår.BOSATT_I_RIKET,
    erDeltBosted: Boolean = false,
    erDeltBostedSkalIkkeDeles: Boolean = false,
    erEksplisittAvslagPåSøknad: Boolean? = null,
): PersonResultat {
    val personResultat =
        PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = person.aktør,
        )

    if (lagFullstendigVilkårResultat) {
        personResultat.setSortedVilkårResultater(
            Vilkår
                .hentVilkårFor(
                    personType = personType,
                    fagsakType = FagsakType.NORMAL,
                    behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
                ).map {
                    VilkårResultat(
                        personResultat = personResultat,
                        periodeFom = if (it.gjelderAlltidFraBarnetsFødselsdato()) person.fødselsdato else periodeFom,
                        periodeTom = periodeTom,
                        vilkårType = it,
                        resultat = resultat,
                        begrunnelse = "",
                        sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                        utdypendeVilkårsvurderinger =
                            listOfNotNull(
                                when {
                                    erDeltBosted && it == Vilkår.BOR_MED_SØKER -> UtdypendeVilkårsvurdering.DELT_BOSTED
                                    erDeltBostedSkalIkkeDeles && it == Vilkår.BOR_MED_SØKER -> UtdypendeVilkårsvurdering.DELT_BOSTED_SKAL_IKKE_DELES
                                    else -> null
                                },
                            ),
                        erEksplisittAvslagPåSøknad = erEksplisittAvslagPåSøknad,
                    )
                }.toSet(),
        )
    } else {
        personResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = personResultat,
                    periodeFom = periodeFom,
                    periodeTom = periodeTom,
                    vilkårType = vilkårType,
                    resultat = resultat,
                    begrunnelse = "",
                    sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                    erEksplisittAvslagPåSøknad = erEksplisittAvslagPåSøknad,
                ),
            ),
        )
    }
    return personResultat
}

fun vurderVilkårsvurderingTilInnvilget(
    vilkårsvurdering: Vilkårsvurdering,
    barn: Person,
    innvilgetFom: LocalDate? = null,
) {
    vilkårsvurdering.personResultater.filter { it.aktør == barn.aktør }.forEach { personResultat ->
        personResultat.vilkårResultater.forEach {
            if (it.vilkårType == Vilkår.UNDER_18_ÅR) {
                it.resultat = Resultat.OPPFYLT
                it.periodeFom = barn.fødselsdato
                it.periodeTom = barn.fødselsdato.plusYears(18)
            } else {
                it.resultat = Resultat.OPPFYLT
                it.periodeFom = innvilgetFom ?: LocalDate.now()
            }
        }
    }
}

fun lagVilkårsvurdering(
    id: Long = 0L,
    behandling: Behandling = lagBehandling(),
    aktiv: Boolean = true,
    lagPersonResultater: (vilkårsvurdering: Vilkårsvurdering) -> Set<PersonResultat> = {
        setOf(
            lagPersonResultat(
                vilkårsvurdering = it,
                aktør = behandling.fagsak.aktør,
            ),
        )
    },
): Vilkårsvurdering {
    val vilkårsvurdering =
        Vilkårsvurdering(
            id = id,
            behandling = behandling,
            aktiv = aktiv,
        )
    vilkårsvurdering.personResultater = lagPersonResultater(vilkårsvurdering)
    return vilkårsvurdering
}

fun lagPersonResultat(
    id: Long = 0L,
    vilkårsvurdering: Vilkårsvurdering,
    aktør: Aktør = randomAktør(),
    lagVilkårResultater: (personResultat: PersonResultat) -> Set<VilkårResultat> = {
        setOf(
            lagVilkårResultat(
                behandlingId = vilkårsvurdering.behandling.id,
                personResultat = it,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.now().minusMonths(1),
                periodeTom = LocalDate.now().plusYears(2),
                begrunnelse = "",
            ),
            lagVilkårResultat(
                behandlingId = vilkårsvurdering.behandling.id,
                personResultat = it,
                vilkårType = Vilkår.LOVLIG_OPPHOLD,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.now().minusMonths(1),
                periodeTom = LocalDate.now().plusYears(2),
                begrunnelse = "",
            ),
        )
    },
    lagAnnenVurderinger: (personResultat: PersonResultat) -> Set<AnnenVurdering> = {
        setOf(
            lagAnnenVurdering(
                personResultat = it,
            ),
        )
    },
): PersonResultat {
    val personResultat =
        PersonResultat(
            id = id,
            vilkårsvurdering = vilkårsvurdering,
            aktør = aktør,
        )
    personResultat.setSortedVilkårResultater(lagVilkårResultater(personResultat))
    personResultat.andreVurderinger.addAll(lagAnnenVurderinger(personResultat))
    return personResultat
}

fun lagAnnenVurdering(
    id: Long = 0L,
    personResultat: PersonResultat,
    resultat: Resultat = Resultat.OPPFYLT,
    type: AnnenVurderingType = AnnenVurderingType.OPPLYSNINGSPLIKT,
    begrunnelse: String? = null,
): AnnenVurdering =
    AnnenVurdering(
        id = id,
        personResultat = personResultat,
        resultat = resultat,
        type = type,
        begrunnelse = begrunnelse,
    )

fun lagVilkårsvurdering(
    søkerAktør: Aktør,
    behandling: Behandling,
    resultat: Resultat,
    søkerPeriodeFom: LocalDate? = LocalDate.now().minusMonths(1),
    søkerPeriodeTom: LocalDate? = LocalDate.now().plusYears(2),
    medAndreVurderinger: Boolean = true,
): Vilkårsvurdering {
    val vilkårsvurdering =
        Vilkårsvurdering(
            behandling = behandling,
        )
    val personResultat =
        PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = søkerAktør,
        )
    personResultat.setSortedVilkårResultater(
        setOf(
            VilkårResultat(
                personResultat = personResultat,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = resultat,
                periodeFom = søkerPeriodeFom,
                periodeTom = søkerPeriodeTom,
                begrunnelse = "",
                sistEndretIBehandlingId = behandling.id,
            ),
            VilkårResultat(
                personResultat = personResultat,
                vilkårType = Vilkår.LOVLIG_OPPHOLD,
                resultat = resultat,
                periodeFom = søkerPeriodeFom,
                periodeTom = søkerPeriodeTom,
                begrunnelse = "",
                sistEndretIBehandlingId = behandling.id,
            ),
        ),
    )
    if (medAndreVurderinger) {
        personResultat.andreVurderinger.add(
            AnnenVurdering(
                personResultat = personResultat,
                resultat = resultat,
                type = AnnenVurderingType.OPPLYSNINGSPLIKT,
                begrunnelse = null,
            ),
        )
    }

    vilkårsvurdering.personResultater = setOf(personResultat)
    return vilkårsvurdering
}

fun lagVilkårResultat(
    vilkår: Vilkår,
    vilkårRegelverk: Regelverk? = null,
    fom: YearMonth? = null,
    tom: YearMonth? = null,
    behandlingId: Long = 0,
) = VilkårResultat(
    personResultat = null,
    vilkårType = vilkår,
    resultat = Resultat.OPPFYLT,
    periodeFom = fom?.toLocalDate(),
    periodeTom = tom?.toLocalDate(),
    begrunnelse = "",
    sistEndretIBehandlingId = behandlingId,
    vurderesEtter = vilkårRegelverk,
)

fun lagVilkårResultat(
    id: Long = 0L,
    personResultat: PersonResultat? = null,
    vilkårType: Vilkår = Vilkår.BOSATT_I_RIKET,
    resultat: Resultat = Resultat.OPPFYLT,
    periodeFom: LocalDate? = LocalDate.of(2009, 12, 24),
    periodeTom: LocalDate? = LocalDate.of(2010, 1, 31),
    begrunnelse: String = "",
    behandlingId: Long = lagBehandling().id,
    utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList(),
    erEksplisittAvslagPåSøknad: Boolean = false,
    standardbegrunnelser: List<IVedtakBegrunnelse> = emptyList(),
    vurderesEtter: Regelverk? = null,
) = VilkårResultat(
    id = id,
    personResultat = personResultat,
    vilkårType = vilkårType,
    resultat = resultat,
    periodeFom = periodeFom,
    periodeTom = periodeTom,
    begrunnelse = begrunnelse,
    sistEndretIBehandlingId = behandlingId,
    utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger,
    erEksplisittAvslagPåSøknad = erEksplisittAvslagPåSøknad,
    standardbegrunnelser = standardbegrunnelser,
    vurderesEtter = vurderesEtter,
)

fun oppfyltVilkår(
    vilkår: Vilkår,
    regelverk: Regelverk? = null,
) = VilkårRegelverkResultat(
    vilkår = vilkår,
    regelverkResultat =
        when (regelverk) {
            Regelverk.NASJONALE_REGLER -> RegelverkResultat.OPPFYLT_NASJONALE_REGLER
            Regelverk.EØS_FORORDNINGEN -> RegelverkResultat.OPPFYLT_EØS_FORORDNINGEN
            else -> RegelverkResultat.OPPFYLT_REGELVERK_IKKE_SATT
        },
)

fun ikkeOppfyltVilkår(vilkår: Vilkår) =
    VilkårRegelverkResultat(
        vilkår = vilkår,
        regelverkResultat = RegelverkResultat.IKKE_OPPFYLT,
    )

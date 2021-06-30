package no.nav.familie.ba.sak.kjerne.automatiskvurdering

data class PersonResultat(val rolle: Rolle, val vilkår: List<Vilkårsresultat>)

enum class Rolle {
    MOR,
    BARN
}
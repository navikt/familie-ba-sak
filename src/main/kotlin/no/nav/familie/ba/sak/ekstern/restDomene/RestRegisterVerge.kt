package no.nav.familie.ba.sak.ekstern.restDomene

data class EnsligMindreårligInfo(val Navn: String, val Adresse: String, val Ident: String?)

data class InstitusjonInfo(val TSR: String)

data class RestRegisterVerge(val ensligMindreårligInfo: EnsligMindreårligInfo?, val institusjonInfo: InstitusjonInfo?)

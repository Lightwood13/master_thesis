package org.kry.thesis.domain

import org.hibernate.annotations.*
import org.hibernate.annotations.Cache
import java.time.*
import javax.persistence.*
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "country")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Country(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val name: String,

    @Column(nullable = false, unique = true)
    val code: String,

    @Column(nullable = false)
    val timezone: String,

    @Column
    var lastUpdated: LocalDate?
)

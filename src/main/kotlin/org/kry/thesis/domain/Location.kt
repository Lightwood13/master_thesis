package org.kry.thesis.domain

import org.hibernate.annotations.*
import org.hibernate.annotations.Cache
import java.time.*
import javax.persistence.*
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "location")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Location(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    val id: Long? = null,

    @Column(nullable = false)
    val latitude: Float,

    @Column(nullable = false)
    val longitude: Float,

    @ManyToOne
    @JoinColumn(nullable = false)
    val country: Country,

    @Column
    var lastUpdated: LocalDate? = null
)

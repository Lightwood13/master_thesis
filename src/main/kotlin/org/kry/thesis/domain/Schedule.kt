package org.kry.thesis.domain

import org.hibernate.annotations.*
import org.hibernate.annotations.Cache
import java.time.*
import javax.persistence.*
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "schedule")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Schedule(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    val id: Long? = null,

    @ManyToOne
    @JoinColumn(nullable = false)
    val heater: Heater,

    @Column(nullable = false)
    val scheduleDate: LocalDate,

    @Column(nullable = false)
    var data: String
)

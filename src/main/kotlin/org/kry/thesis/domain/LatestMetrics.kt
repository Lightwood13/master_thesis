package org.kry.thesis.domain

import org.hibernate.annotations.*
import org.hibernate.annotations.Cache
import java.time.*
import javax.persistence.*
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "latest_metrics")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class LatestMetrics(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    val id: Long? = null,

    @OneToOne
    val heater: Heater? = null,

    @Column(nullable = false)
    var metrics: String,

    @Column(nullable = false)
    var timestamp: Instant
)

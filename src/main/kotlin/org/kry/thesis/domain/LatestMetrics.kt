package org.kry.thesis.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table
import javax.validation.constraints.NotNull

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

    @get: NotNull
    @Column(name = "metrics", nullable = false)
    var metrics: String? = null
)

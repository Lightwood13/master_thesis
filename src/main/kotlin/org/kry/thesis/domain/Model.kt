package org.kry.thesis.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.Instant
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table(name = "model")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Model(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    val id: Long? = null,

    @JsonIgnore
    @ManyToOne
    @JoinColumn(nullable = false)
    val heater: Heater,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    val targetTemperature: Float,

    @Column(nullable = false)
    val minTemperature: Float,

    @Column(nullable = false)
    val maxTemperature: Float,

    @get: NotNull
    @Column(nullable = false)
    val createdOn: Instant,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ModelStatus
)

enum class ModelStatus {
    Created,
    Training,
    Working
}

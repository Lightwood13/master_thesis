package org.kry.thesis.domain

import com.fasterxml.jackson.annotation.*
import org.hibernate.annotations.*
import org.hibernate.annotations.Cache
import java.time.*
import javax.persistence.*
import javax.persistence.Entity
import javax.persistence.Table

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

    @Column(nullable = false)
    val createdOn: Instant,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ModelStatus
)

enum class ModelStatus {
    Created,
    Training,
    Trained
}

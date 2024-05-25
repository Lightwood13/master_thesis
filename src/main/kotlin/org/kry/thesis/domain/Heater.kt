package org.kry.thesis.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.Instant
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity
@Table(name = "heater")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Heater(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    val id: Long? = null,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "serial", nullable = false, unique = true)
    var serial: String,

    @Column(name = "password_hash", nullable = false)
    val passwordHash: String,

    @Column(name = "power", nullable = false)
    val power: Float,

    @ManyToOne
    @JsonIgnore
    var owner: User? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule", nullable = false)
    var schedule: Schedule,

    @Enumerated(EnumType.STRING)
    @Column(name = "calibration_status", nullable = false)
    var calibrationStatus: CalibrationStatus,

    @Column(name = "calibration_start")
    var calibrationStart: Instant? = null,

    @Column(name = "calibration_end")
    var calibrationEnd: Instant? = null,

    @Column(name = "calibration_percentage")
    var calibrationPercentage: Float? = null,

    @OneToOne
    var activeModel: Model? = null,

    @OneToMany(mappedBy = "heater", fetch = FetchType.EAGER)
    var models: MutableList<Model> = mutableListOf(),

    @OneToOne
    var location: Location? = null
)

enum class Schedule {
    IDLE,
    CALIBRATING,
    MODEL
}

enum class CalibrationStatus {
    NOT_CALIBRATED,
    CALIBRATION_IN_PROGRESS,
    CALIBRATED
}

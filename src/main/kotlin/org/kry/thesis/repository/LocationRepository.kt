package org.kry.thesis.repository

import org.kry.thesis.domain.Location
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LocationRepository : JpaRepository<Location, Long>

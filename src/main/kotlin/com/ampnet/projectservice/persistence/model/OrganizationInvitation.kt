package com.ampnet.projectservice.persistence.model

import com.ampnet.projectservice.enums.OrganizationRole
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "organization_invitation")
@Suppress("LongParameterList")
class OrganizationInvitation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    var email: String,

    @Column(nullable = false)
    var invitedByUserUuid: UUID,

    @Column(name = "role_id", nullable = false)
    var role: OrganizationRole,

    @Column(nullable = false)
    var createdAt: ZonedDateTime,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizationUuid", nullable = false)
    var organization: Organization
)

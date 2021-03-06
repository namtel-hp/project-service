package com.ampnet.projectservice.persistence.model

import com.ampnet.projectservice.enums.OrganizationPrivilegeType
import com.ampnet.projectservice.enums.OrganizationRole
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "organization_membership")
class OrganizationMembership(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    var organizationUuid: UUID,

    @Column(nullable = false)
    var userUuid: UUID,

    @Column(name = "role_id", nullable = false)
    var role: OrganizationRole,

    @Column(nullable = false)
    var createdAt: ZonedDateTime
) {
    constructor(organizationUuid: UUID, userUuid: UUID, role: OrganizationRole, createdAt: ZonedDateTime) : this(
        0, organizationUuid, userUuid, role, createdAt
    )

    private fun getPrivileges(): List<OrganizationPrivilegeType> = role.getPrivileges()

    fun hasPrivilegeToSeeOrganizationUsers(): Boolean = getPrivileges().contains(OrganizationPrivilegeType.PR_USERS)

    fun hasPrivilegeToWriteOrganizationUsers(): Boolean = getPrivileges().contains(OrganizationPrivilegeType.PW_USERS)

    fun hasPrivilegeToWriteOrganization(): Boolean = getPrivileges().contains(OrganizationPrivilegeType.PW_ORG)

    fun hasPrivilegeToWriteProject(): Boolean = getPrivileges().contains(OrganizationPrivilegeType.PW_PROJECT)
}

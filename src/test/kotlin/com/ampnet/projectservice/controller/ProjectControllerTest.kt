package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.request.ImageLinkListRequest
import com.ampnet.projectservice.controller.pojo.request.LinkRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectRequest
import com.ampnet.projectservice.controller.pojo.response.DocumentResponse
import com.ampnet.projectservice.controller.pojo.response.ProjectResponse
import com.ampnet.projectservice.enums.OrganizationRoleType
import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.security.WithMockCrowdfoundUser
import com.ampnet.projectservice.controller.pojo.request.ProjectUpdateRequest
import com.ampnet.projectservice.controller.pojo.response.ProjectListResponse
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class ProjectControllerTest : ControllerTestBase() {

    private val projectPath = "/project"

    private lateinit var organization: Organization
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllProjects()
        databaseCleanerService.deleteAllOrganizations()
        organization = createOrganization("Test organization", userUuid)
        testContext = TestContext()
    }

    @Test
    fun mustBeAbleToGetSpecificProject() {
        suppose("Project exists") {
            testContext.project = createProject("My project", organization, userUuid)
        }

        verify("Project response is valid") {
            val result = mockMvc.perform(get("/public/project/${testContext.project.uuid}"))
                .andExpect(status().isOk)
                .andReturn()

            val projectResponse: ProjectResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(projectResponse.uuid).isEqualTo(testContext.project.uuid)
            assertThat(projectResponse.name).isEqualTo(testContext.project.name)
            assertThat(projectResponse.description).isEqualTo(testContext.project.description)
            assertThat(projectResponse.location).isEqualTo(testContext.project.location)
            assertThat(projectResponse.locationText).isEqualTo(testContext.project.locationText)
            assertThat(projectResponse.returnOnInvestment).isEqualTo(testContext.project.returnOnInvestment)
            assertThat(projectResponse.startDate).isEqualTo(testContext.project.startDate)
            assertThat(projectResponse.endDate).isEqualTo(testContext.project.endDate)
            assertThat(projectResponse.expectedFunding).isEqualTo(testContext.project.expectedFunding)
            assertThat(projectResponse.currency).isEqualTo(testContext.project.currency)
            assertThat(projectResponse.minPerUser).isEqualTo(testContext.project.minPerUser)
            assertThat(projectResponse.maxPerUser).isEqualTo(testContext.project.maxPerUser)
            assertThat(projectResponse.mainImage).isEqualTo(testContext.project.mainImage)
            assertThat(projectResponse.gallery).isEqualTo(testContext.project.gallery.orEmpty())
            assertThat(projectResponse.news).isEqualTo(testContext.project.newsLinks.orEmpty())
            assertThat(projectResponse.active).isEqualTo(testContext.project.active)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustReturnErrorForMissingOrganization() {
        suppose("User is not a member of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
        }

        verify("Controller will return forbidden for missing organization membership") {
            val request = createProjectRequest(UUID.randomUUID(), "Error project")
            mockMvc.perform(
                    post(projectPath)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustReturnErrorForUserWithoutOrganizationMembership() {
        verify("Controller will forbidden for user without membership to create project") {
            val request = createProjectRequest(organization.uuid, "Error project")
            mockMvc.perform(
                    post(projectPath)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustReturnErrorForUserOrganizationMembership() {
        suppose("User is a member of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_MEMBER)
        }

        verify("Controller will return forbidden for missing organization membership") {
            val request = createProjectRequest(organization.uuid, "Error project")
            mockMvc.perform(
                    post(projectPath)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToCreateProject() {
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }

        verify("Controller will return create project") {
            testContext.projectRequest = createProjectRequest(organization.uuid, "Das project")
            val result = mockMvc.perform(
                    post(projectPath)
                            .content(objectMapper.writeValueAsString(testContext.projectRequest))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk)
                    .andReturn()

            val projectResponse: ProjectResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(projectResponse.uuid).isNotNull()
            assertThat(projectResponse.name).isEqualTo(testContext.projectRequest.name)
            assertThat(projectResponse.description).isEqualTo(testContext.projectRequest.description)
            assertThat(projectResponse.location).isEqualTo(testContext.projectRequest.location)
            assertThat(projectResponse.locationText).isEqualTo(testContext.projectRequest.locationText)
            assertThat(projectResponse.returnOnInvestment).isEqualTo(testContext.projectRequest.returnOnInvestment)
            assertThat(projectResponse.startDate).isEqualTo(testContext.projectRequest.startDate)
            assertThat(projectResponse.endDate).isEqualTo(testContext.projectRequest.endDate)
            assertThat(projectResponse.expectedFunding).isEqualTo(testContext.projectRequest.expectedFunding)
            assertThat(projectResponse.currency).isEqualTo(testContext.projectRequest.currency)
            assertThat(projectResponse.minPerUser).isEqualTo(testContext.projectRequest.minPerUser)
            assertThat(projectResponse.maxPerUser).isEqualTo(testContext.projectRequest.maxPerUser)
            assertThat(projectResponse.active).isEqualTo(testContext.projectRequest.active)
            assertThat(projectResponse.mainImage).isNullOrEmpty()
            assertThat(projectResponse.gallery).isNullOrEmpty()
            assertThat(projectResponse.news).isNullOrEmpty()

            testContext.projectUuid = projectResponse.uuid
        }
        verify("Project is stored in database") {
            val optionalProject = projectRepository.findByIdWithOrganization(testContext.projectUuid)
            assertThat(optionalProject).isPresent
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToUpdateProject() {
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("Project exists") {
            testContext.project = createProject("My project", organization, userUuid)
        }

        verify("Admin can update project") {
            testContext.projectUpdateRequest =
                    ProjectUpdateRequest("new name", "description", "newLoc", "New Location", "0.1%", false)
            val result = mockMvc.perform(
                    post("$projectPath/${testContext.project.uuid}")
                            .content(objectMapper.writeValueAsString(testContext.projectUpdateRequest))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk)
                    .andReturn()

            val projectResponse: ProjectResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(projectResponse.uuid).isEqualTo(testContext.project.uuid)
            assertThat(projectResponse.name).isEqualTo(testContext.projectUpdateRequest.name)
            assertThat(projectResponse.description).isEqualTo(testContext.projectUpdateRequest.description)
            assertThat(projectResponse.location).isEqualTo(testContext.projectUpdateRequest.location)
            assertThat(projectResponse.locationText).isEqualTo(testContext.projectUpdateRequest.locationText)
            assertThat(projectResponse.returnOnInvestment)
                    .isEqualTo(testContext.projectUpdateRequest.returnOnInvestment)
            assertThat(projectResponse.active).isEqualTo(testContext.projectUpdateRequest.active)
        }
        verify("Project is updated") {
            val optionalProject = projectRepository.findById(testContext.project.uuid)
            assertThat(optionalProject).isPresent
            val updatedProject = optionalProject.get()
            assertThat(updatedProject.name).isEqualTo(testContext.projectUpdateRequest.name)
            assertThat(updatedProject.description).isEqualTo(testContext.projectUpdateRequest.description)
            assertThat(updatedProject.location).isEqualTo(testContext.projectUpdateRequest.location)
            assertThat(updatedProject.locationText).isEqualTo(testContext.projectUpdateRequest.locationText)
            assertThat(updatedProject.returnOnInvestment).isEqualTo(testContext.projectUpdateRequest.returnOnInvestment)
            assertThat(updatedProject.active).isEqualTo(testContext.projectUpdateRequest.active)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetAllProjects() {
        suppose("Project exists") {
            testContext.project = createProject("My project", organization, userUuid)
        }
        suppose("Another organization has project") {
            val secondOrganization = createOrganization("Second organization", userUuid)
            testContext.secondProject = createProject("Second project", secondOrganization, userUuid)
        }

        verify("Controller will return all projects") {
            val result = mockMvc.perform(get(projectPath))
                    .andExpect(status().isOk)
                    .andReturn()

            val projectsResponse: ProjectListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(projectsResponse.projects).hasSize(2)
            assertThat(projectsResponse.projects.map { it.uuid })
                    .containsAll(listOf(testContext.project.uuid, testContext.secondProject.uuid))
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetListOfProjectsForOrganization() {
        suppose("Organization has 3 projects") {
            testContext.project = createProject("Project 1", organization, userUuid)
            createProject("Project 2", organization, userUuid)
            createProject("Project 3", organization, userUuid)
        }
        suppose("Second organization has project") {
            val secondOrganization = createOrganization("Second organization", userUuid)
            testContext.secondProject = createProject("Second project", secondOrganization, userUuid)
        }

        verify("Controller will return all projects for specified organization") {
            val result = mockMvc.perform(get("$projectPath/organization/${organization.uuid}"))
                    .andExpect(status().isOk)
                    .andReturn()

            val projectListResponse: ProjectListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(projectListResponse.projects).hasSize(3)
            assertThat(projectListResponse.projects.map { it.uuid }).doesNotContain(testContext.secondProject.uuid)

            val filterResponse = projectListResponse.projects.filter { it.uuid == testContext.project.uuid }
            assertThat(filterResponse).hasSize(1)
            val projectResponse = filterResponse.first()
            assertThat(projectResponse.name).isEqualTo(testContext.project.name)
            assertThat(projectResponse.description).isEqualTo(testContext.project.description)
            assertThat(projectResponse.location).isEqualTo(testContext.project.location)
            assertThat(projectResponse.locationText).isEqualTo(testContext.project.locationText)
            assertThat(projectResponse.returnOnInvestment).isEqualTo(testContext.project.returnOnInvestment)
            assertThat(projectResponse.startDate).isEqualTo(testContext.project.startDate)
            assertThat(projectResponse.endDate).isEqualTo(testContext.project.endDate)
            assertThat(projectResponse.expectedFunding).isEqualTo(testContext.project.expectedFunding)
            assertThat(projectResponse.currency).isEqualTo(testContext.project.currency)
            assertThat(projectResponse.minPerUser).isEqualTo(testContext.project.minPerUser)
            assertThat(projectResponse.maxPerUser).isEqualTo(testContext.project.maxPerUser)
            assertThat(projectResponse.mainImage).isEqualTo(testContext.project.mainImage)
            assertThat(projectResponse.active).isEqualTo(testContext.project.active)
            assertThat(projectResponse.news).isEqualTo(testContext.project.newsLinks.orEmpty())
            assertThat(projectResponse.gallery).isEqualTo(testContext.project.newsLinks.orEmpty())
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustReturnForbiddenIfUserIsMissingOrgPrivileges() {
        suppose("User is a member of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_MEMBER)
        }
        suppose("Project exists") {
            testContext.project = createProject("My project", organization, userUuid)
        }

        verify("User cannot update project") {
            testContext.projectUpdateRequest =
                    ProjectUpdateRequest("new name", "description", "newLoc", "New Location", "0.1%", false)
            mockMvc.perform(
                    post("$projectPath/${testContext.project.uuid}")
                            .content(objectMapper.writeValueAsString(testContext.projectUpdateRequest))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustReturnErrorForUpdatingNonExistingProject() {
        verify("User cannot update non existing project") {
            testContext.projectUpdateRequest =
                    ProjectUpdateRequest("new name", "description", null, null, null, false)
            val response = mockMvc.perform(
                    post("$projectPath/${UUID.randomUUID()}")
                            .content(objectMapper.writeValueAsString(testContext.projectUpdateRequest))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andReturn()
            verifyResponseErrorCode(response, ErrorCode.PRJ_MISSING)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToAddDocumentForProject() {
        suppose("Project exists") {
            testContext.project = createProject("Project", organization, userUuid)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("File service will store document") {
            testContext.multipartFile = MockMultipartFile("file", "test.txt",
                    "text/plain", "Some document data".toByteArray())
            Mockito.`when`(
                    cloudStorageService.saveFile(testContext.multipartFile.originalFilename,
                            testContext.multipartFile.bytes)
            ).thenReturn(testContext.documentLink)
        }

        verify("User can add document") {
            val result = mockMvc.perform(
                    RestDocumentationRequestBuilders.fileUpload("$projectPath/${testContext.project.uuid}/document")
                            .file(testContext.multipartFile))
                    .andExpect(status().isOk)
                    .andReturn()

            val documentResponse: DocumentResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(documentResponse.id).isNotNull()
            assertThat(documentResponse.name).isEqualTo(testContext.multipartFile.originalFilename)
            assertThat(documentResponse.size).isEqualTo(testContext.multipartFile.size)
            assertThat(documentResponse.type).isEqualTo(testContext.multipartFile.contentType)
            assertThat(documentResponse.link).isEqualTo(testContext.documentLink)
        }
        verify("Document is stored in database and connected to project") {
            val optionalProject = projectRepository.findByIdWithAllData(testContext.project.uuid)
            assertThat(optionalProject).isPresent
            val projectDocuments = optionalProject.get().documents ?: fail("Project documents must not be null")
            assertThat(projectDocuments).hasSize(1)

            val document = projectDocuments[0]
            assertThat(document.name).isEqualTo(testContext.multipartFile.originalFilename)
            assertThat(document.size).isEqualTo(testContext.multipartFile.size)
            assertThat(document.type).isEqualTo(testContext.multipartFile.contentType)
            assertThat(document.link).isEqualTo(testContext.documentLink)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToRemoveProjectDocument() {
        suppose("Project exists") {
            testContext.project = createProject("Project", organization, userUuid)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("Project has some documents") {
            testContext.document =
                    createProjectDocument(testContext.project, userUuid, "Prj doc", testContext.documentLink)
            createProjectDocument(testContext.project, userUuid, "Sec.pdf", "Sec-some-link.pdf")
        }

        verify("User admin can delete document") {
            mockMvc.perform(
                    delete("$projectPath/${testContext.project.uuid}/document/${testContext.document.id}"))
                    .andExpect(status().isOk)
        }
        verify("Document is deleted") {
            val project = projectRepository.findByIdWithAllData(testContext.project.uuid)
            assertThat(project).isPresent
            val documents = project.get().documents
            assertThat(documents).hasSize(1).doesNotContain(testContext.document)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToAddMainImage() {
        suppose("Project exists") {
            testContext.project = createProject("Project", organization, userUuid)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("File service will store image") {
            testContext.multipartFile = MockMultipartFile("image", "image.png",
                    "image/png", "ImageData".toByteArray())
            Mockito.`when`(
                    cloudStorageService.saveFile(testContext.multipartFile.originalFilename,
                            testContext.multipartFile.bytes)
            ).thenReturn(testContext.imageLink)
        }

        verify("User can add main image") {
            mockMvc.perform(
                    RestDocumentationRequestBuilders.fileUpload("$projectPath/${testContext.project.uuid}/image/main")
                            .file(testContext.multipartFile))
                    .andExpect(status().isOk)
        }
        verify("Document is stored in database and connected to project") {
            val optionalProject = projectRepository.findByIdWithAllData(testContext.project.uuid)
            assertThat(optionalProject).isPresent
            assertThat(optionalProject.get().mainImage).isEqualTo(testContext.imageLink)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToAddGalleryImage() {
        suppose("Project exists") {
            testContext.project = createProject("Project", organization, userUuid)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("File service will store image") {
            testContext.multipartFile = MockMultipartFile("image", "image.png",
                    "image/png", "ImageData".toByteArray())
            Mockito.`when`(
                    cloudStorageService.saveFile(testContext.multipartFile.originalFilename,
                            testContext.multipartFile.bytes)
            ).thenReturn(testContext.imageLink)
        }

        verify("User can add main image") {
            mockMvc.perform(
                    RestDocumentationRequestBuilders.fileUpload("$projectPath/${testContext.project.uuid}/image/gallery")
                            .file(testContext.multipartFile))
                    .andExpect(status().isOk)
        }
        verify("Document is stored in database and connected to project") {
            val optionalProject = projectRepository.findByIdWithAllData(testContext.project.uuid)
            assertThat(optionalProject).isPresent
            assertThat(optionalProject.get().gallery).contains(testContext.imageLink)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToRemoveGalleryImage() {
        suppose("Project exists") {
            testContext.project = createProject("Project", organization, userUuid)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("Project has gallery images") {
            testContext.project.gallery = listOf("image-link-1", "image-link-2", "image-link-3")
            projectRepository.save(testContext.project)
        }

        verify("User can add main image") {
            val request = ImageLinkListRequest(listOf("image-link-1"))
            mockMvc.perform(
                    delete("$projectPath/${testContext.project.uuid}/image/gallery")
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk)
        }
        verify("Document is stored in database and connected to project") {
            val optionalProject = projectRepository.findByIdWithAllData(testContext.project.uuid)
            assertThat(optionalProject).isPresent
            assertThat(optionalProject.get().gallery).contains("image-link-2", "image-link-3")
            assertThat(optionalProject.get().gallery).doesNotContain("image-link-1")
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToAddNews() {
        suppose("Project exists") {
            testContext.project = createProject("Project", organization, userUuid)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }

        verify("User can add news link") {
            val request = LinkRequest(testContext.newsLink)
            mockMvc.perform(
                    post("$projectPath/${testContext.project.uuid}/news")
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk)
        }
        verify("News link is added to project") {
            val optionalProject = projectRepository.findById(testContext.project.uuid)
            assertThat(optionalProject).isPresent
            assertThat(optionalProject.get().newsLinks).hasSize(1).contains(testContext.newsLink)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToRemoveNews() {
        suppose("Project exists") {
            testContext.project = createProject("Project", organization, userUuid)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("Project has news links") {
            testContext.project.newsLinks = listOf(testContext.newsLink, "link-2", "link-3")
            projectRepository.save(testContext.project)
        }

        verify("User can remove news link") {
            val request = LinkRequest(testContext.newsLink)
            mockMvc.perform(
                    delete("$projectPath/${testContext.project.uuid}/news")
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk)
        }
        verify("News link is removed to project") {
            val optionalProject = projectRepository.findById(testContext.project.uuid)
            assertThat(optionalProject).isPresent
            assertThat(optionalProject.get().newsLinks).hasSize(2).doesNotContain(testContext.newsLink)
        }
    }

    private class TestContext {
        lateinit var project: Project
        lateinit var secondProject: Project
        lateinit var projectRequest: ProjectRequest
        lateinit var projectUpdateRequest: ProjectUpdateRequest
        lateinit var multipartFile: MockMultipartFile
        lateinit var document: Document
        val documentLink = "link"
        val imageLink = "image-link"
        val newsLink = "news-link"
        lateinit var projectUuid: UUID
    }
}
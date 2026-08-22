package xyz.siwane.shizucorefetch.network

import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.HeaderMap
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// ============ نماذج البيانات الحقيقية القادمة من GitHub API ============

@JsonClass(generateAdapter = true)
data class GitHubUser(
    val login: String,
    val id: Long,
    val avatar_url: String?,
    val html_url: String?,
    val name: String?,
    val email: String?,
    val bio: String?,
    val public_repos: Int = 0,
    val followers: Int = 0,
    val following: Int = 0
)

@JsonClass(generateAdapter = true)
data class GitHubRepo(
    val id: Long,
    val name: String,
    val full_name: String,
    val description: String?,
    val html_url: String,
    val stargazers_count: Int = 0,
    val forks_count: Int = 0,
    val language: String?,
    val owner: GitHubOwner?,
    val topics: List<String>? = null,
    val size: Long = 0
)

@JsonClass(generateAdapter = true)
data class GitHubOwner(
    val login: String,
    val avatar_url: String?,
    val html_url: String?
)

@JsonClass(generateAdapter = true)
data class GitHubSearchResult(
    val total_count: Int,
    val items: List<GitHubRepo>
)

@JsonClass(generateAdapter = true)
data class GitHubRelease(
    val id: Long,
    val tag_name: String,
    val name: String?,
    val body: String?,
    val assets: List<GitHubReleaseAsset>,
    val published_at: String?
)

@JsonClass(generateAdapter = true)
data class GitHubReleaseAsset(
    val id: Long,
    val name: String,
    val size: Long,
    val browser_download_url: String,
    val download_count: Int = 0
)

@JsonClass(generateAdapter = true)
data class GitHubIssue(
    val id: Long,
    val number: Int,
    val title: String,
    val body: String?,
    val user: GitHubOwner?,
    val html_url: String,
    val state: String,
    val comments: Int = 0
)

@JsonClass(generateAdapter = true)
data class GitHubComment(
    val id: Long,
    val body: String,
    val user: GitHubOwner?,
    val created_at: String,
    val updated_at: String?,
    val html_url: String?
)

@JsonClass(generateAdapter = true)
data class GitHubReaction(
    val id: Long,
    val content: String,
    val user: GitHubOwner?
)

@JsonClass(generateAdapter = true)
data class CreateCommentRequest(val body: String)

@JsonClass(generateAdapter = true)
data class CreateReactionRequest(val content: String)

@JsonClass(generateAdapter = true)
data class CreateIssueRequest(val title: String, val body: String, val labels: List<String>? = null)

// ============ واجهة الاتصال ============

@JsonClass(generateAdapter = true)
data class GitHubRateLimitResponse(val resources: GitHubRateLimitResources, val rate: GitHubRateLimitDetail? = null)

@JsonClass(generateAdapter = true)
data class GitHubRateLimitResources(val core: GitHubRateLimitDetail, val search: GitHubRateLimitDetail? = null)

@JsonClass(generateAdapter = true)
data class GitHubRateLimitDetail(val limit: Int, val remaining: Int, val reset: Long, val used: Int)

interface GitHubApiService {

    @GET("user")
    suspend fun getAuthenticatedUser(@Header("Authorization") auth: String): Response<GitHubUser>

    @GET("rate_limit")
    suspend fun getRateLimit(@HeaderMap headers: Map<String, String>): Response<GitHubRateLimitResponse>

    @GET("users/{username}")
    suspend fun getUser(@Path("username") username: String): Response<GitHubUser>

    @GET("user/repos")
    suspend fun getAuthenticatedUserRepos(
        @Header("Authorization") auth: String,
        @Query("per_page") perPage: Int = 30,
        @Query("sort") sort: String = "updated"
    ): Response<List<GitHubRepo>>

    @GET("repos/{owner}/{repo}")
    suspend fun getRepo(@Path("owner") owner: String, @Path("repo") repo: String): Response<GitHubRepo>

    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(@Path("owner") owner: String, @Path("repo") repo: String): Response<GitHubRelease>

    @GET("repos/{owner}/{repo}/issues")
    suspend fun listIssues(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("labels") labels: String? = null
    ): Response<List<GitHubIssue>>

    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreateIssueRequest
    ): Response<GitHubIssue>

    @GET("repos/{owner}/{repo}/issues/{issue_number}/comments")
    suspend fun listComments(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issue_number") issueNumber: Int,
        @Query("per_page") perPage: Int = 100
    ): Response<List<GitHubComment>>

    @POST("repos/{owner}/{repo}/issues/{issue_number}/comments")
    suspend fun addComment(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issue_number") issueNumber: Int,
        @Body body: CreateCommentRequest
    ): Response<GitHubComment>

    @PATCH("repos/{owner}/{repo}/issues/comments/{comment_id}")
    suspend fun editComment(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("comment_id") commentId: Long,
        @Body body: CreateCommentRequest
    ): Response<GitHubComment>

    @DELETE("repos/{owner}/{repo}/issues/comments/{comment_id}")
    suspend fun deleteComment(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("comment_id") commentId: Long
    ): Response<Unit>

    @GET("repos/{owner}/{repo}/issues/comments/{comment_id}/reactions")
    suspend fun listCommentReactions(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("comment_id") commentId: Long
    ): Response<List<GitHubReaction>>

    @POST("repos/{owner}/{repo}/issues/comments/{comment_id}/reactions")
    suspend fun addCommentReaction(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("comment_id") commentId: Long,
        @Body body: CreateReactionRequest
    ): Response<GitHubReaction>

    @DELETE("repos/{owner}/{repo}/issues/comments/{comment_id}/reactions/{reaction_id}")
    suspend fun deleteCommentReaction(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("comment_id") commentId: Long,
        @Path("reaction_id") reactionId: Long
    ): Response<Unit>
}

fun bearer(token: String) = "Bearer $token"

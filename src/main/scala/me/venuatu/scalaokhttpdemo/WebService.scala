package me.venuatu.scalaokhttpdemo

import java.io.IOException
import scala.concurrent.{Future, Promise}

import com.squareup.okhttp.{Callback, OkHttpClient, Request, Response}
import spray.json._, DefaultJsonProtocol._

import scala.concurrent.ExecutionContext.Implicits.global

object WebService {
  // See https://developer.github.com/v3/users/#users
  case class GithubUser(login: String, id: Long, avatarUrl: String, url: String, htmlUrl: String, followersUrl: String, followingUrl: String, gistsUrl: String, starredUrl: String, reposUrl: String, publicRepos: Int, publicGists: Int, followers: Int, following: Int, createdAt: String, updatedAt: String)

  implicit val userFormat = jsonFormat(GithubUser, "login", "id", "avatar_url", "url", "html_url", "followers_url", "following_url", "gists_url", "starred_url", "repos_url", "public_repos", "public_gists", "followers", "following", "created_at", "updated_at")
}

class WebService {
  val client = new OkHttpClient()

  def doRequest(req: Request): Future[Response] = {
    val promise = Promise[Response]()
    client.newCall(req).enqueue(new Callback {
      override def onResponse(response: Response): Unit = promise.success(response)
      override def onFailure(request: Request, e: IOException): Unit = promise.failure(e)
    })
    promise.future
  }

  def getGithubUser(username: String): Future[WebService.GithubUser] = {
    val req = new Request.Builder().url(s"https://api.github.com/users/$username")
              .header("User-Agent", "venuatu android-scala-okhttp-demo").get().build()
    doRequest(req).map {resp =>
      if (!resp.isSuccessful) {
        throw new Exception(s"${resp.code()}\n${resp.body().string()}")
      } else {
        resp.body().string().parseJson.convertTo[WebService.GithubUser]
      }
    }
  }
}

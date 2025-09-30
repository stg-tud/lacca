package kanban

import com.raquo.laminar.api.L.{*, given}
import kanban.routing.currentView
import org.scalajs.dom
import org.scalajs.dom.document
import kanban.service.UcanTokenStore
import scala.concurrent.ExecutionContext.Implicits.global
import kanban.auth.DexieProofResolver
import kanban.auth.ProjectUcanService
import kanban.auth.Capabilities
import rdts.base.Uid
import scala.util.{Success, Failure}

object main {
  def main(args: Array[String]): Unit = {
    lazy val rootDivElement = dom.document.getElementById("root")

    DexieProofResolver
      .seedFromDb()
      .foreach(_ => dom.console.log("proof cache seeded"))

    // Smoke tests for ProjectUcanService query methods
    runProjectUcanServiceSmokeTests()

    // following element inserts the current page's content to root element
    lazy val appElement = {
      div(
        cls := "pageContent",
        child <-- currentView
      )
    }
    render(rootDivElement, appElement)
  }

  private def runProjectUcanServiceSmokeTests(): Unit = {
    dom.console.log("Starting ProjectUcanService smoke tests...")

    // Test 1: Get current user DID
    dom.console.log("Test 1: Getting current user DID...")
    ProjectUcanService.currentUserDid().onComplete {
      case Success(did) =>
        dom.console.log(s"Test 1 Current user DID: $did")
        runTestsWithUserDid(did)
      case Failure(e) =>
        dom.console.log(
          s"Test 1 Failed to get current user DID: ${e.getMessage}"
        )
    }
  }

  private def runTestsWithUserDid(userDid: String): Unit = {

    val testProjectId1 = Uid.gen()
    val testProjectId2 = Uid.gen()

    dom.console.log(
      s"Using test project IDs: ${testProjectId1.delegate}, ${testProjectId2.delegate}"
    )

    // Test 2: Check if user owns a non-existent project (should return false)
    dom.console.log(
      "Test 2: Checking ownership of non-existent project..."
    )
    ProjectUcanService.userOwnsProject(testProjectId1, userDid).onComplete {
      case Success(owns) =>
        dom.console.log(
          s"User owns non-existent project: $owns (expected: false)"
        )
      case Failure(e) =>
        dom.console.log(
          s"Failed to check project ownership: ${e.getMessage}"
        )
    }

    // Test 3: Get tokens for current user
    dom.console.log("Test 3: Getting tokens for current user...")
    ProjectUcanService.tokensForUser(userDid).onComplete {
      case Success(tokens) =>
        dom.console
          .log(s"Found ${tokens.length} tokens for user")
        tokens.foreach { token =>
          dom.console.log(
            s"Token CID: ${token.cid}, Issuer: ${token.iss}, Audience: ${token.aud}"
          )
        }
      case Failure(e) =>
        dom.console
          .log(s"Failed to get tokens for user: ${e.getMessage}")
    }

    // Test 4: Issue a project creation token
    dom.console.log("Test 4: Issuing project creation token...")
    ProjectUcanService
      .issueProjectCreationToken(testProjectId1, lifetimeSeconds = 300)
      .onComplete {
        case Success(cid) =>
          dom.console.log(
            s"Issued project creation token with CID: $cid"
          )
          // Test ownership after issuing token
          testOwnershipAfterTokenCreation(testProjectId1, userDid)
        case Failure(e) =>
          dom.console.log(
            s"Failed to issue project creation token: ${e.getMessage}"
          )
      }

    // Test 5: Generate and save a random token for demonstration
    dom.console.log("Test 5: Generating random demo token...")
    UcanTokenStore.generateAndSaveRandom().onComplete {
      case Success(cid) =>
        dom.console
          .log(s"Generated random demo token with CID: $cid")
        // Re-check tokens after adding demo token
        testTokensAfterDemo(userDid)
      case Failure(e) =>
        dom.console.log(
          s"Failed to generate random token: ${e.getMessage}"
        )
    }
  }

  private def testOwnershipAfterTokenCreation(
      projectId: Uid,
      userDid: String
  ): Unit = {
    dom.console.log(
      "Test 4b: Re-checking ownership after token creation..."
    )
    ProjectUcanService.userOwnsProject(projectId, userDid).onComplete {
      case Success(owns) =>
        dom.console.log(
          s"User owns project after token creation: $owns (expected: true)"
        )
      case Failure(e) =>
        dom.console.log(
          s"Failed to re-check project ownership: ${e.getMessage}"
        )
    }
  }

  private def testTokensAfterDemo(userDid: String): Unit = {
    dom.console.log(
      "Test 5b: Re-checking tokens after demo token creation..."
    )
    ProjectUcanService.tokensForUser(userDid).onComplete {
      case Success(tokens) =>
        dom.console.log(
          s"Found ${tokens.length} tokens for user after demo"
        )
        if (tokens.nonEmpty) {
          dom.console.log("Token details:")
          tokens.foreach { token =>
            dom.console.log(s"- CID: ${token.cid}")
            dom.console.log(s"- Issuer: ${token.iss}")
            dom.console.log(s"- Audience: ${token.aud}")
            dom.console.log(
              s"- Cap Keys: ${token.capKeys.map(_.mkString(", ")).getOrElse("None")}"
            )
          }
        }
        dom.console
          .log("All ProjectUcanService smoke tests completed!")
      case Failure(e) =>
        dom.console
          .log(s" Failed to re-check tokens: ${e.getMessage}")
    }
  }
}

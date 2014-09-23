package me.venuatu.scalaokhttpdemo

import android.view.ViewGroup.LayoutParams
import android.view.{KeyEvent, Window}
import android.view.inputmethod.EditorInfo
import android.widget.TextView.OnEditorActionListener

import scala.concurrent._

import android.app.Activity
import android.os.Bundle
import android.widget._
import macroid.FullDsl._
import macroid._
import spray.json._, DefaultJsonProtocol._

import scala.concurrent.ExecutionContext.Implicits.global

class MainActivity extends Activity with Contexts[Activity] {
  var input = slot[TextView]
  var output = slot[TextView]
  lazy val web = new WebService

  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    val view = l[LinearLayout](
      l[LinearLayout](
        w[EditText] <~ lp[LinearLayout](LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1f)
           <~ wire(input) <~ searchField { output <~ search() },
        w[Button] <~ text(android.R.string.search_go) <~ On.click { output <~ search() }
      ) <~ lp[LinearLayout](LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0f),
      l[ScrollView](
        w[TextView] <~ wire(output)
      ) <~ lp[LinearLayout](LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f)
    ) <~ vertical

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
    setTitle("Search for a github user")
    setContentView(getUi(view))
  }

  def searchField(onSearch: => Ui[Any]) = Tweak[EditText] {text =>
    text.setSingleLine()
    text.setImeOptions(EditorInfo.IME_ACTION_SEARCH)
    text.setOnEditorActionListener(new OnEditorActionListener {
      override def onEditorAction(view: TextView, action: Int, event: KeyEvent): Boolean = {
        if (action == EditorInfo.IME_ACTION_SEARCH) {
          onSearch.run
          true
        } else {
          false
        }
      }
    })
  }

  def search(): Future[Tweak[TextView]] = {
    setProgressBarIndeterminateVisibility(true)
    web.getGithubUser(input.get.getText.toString).map { user =>
      user.toJson.prettyPrint
    }.recover {
      case exc: Throwable => exc.toString
    }.mapUi { str =>
      setProgressBarIndeterminateVisibility(false)
      text(str)
    }
  }
}

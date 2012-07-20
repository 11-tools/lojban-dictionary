package iocikun.juj.lojban.dictionary

import _root_.java.io.FileReader
import _root_.java.io.BufferedReader
import _root_.java.io.InputStreamReader
import _root_.scala.util.control.Breaks._

import _root_.android.app.Activity
import _root_.android.os.Bundle
import _root_.android.view.View
import _root_.android.view.View.OnClickListener
import _root_.android.widget.TextView
import _root_.android.widget.Button
import _root_.android.widget.EditText
import _root_.android.content.res.AssetManager

import iocikun.juj.lojban.dictionary.ReadDictionary

class LojbanDictionary extends Activity with TypedActivity {

	var readDic: ReadDictionary = null;

	lazy val editText = findView(TR.input).asInstanceOf[EditText]
	lazy val textView = findView(TR.textview).asInstanceOf[TextView]
	lazy val button = findView(TR.lojen).asInstanceOf[Button]
	lazy val enloj = findView(TR.enloj).asInstanceOf[Button]
	lazy val rafsi = findView(TR.rafsi).asInstanceOf[Button]
	lazy val cmavo = new BufferedReader(new InputStreamReader(
		getAssets().open("cmavo.txt"), "UTF-8"));
	lazy val reader = new BufferedReader(new InputStreamReader(
		getAssets().open("gismu.txt"), "UTF-8"));
	var list: List[String] = List()
	var clist: List[String] = List()

	override def onCreate(bundle: Bundle) {
		super.onCreate(bundle)
		setContentView(R.layout.main)

		var line = ""
		while({line = reader.readLine(); line != null}) {
			list ::= line
		}

		while({line = cmavo.readLine(); line != null}) {
			clist ::= line
		}

		button.setOnClickListener(new View.OnClickListener() {
			def onClick(v: View) {
				clickFun()
			}
		})

		enloj.setOnClickListener(new View.OnClickListener() {
			def onClick(v: View) {
				enlojFun()
			}
		})

		rafsi.setOnClickListener(new View.OnClickListener() {
			def onClick(v: View) {
				rafsiFun()
			}
		})


		readDic = new ReadDictionary(getAssets())
		findView(TR.textview).setText(readDic.initialString)
	}

	def fun(n: Int): Int = if (n == 0) 1 else n * fun(n - 1)

	def clickFun() {
		textView.setText(editText.getText.toString() +
			reader.readLine())
		var str = editText.getText.toString()
		textView.setText(readDic.lojToEn(str))
	}

	def enlojFun() {
		textView.setText(editText.getText.toString() +
			reader.readLine())
		var str = editText.getText.toString()
		var line = ""
		for(line <- list) {
			if (line.slice(20, 100).startsWith(str)) {
				textView.setText(line)
			}
		}
		for(line <- clist) {
			if (line.slice(20, 100).startsWith(str)) {
				textView.setText(line)
			}
		}
	}

	def rafsiFun() {
		textView.setText(editText.getText.toString() +
			reader.readLine())
		var str = editText.getText.toString()
		var line = ""
		for(line <- list) {
			if (line.slice(7, 10).startsWith(str) ||
				line.slice(11, 14).startsWith(str) ||
				line.slice(15, 19).startsWith(str)) {
				textView.setText(line)
			}
		}
	}

}

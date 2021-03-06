package iocikun.juj.lojban.dictionary

import _root_.java.io.{BufferedReader, InputStreamReader, FileNotFoundException}
import _root_.scala.xml.{XML, Node}

import _root_.android.content.res.AssetManager
import _root_.android.content.SharedPreferences

import _root_.android.util.Log

class ReadDictionary(asset: AssetManager, sp: SharedPreferences) {
	lazy val readDic = new ReadDictionaryGen(
		getDef,
		() => if (sp.contains("lang")) sp.getString("lang", "en") else "en")

	def lojToEn(loj: String): (String, String, List[String]) =
		readDic.lojToEn(loj)

	def enToLoj(en: String):List[(String, String)] =
		readDic.enToLoj(en)

	def rafsi(rafsi: String): (String, String, List[String]) =
		readDic.rafsiToLoj(rafsi)

	def elStr(nlword: Node): (String, String) =
		return ((nlword \ "@valsi").text, "word: " +
			nlword \ "@word" + "<BR/>sense: " + nlword \ "@sense" +
			"<BR/>")

	def allwords: Array[String] = {
		var wordlist: List[String] = List()

		val lang = if (sp.contains("lang"))
			sp.getString("lang", "en") else "en"
		
		if (lang != "ru") {
			val file = new BufferedReader(new InputStreamReader(
				asset.open("completion/en.txt"), "UTF-8"))
			var word: String = null
			while ({word = file.readLine; word != null}) {
				wordlist = word :: wordlist
			}
		}
		if (lang != "en") {
			val file = new BufferedReader(new InputStreamReader(
				asset.open("completion/" + lang + ".txt"), "UTF-8"))
			var word: String = null
			while ({word = file.readLine; word != null}) {
				wordlist = word :: wordlist
			}
		}
		return wordlist.toArray
	}

	def getWords(file_name:String, tag: String): List[String] = {
		var wordlist: List[String] = List()
		val file = new BufferedReader(new InputStreamReader(
			asset.open(file_name), "UTF-8"))
		val xml = XML.load(file)
		for (definition <- (xml \ tag).reverse) {
			for (rafsi <- definition \ "rafsi")
				wordlist = rafsi.text :: wordlist
			wordlist = (definition \ "@word").text :: wordlist
		}
		wordlist
	}

	def getDef(file_name: String, tag: String,
		filter: (String, Node) => Boolean, trgt: String): List[Node] = {
		var list: List[Node] = List()
		val file = new BufferedReader(new InputStreamReader(
			asset.open(file_name), "UTF-8"))
		val xml = XML.load(file)
		for (definition <- xml \ tag) {
			if (filter(trgt, definition)) list = definition :: list
		}
		return list
	}
}

class ReadDictionaryGen(
	getDef: (String, String, (String, Node) => Boolean, String) => List[Node],
	lang: () => String) {

	val mr = new MyRegex

	def filter(trgt: String, definition: Node) =
		(definition \ "@word").toString.toLowerCase == trgt.toLowerCase

	def filterR(trgt: String, definition: Node) = {
		val list = for (r <- definition \ "rafsi") yield r.text
		list contains trgt
	}

	def lojToEn(loj: String): (String, String, List[String]) = {
		val en = getEn(loj)
		val ret = if (en == List())
			return ("", loj + ": no such valsi in the dictionary", List())
			else leStr(en(0))
		val str = ret._2 + "<BR/>"
		var lookupList: List[String] = List()
		for (valsi <- """\{[^}]+\}""".r findAllIn (en(0) \ "notes").text) {
			lookupList = valsi.substring(1, valsi.length - 1) ::
				lookupList
		}
		(ret._1, str, lookupList.reverse)
	}

	def enToLoj(en: String): List[(String, String)] = {
		val loj = getLoj(en)
		if (loj == List())
			return List(("", en + ": no such word in the dictionary"))
		var str: List[(String, String)] = List()
		for (n <- loj) str = elStr(n) :: str
		str.reverse
	}

	def rafsiToLoj(rafsi: String): (String, String, List[String]) = {
		val en = getRafsi(rafsi)
		val ret = if (en == List())
			return ("", rafsi + ": no such rafsi in the dictionary", List())
			else leStr(en(0))
		val str = "<B>" + ret._1 + "</B><BR/>" + ret._2
		var lookupList: List[String] = List()
		for (valsi <- """\{[^}]+\}""".r findAllIn (en(0) \ "notes").text) {
			lookupList = valsi.substring(1, valsi.length - 1) ::
				lookupList
		}
		(ret._1, ret._2 + "<BR/>", lookupList.reverse)
	}

	def getEn(loj: String): List[Node] = {
		if (loj == "") return List()

		val dir = "loj" + lang() + "/"
		val fn = loj.substring(0, 1) + ".xml"

		var ret1: List[Node] = List()

		for (d <- List(dir, "lojen/")) {
			try {
				ret1 = getDef(d + fn, "valsi", filter, loj)
				if (ret1 != List()) return ret1
			} catch {
			case ex: FileNotFoundException =>
				val ret2 = getDef(d + "rest.xml", "valsi",
					filter, loj)
				if (ret2 != List()) return ret2
			}
			if (ret1 == List()) {
				val ret2 = getDef(d + "rest.xml", "valsi",
					filter, loj)
				if (ret2 != List()) return ret2
			}
		}

		return List()
	}

	def getLoj(en: String): List[Node] = {
		if (en == "") return List()

		val dir = lang() + "loj/"
		val fn = en.substring(0, 1) + ".xml"

		for (d <- List(dir, "enloj/")) {
			var ret1: List[Node] = List()
			try {
				ret1 = getDef(d + fn, "nlword", filter, en)
				if (ret1 != List()) return ret1
			} catch {
			case ex: FileNotFoundException =>
			}
			val ret2 = getDef(d + "rest.xml", "nlword", filter, en)
			if (ret2 != List()) return ret1 ::: ret2
		}

		return List()
	}

	def getRafsi(rafsi: String): List[Node] = {
		if (rafsi == "") return List()

		val dir = "rafsi/" + lang() + "/"
		val fn = rafsi.substring(0, 1) + ".xml"

		for (d <- List(dir, "rafsi/en/")) {
			try {
				val ret1 = getDef(d + fn, "valsi", filterR, rafsi)
				if (ret1 != List()) return ret1
			} catch {
			case ex: FileNotFoundException =>
			}
		}

		return List()

	}

	def ampltgt(src: String): String =
/*
		src.replaceAll("RefEntity \"lt\"", "&#60;").
			replaceAll("RefEntity \"gt\"", "&#62;").
			replaceAll("RefEntity \"amp\"", "&#38;")
*/
		src.replaceAll("RefEntity \"lt\"", "&lt;").
			replaceAll("RefEntity \"gt\"", "&gt;").
			replaceAll("RefEntity \"amp\"", "&amp;")

	def leStr(valsi: Node): (String, String) = {
		var rafsiStr = ""
		for (r <- valsi \ "rafsi") rafsiStr += "<BR/><B>rafsi</B>: " + r.text
		return ((valsi \ "@word").text,
			ampltgt("<B>type</B>: " +
			valsi \ "@type" + rafsiStr +
			"<BR/><B>definition</B>: " +
			mr.rep((valsi \ "definition").text) +
			"<BR/><B>notes</B>: " +
			mr.rep((valsi \ "notes").text.filterNot
				{c => '{'.equals(c) || '}'.equals(c)})))
	}

	def elStr(nlword: Node): (String, String) = {
		return ((nlword \ "@valsi").text,
			"<B>word</B>: " +
			nlword \ "@word" + "<BR/><B>sense</B>: " + nlword \ "@sense" +
			"<BR/>")
	}
}

package iocikun.juj.lojban.dictionary

import _root_.java.io.{BufferedReader, InputStreamReader, FileNotFoundException}
import _root_.scala.xml.{XML, Node}

import _root_.android.content.res.AssetManager
import _root_.android.content.SharedPreferences

class ReadDictionary(asset: AssetManager, sp: SharedPreferences) {
	val initialString = "Hello!\nThis is Lojban dictionary!"

	def lojToEn(loj: String): (String, String, List[String]) = {
		val en = getEn(loj)
//		var str = ""
//		for (n <- en) str += leStr(n) + "<BR/><BR/>"
		val ret = if (en == List())
			return (loj, "no such valsi in the dictionary", List())
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
		var str: List[(String, String)] = List()
		for (n <- loj) str = elStr(n) :: str
		str.reverse
	}

	def rafsiToLoj(rafsi: String): (String, String, List[String]) = {
		val en = getRafsi(rafsi)
//		var str = ""
//		for (n <- en) str += leStr(n) + "<BR/><BR/>"
		val ret = if (en == List())
			return (rafsi, "no such rafsi in the dictionary", List())
			else leStr(en(0))
		val str = "<B>" + ret._1 + "</B><BR/>" + ret._2
		var lookupList: List[String] = List()
		for (valsi <- """\{[^}]+\}""".r findAllIn (en(0) \ "notes").text) {
			lookupList = valsi.substring(1, valsi.length - 1) ::
				lookupList
		}
		(ret._1, ret._2 + "<BR/>", lookupList.reverse)
	}

	def leStr(valsi: Node): (String, String) = {
		var rafsiStr = ""
		for (r <- valsi \ "rafsi") rafsiStr += "<BR/>rafsi: " + r.text
		return ((valsi \ "@word").text, "type: " +
			valsi \ "@type" + rafsiStr +
			"<BR/>definition: " + (valsi \ "definition").text +
			"<BR/>notes: " + (valsi \ "notes").text)
	}

	def elStr(nlword: Node): (String, String) = {
		return ((nlword \ "@valsi").text, "word: " +
			nlword \ "@word" + "<BR/>sense: " + nlword \ "@sense" +
			"<BR/>")
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

	def filter(trgt: String, definition: Node) =
		(definition \ "@word").toString == trgt

	def filterR(trgt: String, definition: Node) = {
		val list = for (r <- definition \ "rafsi") yield r.text
		list contains trgt
	}

	def getEn(loj: String): List[Node] = {
		if (loj == "") return List()

		val dir = "loj" + (if (sp.contains("lang"))
			sp.getString("lang", "en") else "en") + "/"
		val fn = loj.substring(0, 1) + ".xml"

		for (d <- List(dir, "lojen/")) {
			try {
				val ret1 = getDef(d + fn, "valsi", filter, loj)
				if (ret1 != List()) return ret1
			} catch {
			case ex: FileNotFoundException =>
				val ret2 = getDef(d + "rest.xml", "valsi",
					filter, loj)
				if (ret2 != List()) return ret2
			}
		}

		return List()
	}

	def getLoj(en: String): List[Node] = {
		if (en == "") return List()

		val dir = (if (sp.contains("lang")) sp.getString("lang", "en")
			else "en") + "loj/"
		val fn = en.substring(0, 1) + ".xml"

		for (d <- List(dir, "enloj/")) {
			try {
				val ret1 = getDef(d + fn, "nlword", filter, en)
				if (ret1 != List()) return ret1
			} catch {
			case ex: FileNotFoundException =>
				val ret2 = getDef(d + "rest.xml", "nlword",
					filter, en)
				if (ret2 != List()) return ret2
			}
		}

		return List()
	}

	def getRafsi(rafsi: String): List[Node] = {
		val fn = if (sp.contains("lang"))
				sp.getString("lang", "en") + ".xml"
			else	"en.xml"

		for (f <- List(fn, "en.xml")) {
			val ret1 = getDef("gismu/" ++ f, "valsi", filterR, rafsi)
			if (ret1 != List()) return ret1
			val ret2 = getDef("cmavo/" ++ f, "valsi", filterR, rafsi)
			if (ret2 != List()) return ret2
		}

		return List()
	}

}

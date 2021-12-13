package ncreep

import ncreep.LinkChain.{build => lc}

object TestData {
  val clickstreamRawData =
    """|other-search	Camp_Tawonga	external	183
	     |Groveland,_California	Camp_Tawonga	link	14
	     |other-empty	Camp_Tawonga	external	24
	     |Mount_Unzen	1792_Unzen_earthquake_and_tsunami	link	280
	     |Tokugawa_Ienari	1792_Unzen_earthquake_and_tsunami	link	14
	     |List_of_disasters_in_Japan_by_death_toll	1792_Unzen_earthquake_and_tsunami	link	31
	     |other-internal	1792_Unzen_earthquake_and_tsunami	external	55
	     |List_of_volcanic_eruptions_by_death_toll	1792_Unzen_earthquake_and_tsunami	link	53
	     |other-search	1792_Unzen_earthquake_and_tsunami	external	696
	     |1771_Great_Yaeyama_Tsunami	1792_Unzen_earthquake_and_tsunami	link	11
	     |2018_Sunda_Strait_tsunami	1792_Unzen_earthquake_and_tsunami	link	20
	     |List_of_earthquakes_in_Japan	1792_Unzen_earthquake_and_tsunami	link	39
	     |Cumbre_Vieja_tsunami_hazard	1792_Unzen_earthquake_and_tsunami	link	22
	     |Megatsunami	1792_Unzen_earthquake_and_tsunami	link	98
	     |List_of_natural_disasters_by_death_toll	1792_Unzen_earthquake_and_tsunami	link	40
	     |something_"with_quotes"	"to_other_quotes	link	930
	     |other-empty	1792_Unzen_earthquake_and_tsunami	external	143
	     |other-search	West_Bradford,_Lancashire	external	69
	     |other-empty	West_Bradford,_Lancashire	external	18
	     |List_of_colleges_in_Mumbai	Shri_M.D._Shah_Mahila_College_of_Arts_and_Commerce	link	25
	     |other-search	Shri_M.D._Shah_Mahila_College_of_Arts_and_Commerce	external	320""".stripMargin

  val link1 = Link("Groveland,_California", "Camp_Tawonga", 14)
  val link2 = Link("Mount_Unzen", "1792_Unzen_earthquake_and_tsunami", 280)
  val link3 = Link("Tokugawa_Ienari", "1792_Unzen_earthquake_and_tsunami", 14)
  val link4 = Link("List_of_disasters_in_Japan_by_death_toll", "1792_Unzen_earthquake_and_tsunami", 31)
  val link5 = Link("List_of_volcanic_eruptions_by_death_toll", "1792_Unzen_earthquake_and_tsunami", 53)
  val link6 = Link("1771_Great_Yaeyama_Tsunami", "1792_Unzen_earthquake_and_tsunami", 11)
  val link7 = Link("2018_Sunda_Strait_tsunami", "1792_Unzen_earthquake_and_tsunami", 20)
  val link8 = Link("List_of_earthquakes_in_Japan", "1792_Unzen_earthquake_and_tsunami", 39)
  val link9 = Link("Cumbre_Vieja_tsunami_hazard", "1792_Unzen_earthquake_and_tsunami", 22)
  val link10 = Link("Megatsunami", "1792_Unzen_earthquake_and_tsunami", 98)
  val link11 = Link("List_of_natural_disasters_by_death_toll", "1792_Unzen_earthquake_and_tsunami", 40)
  val link12 = Link("""something_"with_quotes"""", """"to_other_quotes""", 930)
  val link13 = Link("List_of_colleges_in_Mumbai", "Shri_M.D._Shah_Mahila_College_of_Arts_and_Commerce", 25)

  // the chain here represent a tree where each level branches by either 2 or 1
  //                 a
  //       b1                 b2
  //       c1                 c2
  //   d1      d2         d3      d4
  //   e1      e2         e3      e4
  //f1  f2   f3  f4     f5  f6  f7  f8
  // ...      ...        ...     ....
  val chain1 = lc("a")
  val chain21 = lc("a", 21 -> "b1")
  val chain22 = lc("a", 22 -> "b2")
  val chain31 = lc("a", 21 -> "b1", 31 -> "c1")
  val chain32 = lc("a", 22 -> "b2", 32 -> "c2")
  val chain41 = lc("a", 21 -> "b1", 31 -> "c1", 41 -> "d1")
  val chain42 = lc("a", 21 -> "b1", 31 -> "c1", 42 -> "d2")
  val chain43 = lc("a", 22 -> "b2", 32 -> "c2", 43 -> "d3")
  val chain44 = lc("a", 22 -> "b2", 32 -> "c2", 44 -> "d4")
  val chain51 = lc("a", 21 -> "b1", 31 -> "c1", 41 -> "d1", 51 -> "e1")
  val chain52 = lc("a", 21 -> "b1", 31 -> "c1", 42 -> "d2", 52 -> "e2")
  val chain53 = lc("a", 22 -> "b2", 32 -> "c2", 43 -> "d3", 53 -> "e3")
  val chain54 = lc("a", 22 -> "b2", 32 -> "c2", 44 -> "d4", 54 -> "e4")
  val chain61 = lc("a", 21 -> "b1", 31 -> "c1", 41 -> "d1", 51 -> "e1", 61 -> "f1")
  val chain62 = lc("a", 21 -> "b1", 31 -> "c1", 41 -> "d1", 51 -> "e1", 62 -> "f2")
  val chain63 = lc("a", 21 -> "b1", 31 -> "c1", 42 -> "d2", 52 -> "e2", 63 -> "f3")
  val chain64 = lc("a", 21 -> "b1", 31 -> "c1", 42 -> "d2", 52 -> "e2", 64 -> "f4")
  val chain65 = lc("a", 22 -> "b2", 32 -> "c2", 43 -> "d3", 53 -> "e3", 65 -> "f5")
  val chain66 = lc("a", 22 -> "b2", 32 -> "c2", 43 -> "d3", 53 -> "e3", 66 -> "f6")
  val chain67 = lc("a", 22 -> "b2", 32 -> "c2", 44 -> "d4", 54 -> "e4", 67 -> "f7")
  val chain68 = lc("a", 22 -> "b2", 32 -> "c2", 44 -> "d4", 54 -> "e4", 68 -> "f8")
  val chain71 = lc("a", 21 -> "b1", 31 -> "c1", 41 -> "d1", 51 -> "e1", 61 -> "f1", 71 -> "g1")
  val chain72 = lc("a", 21 -> "b1", 31 -> "c1", 41 -> "d1", 51 -> "e1", 62 -> "f2", 72 -> "g2")
  val chain73 = lc("a", 21 -> "b1", 31 -> "c1", 42 -> "d2", 52 -> "e2", 63 -> "f3", 73 -> "g3")
  val chain74 = lc("a", 21 -> "b1", 31 -> "c1", 42 -> "d2", 52 -> "e2", 64 -> "f4", 74 -> "g4")
  val chain75 = lc("a", 22 -> "b2", 32 -> "c2", 43 -> "d3", 53 -> "e3", 65 -> "f5", 75 -> "g5")
  val chain76 = lc("a", 22 -> "b2", 32 -> "c2", 43 -> "d3", 53 -> "e3", 66 -> "f6", 76 -> "g6")
  val chain77 = lc("a", 22 -> "b2", 32 -> "c2", 44 -> "d4", 54 -> "e4", 67 -> "f7", 77 -> "g7")
  val chain78 = lc("a", 22 -> "b2", 32 -> "c2", 44 -> "d4", 54 -> "e4", 68 -> "f8", 78 -> "g8")

  val chain81 = lc("a", 21 -> "b1", 31 -> "c1", 41 -> "d1", 51 -> "e1", 61 -> "f1", 71 -> "g1", 81 -> "h1")
  val chain82 = lc("a", 21 -> "b1", 31 -> "c1", 41 -> "d1", 51 -> "e1", 61 -> "f1", 71 -> "g1", 82 -> "h2")
  val chain83 = lc("a", 21 -> "b1", 31 -> "c1", 41 -> "d1", 51 -> "e1", 62 -> "f2", 72 -> "g2", 83 -> "h3")
  val chain84 = lc("a", 21 -> "b1", 31 -> "c1", 41 -> "d1", 51 -> "e1", 62 -> "f2", 72 -> "g2", 84 -> "h4")
  val chain85 = lc("a", 21 -> "b1", 31 -> "c1", 42 -> "d2", 52 -> "e2", 63 -> "f3", 73 -> "g3", 85 -> "h5")
  val chain86 = lc("a", 21 -> "b1", 31 -> "c1", 42 -> "d2", 52 -> "e2", 63 -> "f3", 73 -> "g3", 86 -> "h6")
  val chain87 = lc("a", 21 -> "b1", 31 -> "c1", 42 -> "d2", 52 -> "e2", 64 -> "f4", 74 -> "g4", 87 -> "h7")
  val chain88 = lc("a", 21 -> "b1", 31 -> "c1", 42 -> "d2", 52 -> "e2", 64 -> "f4", 74 -> "g4", 88 -> "h8")
  val chain89 = lc("a", 22 -> "b2", 32 -> "c2", 43 -> "d3", 53 -> "e3", 65 -> "f5", 75 -> "g5", 89 -> "h9")
  val chain810 = lc("a", 22 -> "b2", 32 -> "c2", 43 -> "d3", 53 -> "e3", 65 -> "f5", 75 -> "g5", 810 -> "h10")
  val chain811 = lc("a", 22 -> "b2", 32 -> "c2", 43 -> "d3", 53 -> "e3", 66 -> "f6", 76 -> "g6", 811 -> "h11")
  val chain812 = lc("a", 22 -> "b2", 32 -> "c2", 43 -> "d3", 53 -> "e3", 66 -> "f6", 76 -> "g6", 812 -> "h12")
  val chain813 = lc("a", 22 -> "b2", 32 -> "c2", 44 -> "d4", 54 -> "e4", 67 -> "f7", 77 -> "g7", 813 -> "h13")
  val chain814 = lc("a", 22 -> "b2", 32 -> "c2", 44 -> "d4", 54 -> "e4", 67 -> "f7", 77 -> "g7", 814 -> "h14")
  val chain815 = lc("a", 22 -> "b2", 32 -> "c2", 44 -> "d4", 54 -> "e4", 68 -> "f8", 78 -> "g8", 815 -> "h15")
  val chain816 = lc("a", 22 -> "b2", 32 -> "c2", 44 -> "d4", 54 -> "e4", 68 -> "f8", 78 -> "g8", 816 -> "h16")

  // some loops
  val chainLoop31 = lc("a", 22 -> "b2", 9999 -> "a")
  val chainLoop41 = lc("a", 22 -> "b2", 32 -> "c2", 9999 -> "a")
  val chainLoop51 = lc("a", 22 -> "b2", 32 -> "c2", 44 -> "d4", 9999 -> "a")
  val chainLoop61 = lc("a", 22 -> "b2", 32 -> "c2", 44 -> "d4", 54 -> "e4", 9999 -> "a")
  val chainLoop71 = lc("a", 22 -> "b2", 32 -> "c2", 44 -> "d4", 54 -> "e4", 6 -> "f8", 9999 -> "a")
  val chainLoop81 = lc("a", 22 -> "b2", 32 -> "c2", 44 -> "d4", 54 -> "e4", 6 -> "f8", 71 -> "g8", 9999 -> "a")

  val linkChains = List(
    chain1,
    chain21, chain22,
    chain31, chain32, chainLoop31,
    chain41, chain42, chain43, chain44, chainLoop41,
    chain51, chain52, chain53, chain54, chainLoop51,
    chain61, chain62, chain63, chain64, chain65, chain66, chain67, chain68, chainLoop61,
    chain71, chain72, chain73, chain74, chain75, chain76, chain77, chain78, chainLoop71,
    chain81, chain82, chain83, chain84, chain85, chain86, chain87, chain88, chain89, chain810,
    chain811, chain812, chain813, chain814, chain815, chain816, chainLoop81)

  val linksMap = LinksMap.empty
    .addLink(Link(source = "a", target = "b", times = 10))
    .addLink(Link(source = "a", target = "c", times = 2))
    .addLink(Link(source = "a", target = "d", times = 14))
    .addLink(Link(source = "a", target = "e", times = 9))
    .addLink(Link(source = "a", target = "h", times = 1))
    .addLink(Link(source = "b", target = "d", times = 4))
    .addLink(Link(source = "b", target = "f1", times = 5))
    .addLink(Link(source = "b", target = "f2", times = 15))
    .addLink(Link(source = "b", target = "f3", times = 2))
    .addLink(Link(source = "e", target = "g1", times = 19))
    .addLink(Link(source = "e", target = "g2", times = 2))
    .addLink(Link(source = "e", target = "g3", times = 3))
    .addLink(Link(source = "e", target = "f2", times = 5))
    .addLink(Link(source = "c", target = "d", times = 11))
    .addLink(Link(source = "h", target = "i1", times = 3))
    .addLink(Link(source = "h", target = "i2", times = 4))
    .addLink(Link(source = "h", target = "i3", times = 5))
    .addLink(Link(source = "h", target = "i4", times = 6))
}


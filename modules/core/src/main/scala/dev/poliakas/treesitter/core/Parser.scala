package dev.poliakas.treesitter.core

import treesitter.all.*
import scala.scalanative.libc.string.strlen
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

opaque type Parser = Ptr[TSParser]
object Parser:
  inline def from(ptr: Ptr[TSParser]): Parser = ptr
  def create: Parser = ts_parser_new()

  extension (self: Parser)
    inline final def value: Ptr[TSParser] = self
    def delete: Unit = ts_parser_delete(self)

    def parse(
      src: String, 
      mainLanguage: Language, 
      injecteds: List[(Query, String, Language)]
    ): Option[(Tree, Map[Query, Tree])] = 
      self.setLanguage(mainLanguage)
      Zone: zone => 
        given Zone = zone

        val srcC = toCString(src)
        val srcLength = strlen(srcC).toUInt

        self.setLanguage(mainLanguage)

        Option(ts_parser_parse_string(self, null, srcC, srcLength)).map: treePtr => 
          val tree = Tree.from(treePtr)
          val rootNodePtr = alloc[TSNode](1)
          ts_tree_root_node(tree.value)(rootNodePtr)

          val asd = injecteds.view.collect:
            Function.unlift:
              (query, capture, language) => 
                val ranges = QueryIterator(query, src, Node.from(rootNodePtr)).collect:
                  Function.unlift: matcherino => 
                    matcherino.matches.get(capture).map: node =>
                      val rangePtr = alloc[TSRange](1)
                      val range = !rangePtr
                      range.start_point = ts_node_start_point(node.value)
                      range.end_point = ts_node_end_point(node.value)
                      range.start_byte = ts_node_start_byte(node.value)
                      range.end_byte = ts_node_end_byte(node.value)
                      Range.from(rangePtr)

                self.setLanguage(language)
                self.setRanges(ranges.toList)
                Option(ts_parser_parse_string(self, null, srcC, srcLength)).map(query -> Tree.from(_))

          tree -> asd.toMap

    def setLanguage(l: Language): Unit = 
      ts_parser_set_language(self, l.value)

    def setRanges(ranges: List[Range])(using Zone): Unit =
      val rangesC = alloc[TSRange](ranges.size)

      for i <- (0 until ranges.size) do 
        !(rangesC + i) = (!Range.value(ranges(i)))

      ts_parser_set_included_ranges(self, rangesC, ranges.size.toUInt)

    def parseString(oldTree: Option[Tree], src: String): Option[Tree] =
      Zone: zone =>
        given Zone = zone
        val srcC = toCString(src)

        Option(
          Tree.from(
            ts_parser_parse_string(
              self,
              oldTree.map(_.value).orNull,
              srcC,
              strlen(srcC).toUInt
            )
          )
        )

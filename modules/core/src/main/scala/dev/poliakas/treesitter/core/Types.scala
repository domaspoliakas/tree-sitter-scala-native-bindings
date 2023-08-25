package dev.poliakas.treesitter.core

import treesitter.all.*
import scala.scalanative.libc.string.strlen
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

opaque type Language = Ptr[TSLanguage]
object Language:
  def from(ptr: Ptr[TSLanguage]): Language = ptr
  extension (self: Language)
    inline final def value: Ptr[TSLanguage] = self
    def newParser: Parser =
      val parser = ts_parser_new()
      ts_parser_set_language(parser, self)
      parser

opaque type Tree = Ptr[TSTree]
object Tree:
  def from(ptr: Ptr[TSTree]): Tree = ptr
  extension (self: Tree) inline final def value: Ptr[TSTree] = self

opaque type Parser = Ptr[TSParser]
object Parser:
  def from(ptr: Ptr[TSParser]): Parser = ptr
  def create(language: Language): Parser = ts_parser_new()

  extension (self: Parser)
    inline final def value: Ptr[TSParser] = self
    def delete: Unit = ts_parser_delete(self)
    def setRanges(ranges: List[Range])(using Zone): Unit =
      val rangesC = alloc[TSRange](ranges.size)

      for i <- (0 until ranges.size) do !(rangesC + i) = (!Range.value(ranges(0)))

      ts_parser_set_included_ranges(self, rangesC, ranges.size.toUInt)

    def parseString(oldTree: Option[Tree], src: String): Option[Tree] =
      Zone: zone =>
        given Zone = zone
        val srcC = toCString(src)

        Option(
          ts_parser_parse_string(
            self,
            oldTree.orNull,
            srcC,
            strlen(srcC).toUInt
          )
        )

opaque type Node = Ptr[TSNode]
object Node:
  def from(ptr: Ptr[TSNode]): Node = ptr
  extension (self: Node)
    inline final def value: Ptr[TSNode] = self
    def childCount: UInt = ts_node_child_count(self)
    def sliceFrom(source: String): String =
      val startByte = ts_node_start_byte(self)
      val endByte = ts_node_end_byte(self)
      new String(source.getBytes().slice(startByte.toInt, endByte.toInt))

opaque type Range = Ptr[TSRange]
object Range:
  def from(ptr: Ptr[TSRange]): Range = ptr
  extension (self: Range) inline final def value: Ptr[TSRange] = self

opaque type QueryCursor = Ptr[TSQueryCursor]
object QueryCursor:
  def from(ptr: Ptr[TSQueryCursor]): QueryCursor = ptr
  extension (self: QueryCursor)
    inline final def value: Ptr[TSQueryCursor] = self
    def delete: Unit = ts_query_cursor_delete(self)

opaque type QueryMatch = Ptr[TSQueryMatch]
object QueryMatch:
  def from(ptr: Ptr[TSQueryMatch]): QueryMatch = ptr
  extension (self: QueryMatch) inline final def value: Ptr[TSQueryMatch] = self

opaque type Quantifier = TSQuantifier
object Quantifier:
  def from(quantifier: TSQuantifier): Quantifier = quantifier
  extension (self: Quantifier) inline final def value: TSQuantifier = self

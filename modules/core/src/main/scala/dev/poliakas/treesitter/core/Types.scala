package dev.poliakas.treesitter.core

import treesitter.all.*
import scala.scalanative.libc.string.strlen
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

opaque type Language = Ptr[TSLanguage]
object Language:
  inline def from(ptr: Ptr[TSLanguage]): Language = ptr
  extension (self: Language)
    inline final def value: Ptr[TSLanguage] = self
    def newParser: Parser =
      val parser = ts_parser_new()
      ts_parser_set_language(parser, self)
      Parser.from(parser)

opaque type Tree = Ptr[TSTree]
object Tree:
  inline def from(ptr: Ptr[TSTree]): Tree = ptr
  extension (self: Tree) 
    inline final def value: Ptr[TSTree] = self
    def rootNode(using Zone): Node =
      val node = alloc[TSNode](1)
      ts_tree_root_node(self)(node)
      Node.from(node)

opaque type Node = Ptr[TSNode]
object Node:
  inline def from(ptr: Ptr[TSNode]): Node = ptr
  extension (self: Node)
    inline final def value: Ptr[TSNode] = self
    def childCount: Int = ts_node_child_count(self).toInt
    def startByte: Int = ts_node_start_byte(self).toInt
    def endByte: Int = ts_node_end_byte(self).toInt
    def sliceFrom(source: String): String =
      new String(source.getBytes().slice(startByte, endByte))
    def nodeString: String = 
      fromCString(ts_node_string(self))

opaque type Range = Ptr[TSRange]
object Range:
  inline def from(ptr: Ptr[TSRange]): Range = ptr

  extension (self: Range) 
    inline final def value: Ptr[TSRange] = self

opaque type InputEdit = Ptr[TSInputEdit]
object InputEdit:
  inline def from(ptr: Ptr[TSInputEdit]): InputEdit = ptr

  extension (self: InputEdit) 
    inline final def value: Ptr[TSInputEdit] = self

opaque type Point = Ptr[TSPoint]
object Point:
  inline def from(ptr: Ptr[TSPoint]): Point = ptr
  def apply(row: Int, column: Int)(using Zone): Point =
    val pointPtr = alloc[TSPoint](1)
    val point = !pointPtr
    point.row = row.toUInt
    point.column = column.toUInt
    pointPtr

  extension (self: Point) 
    inline final def value: Ptr[TSPoint] = self
    inline final def column: Int = (!self).column.toInt
    inline final def row: Int = (!self).row.toInt

opaque type QueryCursor = Ptr[TSQueryCursor]
object QueryCursor:
  inline def from(ptr: Ptr[TSQueryCursor]): QueryCursor = ptr
  extension (self: QueryCursor)
    inline final def value: Ptr[TSQueryCursor] = self
    def delete: Unit = ts_query_cursor_delete(self)

opaque type QueryMatch = Ptr[TSQueryMatch]
object QueryMatch:
  inline def from(ptr: Ptr[TSQueryMatch]): QueryMatch = ptr
  extension (self: QueryMatch) 
    inline final def value: Ptr[TSQueryMatch] = self

opaque type Quantifier = TSQuantifier
object Quantifier:
  inline def from(quantifier: TSQuantifier): Quantifier = quantifier
  extension (self: Quantifier) inline final def value: TSQuantifier = self

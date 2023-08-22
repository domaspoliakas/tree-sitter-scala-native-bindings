package dev.poliakas.treesitter.core

import treesitter.all.*
import scala.scalanative.libc.string.strlen
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UInt

opaque type Language = Ptr[TSLanguage]
object Language:
  def from(ptr: Ptr[TSLanguage]): Language = ptr
  extension (self: Language) inline final def value: Ptr[TSLanguage] = self

opaque type Tree = Ptr[TSTree]
object Tree:
  def from(ptr: Ptr[TSTree]): Tree = ptr
  extension (self: Tree)
    inline final def value: Ptr[TSTree] = self
    def rootNode(using Zone) = ts_tree_root_node(self)

opaque type Parser = Ptr[TSParser]
object Parser:
  def from(ptr: Ptr[TSParser]): Parser = ptr
  def create(language: Language): Parser = ts_parser_new()

  extension (self: Parser)
    inline final def value: Ptr[TSParser] = self
    def delete: Unit = ts_parser_delete(self)

opaque type Node = Ptr[TSNode]
object Node:
  def from(ptr: Ptr[TSNode]): Node = ptr
  extension (self: Node)
    inline final def value: Ptr[TSNode] = self
    def childCount: UInt = ts_node_child_count(self)

opaque type Range = Ptr[TSRange]
object Range:
  def from(ptr: Ptr[TSRange]): Range = ptr
  extension (self: Range) inline final def value: Ptr[TSRange] = self

opaque type Query = Ptr[TSQuery]
object Query:
  enum CreationError:
    case LanguageError
    case NodeTypeError(badNode: String)
    case FieldError(badField: String)
    case CaptureError(badCapture: String)
    case SyntaxError(message: String)
    case StructureError


  def from(ptr: Ptr[TSQuery]): Query = ptr
  def create(
      language: Language,
      queryString: String
  ): Either[Query.CreationError, Query] =
    Zone: zone =>
      given Zone = zone

      val queryC = toCString(queryString)
      val errorOffset = alloc[uint32_t](1)
      val errorType = alloc[TSQueryError](1)
      val query = ts_query_new(
        language,
        queryC,
        strlen(queryC).toUInt,
        errorOffset,
        errorType
      )

      if (query != null) Right(query)
      else
        val err = !errorType
        if err == TSQueryError.TSQueryErrorLanguage then
          Left(CreationError.LanguageError)
        else if err == TSQueryError.TSQueryErrorNodeType then
          Left(CreationError.NodeTypeError(getOffender(queryC, errorOffset)))
        else if err == TSQueryError.TSQueryErrorField then
          Left(CreationError.FieldError(getOffender(queryC, errorOffset)))
        else if err == TSQueryError.TSQueryErrorNodeType then
          Left(CreationError.CaptureError(getOffender(queryC, errorOffset)))
        else if err == TSQueryError.TSQueryErrorSyntax then
          Left(CreationError.SyntaxError(constructMessage(queryC, errorOffset)))
        else if err == TSQueryError.TSQueryErrorStructure then
          Left(CreationError.SyntaxError(constructMessage(queryC, errorOffset)))

        else throw new Exception("An unrecognised query error has occurred")

  private def getOffender(str: CString, offset: Ptr[uint32_t]): String =
    fromCString((str + (!offset))).takeWhile(c =>
      c.isDigit || c.isLetter || c == '_' || c == '-'
    )

  private def constructMessage(queryC: CString, errorOffset: Ptr[uint32_t]): String = 
    var lineStart = 0
    var lineEnd = strlen(queryC).toInt

    var i = (!errorOffset).toInt
    while (i >= 0) {
      val b: Byte = (!(queryC + i))
      if b == '\n' then
        lineStart = i
        i = -1
      else 
        i = i - 1
    }

    while (i <= strlen(queryC).toInt) {
      val b: Byte = (!(queryC + i))
      if b == '\n' then
        lineEnd = i
        i = strlen(queryC).toInt + 1
      else 
        i = i + 1
    }

    val column = (!errorOffset).toInt - lineStart
    val line = fromCString(queryC + lineStart).take(lineEnd - lineStart)

    s"\n$line\n${" " * column}^"

  extension (self: Query)
    inline final def value: Ptr[TSQuery] = self
    def destroy: Unit = ts_query_delete(self)

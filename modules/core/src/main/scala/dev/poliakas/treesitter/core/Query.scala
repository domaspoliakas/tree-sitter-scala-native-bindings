package dev.poliakas.treesitter.core

import scala.scalanative.libc.string.strlen
import scala.scalanative.unsafe.*
import treesitter.functions.*
import treesitter.types.*
import treesitter.aliases
import scala.scalanative.unsigned.*

opaque type Query = Ptr[TSQuery]
object Query:
  enum CreationError:
    case LanguageError
    case NodeTypeError(badNode: String)
    case FieldError(badField: String)
    case CaptureError(badCapture: String)
    case SyntaxError(message: String)
    case StructureError

  // each argument is either a capture (left) or string literal (right)
  
  enum PredicateArg:
    case StringLiteral(literal: String)
    case Capture(captureName: String)

  enum Predicate:
    case Eq(negated: Boolean, left: PredicateArg, right: PredicateArg)
    case AnyOf(negated: Boolean, element: PredicateArg, set: List[PredicateArg])

  // case class Predicate(function: String, args: List[Either[String, String]])

  extension (self: Query)
    inline final def value: Ptr[TSQuery] = self
    def delete: Unit = ts_query_delete(self)
    def newCursor(at: Node): QueryCursor =
      val cursor = ts_query_cursor_new()
      ts_query_cursor_exec(cursor, self, at.value)
      QueryCursor.from(cursor)

    def stringLiteralCount: Int =
      ts_query_string_count(self).toInt

    def captureCount: Int =
      ts_query_capture_count(self).toInt

    def patternCount: Int =
      ts_query_pattern_count(self).toInt

    def captures: List[String] =
      Zone: zone =>
        given Zone = zone
        val length = alloc[UInt](1)
        for i <- (0 until captureCount).toList yield
          val nameC = ts_query_capture_name_for_id(self, i.toUInt, length)
          fromCString(nameC)

    // a list of quantifiers by pattern
    def quantifiersByPattern: List[List[Quantifier]] =
      Zone: zone =>
        given Zone = zone
        for i <- (0 until patternCount).toList
        yield for j <- (0 until captureCount).toList
        yield Quantifier.from(
          ts_query_capture_quantifier_for_id(self, i.toUInt, j.toUInt)
        )

    def predicatesByPattern: List[List[Predicate]] =
      Zone: zone =>
        given Zone = zone

        val strings = stringLiterals
        val cpt = captures

        val length = alloc[UInt](1)
        for i <- (0 until patternCount).toList yield
          val predoo = ts_query_predicates_for_pattern(self, i.toUInt, length)
          val predicateSteps =
            for j <- (0 until (!length).toInt).toList yield !(predoo + j)

          val patternStartByte = ts_query_start_byte_for_pattern(self, i.toUInt)

          var res: List[Predicate] = Nil
          var currentPredicateFunction: String = null
          var currentPredicateArgs: List[PredicateArg] = Nil

          for step <- predicateSteps do
            if (step.`type` == TSQueryPredicateStepType.TSQueryPredicateStepTypeDone) then

              val newPred = 
                if currentPredicateFunction == null then
                  throw new Exception("A predicate with no function detected?")
                else if currentPredicateFunction == "eq?" || currentPredicateFunction == "not-eq?" then
                  if currentPredicateArgs.size != 2 then
                    throw new Exception(s"eq/not-eq must have 2 arguments, got ${currentPredicateArgs.size}")

                  Predicate.Eq(currentPredicateFunction == "not-eq?", currentPredicateArgs(0), currentPredicateArgs(1))
                else if currentPredicateFunction == "any-of?" || currentPredicateFunction == "not-any-of?" then
                  if currentPredicateArgs.size >= 2 then
                    throw new Exception(s"any-of/not-any-of must have 2 or more arguments, got ${currentPredicateArgs.size}")

                  Predicate.AnyOf(currentPredicateFunction == "not-eq?", currentPredicateArgs.head, currentPredicateArgs.tail)
                else throw new Exception(s"Unsupported predicate function: $currentPredicateFunction")

              res = newPred :: res
              currentPredicateFunction = null
              currentPredicateArgs = Nil

            else if (step.`type` == TSQueryPredicateStepType.TSQueryPredicateStepTypeString) then

              val str = stringLiterals(step.value_id.toInt)

              if currentPredicateFunction == null then
                currentPredicateFunction = str
              else
                currentPredicateArgs = PredicateArg.StringLiteral(str) :: currentPredicateArgs

            else if (step.`type` == TSQueryPredicateStepType.TSQueryPredicateStepTypeCapture) then
              currentPredicateArgs = PredicateArg.Capture(cpt(step.value_id.toInt)) :: currentPredicateArgs

            else throw new Exception("Unknown query predicate step type")

          res

    def stringLiterals: List[String] =
      Zone: zone =>
        given Zone = zone
        val length = alloc[UInt](1)
        for i <- (0 until stringLiteralCount).toList yield
          val strC = ts_query_string_value_for_id(self, i.toUInt, length)
          fromCString(strC)

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
        language.value,
        queryC,
        strlen(queryC).toUInt,
        errorOffset,
        errorType
      )

      if (query != null) Right(query)
      else
        val err = !errorType
        if err == TSQueryError.TSQueryErrorLanguage then Left(CreationError.LanguageError)
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
    fromCString((str + (!offset))).takeWhile(c => c.isDigit || c.isLetter || c == '_' || c == '-')

  private def constructMessage(
      queryC: CString,
      errorOffset: Ptr[uint32_t]
  ): String =
    var lineStart = 0
    var lineEnd = strlen(queryC).toInt

    var i = (!errorOffset).toInt
    while (i >= 0) {
      val b: Byte = (!(queryC + i))
      if b == '\n' then
        lineStart = i
        i = -1
      else i = i - 1
    }

    while (i <= strlen(queryC).toInt) {
      val b: Byte = (!(queryC + i))
      if b == '\n' then
        lineEnd = i
        i = strlen(queryC).toInt + 1
      else i = i + 1
    }

    val column = (!errorOffset).toInt - lineStart
    val line = fromCString(queryC + lineStart).take(lineEnd - lineStart)

    s"\n$line\n${" " * column}^"

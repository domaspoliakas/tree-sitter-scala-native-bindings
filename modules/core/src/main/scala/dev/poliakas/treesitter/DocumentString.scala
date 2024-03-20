package dev.poliakas.treesitter

import cats.syntax.all.*
import dev.poliakas.treesitter.core.*
import treesitter.all.*
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*
import scala.scalanative.libc.string.strlen

case class DocumentString(source: String, lineBreaks: Vector[Int]):
  def byteFor(point: Point): Int =
    byteForTS(!point.value)

  private def byteForTS(point: TSPoint): Int =
    lineBreaks(point.row.toInt) + point.column.toInt

  def amend(
    from: Point, 
    to: Point,
    newString: String
  )(using Zone): (InputEdit, DocumentString) = 
    val startByte = byteFor(from)
    val oldEndByte = byteFor(to)

    val oldLength = oldEndByte - startByte
    val newLength = newString.getBytes().length
    val diff = newLength - oldLength

    val newEndByte = oldEndByte + diff

    val tsInputEditPtr = alloc[TSInputEdit](1)
    val tsInputEdit = !tsInputEditPtr

    tsInputEdit.start_byte = startByte.toUInt
    tsInputEdit.old_end_byte = oldEndByte.toUInt
    tsInputEdit.new_end_byte = newEndByte.toUInt

    tsInputEdit.start_point = !from.value
    tsInputEdit.old_end_point = !to.value

    val newStrLines = newString.split("\n")

    val potato: UInt = 
      if newStrLines.length == 1 then
        ((!to.value).column.toInt + newStrLines.head.length).toUInt
      else
        newStrLines.last.length.toUInt

    tsInputEdit.new_end_point.row = ((!to.value).row.toInt + (newStrLines.length - 1)).toUInt
    tsInputEdit.new_end_point.column = potato

    val newDocStrArray = Array.fill[Byte](source.length + diff)('X')

    source.getBytes().copyToArray(newDocStrArray, 0, startByte + 1)
    newString.getBytes().copyToArray(newDocStrArray, startByte + 1, newString.length)
    source.getBytes().copyToArray(newDocStrArray, startByte + 1 + newString.length, source.length - oldEndByte)


    (InputEdit.from(tsInputEditPtr), DocumentString(new String(newDocStrArray), lineBreaks))

object DocumentString:
  def apply(s: String): DocumentString =
    var lineBreaks = List(0)
    for i <- 0 until s.length() do
      if s(i) == '\n' then
        lineBreaks = i :: lineBreaks

    DocumentString(s, lineBreaks.reverse.toVector)

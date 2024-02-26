package dev.poliakas.treesitter

import cats.syntax.all.*
import scala.scalanative.unsafe.*
import dev.poliakas.treesitter.core.*

object DocumentStringSuite extends weaver.FunSuite:

  test("line breaks get detected properly"):
    val in = "hello\nhello"
    val doc = DocumentString(in)
    expect(doc.lineBreaks === Vector(0, 5))

  test("amend: append at the end"):
    Zone: zone =>
      given Zone = zone
      val in = DocumentString("hello: hello")
      val (editTS, out) = in.amend(
        Point(0, 12),
        Point(0, 12),
        "a"
      )

      val edit = !editTS.value

      expect( out.source == "hello: helloa") and
        expect(edit.start_byte.toInt == 12) and
        expect(edit.old_end_byte.toInt == 12) and
        expect(edit.new_end_byte.toInt == 13) and
        expect(edit.start_point.row.toInt == 0) and
        expect(edit.start_point.column.toInt == 12) and
        expect(edit.old_end_point.row.toInt == 0) and
        expect(edit.old_end_point.column.toInt == 12) and
        expect(edit.new_end_point.row.toInt == 0) and
        expect(edit.new_end_point.column.toInt == 13)

  test("amend: add in the middle"):
    Zone: zone =>
      given Zone = zone
      val in = DocumentString("hello: hello")
      val (editTS, out) = in.amend(
        Point(0, 6),
        Point(0, 6),
        "a"
      )

      val edit = !editTS.value

      expect( out.source == "hello: ahello") and
        expect(edit.start_byte.toInt == 12) and
        expect(edit.old_end_byte.toInt == 12) and
        expect(edit.new_end_byte.toInt == 13) and
        expect(edit.start_point.row.toInt == 0) and
        expect(edit.start_point.column.toInt == 12) and
        expect(edit.old_end_point.row.toInt == 0) and
        expect(edit.old_end_point.column.toInt == 12) and
        expect(edit.new_end_point.row.toInt == 0) and
        expect(edit.new_end_point.column.toInt == 13)


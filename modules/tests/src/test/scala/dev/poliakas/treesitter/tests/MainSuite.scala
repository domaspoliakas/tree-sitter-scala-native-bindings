package dev.poliakas.treesitter
package tests

import cats.syntax.all.*

import dev.poliakas.treesitter.core.*
import dev.poliakas.treesitter.yaml.tree_sitter_yaml
import dev.poliakas.treesitter.sql.tree_sitter_sql
import treesitter.all.*
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*
import scala.scalanative.libc.string.strlen

object MainSuite extends weaver.FunSuite:

  // test("hek"):
  //   Zone: zone =>
  //     given Zone = zone

  //     val parserYaml = yaml.Language.newParser

  //     val parserSql = sql.Language.newParser

  //     val source =
  //       """hello: 
  //         |  sql: SELECT column FROM table;
  //         |  potato: tomato
  //         |  tomato:
  //         |    sql: SELECT potato FROM tomato WHERE 1 = 1""".stripMargin

  //     val injectionQueryString =
  //       """(block_mapping_pair key: (flow_node) @key (#eq? @key "sql") value: (flow_node) @value)"""

  //     val q = Query.create(yaml.Language, injectionQueryString)

  //     q match
  //       case Left(err) =>
  //         failure(err.toString())
  //       case Right(q) =>
  //         Parser.create.parse(source, yaml.Language, List((q, "value", sql.Language))) match
  //           case None =>
  //             failure("Uh oh")

  //           case Some((tree, others)) =>
  //             val othersL = others.toList
  //             expect(tree.rootNode.nodeString == "hek") and
  //               expect(othersL.size == 1) and
  //               expect(othersL.apply(0)._2.rootNode.nodeString == "yak")

  test("incremental parsing - adding"):
    Zone: zone =>
      given Zone = zone

      val source =
        """hello: hello""".stripMargin

      val sourceAfter =
        """hello: helloa"""

      val edit = (Point(0, 12), Point(0, 12), "a")

      val parser = Parser.create

      parser.parse(source, yaml.Language, Nil) match
        case None => 
          failure("uh oh")
        case Some((tree, _)) =>

          val sourceDoc = DocumentString(source)
          val startByte = sourceDoc.byteFor(edit._1)
          val oldEndByte = sourceDoc.byteFor(edit._2)

          val oldLength = oldEndByte - startByte
          val newLength = edit._3.getBytes().length

          val diff = newLength - oldLength

          val newEndByte = oldEndByte + diff

          val tsInputEditPtr = alloc[TSInputEdit](1)
          val tsInputEdit = !tsInputEditPtr

          val newEndPoint = Point(row = edit._2.row, column = edit._2.column + diff)

          tsInputEdit.start_byte = startByte.toUInt
          tsInputEdit.old_end_byte = oldEndByte.toUInt
          tsInputEdit.new_end_byte = newEndByte.toUInt
          tsInputEdit.start_point = !edit._1.value
          tsInputEdit.old_end_point = !edit._2.value
          tsInputEdit.new_end_point = !newEndPoint.value

          val newStringBytes = new Array[Byte](source.length() + diff)
          val (pre, post) = source.getBytes().splitAt(startByte)

          pre.copyToArray(newStringBytes)
          edit._3.getBytes().copyToArray(newStringBytes, pre.length)
          post.copyToArray(newStringBytes, newEndByte)

          val asd = new String(newStringBytes)
          expect(asd == "hello: helloa")

          ts_tree_edit(tree.value, tsInputEditPtr)

          val newTree = ts_parser_parse_string(parser.value, tree.value, toCString(asd), strlen(toCString(asd)).toUInt)

          expect(newTree != null)







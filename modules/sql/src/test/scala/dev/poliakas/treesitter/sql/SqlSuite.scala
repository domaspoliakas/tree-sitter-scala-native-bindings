package dev.poliakas.treesitter.sql

import treesitter.all.*
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*
import scala.scalanative.libc.string.strlen
import scala.scalanative.libc.stdio

object SqlSuite extends weaver.FunSuite {

  test("hek"): 
    Zone: zone => 
      given Zone = zone
      val parser = ts_parser_new()
      ts_parser_set_language(parser, tree_sitter_sql())

      val source = toCString("""SELECT 1 FROM some_table""".stripMargin)

      val tree = 
        ts_parser_parse_string(parser, null, source, strlen(source).toUInt)

      val root_node = 
        ts_tree_root_node(tree)

      val e1 = expect("program" == fromCString(ts_node_type(root_node)))

      val childCount = ts_node_child_count(root_node)

      val e2 = expect(1 == childCount.toInt)

      val statementNode = ts_node_child(root_node, 0.toUInt)

      val e3 = expect("statement" == fromCString(ts_node_type(statementNode)))

      val statementChildrenCount = ts_node_child_count(statementNode)

      val e4 = expect(2 == statementChildrenCount.toInt)

      val statementChildOne = ts_node_child(statementNode, 0.toUInt)
      val e5 = expect("select" == fromCString(ts_node_type(statementChildOne)))

      val statementChildTwo = ts_node_child(statementNode, 1.toUInt)
      val e6 = expect("from" == fromCString(ts_node_type(statementChildTwo)))

      e1 and e2 and e3 and e4 and e5 and e6


}

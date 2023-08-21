package dev.poliakas.treesitter.json 

import treesitter.all.*
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*
import scala.scalanative.libc.string.strlen
import scala.scalanative.libc.stdio

object JsonSuite extends weaver.FunSuite {

  test("hek"): 
    Zone: zone => 
      given Zone = zone
      val parser = ts_parser_new()
      ts_parser_set_language(parser, tree_sitter_yaml())

      val source = c"""hello: hello"""

      val tree = 
        ts_parser_parse_string(parser, null, source, strlen(source).toUInt)

      val root_node = ts_tree_root_node(tree)

      def printChildren(start: TSNode): String =
        def go(node: TSNode, level: Int, acc: String): String =
          val nodeType = fromCString(ts_node_type(node))
          var acc1 = s"$acc${" " * level}$nodeType\n"
          // asInstanceOf is a bug in codegen
          val childrenCount = ts_node_child_count(node)
          if childrenCount != 0.toUInt then
            for childId <- 0 until childrenCount.toInt do
              val childNode =
                ts_node_child(node, childId.toUInt)
              acc1 = s"$acc1${go(childNode, level + 1, acc1)}"
          acc1
        end go

        go(start, 0, "")
      end printChildren

      val res = printChildren(root_node)


      expect("potato" == res)
  

}

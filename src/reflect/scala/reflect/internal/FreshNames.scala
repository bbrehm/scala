/*
 * Scala (https://www.scala-lang.org)
 *
 * Copyright EPFL and Lightbend, Inc. dba Akka
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package scala
package reflect
package internal

import scala.reflect.internal.util.FreshNameCreator
import scala.util.matching.Regex

trait FreshNames { self: Names with StdNames =>
  // scala/bug#6879 Keeps track of counters that are supposed to be globally unique
  //         as opposed to traditional freshers that are unique to compilation units.
  val globalFreshNameCreator = new FreshNameCreator

  // default fresh name creator used to abstract over currentUnit.fresh and runtime fresh name creator
  def currentFreshNameCreator: FreshNameCreator

  // create fresh term/type name using implicit fresh name creator
  def freshTermName(prefix: String = nme.FRESH_TERM_NAME_PREFIX)(implicit creator: FreshNameCreator): TermName = newTermName(creator.newName(prefix))
  def freshTypeName(prefix: String)(implicit creator: FreshNameCreator): TypeName = newTypeName(creator.newName(prefix))

  // Extractor that matches names which were generated by some
  // FreshNameCreator with known prefix. Extracts user-specified
  // prefix that was used as a parameter to newName by stripping
  // global creator prefix and unique numerical suffix.
  // The creator prefix and numerical suffix may both be empty.
  class FreshNameExtractor(creatorPrefix: String = "") {

    // name should start with creatorPrefix and end with number
    val freshlyNamed = {
      val pre = if (!creatorPrefix.isEmpty) Regex quote creatorPrefix else ""
      s"""$pre(.*?)\\d*""".r
    }

    def unapply(name: Name): Option[String] =
      name.toString match {
        case freshlyNamed(prefix) => Some(prefix)
        case _                    => None
      }
  }
}

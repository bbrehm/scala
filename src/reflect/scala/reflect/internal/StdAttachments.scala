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

trait StdAttachments {
  self: SymbolTable =>

  /**
   * Common code between reflect-internal Symbol and Tree related to Attachments.
   */
  trait Attachable {
    protected var rawatt: scala.reflect.macros.Attachments { type Pos = Position } = NoPosition
    def attachments = rawatt
    def setAttachments(attachments: scala.reflect.macros.Attachments { type Pos = Position }): this.type = { rawatt = attachments; this }
    def updateAttachment[T: ClassTag](attachment: T): this.type = { rawatt = rawatt.update(attachment); this }
    def removeAttachment[T: ClassTag]: this.type = { rawatt = rawatt.remove[T]; this }
    def getAndRemoveAttachment[T: ClassTag]: Option[T] = {
      val r = attachments.get[T]
      if (r.nonEmpty) removeAttachment[T]
      r
    }
    def hasAttachment[T: ClassTag]: Boolean = rawatt.contains[T]

    // cannot be final due to SynchronizedSymbols
    def pos: Position = rawatt.pos
    def pos_=(pos: Position): Unit = rawatt = (rawatt withPos pos)
    def setPos(newpos: Position): this.type = { pos = newpos; this }
  }

  /** Attachment that knows how to import itself into another universe. */
  trait ImportableAttachment {
    def importAttachment(importer: Importer): this.type
  }

  /** Attachment that doesn't contain any reflection artifacts and can be imported as-is. */
  trait PlainAttachment extends ImportableAttachment {
    def importAttachment(importer: Importer): this.type = this
  }

  /** Stores the trees that give rise to a refined type to be used in reification.
   *  Unfortunately typed `CompoundTypeTree` is lacking essential info, and the reifier cannot use `CompoundTypeTree.tpe`.
   *  Therefore we need this hack (see `Reshape.toPreTyperTypeTree` for a detailed explanation).
   */
  case class CompoundTypeTreeOriginalAttachment(parents: List[Tree], stats: List[Tree])

  /** Attached to a Function node during type checking when the expected type is a SAM type (and not a built-in FunctionN).
    *
    * Ideally, we'd move to Dotty's Closure AST, which tracks the environment,
    * the lifted method that has the implementation, and the target type.
    * For backwards compatibility, an attachment is the best we can do right now.
    *
    * @param samTp the expected type that triggered sam conversion (may be a subtype of the type corresponding to sam's owner)
    * @param sam the single abstract method implemented by the Function we're attaching this to
    * @param synthCls the (synthetic) class representing the eventual implementation class (spun at runtime by LMF on the JVM)
    */
  case class SAMFunction(samTp: Type, sam: Symbol, synthCls: Symbol) extends PlainAttachment

  case object DelambdafyTarget extends PlainAttachment

  /** When present, indicates that the host `Ident` has been created from a backquoted identifier.
   */
  case object BackquotedIdentifierAttachment extends PlainAttachment

  /** Indicates that a selection was postfix (on Select tree) */
  case object PostfixAttachment extends PlainAttachment
  /** Indicates that an application was infix (on Apply tree) */
  case object InfixAttachment extends PlainAttachment
  /** Indicates that an application to `()` was inserted by the compiler */
  case object AutoApplicationAttachment extends PlainAttachment

  /** A pattern binding exempt from unused warning.
   *
   *  Its host `Ident` has been created from a pattern2 binding, `case x @ p`.
   *  In the absence of named parameters in patterns, allows nuanced warnings for unused variables.
   *  Hence, `case X(x = _) =>` would not warn; for now, `case X(x @ _) =>` is documentary if x is unused.
   */
  case object NoWarnAttachment extends PlainAttachment

  /** A pattern binding that shadows a symbol in scope. Removed by refchecks.
   */
  case class PatShadowAttachment(shadowed: Symbol)

  /** Indicates that a `ValDef` was synthesized from a pattern definition, `val P(x)`.
   */
  case object PatVarDefAttachment extends PlainAttachment

  /** Indicates that a definition was part of either a pattern or "sequence shorthand"
   *  that introduced multiple definitions. All variables must be either `val` or `var`.
   */
  case object MultiDefAttachment extends PlainAttachment

  /** Identifies trees are either result or intermediate value of for loop desugaring.
   */
  case object ForAttachment extends PlainAttachment

  /** Identifies unit constants which were inserted by the compiler (e.g. gen.mkBlock)
   */
  case object SyntheticUnitAttachment extends PlainAttachment

  /** Untyped list of subpatterns attached to selector dummy. */
  case class SubpatternsAttachment(patterns: List[Tree])

  abstract class InlineAnnotatedAttachment
  case object NoInlineCallsiteAttachment extends InlineAnnotatedAttachment
  case object InlineCallsiteAttachment extends InlineAnnotatedAttachment

  /** Attached to a local class that has its outer field elided. A `null` constant may be passed
    * in place of the outer parameter, can help callers to avoid capturing the outer instance.
    */
  case object OuterArgCanBeElided extends PlainAttachment

  case object UseInvokeSpecial extends PlainAttachment

  /** An attachment carrying information between uncurry and erasure */
  case class TypeParamVarargsAttachment(val typeParamRef: Type)

  /** Attached to a class symbol to indicate that its children have been observed
    * via knownDirectSubclasses. Children added subsequently will trigger an
    * error to indicate that the earlier observation was incomplete.
    */
  case object KnownDirectSubclassesCalled extends PlainAttachment

  case object DottyEnumSingleton extends PlainAttachment

  class DottyParameterisedTrait(val params: List[Symbol])

  class DottyOpaqueTypeAlias(val tpe: Type)

  class QualTypeSymAttachment(val sym: Symbol)

  case object ConstructorNeedsFence extends PlainAttachment

  /** Mark the syntax for linting purposes. */
  case object MultiargInfixAttachment extends PlainAttachment

  case object NullaryOverrideAdapted extends PlainAttachment

  // When typing a Def with this attachment, change the owner of its RHS from origalOwner to the symbol of the Def
  case class ChangeOwnerAttachment(originalOwner: Symbol)

  case object InterpolatedString extends PlainAttachment

  case object VirtualStringContext extends PlainAttachment

  case object CaseApplyInheritAccess extends PlainAttachment

  // Use of _root_ is in correct leading position of selection
  case object RootSelection extends PlainAttachment

  /** Marks a Typed tree with Unit tpt. */
  case object TypedExpectingUnitAttachment extends PlainAttachment
  def explicitlyUnit(tree: Tree): Boolean = tree.hasAttachment[TypedExpectingUnitAttachment.type]

  /** For `val i = 42`, marks field as inferred so accessor (getter) can warn if implicit. */
  case object FieldTypeInferred extends PlainAttachment

  case class LookupAmbiguityWarning(msg: String, fix: String) extends PlainAttachment

  /** Java sealed classes may be qualified with a permits clause specifying allowed subclasses. */
  case class PermittedSubclasses(permits: List[Tree]) extends PlainAttachment
  case class PermittedSubclassSymbols(permits: List[Symbol]) extends PlainAttachment

  case class NamePos(pos: Position) extends PlainAttachment

  /** Not a named arg in an application. Used for suspicious literal booleans. */
  case object UnnamedArg extends PlainAttachment

  /** Adapted under value discard at typer. */
  case object DiscardedValue extends PlainAttachment
  /** Discarded pure expression observed at refchecks. */
  case object DiscardedExpr extends PlainAttachment
  /** Anonymous parameter of `if (_)` may be inferred as Boolean. */
  case object BooleanParameterType extends PlainAttachment
}

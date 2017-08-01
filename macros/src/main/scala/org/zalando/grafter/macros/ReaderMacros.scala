package org.zalando.grafter.macros

import scala.meta._

object ReaderMacros {

  /** get the annotated class and, if available, companion object */
  def annotatedClass(name: String)(annotated: Any): (Defn.Class, Option[Defn.Object]) = {

    annotated match {
      case block: Term.Block =>
        block.stats match {
          case (classDef: Defn.Class) :: (companionDef: Defn.Object) :: _ =>
            (classDef, Some(companionDef))

          case _ =>
            abort(s"the @$name annotation must annotate a class, no statements found")
        }

      case classDef: Defn.Class =>
        (classDef, None)

      case other =>
        abort(s"the @$name annotation must annotate a class, found $other")
    }
  }

  /** get the annotated trait and, if available, companion object */
  def annotatedTrait(name: String)(annotated: Tree): (Defn.Trait, Option[Defn.Object]) =
    annotated match {
      case block: Term.Block =>
        block.stats match {
          case (traitDef: Defn.Trait) :: (companionDef: Defn.Object) :: _ =>
            (traitDef, Some(companionDef))

          case _ =>
            abort(s"the @$name annotation must annotate a trait, no statements found")
        }

      case traitDef: Defn.Trait =>
        (traitDef, None)

      case other =>
        abort(s"the @$name annotation must annotate a trait, found $other")
    }

  def output(classDef: Defn with Member.Type, objectDef: Option[Defn.Object])(out: Stat*): Term.Block  = {
    val o = objectDef.getOrElse(q"object ${Term.Name(classDef.name.value)}")
    val extendedObject = o.copy(templ = o.templ.copy(stats = o.templ.stats ++ out.toList))

    q"""
      $classDef
      $extendedObject
    """
  }

  def collectParamTypesAndNames(params: Seq[Tree]): Map[Type.Name, Term.Name] =
    collectParamTypesAndNamesAsList(params).groupBy(_._1.value).map {
      case (typeName, termNames) => (Type.Name(typeName), termNames.head._2)
    }

  def collectParamTypesAndNamesAsList(params: Seq[Tree]): List[(Type.Name, Term.Name)] =
    params.toList.collect {
      case param"$paramName: $paramType" =>
        paramType match {
          case Some(t: Type) =>
            Option((Type.Name(t.syntax), Term.Name(paramName.syntax)))
          case _ =>
            None
        }
    }.flatten

  implicit class StringOps(s: String) {
    def uncapitalize: String =
      s.take(1).map(_.toLower)++s.drop(1)
  }

  def abort(message: String) =
    throw new scala.macros.internal.AbortMacroException(scala.macros.Position.None, message)
}

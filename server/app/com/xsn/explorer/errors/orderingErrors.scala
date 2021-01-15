package com.xsn.explorer.errors

import com.alexitc.playsonify.core.I18nService
import com.alexitc.playsonify.models.{
  FieldValidationError,
  InputValidationError,
  PublicError
}

sealed trait OrderingError

case object UnknownOrderingFieldError
    extends OrderingError
    with InputValidationError {

  override def toPublicErrorList[L](
      i18nService: I18nService[L]
  )(implicit lang: L): List[PublicError] = {
    val message = i18nService.render("error.ordering.unknownField")
    val error = FieldValidationError("orderBy", message)

    List(error)
  }
}

case object InvalidOrderingConditionError
    extends OrderingError
    with InputValidationError {

  override def toPublicErrorList[L](
      i18nService: I18nService[L]
  )(implicit lang: L): List[PublicError] = {
    val message = i18nService.render("error.ordering.unknownCondition")
    val error = FieldValidationError("orderBy", message)

    List(error)
  }
}

case object InvalidOrderingError
    extends OrderingError
    with InputValidationError {

  override def toPublicErrorList[L](
      i18nService: I18nService[L]
  )(implicit lang: L): List[PublicError] = {
    val message = i18nService.render("error.ordering.invalid")
    val error = FieldValidationError("orderBy", message)

    List(error)
  }
}

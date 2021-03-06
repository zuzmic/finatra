.. _validation:

Validation Framework
====================

Finatra provides a simple validation framework inspired by `JSR-303 <https://docs.oracle.com/javaee/6/tutorial/doc/gircz.html>`__ and `JSR-380 <https://jcp.org/en/jsr/detail?id=380>`__.

Constraints
-----------
Constraints are Java interfaces that can be used to annotate a case class field or case class method with specific validation criteria.
Similar to JSR-380 specification, the validation framework supports the following built in constraints (for adding new Constraints, see `Defining Additional Constraints <#define-additional-constraints>`__):

-  `@CountryCode <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/CountryCode.java>`__
-  `@FutureTime <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/FutureTime.java>`__
-  `@PastTime <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/PastTime.java>`__
-  `@Pattern <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/Pattern.java>`__
-  `@Max <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/Max.java>`__
-  `@Min <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/Min.java>`__
-  `@NotEmpty <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/NotEmpty.java>`__
-  `@OneOf <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/OneOf.java>`__
-  `@Range <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/Range.java>`__
-  `@Size <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/Size.java>`__
-  `@TimeGranularity <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/TimeGranularity.java>`__
-  `@UUID <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/constraints/UUID.java>`__
-  `@MethodValidation <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/MethodValidation.java>`__

Using a `Validator`
-------------------
The validation framework can be used without any dependencies. You can validate a case class's fields and methods that are annotated with any built in or additional
constraints by simply instantiating a `Validator <https://github.com/twitter/finatra/blob/develop/validation/src/main/scala/com/twitter/finatra/validation/Validator.scala>`__
and call `validate()` with the case class.

The call will return a `Unit` when all validations pass. Otherwise, it will throw a
`ValidationException <https://github.com/twitter/finatra/blob/develop/validation/src/main/scala/com/twitter/finatra/validation/ValidationException.scala>`__,
where the `errors` field contains a list of invalid `ValidationResult <https://github.com/twitter/finatra/blob/develop/validation/src/main/scala/com/twitter/finatra/validation/ValidationResult.scala>`__.

For instance, assuming we have the following case class defined:

.. code:: scala

    case class Things(@Size(min = 1, max = 2) names: Seq[String])

We can now validate that an instance conforms to the constraints, e.g.,

.. code:: scala

    val myThings = Things(Seq.empty[String])
    val validator = injector.instance[Validator]
    try {
      validator.validate(myThings)
    } catch {
      case e: ValidationException =>
        error(e, e.getMessage)
    }

Here the `ValidationException <https://github.com/twitter/finatra/blob/develop/validation/src/main/scala/com/twitter/finatra/validation/ValidationException.scala>`__
would get thrown since the instance does not conform to the constraints. You can iterate over the
contained ValidationException#errors to see all the errors which triggered a failed validation.
Whereas the following would pass:

.. code:: scala

    val myThings = Things(Seq(“Bob Vila”))
    val validator = injector.instance[Validator]
    try {
      validator.validate(myThings)
    } catch {
      case e: ValidationException =>
        error(e, e.getMessage)
    }

.. warning::

    Case classes with multiple constructors or multiple constructor lists with non-public constructor args are **not-supported**.

Instantiate a `Validator`
^^^^^^^^^^^^^^^^^^^^^^^^^
There are 2 ways to obtain a `Validator` instance:

- Through dependency injection. Finatra HttpServer injects a default `Validator <https://github.com/twitter/finatra/blob/develop/validation/src/main/scala/com/twitter/finatra/validation/Validator.scala>`__, you can override it by providing a customized `Validator` in a `TwitterModule`, and add that module to your server definition. Please checkout `Injecting a customized Validator  <#injecting-a-customized-validator>`__ for instructions.
- Create a `Validator` instance in the air when you need it. You can call `Validator()` to obtain a validator instance with default `MessageResolver <https://github.com/twitter/finatra/blob/develop/validation/src/main/scala/com/twitter/finatra/validation/MessageResolver.scala>`__ and default `cache size <https://github.com/twitter/finatra/blob/f51aa1ff0ff40a7444ca38100a7623c6bfccdd2c/validation/src/main/scala/com/twitter/finatra/validation/Validator.scala#L23>`__, or create a validator instance with customized `messageResolver` by calling `Validator(messageResolver)`. You can also leverage `Validator.Builder` to customize more attributes of the `Validator` including `cacheSize`. Please see the `example <#injecting-a-customized-validator>`__ on how to use `Builder`.

.. note::

    Instantiating a `Validator` through dependency injection is the recommended way to use the framework. Please avoid creating a `Validator` in the air
    if you have already injected it in the object graph.

Define additional constraints
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
To define new constraints, you can extend the `Constraint <https://github.com/twitter/finatra/blob/develop/validation/src/main/java/com/twitter/finatra/validation/Constraint.java>`__ interface,
and define a matching `ConstraintValidator <https://github.com/twitter/finatra/blob/develop/validation/src/main/scala/com/twitter/finatra/validation/ConstraintValidator.scala>`__ that is referred to in the
`validatedBy` field of the new constraint definition.

Define an additional `Constraint`:

.. code:: java

    import java.lang.annotation.ElementType;
    import java.lang.annotation.Retention;
    import java.lang.annotation.RetentionPolicy;
    import java.lang.annotation.Target;

    import com.twitter.finatra.validation.Constraint;

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = StateConstraintValidator.class)
    public @interface StateConstraint {}

Define an additional `ConstraintValidator`:

.. code:: scala

    import com.twitter.finatra.validation.{ConstraintValidator, MessageResolver, ValidationResult}

    class StateConstraintValidator(messageResolver: MessageResolver)
        extends ConstraintValidator[StateConstraint, String](messageResolver) {

      override def isValid(annotation: StateConstraint, value: String): ValidationResult =
        ValidationResult.validate(
          value.equalsIgnoreCase("CA"),
          "Please register with state CA"
        )
    }

The validation framework will locate the new `StateConstraint` and perform the validation logic
defined in its matching `StateConstraintValidator` automatically at run time.

Injecting a customized Validator
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
You can switch to use another `MessageResolver` or change the `cacheSize` of the default `Validator`.

Provide a customized Validator in a `TwitterModule`:

.. code:: scala

    import com.twitter.finatra.validation.{Validator, ValidatorModule}
    import com.twitter.inject.Injector

    object CustomizedValidatorModule extends ValidatorModule {
      override def configureValidator(injector: Injector, builder: Validator.Builder): Validator.Builder =
        builder
          .withCacheSize(512)
          .withMessageResolver(new CustomizedMessageResolver())
    }

Override `validatorModule` in your server definition:

.. code:: scala

    ValidationServer extends HttpServer {
      override val name: String = "validation-server"
      override def validatorModule: TwitterModule = CustomizedValidatorModule

      override protected def configureHttp(router: HttpRouter): Unit = {
        router
          .filter[CommonFilters]
          .add[ValidationController]
          ...
      }
    }

.. note::

    By overriding the default `validatorModule`, you are also replacing the default `Validator` in
    the `jacksonModule <https://github.com/twitter/finatra/blob/ef7197324cba8dfe6274ffb4570dea6c34b9fc33/http/src/main/scala/com/twitter/finatra/http/servers.scala#L526>`__.
    The newly defined `Validator` will be used to apply the validation logic in the
    `ScalaObjectMapper <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/jackson/ScalaObjectMapper.scala>`__
    and during HTTP request parsing.

    See `Integration with Finatra Jackson Support <#integration-with-finatra-jackson-support>`__
    for more information about how validations works in Finatra Jackson Framework.

Integration with Finatra Jackson Support
----------------------------------------
The validation framework integrates with Finatra's custom `case class` deserializer to efficiently
apply per field and method validations as request parsing is performed.

Assume you have the following HTTP request case class defined:

.. code:: scala

    case class ValidateUserRequest(
      @NotEmpty @Pattern(regexp = "[a-z]+") userName: String,
      @Max(value = 9999) id: Long,
      title: String
    )

And in your controller, you define a Post endpoint as:

.. code:: scala

    post("/validate_user") { _: ValidateUserRequest =>
      ...
    }

When you perform a call to the POST `/validate_user/`, Finatra will deserialize the JSON you passed to the call
to a `ValidationUserRequest` case class, and perform validations of the annotated fields. If any validation
fails, the case class will not be created and a
`CaseClassMappingException <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/internal/caseclass/exceptions/CaseClassMappingException.scala>`__
will be thrown.

For more information, please refer to `JSON Validation Framework <../json/validations.html>`__.

Method Validations
------------------

A method validation is a case class method annotated with ``@MethodValidation`` which is intended to be used for validating fields of the cases class. Reasons to use a method validation include:

-  For non-generic validations. ``@MethodValidation`` can be used instead of defining a reusable annotation and validator.
-  Cross-field validations (e.g. `startDate` before `endDate`)

For an example see the `User <https://github.com/twitter/finatra/blob/d874b1a92cd2cc257bf0d170cfb46a486df3fb5d/validation/src/test/scala/com/twitter/finatra/validation/tests/caseclasses.scala#L104>`__ test case class.

The ``@MethodValidation`` annotation also supports specifying an optional ``fields`` parameter to
state which fields are being evaluated in the validation. If the evaluation fails the resulting
exception will contain details about each of the fields specified in the annotation.

Best Practices, Guidance & Limitations
--------------------------------------

Case classes are generally expected to be defined like simple `POJOs <https://en.wikipedia.org/wiki/Plain_old_Java_object>`__,
or `JavaBeans <https://en.wikipedia.org/wiki/JavaBeans#JavaBean_conventions>`__ (but without the
need to implement `java.io.Serializable`). Constraints are expected to be defined on case class
constructor fields.

- When defining constraints, case classes MUST be defined with a single constructor. If multiple
  constructors are defined, there is currently no way to signal to the Validation framework which
  constructor should be used for finding constraint annotations.
- When defining constraints, case classes MUST be defined with all constructors args publicly visible.
  E.g., a constructor defined like so:

  .. code:: scala

      case class DoublePerson(@NotEmpty name: String)(@NotEmpty otherName: String)

  will **not work** properly as second parameter list fields by default are not publicly visible. If you
  find you need to do this case, you will need to explicitly make the fields in the second parameter
  list visible:

  .. code:: scala

      case class DoublePerson(@NotEmpty name: String)(@NotEmpty val otherName: String)
- Nested case classes are currently not supported. That is, validation will not occur for fields which are
  case classes themselves with constructor field annotated with constraints. E.g.,

  .. code:: scala

      case class Bar(@Max(1000) id: Int, @NonEmpty name: String)

      case Foo(@Min(1) id: Int, bar: Bar)

  `Bar` validations will not be performed. We hope to support the case of validating nested case classes
  in a future version of the API.

  .. important ::

    However, note when used within the `Finatra Jackson Integration <../json/index.html>`__,
    nested case class validations will be performed due to the library's integration with Jackson's
    deserializers.

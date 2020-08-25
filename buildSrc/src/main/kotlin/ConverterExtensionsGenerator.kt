import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import net.imglib2.converter.BiConverter
import net.imglib2.converter.Converter
import net.imglib2.converter.Converters
import net.imglib2.converter.readwrite.SamplerConverter
import net.imglib2.type.Type
import net.imglib2.type.numeric.IntegerType
import net.imglib2.type.numeric.RealType
import net.imglib2.type.numeric.integer.*
import net.imglib2.type.numeric.real.DoubleType
import net.imglib2.type.numeric.real.FloatType
import kotlin.reflect.KClass

private const val convertName = "convert"
private const val asTypeName = "asType"

private val realAndIntegerTypes = mapOf<String, KClass<out RealType<*>>>(
        "doubles" to DoubleType::class,
        "floats" to FloatType::class,
        "longs" to LongType::class,
        "ints" to IntType::class,
        "shorts" to ShortType::class,
        "bytes" to ByteType::class,
        "unsignedLongs" to UnsignedLongType::class,
        "unsignedInts" to UnsignedIntType::class,
        "unsignedShorts" to UnsignedShortType::class,
        "unsignedBytes" to UnsignedByteType::class
)

fun generateConverterExtensions(`as`: String, fileName: String): String {
    val container = containers[`as`] ?: error("Key `$`as`' not present in $containers")
    val kotlinFile =  FileSpec
            .builder("net.imglib2.imklib", fileName)
            .addAliasedImport(container, `as`)
            .addConverterExtensions(container)
            .addTypeConversionExtensions(container)
    return StringBuilder().also { sb -> kotlinFile.build().writeTo(sb) }.toString()
}

private fun FileSpec.Builder.addConverterExtensions(container: ClassName): FileSpec.Builder {
    val t = TypeVariableName("T")
    val u = TypeVariableName("U")
    val v = TypeVariableName("V")
    val typeOfU = Type::class.asTypeName().parameterizedBy(u)
    val typeOfV = Type::class.asTypeName().parameterizedBy(v)
    val uType = TypeVariableName("U", typeOfU)
    val vType = TypeVariableName("V", typeOfV)
    val tIn = TypeVariableName("T", KModifier.IN)
    return this
            .addFunction(generateConverterExtension(container, t, uType))
            .addFunction(generateConverterExtensionLambda(container, t, uType))
            .addFunction(generateBiconverterExtension(container, t, u, vType))
            .addFunction(generateBiconverterExtensionLambda(container, t, u, vType))
//    TODO need to fix variance for SamplerConverter before we can use this
//            .addFunction(generateSamplerConverterExtension(container, tIn, uType))
//    fun <T, U: Type<U>> RA<T>.convert(converter: SamplerConverter<in T, U>) = Converters.convert(this, converter)
}

private fun generateConverterExtension(container: ClassName, t: TypeVariableName, u: TypeVariableName): FunSpec {
    return typedFuncSpecBuilder(convertName, container.parameterizedBy(t), t, u)
            .addParameter("u", u)
            .addParameter("converter", Converter::class.asClassName().parameterizedBy(t, u))
            .addStatement("return %T.convert(this,·converter,·u)", Converters::class)
            .build()
}

private fun generateConverterExtensionLambda(container: ClassName, t: TypeVariableName, u: TypeVariableName): FunSpec {
    return typedFuncSpecBuilder(convertName, container.parameterizedBy(t), t, u)
            .addModifiers(KModifier.INLINE)
            .addParameter("u", u)
            .addParameter(higherOrderFunctionParameter("converter", t, u))
            .addStatement("return convert(u,·Converter·{·a,·b·->·converter(a,·b)·})")
            .build()
}

private fun generateBiconverterExtension(container: ClassName, t: TypeVariableName, u: TypeVariableName, v: TypeVariableName): FunSpec {
    return typedFuncSpecBuilder(convertName, container.parameterizedBy(t), t, u, v)
            .addParameter("that", container.parameterizedBy(u))
            .addParameter("v", v)
            .addParameter("converter", BiConverter::class.asClassName().parameterizedBy(t, u, v))
            .addStatement("return %T.convert(this,·that,·converter,·v)", Converters::class)
            .build()
}

private fun generateBiconverterExtensionLambda(container: ClassName, t: TypeVariableName, u: TypeVariableName, v: TypeVariableName): FunSpec {
    return typedFuncSpecBuilder(convertName, container.parameterizedBy(t), t, u, v)
            .addModifiers(KModifier.INLINE)
            .addParameter("that", container.parameterizedBy(u))
            .addParameter("v", v)
            .addParameter(higherOrderFunctionParameter("converter", t, u, v))
            .addStatement("return convert(that,·v,·BiConverter·{·a,·b,·c·->·converter(a,·b,·c)·})")
            .build()
}

private fun generateSamplerConverterExtension(container: ClassName, t: TypeVariableName, u: TypeVariableName): FunSpec {
    return typedFuncSpecBuilder(convertName, container.parameterizedBy(t), TypeVariableName(t.name, variance = null), u)
            .addParameter("converter", SamplerConverter::class.asClassName().parameterizedBy(t, u))
            .addStatement("return %T.convert(this,·converter)", Converters::class)
            .build()
}

private fun higherOrderFunctionParameter(
        name: String,
        vararg className: TypeName,
        returnType: TypeName = Unit::class.asClassName()): ParameterSpec {
    val lambdaType = LambdaTypeName.get(null, *className, returnType = returnType)
    return ParameterSpec.builder(name, lambdaType, KModifier.CROSSINLINE).build()
}

private fun FileSpec.Builder.addTypeConversionExtensions(container: ClassName): FileSpec.Builder {
    return this
            .addFunction(generateRealTypeConversionExtensions(container))
            .addFunction(generateRealTypeConversionExtensionsFromWildcard(container))
            .addFunction(generateIntegerTypeConversionExtensions(container))
            .addFunction(generateIntegerTypeConversionExtensionsFromWildcard(container))
            .let { generateRealTypeToRealTypeConversionExtensions(container).fold(it) { acc, f -> acc.addFunction(f) } }
            .let { generateRealTypeWildcardToRealTypeConversionExtensions(container).fold(it) { acc, f -> acc.addFunction(f)} }
            .let { generateIntegerTypeToRealTypeConversionExtensions(container).fold(it) { acc, f -> acc.addFunction(f) } }
            .let { generateIntegerTypeWildcardToRealTypeConversionExtensions(container).fold(it) { acc, f -> acc.addFunction(f)} }
}

private fun generateIntegerTypeToRealTypeConversionExtensions(container: ClassName) = realAndIntegerTypes.map { generateIntegerTypeToRealTypeConversionExtensions("as${it.key.capitalize()}", container, it.value) }

private fun generateIntegerTypeToRealTypeConversionExtensions(name: String, container: ClassName, to: KClass<out RealType<*>>): FunSpec {
    val t = TypeVariableName("T", IntegerType::class.asTypeName().parameterizedBy(TypeVariableName("T")))
    return typedFuncSpecBuilder(name, container.parameterizedBy(t), t)
            .addStatement("return asType(%T())", to.asTypeName())
            .addAnnotation(AnnotationSpec.builder(JvmName::class).addMember("name = %S", "${name}FromIntegerType").build())
            .build()
}

private fun generateRealTypeToRealTypeConversionExtensions(container: ClassName) = realAndIntegerTypes.map { generateRealTypeToRealTypeConversionExtensions("as${it.key.capitalize()}", container, it.value) }

private fun generateRealTypeToRealTypeConversionExtensions(name: String, container: ClassName, to: KClass<out RealType<*>>): FunSpec {
    val t = TypeVariableName("T", RealType::class.asTypeName().parameterizedBy(TypeVariableName("T")))
    return typedFuncSpecBuilder(name, container.parameterizedBy(t), t)
            .addStatement("return asType(%T())", to.asTypeName())
            .addAnnotation(AnnotationSpec.builder(JvmName::class).addMember("name = %S", "${name}FromRealType").build())
            .build()
}

private fun generateRealTypeConversionExtensions(container: ClassName)
        = generateGenericTypeConversionExtension(container, RealType::class.asTypeName(), RealType::class.asTypeName(), "realDouble", "setReal")

private fun generateIntegerTypeConversionExtensions(container: ClassName)
        = generateGenericTypeConversionExtension(container, IntegerType::class.asTypeName(), IntegerType::class.asTypeName(), "integerLong", "setInteger")

private fun generateGenericTypeConversionExtension(container: ClassName, typeT: ClassName, typeU: ClassName, getter: String, setter: String): FunSpec {
    val t = TypeVariableName("T")
    val u = TypeVariableName("U")
    val typeOfT = typeT.parameterizedBy(t)
    val typeOfU = typeU.parameterizedBy(u)
    val tType = TypeVariableName("T", typeOfT)
    val uType = TypeVariableName("U", typeOfU)
    return typedFuncSpecBuilder(asTypeName, container.parameterizedBy(t), tType, uType)
            .addParameter("u", u)
            .addStatement("return if·(u::class·==·type::class)·this·as·%T else·convert(u)·{·s,·t·->·t.$setter(s.$getter)·}", container.parameterizedBy(u))
            .build()
}

private fun generateIntegerTypeWildcardToRealTypeConversionExtensions(container: ClassName) = realAndIntegerTypes.map { generateIntegerTypeWildcardToRealTypeConversionExtensions("as${it.key.capitalize()}", container, it.value) }

private fun generateIntegerTypeWildcardToRealTypeConversionExtensions(name: String, container: ClassName, to: KClass<out RealType<*>>): FunSpec {
    val wc = IntegerType::class.asTypeName().parameterizedBy(TypeVariableName("*"))
    return typedFuncSpecBuilder(name, container.parameterizedBy(wc))
            .addStatement("return asType(%T())", to.asTypeName())
            .addAnnotation(AnnotationSpec.builder(JvmName::class).addMember("name = %S", "${name}FromIntegerTypeWildcard").build())
            .build()
}

private fun generateRealTypeWildcardToRealTypeConversionExtensions(container: ClassName) = realAndIntegerTypes.map { generateRealTypeWildcardToRealTypeConversionExtensions("as${it.key.capitalize()}", container, it.value) }

private fun generateRealTypeWildcardToRealTypeConversionExtensions(name: String, container: ClassName, to: KClass<out RealType<*>>): FunSpec {
    val wc = RealType::class.asTypeName().parameterizedBy(TypeVariableName("*"))
    return typedFuncSpecBuilder(name, container.parameterizedBy(wc))
            .addStatement("return asType(%T())", to.asTypeName())
            .addAnnotation(AnnotationSpec.builder(JvmName::class).addMember("name = %S", "${name}FromRealTypeWildcard").build())
            .build()
}

private fun generateRealTypeConversionExtensionsFromWildcard(container: ClassName)
        = generateGenericTypeConversionExtensionFromWildcard(container, RealType::class.asTypeName(), "getRealDouble()", "setReal")

private fun generateIntegerTypeConversionExtensionsFromWildcard(container: ClassName)
        = generateGenericTypeConversionExtensionFromWildcard(container, IntegerType::class.asTypeName(), "getIntegerLong()", "setInteger")

private fun generateGenericTypeConversionExtensionFromWildcard(container: ClassName, typeT: ClassName, getter: String, setter: String): FunSpec {
    val t = TypeVariableName("T")
    val r = TypeVariableName("*")
    val typeOfT = typeT.parameterizedBy(t)
    val typeOfR = typeT.parameterizedBy(r)
    val tType = TypeVariableName("T", typeOfT)
    return typedFuncSpecBuilder(asTypeName, container.parameterizedBy(WildcardTypeName.producerOf(typeOfR)), tType)
            .addParameter("t", t)
            .addAnnotation(AnnotationSpec.builder(JvmName::class).addMember("name = %S", "from${typeT.simpleName}Wildcard").build())
            .addStatement("return if·(t::class·==·getType()::class)·this·as·%T else·convert(t)·{·s,·u·->·u.$setter(s.$getter)·}", container.parameterizedBy(t))
            .build()
}

// TOOD
//fun <C: ComplexType<C>, R: RealType<R>> RA<C>.real(type: R) = convert(ComplexPart.REAL.converter(type))
//fun <C: ComplexType<C>, R: RealType<R>> RA<C>.imaginary(type: R) = convert(ComplexPart.IMAGINARY.converter(type))
//val <C: ComplexType<C>> RA<C>.real get() = real(DoubleType())
//val <C: ComplexType<C>> RA<C>.imaginary get() = imaginary(DoubleType())

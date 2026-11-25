package dev.huniee.hayul.patches.runtime

import app.morphe.patcher.patch.PatchException
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.builder.MethodImplementationBuilder
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction12x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableTypeReference
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import dev.huniee.hayul.patches.shared.Constants.FACTORY_TYPE
import dev.huniee.hayul.patches.shared.requireSupportedPackage

private const val PACKAGE_MANAGER_TYPE = "Lcom/yc/pm/PackageManagerStub;"
private const val GET_PACKAGE_INFO_TYPE = "Lcom/yc/pm/PackageManagerStub\$GetPackageInfo;"
private const val UTILS_TYPE = "Ldev/seola/apppatcher/stub/Utils;"
private const val PACKAGE_INFO_TYPE = "Landroid/content/pm/PackageInfo;"

/**
 * 패치 번들에 든 원본 Hayul DEX(assets/patcher.dex)를 앱에 맞게 고칩니다.
 *
 * 원본에서 WebView 훅(WebViewUpdateServiceStub)과 기기 정보 변조를 빼고, 서명 교체를 자기 패키지 조회로 제한합니다.
 */
internal object HayulPayload {
    fun forPackage(packageName: String): ByteArray {
        requireSupportedPackage(packageName)
        val original = javaClass.classLoader.getResourceAsStream("extensions/hayul-original.dex")
            ?: throw PatchException("패치 번들(.mpp)에 Hayul patcher.dex가 없습니다.")
        val dex = original.buffered().use { DexBackedDexFile.fromInputStream(Opcodes.getDefault(), it) }
        return transform(dex.classes, packageName)
    }

    private fun transform(classes: Iterable<ClassDef>, packageName: String): ByteArray {
        requireSupportedPackage(packageName)
        var initializerReplaced = false
        var signatureHookReplaced = false
        val sanitized = classes.filterNot {
            it.type.startsWith("Lcom/yc/pm/WebViewUpdateServiceStub")
        }.map { clazz ->
            val methods = clazz.methods.map { method ->
                when {
                    clazz.type == FACTORY_TYPE && method.name == "<clinit>" -> {
                        initializerReplaced = true
                        method.withImplementation(factoryInitializer())
                    }
                    clazz.type == GET_PACKAGE_INFO_TYPE && method.name == "call" &&
                        method.parameterTypes.map { it.toString() } == listOf(
                            "Ljava/lang/Object;", "Ljava/lang/reflect/Method;", "[Ljava/lang/Object;",
                        ) -> {
                        signatureHookReplaced = true
                        method.withImplementation(signatureHook(packageName))
                    }
                    else -> method
                }
            }
            ImmutableClassDef(
                clazz.type, clazz.accessFlags, clazz.superclass, clazz.interfaces,
                clazz.sourceFile, clazz.annotations, clazz.fields, methods,
            )
        }
        if (!initializerReplaced || !signatureHookReplaced) {
            throw PatchException("패치 번들의 Hayul DEX에서 PatcherAppComponentFactory 초기화 코드나 GetPackageInfo 서명 훅을 찾지 못했습니다.")
        }
        val store = MemoryDataStore()
        DexPool.writeTo(store, ImmutableDexFile(Opcodes.getDefault(), sanitized))
        return store.data
    }

    /**
     * PatcherAppComponentFactory의 static 초기화 코드를 바꿉니다.
     *
     * 원본은 PackageManager 훅, WebView 훅, 기기 정보 변조(Samsung SM-X906N)를 실행합니다.
     * 여기서는 PackageManager 훅만 실행합니다.
     */
    private fun factoryInitializer(): MethodImplementation = MethodImplementationBuilder(0).apply {
        addInstruction(BuilderInstruction35c(
            Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0,
            ImmutableMethodReference(PACKAGE_MANAGER_TYPE, "replaceService", emptyList(), "V"),
        ))
        addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))
    }.methodImplementation

    /**
     * GetPackageInfo.call을 다시 씁니다.
     *
     * 원본은 어느 앱을 조회하든 서명을 바꿉니다. 여기서는 조회 결과가 [packageName]이고 서명이 있을 때만
     * Utils.replaceSignature로 원본 서명을 넣습니다.
     */
    private fun signatureHook(packageName: String): MethodImplementation =
        MethodImplementationBuilder(7).apply {
            // v0~v2는 지역 변수, p0~p3는 v3~v6입니다.
            addInstruction(BuilderInstruction35c(
                Opcode.INVOKE_VIRTUAL, 3, 5, 4, 6, 0, 0,
                ImmutableMethodReference(
                    "Ljava/lang/reflect/Method;", "invoke",
                    listOf("Ljava/lang/Object;", "[Ljava/lang/Object;"), "Ljava/lang/Object;",
                ),
            ))
            addInstruction(BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0))
            addInstruction(BuilderInstruction21c(Opcode.CHECK_CAST, 0, ImmutableTypeReference(PACKAGE_INFO_TYPE)))
            addInstruction(BuilderInstruction21t(Opcode.IF_EQZ, 0, getLabel("return")))
            addInstruction(BuilderInstruction22c(
                Opcode.IGET_OBJECT, 1, 0,
                ImmutableFieldReference(PACKAGE_INFO_TYPE, "packageName", "Ljava/lang/String;"),
            ))
            addInstruction(BuilderInstruction21c(Opcode.CONST_STRING, 2, ImmutableStringReference(packageName)))
            addInstruction(BuilderInstruction35c(
                Opcode.INVOKE_VIRTUAL, 2, 2, 1, 0, 0, 0,
                ImmutableMethodReference("Ljava/lang/String;", "equals", listOf("Ljava/lang/Object;"), "Z"),
            ))
            addInstruction(BuilderInstruction11x(Opcode.MOVE_RESULT, 1))
            // WebView 제공 앱을 포함한 다른 패키지는 실제 서명을 그대로 돌려줍니다.
            addInstruction(BuilderInstruction21t(Opcode.IF_EQZ, 1, getLabel("return")))
            addInstruction(BuilderInstruction22c(
                Opcode.IGET_OBJECT, 1, 0,
                ImmutableFieldReference(PACKAGE_INFO_TYPE, "signatures", "[Landroid/content/pm/Signature;"),
            ))
            addInstruction(BuilderInstruction21t(Opcode.IF_EQZ, 1, getLabel("return")))
            addInstruction(BuilderInstruction12x(Opcode.ARRAY_LENGTH, 2, 1))
            addInstruction(BuilderInstruction21t(Opcode.IF_EQZ, 2, getLabel("return")))
            addInstruction(BuilderInstruction35c(
                Opcode.INVOKE_STATIC, 1, 1, 0, 0, 0, 0,
                ImmutableMethodReference(UTILS_TYPE, "replaceSignature", listOf("[Landroid/content/pm/Signature;"), "V"),
            ))
            addLabel("return")
            addInstruction(BuilderInstruction11x(Opcode.RETURN_OBJECT, 0))
        }.methodImplementation

    private fun Method.withImplementation(body: MethodImplementation): Method = ImmutableMethod(
        definingClass, name, parameters, returnType, accessFlags,
        annotations, hiddenApiRestrictions, body,
    )
}

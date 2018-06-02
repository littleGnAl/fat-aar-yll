package com.littlegnal.aarcollect

import com.google.common.base.Charsets
import com.google.common.base.Strings
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.io.Files
import com.littlegnal.aarcollect.structs.AndroidArchiveLibrary
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import java.io.File
import java.io.IOException
import javax.lang.model.element.Modifier
import javax.xml.parsers.DocumentBuilderFactory

object RSourceGenerator {

  @Throws(IOException::class)
  fun generate(
    outputDir: File,
    androidLibrary: AndroidArchiveLibrary
  ) {
    // check
    val symbolFile = androidLibrary.getSymbolFile()
    val manifestFile = androidLibrary.getManifest()
    if (!symbolFile.exists()) {
      return
    }
    if (!manifestFile.exists()) {
      throw RuntimeException("Can not find " + manifestFile)
    }
    // read R.txt
    val lines = Files.readLines(symbolFile, Charsets.UTF_8)
    val symbolItemsMap = Maps.newHashMap<String, List<TextSymbolItem>>()
    for (line in lines) {
      val strings = line.split(" ".toRegex(), 4)
          .toTypedArray()
      val symbolItem = TextSymbolItem()
      symbolItem.type = strings[0]
      symbolItem.clazz = strings[1]
      symbolItem.name = strings[2]
      symbolItem.value = strings[3]
      var symbolItems: MutableList<TextSymbolItem>? =
        symbolItemsMap[symbolItem.clazz] as MutableList<TextSymbolItem>?
      if (symbolItems == null) {
        symbolItems = Lists.newArrayList()
        symbolItemsMap.put(symbolItem.clazz, symbolItems)
      }
      symbolItems!!.add(symbolItem)
    }
    if (symbolItemsMap.isEmpty()) {
      // empty R.txt
      return
    }
    // parse package name
    var packageName: String? = null
    try {
      val dbf = DocumentBuilderFactory.newInstance()
      val doc = dbf.newDocumentBuilder()
          .parse(manifestFile)
      val element = doc.documentElement
      packageName = element.getAttribute("package")
    } catch (ignored: Exception) {
    }

    if (Strings.isNullOrEmpty(packageName)) {
      throw RuntimeException("Parse package from $manifestFile error!")
    }

    // write R.java
    val classBuilder = TypeSpec.classBuilder("R")
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addJavadoc("AUTO-GENERATED FILE.  DO NOT MODIFY.\n")
        .addJavadoc("\n")
        .addJavadoc(
            "This class was automatically generated by the\n"
                + "fat-aar-plugin \n"
                + "from the R.txt of the dependency it found.\n"
                + "It should not be modified by hand."
        )
    for (clazz in symbolItemsMap.keys) {
      val icb = TypeSpec.classBuilder(clazz)
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
      val tsis = symbolItemsMap[clazz]
      for (item in tsis!!.iterator()) {
        var typeName: TypeName? = null
        if ("int" == item.type) {
          typeName = TypeName.INT
        }
        if ("int[]" == item.type) {
          typeName = TypeName.get(IntArray::class.java)
        }
        if (typeName == null) {
          throw RuntimeException("Unknown class type in " + symbolFile)
        }
        val fieldSpec = FieldSpec.builder(typeName, item.name)
            .addModifiers(
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL
            )  // Is the "final" necessary?
            .initializer(item.value)
            .build()
        icb.addField(fieldSpec)
      }
      classBuilder.addType(icb.build())
    }
    val javaFile = JavaFile.builder(packageName!!, classBuilder.build())
        .build()
    javaFile.writeTo(outputDir)
  }

  private class TextSymbolItem {
    internal var type: String? = null
    internal var clazz: String? = null
    internal var name: String? = null
    internal var value: String? = null
  }
}
/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gson;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.common.MoreAsserts;
import com.google.gson.common.TestTypes.ArrayOfArrays;
import com.google.gson.common.TestTypes.ArrayOfObjects;
import com.google.gson.common.TestTypes.BagOfPrimitiveWrappers;
import com.google.gson.common.TestTypes.BagOfPrimitives;
import com.google.gson.common.TestTypes.ClassWithCustomTypeConverter;
import com.google.gson.common.TestTypes.ClassWithEnumFields;
import com.google.gson.common.TestTypes.ClassWithNoFields;
import com.google.gson.common.TestTypes.ClassWithPrivateNoArgsConstructor;
import com.google.gson.common.TestTypes.ClassWithSubInterfacesOfCollection;
import com.google.gson.common.TestTypes.ClassWithTransientFields;
import com.google.gson.common.TestTypes.ContainsReferenceToSelfType;
import com.google.gson.common.TestTypes.MyEnum;
import com.google.gson.common.TestTypes.MyEnumCreator;
import com.google.gson.common.TestTypes.MyParameterizedType;
import com.google.gson.common.TestTypes.Nested;
import com.google.gson.common.TestTypes.PrimitiveArray;
import com.google.gson.common.TestTypes.SubTypeOfNested;
import com.google.gson.reflect.TypeInfo;
import com.google.gson.reflect.TypeToken;

/**
 * Small test for Json Deserialization
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public class JsonDeserializerTest extends TestCase {

  private Gson gson = null;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    gson = new Gson();
  }

  public void testInvalidJson() throws Exception {
    try {
      gson.fromJson(BagOfPrimitives.class, "adfasdf1112,,,\":");
      fail("Bad JSON should throw a ParseException");
    } catch (ParseException expected) { }

    try {
      gson.fromJson(BagOfPrimitives.class, "{adfasdf1112,,,\":}");
      fail("Bad JSON should throw a ParseException");
    } catch (ParseException expected) { }
  }

  public void testBagOfPrimitives() {
    BagOfPrimitives src = new BagOfPrimitives(10, 20, false, "stringValue");
    String json = src.getExpectedJson();
    BagOfPrimitives target = gson.fromJson(BagOfPrimitives.class, json);
    assertEquals(json, target.getExpectedJson());
  }

  public void testStringValue() throws Exception {
    String value = "someRandomStringValue";
    String actual = gson.fromJson(String.class, "\"" + value + "\"");
    assertEquals(value, actual);
  }

  @SuppressWarnings("unchecked")
  public void testRawCollectionOfBagOfPrimitives() {
    try {
      BagOfPrimitives bag = new BagOfPrimitives(10, 20, false, "stringValue");
      String json = '[' + bag.getExpectedJson() + ',' + bag.getExpectedJson() + ']';
      Collection target = gson.fromJson(Collection.class, json);
      assertEquals(2, target.size());
      for (BagOfPrimitives bag1 : (Collection<BagOfPrimitives>) target) {
        assertEquals(bag.getExpectedJson(), bag1.getExpectedJson());
      }
      fail("Raw collection of objects should not work");
    } catch (ParseException expected) {      
    }
  }
  
  public void testReallyLongValues() {
    String json = "333961828784581";
    long value = gson.fromJson(Long.class, json);
    assertEquals(333961828784581L, value);
  }

  public void testStringValueAsSingleElementArray() throws Exception {
    String value = "someRandomStringValue";
    String actual = gson.fromJson(String.class, "[\"" + value + "\"]");
    assertEquals(value, actual);
  }

  public void testPrimitiveLongAutoboxed() {
    long expected = 1L;
    long actual = gson.fromJson(long.class, "1");
    assertEquals(expected, actual);

    actual = gson.fromJson(Long.class, "1");
    assertEquals(expected, actual);
  }

  public void testPrimitiveLongAutoboxedInASingleElementArray() {
    long expected = 1L;
    long actual = gson.fromJson(long.class, "[1]");
    assertEquals(expected, actual);

    actual = gson.fromJson(Long.class, "[1]");
    assertEquals(expected, actual);
  }

  public void testPrimitiveIntegerAutoboxed() {
    int expected = 1;
    int actual = gson.fromJson(int.class, "1");
    assertEquals(expected, actual);

    actual = gson.fromJson(Integer.class, "1");
    assertEquals(expected, actual);
  }

  public void testPrimitiveIntegerAutoboxedInASingleElementArray() {
    int expected = 1;
    int actual = gson.fromJson(int.class, "[1]");
    assertEquals(expected, actual);

    actual = gson.fromJson(Integer.class, "[1]");
    assertEquals(expected, actual);
  }

  public void testPrimitiveBooleanAutoboxed() {
    assertEquals(Boolean.FALSE, gson.fromJson(Boolean.class, "[false]"));
    assertEquals(Boolean.TRUE, gson.fromJson(Boolean.class, "[true]"));

    boolean value = gson.fromJson(boolean.class, "false");
    assertEquals(false, value);
    value = gson.fromJson(boolean.class, "true");
    assertEquals(true, value);
  }

  public void testPrimitiveBooleanAutoboxedInASingleElementArray() {
    assertEquals(Boolean.FALSE, gson.fromJson(Boolean.class, "[false]"));
    assertEquals(Boolean.TRUE, gson.fromJson(Boolean.class, "[true]"));

    boolean value = gson.fromJson(boolean.class, "[false]");
    assertEquals(false, value);
    value = gson.fromJson(boolean.class, "[true]");
    assertEquals(true, value);
  }

  public void testBagOfPrimitiveWrappers() {
    BagOfPrimitiveWrappers target = new BagOfPrimitiveWrappers(10L, 20, false);
    String jsonString = target.getExpectedJson();
    target = gson.fromJson(BagOfPrimitiveWrappers.class, jsonString);
    assertEquals(jsonString, target.getExpectedJson());
  }

  public void testDirectedAcyclicGraph() {
    String json = "{\"children\":[{\"children\":[{\"children\":[]}]},{\"children\":[]}]}";
    ContainsReferenceToSelfType target = gson.fromJson(ContainsReferenceToSelfType.class, json);
    assertNotNull(target);
    assertEquals(2, target.children.size());
  }

  public void testEmptyCollectionInAnObject() {
    String json = "{\"children\":[]}";
    ContainsReferenceToSelfType target = gson.fromJson(ContainsReferenceToSelfType.class, json);
    assertNotNull(target);
    assertTrue(target.children.isEmpty());
  }

  public void testPrimitiveArrayInAnObject() {
    String json = "{\"longArray\":[0,1,2,3,4,5,6,7,8,9]}";
    PrimitiveArray target = gson.fromJson(PrimitiveArray.class, json);
    assertEquals(json, target.getExpectedJson());
  }

  public void testPrimitiveArray() {
    String json = "[0,1,2,3,4,5,6,7,8,9]";
    int[] target = gson.fromJson(int[].class, json);
    int[] expected = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
    MoreAsserts.assertEquals(expected, target);
  }

  public void testCollectionOfInteger() {
    String json = "[0,1,2,3,4,5,6,7,8,9]";
    Type collectionType = new TypeToken<Collection<Integer>>() { }.getType();
    Collection<Integer> target = gson.fromJson(collectionType, json);
    int[] expected = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    MoreAsserts.assertEquals(expected, toIntArray(target));
  }

  @SuppressWarnings("unchecked")
  public void testRawCollectionOfInteger() {
    String json = "[0,1,2,3,4,5,6,7,8,9]";
	Collection<Integer> target = gson.fromJson(Collection.class, json);
	Collection<Integer> expected = ImmutableList.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
	MoreAsserts.assertEquals(toIntArray(expected), toIntArray(target));
  }

  public void testListOfIntegerCollections() throws Exception {
    String json = "[[1,2,3],[4,5,6],[7,8,9]]";
    Type collectionType = new TypeToken<Collection<Collection<Integer>>>() {}.getType();
    List<Collection<Integer>> target = gson.fromJson(collectionType, json);
    List<Collection<Integer>> expected = Lists.newArrayList();
    for (int i = 0; i < 3; i++) {
      int start = (3 * i) + 1;
      expected.add(ImmutableList.of(start, start + 1, start + 2));
    }

    for (int i = 0; i < 3; i++) {
      MoreAsserts.assertEquals(toIntArray(expected.get(i)), toIntArray(target.get(i)));
    }
  }
  
  @SuppressWarnings("unchecked")
  private static int[] toIntArray(Collection collection) {
    int[] ints = new int[collection.size()];
    int i = 0;
    for (Iterator iterator = collection.iterator(); iterator.hasNext(); ++i) {
      Object obj = iterator.next();
      if (obj instanceof Integer) {
        ints[i] = ((Integer)obj).intValue();
      } else if (obj instanceof Long) {
        ints[i] = ((Long)obj).intValue();
      }
    }
    return ints;
  }

  public void testClassWithTransientFields() throws Exception {
    String json = "{\"longValue\":[1]}";
    ClassWithTransientFields target = gson.fromJson(
        ClassWithTransientFields.class, json);
    assertEquals(json, target.getExpectedJson());
  }

  public void testTransientFieldsPassedInJsonAreIgnored() throws Exception {
    String json = "{\"transientLongValue\":1,\"longValue\":[1]}";
    ClassWithTransientFields target = gson.fromJson(
        ClassWithTransientFields.class, json);
    assertFalse(target.transientLongValue != 1);
  }

  public void testClassWithNoFields() {
    String json = "{}";
    ClassWithNoFields target = gson.fromJson(ClassWithNoFields.class, json);
    assertNotNull(target);
  }

  public void testTopLevelCollections() {
    Type type = new TypeToken<Collection<Integer>>() {
    }.getType();
    Collection<Integer> collection = gson.fromJson(type, "[1,2,3,4,5,6,7,8,9]");
    assertEquals(9, collection.size());
  }

  public void testTopLevelArray() {
    int[] expected = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
    int[] actual = gson.fromJson(int[].class, "[1,2,3,4,5,6,7,8,9]");
    MoreAsserts.assertEquals(expected, actual);
  }

  public void testEmptyArray() {
    int[] actualObject = gson.fromJson(int[].class, "[]");
    assertTrue(actualObject.length == 0);

    Integer[] actualObject2 = gson.fromJson(Integer[].class, "[]");
    assertTrue(actualObject2.length == 0);
  }

  public void testNested() {
    String json = "{\"primitive1\":{\"longValue\":10,\"intValue\":20,\"booleanValue\":false,"
        + "\"stringValue\":\"stringValue\"},\"primitive2\":{\"longValue\":30,\"intValue\":40,"
        + "\"booleanValue\":true,\"stringValue\":\"stringValue\"}}";
    Nested target = gson.fromJson(Nested.class, json);
    assertEquals(json, target.getExpectedJson());
  }

  public void testInheritence() {
    String json = "{\"value\":5,\"primitive1\":{\"longValue\":10,\"intValue\":20,"
        + "\"booleanValue\":false,\"stringValue\":\"stringValue\"},\"primitive2\":"
        + "{\"longValue\":30,\"intValue\":40,\"booleanValue\":true,"
        + "\"stringValue\":\"stringValue\"}}";
    SubTypeOfNested target = gson.fromJson(SubTypeOfNested.class, json);
    assertEquals(json, target.getExpectedJson());
  }

  public void testNull() {
    try {
      gson.fromJson(Object.class, "");
      fail("Null strings should not be allowed");
    } catch (ParseException expected) {
    }
  }

  public void testNullFields() {
    String json = "{\"primitive1\":{\"longValue\":10,\"intValue\":20,\"booleanValue\":false"
        + ",\"stringValue\":\"stringValue\"}}";
    Nested target = gson.fromJson(Nested.class, json);
    assertEquals(json, target.getExpectedJson());
  }

  public void testSubInterfacesOfCollection() {
    String json = "{\"list\":[0,1,2,3],\"queue\":[0,1,2,3],\"set\":[0.1,0.2,0.3,0.4],"
        + "\"sortedSet\":[\"a\",\"b\",\"c\",\"d\"]"
//        + ",\"navigableSet\":[\"abc\",\"def\",\"ghi\",\"jkl\"]"
        + "}";
    ClassWithSubInterfacesOfCollection target = gson.fromJson(
        ClassWithSubInterfacesOfCollection.class, json);
    assertEquals(json, target.getExpectedJson());
  }

  public void testCustomDeserializer() {
    gson.registerDeserializer(ClassWithCustomTypeConverter.class,
        new JsonDeserializer<ClassWithCustomTypeConverter>() {
      public ClassWithCustomTypeConverter fromJson(Type typeOfT, JsonElement json, 
          JsonDeserializer.Context context) {
        JsonObject jsonObject = json.getAsJsonObject();
        int value = jsonObject.get("bag").getAsInt();
        return new ClassWithCustomTypeConverter(new BagOfPrimitives(value, 
            value, false, ""), value);
      }
    });
    String json = "{\"bag\":5,\"value\":25}";
    ClassWithCustomTypeConverter target = gson.fromJson(ClassWithCustomTypeConverter.class, json);
    assertEquals(5, target.getBag().getIntValue());
  }

  public void testNestedCustomTypeConverters() {
    gson.registerDeserializer(BagOfPrimitives.class, new JsonDeserializer<BagOfPrimitives>() {
      public BagOfPrimitives fromJson(Type typeOfT, JsonElement json, 
          JsonDeserializer.Context context) throws ParseException {
        int value = json.getAsInt();
        return new BagOfPrimitives(value, value, false, "");
      }
    });
    String json = "{\"bag\":7,\"value\":25}";
    ClassWithCustomTypeConverter target = gson.fromJson(ClassWithCustomTypeConverter.class, json);
    assertEquals(7, target.getBag().getIntValue());
  }

  public void testArrayOfObjects() {
    String json = new ArrayOfObjects().getExpectedJson();
    ArrayOfObjects target = gson.fromJson(ArrayOfObjects.class, json);
    assertEquals(json, target.getExpectedJson());
  }

  public void testArrayOfArrays() {
    String json = new ArrayOfArrays().getExpectedJson();
    ArrayOfArrays target = gson.fromJson(ArrayOfArrays.class, json);
    assertEquals(json, target.getExpectedJson());
  }

  private static class MyParameterizedDeserializer<T>
      implements JsonDeserializer<MyParameterizedType<T>> {
    @SuppressWarnings("unchecked")
    public MyParameterizedType<T> fromJson(Type typeOfT, JsonElement json, 
        JsonDeserializer.Context context) throws ParseException {
      Type genericClass = new TypeInfo<Object>(typeOfT).getGenericClass();
      String className = new TypeInfo<Object>(genericClass).getTopLevelClass().getSimpleName();
      T value = (T) json.getAsJsonObject().get(className).getAsObject();
      return new MyParameterizedType<T>(value);
    }
  }

  private static class MyParameterizedTypeInstanceCreator<T>
      implements InstanceCreator<MyParameterizedType<T>> {
    private final T defaultValue;
    MyParameterizedTypeInstanceCreator(T defaultValue) {
      this.defaultValue = defaultValue;
    }

    public MyParameterizedType<T> createInstance(Type type) {
      return new MyParameterizedType<T>(defaultValue);
    }
  }

  public void testParameterizedTypesWithCustomDeserializer() {
    Type ptIntegerType = new TypeToken<MyParameterizedType<Long>>() {}.getType();
    Type ptStringType = new TypeToken<MyParameterizedType<String>>() {}.getType();
    gson.registerDeserializer(ptIntegerType, new MyParameterizedDeserializer<Long>());
    gson.registerDeserializer(ptStringType, new MyParameterizedDeserializer<String>());
    gson.registerInstanceCreator(ptIntegerType,
        new MyParameterizedTypeInstanceCreator<Long>(new Long(0)));
    gson.registerInstanceCreator(ptStringType,
        new MyParameterizedTypeInstanceCreator<String>(""));

    String json = new MyParameterizedType<Long>(new Long(10)).getExpectedJson();
    MyParameterizedType<Long> intTarget = gson.fromJson(ptIntegerType, json);
    assertEquals(json, intTarget.getExpectedJson());

    json = new MyParameterizedType<String>("abc").getExpectedJson();
    MyParameterizedType<String> stringTarget = gson.fromJson(ptStringType, json);
    assertEquals(json, stringTarget.getExpectedJson());
  }

  public void testTopLevelEnum() {
    gson.registerInstanceCreator(MyEnum.class, new MyEnumCreator());
    String json = MyEnum.VALUE1.getExpectedJson();
    MyEnum target = gson.fromJson(MyEnum.class, json);
    assertEquals(json, target.getExpectedJson());
  }

  public void testTopLevelEnumInASingleElementArray() {
    gson.registerInstanceCreator(MyEnum.class, new MyEnumCreator());
    String json = "[" + MyEnum.VALUE1.getExpectedJson() + "]";
    MyEnum target = gson.fromJson(MyEnum.class, json);
    assertEquals(json, "[" + target.getExpectedJson() + "]");
  }

  public void testClassWithEnumField() {
    gson.registerInstanceCreator(MyEnum.class, new MyEnumCreator());
    String json = new ClassWithEnumFields().getExpectedJson();
    ClassWithEnumFields target = gson.fromJson(ClassWithEnumFields.class, json);
    assertEquals(json, target.getExpectedJson());
  }

  public void testCollectionOfEnums() {
    gson.registerInstanceCreator(MyEnum.class, new MyEnumCreator());
    Type type = new TypeToken<Collection<MyEnum>>() {
    }.getType();
    String json = "[\"VALUE1\",\"VALUE2\"]";
    Collection<MyEnum> target = gson.fromJson(type, json);
    MoreAsserts.assertContains(target, MyEnum.VALUE1);
    MoreAsserts.assertContains(target, MyEnum.VALUE2);
  }

  public void testPrivateNoArgConstructor() {
    ClassWithPrivateNoArgsConstructor target =
      gson.fromJson(ClassWithPrivateNoArgsConstructor.class, "{\"a\":20}");
    assertEquals(20, target.a);
  }
  
  public void testDefaultSupportForUrl() throws Exception {
    String urlValue = "http://google.com/";
    String json = '"' + urlValue + '"';
    URL target = gson.fromJson(URL.class, json);
    assertEquals(urlValue, target.toExternalForm());
  }
  
  public void testDefaultSupportForUri() throws Exception {
    String uriValue = "http://google.com/";
    String json = '"' + uriValue + '"';
    URI target = gson.fromJson(URI.class, json);
    assertEquals(uriValue, target.toASCIIString());
  }
  
  public void testMap() throws Exception {
    String json = "{\"a\":1,\"b\":2}";
    Type typeOfMap = new TypeToken<Map<String,Integer>>(){}.getType();
    Map<String, Integer> target = gson.fromJson(typeOfMap, json);
    assertEquals(1, target.get("a").intValue());
    assertEquals(2, target.get("b").intValue());
  }
}